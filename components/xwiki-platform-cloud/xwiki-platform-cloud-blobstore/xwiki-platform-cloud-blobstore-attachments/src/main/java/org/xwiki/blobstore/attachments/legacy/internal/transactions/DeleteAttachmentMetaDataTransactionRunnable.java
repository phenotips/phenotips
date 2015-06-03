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

import java.util.List;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.store.TransactionRunnable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentContent;

/**
 * The transaction runnable for deleting attachment metadata.
 * 
 * @version $Id$
 */
public class DeleteAttachmentMetaDataTransactionRunnable extends TransactionRunnable<XWikiHibernateTransaction>
{
    /**
     * The logger.
     */
    private Logger logger = LoggerFactory.getLogger(DeleteAttachmentMetaDataTransactionRunnable.class);

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
     * @param xwikiAttachment The XWiki attachment.
     * @param xwikiContext The XWiki context.
     */
    public DeleteAttachmentMetaDataTransactionRunnable(XWikiAttachment xwikiAttachment, XWikiContext xwikiContext)
    {
        this.xwikiAttachment = xwikiAttachment;
        this.xwikiContext = xwikiContext;
    }

    @Override
    protected void onRun() throws Exception
    {
        logger.debug("Deleting attachment metadata");

        final Session session = xwikiContext.getWiki().getHibernateStore().getSession(xwikiContext);
        session.delete(new XWikiAttachmentContent(xwikiAttachment));

        if (xwikiAttachment.getDoc().removeAttachment(xwikiAttachment) == null)
        	logger.error("Could not remove attachment for name {" + xwikiAttachment.getFilename() + "}");
        	
        xwikiContext.getWiki().getStore().saveXWikiDoc(xwikiAttachment.getDoc(), xwikiContext, false);

        session.delete(xwikiAttachment);
    }
    
    @Override
    protected void onRollback() throws Exception {
    	logger.warn("Rollback occurred while deleting an attachment, the file store may be inconsistent with the database");
    }
    
    @Override
    protected void onCommit() throws Exception {
    	logger.info("Commit occurred while deleting an attachment");
    }

}
