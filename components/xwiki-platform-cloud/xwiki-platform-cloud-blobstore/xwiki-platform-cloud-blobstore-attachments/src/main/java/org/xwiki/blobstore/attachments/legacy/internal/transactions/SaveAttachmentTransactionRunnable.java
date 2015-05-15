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

import org.xwiki.blobstore.BlobStore;
import org.xwiki.blobstore.attachments.legacy.internal.Utils;
import org.xwiki.store.TransactionRunnable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * The transaction runnable for saving an attachment.
 * 
 * @version $Id$
 */
public class SaveAttachmentTransactionRunnable extends TransactionRunnable<XWikiHibernateTransaction>
{
    /**
     * The blob store provider.
     */
    private Provider<BlobStore> blobStoreProvider;

    /**
     * The XWiki attachment.
     */
    private XWikiAttachment xwikiAttachment;

    /**
     * The XWiki context.
     */
    private XWikiContext xwikiContext;

    /**
     * Constructor.
     * 
     * @param blobStoreProvider The blob store provider.
     * @param xwikiAttachment The XWiki attachment.
     * @param xwikiContext The XWiki context.
     * @param updateDocument true if the document containing the attachment should be saved.
     */
    public SaveAttachmentTransactionRunnable(Provider<BlobStore> blobStoreProvider, XWikiAttachment xwikiAttachment,
        XWikiContext xwikiContext, boolean updateDocument)
    {
        this.blobStoreProvider = blobStoreProvider;
        this.xwikiAttachment = xwikiAttachment;
        this.xwikiContext = xwikiContext;

        if (updateDocument) {
            new UpdateDocumentTransactionRunnable(xwikiAttachment.getDoc(), xwikiContext).runIn(this);
        }
    }

    @Override
    protected void onRun() throws Exception
    {
        BlobStore blobStore = blobStoreProvider.get();

        String path = Utils.generatePath(xwikiAttachment.getReference());

        blobStore.putBlob(path, xwikiAttachment.getContentInputStream(xwikiContext), xwikiAttachment.getFilesize());
    }

}
