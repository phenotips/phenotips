Introduction
============

This module contains a generic `BlobStore` API that is used by other modules to interact with a blobstore.

There are several parameters that must be defined in `WEB-INF/xwiki.properties` file in order to configure the blobstore:

* `xwiki.store.attachments.blobstore=BLOBSTORE_HINT` the hint for selecting the blobstore implementation.
* `xwiki.store.attachments.blobstore.bucket=BUCKET_NAME` where `BUCKET_NAME` the bucket where you want to store your attachments.
* `xwiki.store.attachments.blobstore.namespace=NAMESPACE` where `NAMESPACE` is a string that will be used as the first path component for storing all the attachments of this wiki. This is used to host multiple wikis or farms in the same bucket. 
* `xwiki.store.attachments.blobstore.identity=IDENTITY_TOKEN`. The string that identifies the user accessing to the blobstore (depending on the blobstore used)
* `xwiki.store.attachments.blobstore.credential=PASSWORD_TOKEN`. The password for accessing the blobstore (dependent on the blobstore used)
