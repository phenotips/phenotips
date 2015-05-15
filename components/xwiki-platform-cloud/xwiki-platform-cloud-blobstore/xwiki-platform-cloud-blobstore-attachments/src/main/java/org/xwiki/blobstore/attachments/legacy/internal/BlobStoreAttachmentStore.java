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
package org.xwiki.blobstore.attachments.legacy.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.blobstore.BlobStore;
import org.xwiki.blobstore.attachments.legacy.internal.transactions.DeleteAttachmentTransactionRunnable;
import org.xwiki.blobstore.attachments.legacy.internal.transactions.SaveAttachmentTransactionRunnable;
import org.xwiki.blobstore.attachments.legacy.internal.transactions.XWikiHibernateTransaction;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiAttachmentStoreInterface;

/**
 * The cloud attachment store.
 * 
 * @version $Id$
 */
@Component
@Named("blobstore")
@Singleton
public class BlobStoreAttachmentStore implements XWikiAttachmentStoreInterface
{
    /**
     * The blob store provider.
     */
    @Inject
    @Named("blobstore")
    private Provider<BlobStore> blobStoreProvider;

    /**
     * Logger.
     */
    @Inject
    private Logger logger;

    @Override
    public void cleanUp(XWikiContext xwikiContext)
    {
        logger.debug("cleanUp()");
    }

    @Override
    public void deleteXWikiAttachment(XWikiAttachment xwikiAttachment, boolean updateDocument,
        XWikiContext xwikiContext, boolean bTransaction) throws XWikiException
    {
        logger.debug("deleteXWikiAttachment()");

        XWikiHibernateTransaction transaction = new XWikiHibernateTransaction(xwikiContext);

        DeleteAttachmentTransactionRunnable transactionRunnable =
            new DeleteAttachmentTransactionRunnable(blobStoreProvider, xwikiAttachment, xwikiContext, updateDocument);
        transactionRunnable.runIn(transaction);

        try {
            transaction.start();
        } catch (Exception e) {
            if (e instanceof XWikiException) {
                throw (XWikiException) e;
            }

            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_DELETING_ATTACHMENT, "Exception while deleting attachments",
                e);
        }

    }

    @Override
    public void deleteXWikiAttachment(XWikiAttachment xwikiAttachment, XWikiContext xwikiContext, boolean bTransaction)
        throws XWikiException
    {
        this.deleteXWikiAttachment(xwikiAttachment, true, xwikiContext, bTransaction);
    }

    @Override
    public void loadAttachmentContent(XWikiAttachment xwikiAttachment, XWikiContext xwikiContext, boolean bTransaction)
        throws XWikiException
    {
        logger.debug("loadAttachmentContent()");

        BlobStore blobStore = blobStoreProvider.get();

        String path = Utils.generatePath(xwikiAttachment.getReference());

        InputStream blobData = blobStore.getBlob(path);

        try {
            xwikiAttachment.setContent(blobData);
        } catch (IOException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_FILENOTFOUND,
                String.format("Blob %s not found", xwikiAttachment.getReference().getName()), e);
        }

    }

    @Override
    public void saveAttachmentContent(XWikiAttachment xwikiAttachment, boolean updateDocument,
        XWikiContext xwikiContext, boolean bTransaction) throws XWikiException
    {
        logger.debug("saveAttachmentContent()");

        XWikiHibernateTransaction transaction = new XWikiHibernateTransaction(xwikiContext);

        SaveAttachmentTransactionRunnable transactionRunnable =
            new SaveAttachmentTransactionRunnable(blobStoreProvider, xwikiAttachment, xwikiContext, updateDocument);
        transactionRunnable.runIn(transaction);

        try {
            transaction.start();
        } catch (Exception e) {
            if (e instanceof XWikiException) {
                throw (XWikiException) e;
            }

            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_ATTACHMENT, "Exception while saving attachments", e);
        }

    }

    @Override
    public void saveAttachmentContent(XWikiAttachment xwikiAttachment, XWikiContext xwikiContext, boolean bTransaction)
        throws XWikiException
    {
        this.saveAttachmentContent(xwikiAttachment, true, xwikiContext, bTransaction);
    }

    @Override
    public void saveAttachmentsContent(List<XWikiAttachment> attachmentList, XWikiDocument xwikiDocument,
        boolean updateDocument, XWikiContext xwikiContext, boolean bTransaction) throws XWikiException
    {
        logger.debug("saveAttachmentsContent()");
    }

}
