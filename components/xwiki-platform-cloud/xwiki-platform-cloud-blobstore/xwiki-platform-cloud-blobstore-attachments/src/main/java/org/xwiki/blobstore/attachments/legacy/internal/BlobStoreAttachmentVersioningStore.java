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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;
import com.xpn.xwiki.store.AttachmentVersioningStore;

/**
 * The cloud attachment versioning store.
 * 
 * @version $Id$
 */
@Component
@Named("blobstore")
@Singleton
public class BlobStoreAttachmentVersioningStore implements AttachmentVersioningStore
{
    /**
     * Logger.
     */
    @Inject
    private Logger logger;

    @Override
    public void deleteArchive(XWikiAttachment xwikiAttachment, XWikiContext xwikiContext, boolean transaction)
        throws XWikiException
    {
        logger.debug("deleteArchive()");
    }

    @Override
    public XWikiAttachmentArchive loadArchive(XWikiAttachment xwikiAttachment, XWikiContext xwikiContext,
        boolean transaction) throws XWikiException
    {
        logger.debug("loadArchive()");

        return null;
    }

    @Override
    public void saveArchive(XWikiAttachmentArchive xwikiAttachment, XWikiContext xwikiContext, boolean transaction)
        throws XWikiException
    {
        logger.debug("saveArchive()");
    }

}
