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
package org.xwiki.blobstore.attachments.legacy.internal.transactions;

import javax.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.blobstore.BlobStore;
import org.xwiki.store.TransactionRunnable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * The transaction runnable for deleting an attachment. This is basically a transaction that contains the one for
 * removing the metadata and, then, the actual blob on the blobstore.
 * 
 * @version $Id$
 */
public class DeleteAttachmentTransactionRunnable extends TransactionRunnable<XWikiHibernateTransaction>
{
	
    /**
     * The logger.
     */
    private Logger logger = LoggerFactory.getLogger(DeleteAttachmentTransactionRunnable.class);
	
    /**
     * Constructor.
     * 
     * @param blobStoreProvider The blob store provider.
     * @param xwikiAttachment The XWiki attachment.
     * @param xwikiContext The XWiki context.
     * @param updateDocument true if the document containing the attachment should be saved.
     */
    public DeleteAttachmentTransactionRunnable(Provider<BlobStore> blobStoreProvider, XWikiAttachment xwikiAttachment,
        XWikiContext xwikiContext, boolean updateDocument)
    {
        /*
         * This is simply a composite transaction that contains first the removal of the attachment metadata and then
         * the removal of the actual data in the blobstore.
         */
    	new DeleteAttachmentMetaDataTransactionRunnable(xwikiAttachment, xwikiContext).runIn(this);
    	new DeleteAttachmentDataTransactionRunnable(xwikiAttachment.getReference(), blobStoreProvider).runIn(this);
    }

}
