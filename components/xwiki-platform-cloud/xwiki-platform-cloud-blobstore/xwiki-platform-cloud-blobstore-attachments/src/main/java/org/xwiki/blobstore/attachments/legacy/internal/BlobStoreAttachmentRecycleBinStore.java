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

import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DeletedAttachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.AttachmentRecycleBinStore;

/**
 * The recycle store for cloud attachments.
 * 
 * @version $Id$
 */
@Component
@Named("blobstore")
@Singleton
public class BlobStoreAttachmentRecycleBinStore implements AttachmentRecycleBinStore
{
    /**
     * Logger.
     */
    @Inject
    private Logger logger;

    @Override
    public void deleteFromRecycleBin(long index, XWikiContext xwikiContext, boolean transaction) throws XWikiException
    {
        logger.debug("deleteFromRecycleBin()");
    }

    @Override
    public List<DeletedAttachment> getAllDeletedAttachments(XWikiAttachment xwikiAttachment, XWikiContext xwikiContext,
        boolean transaction) throws XWikiException
    {
        logger.debug("getAllDeletedAttachments(...)");

        return Collections.EMPTY_LIST;
    }

    @Override
    public List<DeletedAttachment> getAllDeletedAttachments(XWikiDocument xwikiDocument, XWikiContext xwikiContext,
        boolean transaction) throws XWikiException
    {
        logger.debug("getAllDeletedAttachments()");

        return Collections.EMPTY_LIST;
    }

    @Override
    public DeletedAttachment getDeletedAttachment(long index, XWikiContext xwikiContext, boolean transaction)
        throws XWikiException
    {
        logger.debug("getDeletedAttachment()");

        return null;
    }

    @Override
    public XWikiAttachment restoreFromRecycleBin(XWikiAttachment xwikiAttachment, long index,
        XWikiContext xwikiContext, boolean transaction) throws XWikiException
    {
        logger.debug("restoreFromRecycleBin()");

        return null;
    }

    @Override
    public void saveToRecycleBin(XWikiAttachment arg0, String arg1, Date arg2, XWikiContext arg3, boolean arg4)
        throws XWikiException
    {
        logger.debug("saveToRecycleBin()");
    }

}
