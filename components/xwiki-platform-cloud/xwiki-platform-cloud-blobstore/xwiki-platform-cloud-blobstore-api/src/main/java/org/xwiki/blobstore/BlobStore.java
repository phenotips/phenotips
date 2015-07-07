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
package org.xwiki.blobstore;

import java.io.InputStream;

import org.xwiki.component.annotation.ComponentRole;

/**
 * An interface containing relevant operations we want to perform on S3.
 * 
 * @version $Id$
 */
@ComponentRole
public interface BlobStore
{
    /**
     * The bucket to be used for storing XWiki attachment data.
     */
    String BLOBSTORE_BUCKET_PROPERTY = "xwiki.store.attachments.blobstore.bucket";

    /**
     * The credential to be used for the authentication.
     */
    String BLOBSTORE_CREDENTIAL_PROPERTY = "xwiki.store.attachments.blobstore.credential";

    /**
     * The identity of the user XWiki will use to authenticate. Blob store dependent.
     */
    String BLOBSTORE_IDENTITY_PROPERTY = "xwiki.store.attachments.blobstore.identity";

    /**
     * The blob store to use.
     */
    String BLOBSTORE_PROPERTY = "xwiki.store.attachments.blobstore";

    /**
     * The namespace to use for storing data in the blobstore.
     */
    String BLOBSTORE_NAMESPACE_PROPERTY = "xwiki.store.attachments.blobstore.namespace";

    /**
     * Store a blob.
     * 
     * @param key The key to be used for storing the blog.
     * @param content The input stream from which reading the data to be stored. This method could buffer the content
     *            entirely in memory before storing it. If you know the data length you might be willing to use
     *            {@link #putBlob(String, InputStream, long)}
     */
    void putBlob(String key, InputStream content);

    /**
     * Store a blob.
     * 
     * @param path The path to be used for storing the blog.
     * @param content The inputstream from which reading the data to be stored.
     * @param length The content length
     */
    void putBlob(String path, InputStream content, long length);

    /**
     * Return a stream for reading the blob content.
     * 
     * @param path The path for retrieving the blob.
     * @return An input stream for reading blob data.
     */
    InputStream getBlob(String path);

    /**
     * Remove a blob from the blob store.
     * 
     * @param path The path to the blob to be removed.
     */
    void deleteBlob(String path);
}
