/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.blobstore.s3.internal;

import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.blobstore.BlobStore;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

/**
 * Amazon S3 blob store implementation. This is a singleton in order to reuse as much as possible the Amazon S3 client
 * object (https://forums.aws.amazon.com/thread.jspa?threadID=50723)
 * 
 * @version $Id: 32244cef4dae1e1885048c25b2d06b4cec683771$
 */
@Component
@Named("s3")
@Singleton
public class S3BlobStore implements BlobStore, Initializable
{
    /**
     * The bucket to be used for storing data.
     */
    private String bucket;

    /**
     * The S3 client. No particular mechanisms are used in the code to deal with multiple thread interactions because
     * the Amazon S3 client is thread safe: https://forums.aws.amazon.com/thread.jspa?threadID=50723
     */
    private AmazonS3Client client;

    /**
     * Configuration.
     */
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configurationSource;

    /**
     * Logger.
     */
    @Inject
    private Logger logger;

    /**
     * The namespace where to store data.
     */
    private Object namespace;

    @Override
    public void initialize() throws InitializationException
    {
        final String formatString = "%s property is not defined.";

        logger.error("Using {}", configurationSource.getClass().getName());

        bucket = configurationSource.getProperty(BlobStore.BLOBSTORE_BUCKET_PROPERTY);
        if (bucket == null) {
            throw new InitializationException(String.format(formatString, BlobStore.BLOBSTORE_BUCKET_PROPERTY));
        }

        String accessKey = configurationSource.getProperty(BlobStore.BLOBSTORE_IDENTITY_PROPERTY);
        if (accessKey == null) {
            throw new InitializationException(String.format(formatString, BlobStore.BLOBSTORE_IDENTITY_PROPERTY));
        }

        String secretKey = configurationSource.getProperty(BlobStore.BLOBSTORE_CREDENTIAL_PROPERTY);
        if (secretKey == null) {
            throw new InitializationException(String.format(formatString, BlobStore.BLOBSTORE_CREDENTIAL_PROPERTY));
        }

        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        client = new AmazonS3Client(credentials);
        boolean bucketExists = client.doesBucketExist(bucket);
        if (!bucketExists) {
            client.createBucket(bucket);
        }

        namespace = configurationSource.getProperty(BlobStore.BLOBSTORE_NAMESPACE_PROPERTY);

        logger.debug("S3 blob store initialized using namespace '{}' and bucket '{}'", namespace != null ? namespace
            : "no namespace specified", bucket);
    }

    @Override
    public void deleteBlob(String path)
    {
        String normalizedPath = normalizePath(path);

        logger.debug("Deleting blob '{}' from bucket '{}'", normalizedPath, bucket);

        client.deleteObject(bucket, normalizedPath);
    }

    @Override
    public InputStream getBlob(String path)
    {
        String normalizedPath = normalizePath(path);

        logger.debug("Getting blob '{}' from bucket '{}'", normalizedPath, bucket);

        S3Object object = client.getObject(bucket, normalizedPath);
        if (object != null) {
            return object.getObjectContent();
        }

        return null;
    }

    @Override
    public void putBlob(String path, InputStream content)
    {
        putBlob(normalizePath(path), content, 0);
    }

    @Override
    public void putBlob(String path, InputStream content, long length)
    {
        String normalizedPath = normalizePath(path);

        logger.debug("Putting blob to '{}'", normalizedPath);

        ObjectMetadata objectMetadata = new ObjectMetadata();
        if (length > 0) {
            objectMetadata.setContentLength(length);
        }

        client.putObject(bucket, normalizedPath, content, objectMetadata);
    }

    /**
     * Return the actual path for retrieving the blob by taking into account the namespace.
     * 
     * @param path The path provided by the user.
     * @return The actual path that takes into account the namespace, if provided in the configuration.
     */
    private String normalizePath(String path)
    {
        if (namespace != null) {
            return String.format("%s/%s", namespace, path);
        }

        return path;
    }
}
