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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.store.TransactionRunnable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * A transaction runnable for saving a document.
 * 
 * @version $Id$
 */
public class UpdateDocumentTransactionRunnable extends TransactionRunnable<XWikiHibernateTransaction>
{
    /**
     * The logger.
     */
    private Logger logger = LoggerFactory.getLogger(UpdateDocumentTransactionRunnable.class);

    /**
     * The document.
     */
    private XWikiDocument document;

    /**
     * The XWiki context.
     */
    private XWikiContext xwikiContext;

    /**
     * Constructor.
     * 
     * @param document The document to be updated.
     * @param xwikiContext The XWiki context.
     */
    public UpdateDocumentTransactionRunnable(XWikiDocument document, XWikiContext xwikiContext)
    {
        this.document = document;
        this.xwikiContext = xwikiContext;
    }

    @Override
    protected void onRun() throws Exception
    {
        logger.debug("Updating document '{}'", document.getFullName());

        xwikiContext.getWiki().getStore().saveXWikiDoc(document, xwikiContext);
    }

}
