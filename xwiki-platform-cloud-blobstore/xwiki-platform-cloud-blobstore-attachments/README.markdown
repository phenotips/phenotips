Introduction
============

This module allows XWiki to store attachments in a blobstore.

In order to use a blobstore as the attachment storage you need to edit `WEB-INF/xwiki.cfg` and set the following properties:

* `xwiki.store.attachment.hint=blobstore`
* `xwiki.store.attachment.versioning.hint=void`
* `xwiki.store.attachment.recyclebin.hint=blobstore`

Current limitations
-------------------

There is no support for versioning and recycle bin.

