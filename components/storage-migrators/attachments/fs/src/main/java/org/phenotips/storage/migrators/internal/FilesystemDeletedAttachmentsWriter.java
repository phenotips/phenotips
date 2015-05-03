/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.phenotips.storage.migrators.internal;

import org.phenotips.storage.migrators.DataWriter;
import org.phenotips.storage.migrators.Type;

import org.xwiki.component.annotation.Component;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DeletedAttachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.store.AttachmentRecycleBinStore;

/**
 * {@link DataWriter} that can write {@link DeletedAttachment deleted attachments} into the filesystem storage engine.
 *
 * @version $Id$
 * @since 1.0RC1
 */
@Component
@Named("deleted attachments/file")
@Singleton
public class FilesystemDeletedAttachmentsWriter implements DataWriter<DeletedAttachment>
{
    private static final Type TYPE = new Type("deleted attachments", "file");

    @Inject
    private Logger logger;

    @Inject
    @Named("file")
    private AttachmentRecycleBinStore store;

    @Inject
    private Provider<XWikiContext> context;

    @Override
    public Type getType()
    {
        return TYPE;
    }

    @Override
    public boolean storeEntity(DeletedAttachment entity)
    {
        if (entity == null) {
            return true;
        }
        try {
            XWikiAttachment attachment = entity.restoreAttachment(null, this.context.get());
            this.store.saveToRecycleBin(attachment, entity.getDeleter(), entity.getDate(), this.context.get(), false);
            this.logger.debug("Imported deleted attachment [{}@{}#{}] into the filesystem trash store",
                entity.getDocName(), entity.getFilename(), entity.getId());
            return true;
        } catch (XWikiException ex) {
            this.logger.error("Failed to store deleted attachment into the filesystem store: {}", ex.getMessage(), ex);
            return false;
        }
    }
}
