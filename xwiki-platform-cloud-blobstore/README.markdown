Introduction
============

This extension allows XWiki to store attachments on a blob store.

Installation
------------

* Do a `mvn install` and the copy the JARs generated in the three modules in your `WEB-INF/lib` (they are in the `target` directory of the respective modules)
* Edit the `WEB-INF/xwiki.cfg` and set the following properties:
	* `xwiki.store.attachment.hint=blobstore`
	* `xwiki.store.attachment.versioning.hint=void`
	* `xwiki.store.attachment.recyclebin.hint=blobstore`
* Edit the `WEB-INF/xwiki.properties` and set the following properties:
	* `xwiki.store.attachments.blobstore=s3`
	* `xwiki.store.attachments.blobstore.bucket=BUCKET_NAME` where `BUCKET_NAME` is the Amazon S3 bucket where you want to store your attachments.
	* `xwiki.store.attachments.blobstore.namespace=NAMESPACE` where `NAMESPACE` is a string that will be used as the first path component for storing all the attachments of this wiki. This is used to host multiple wikis or farms in the same bucket. 
	* `xwiki.store.attachments.blobstore.identity=ACCESS_KEY`. You can get your access key on the security credentials panel on http://aws.amazon.com
	* `xwiki.store.attachments.blobstore.credential=SECRET_ACCESS_KEY`. You can get your secret access key on the security credentials panel on http://aws.amazon.com

Current limitations
-------------------

There is no support for versioning and recycle bin.
