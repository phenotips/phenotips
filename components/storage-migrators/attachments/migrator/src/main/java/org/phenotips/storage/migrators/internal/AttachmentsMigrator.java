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
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.storage.migrators.internal;

import org.phenotips.storage.migrators.DataTypeMigrator;

import org.xwiki.component.annotation.Component;

import javax.inject.Named;
import javax.inject.Singleton;

import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * {@link DataTypeMigrator} for migrating attachments. The current implementation of the XWiki storage engine forces the
 * attachment metadata to be stored in the database, so this migrates the attachment content and its history.
 *
 * @version $Id$
 * @since 1.0RC1
 */
@Component(roles = { DataTypeMigrator.class })
@Named("attachments")
@Singleton
public class AttachmentsMigrator extends AbstractDataTypeMigrator<XWikiAttachment>
{
    @Override
    protected String getStoreConfigurationKey()
    {
        return "xwiki.store.attachment.hint";
    }

    @Override
    public String getDataType()
    {
        return "attachments";
    }
}
