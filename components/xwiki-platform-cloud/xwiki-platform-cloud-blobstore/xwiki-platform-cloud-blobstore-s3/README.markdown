Introduction
============

This module contains a blobstore implementation for Amazon S3.

There are several parameters that must be defined in `WEB-INF/xwiki.properties` file in order to configure the Amazon S3 blobstore:

* `xwiki.store.attachments.blobstore=s3` to select the Amazon S3 blobstore.
* `xwiki.store.attachments.blobstore.bucket=BUCKET_NAME` where `BUCKET_NAME` the bucket where you want to store your attachments.
* `xwiki.store.attachments.blobstore.namespace=NAMESPACE` where `NAMESPACE` is a string that will be used as the first path component for storing all the attachments of this wiki. This is used to host multiple wikis or farms in the same bucket. 
* `xwiki.store.attachments.blobstore.identity=IDENTITY_TOKEN`. Your Amazon Access Key ID for your account. You can retrieve it on https://aws-portal.amazon.com/gp/aws/securityCredentials
* `xwiki.store.attachments.blobstore.credential=PASSWORD_TOKEN`. Your Amazon Secret Access Key for your account. You can retrieve it on https://aws-portal.amazon.com/gp/aws/securityCredentials
