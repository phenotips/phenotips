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
import org.xwiki.blobstore.attachments.legacy.internal.Utils;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.store.TransactionRunnable;

/**
 * The transaction runnable for deleting all attachment data on the blobstore.
 * 
 * @version $Id: f254cbd0da36b114ffd3a4d4c9f8b1bef9cc4c41 $
 */
public class DeleteAttachmentDataTransactionRunnable extends TransactionRunnable<XWikiHibernateTransaction>
{
    /**
     * The logger.
     */
    private Logger logger = LoggerFactory.getLogger(DeleteAttachmentDataTransactionRunnable.class);

    /**
     * The blob store provider.
     */
    private Provider<BlobStore> blobStoreProvider;

    /**
     * The entity reference for the attachment data.
     */
    private EntityReference attachmentReference;

    /**
     * Constructor.
     * 
     * @param attachmentReference The reference for the attachment.
     * @param blobStoreProvider The blob store provider.
     */
    public DeleteAttachmentDataTransactionRunnable(EntityReference attachmentReference,
        Provider<BlobStore> blobStoreProvider)
    {
        this.attachmentReference = attachmentReference;
        this.blobStoreProvider = blobStoreProvider;
    }

    @Override
    protected void onRun() throws Exception
    {
        String path = Utils.generatePath(attachmentReference);

        logger.debug("Deleting blob {}", path);

        BlobStore blobStore = blobStoreProvider.get();

        blobStore.deleteBlob(path);       
    }

}
