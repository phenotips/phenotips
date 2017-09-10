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
package org.phenotips.data.permissions.internal;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

@Component
@Singleton
public class DefaultPermissionsHelper implements PermissionsHelper
{
    private static final EntityReference USER_CLASS = new EntityReference("XWikiUsers", EntityType.DOCUMENT,
        new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE));

    private static final EntityReference GROUP_CLASS = new EntityReference("XWikiGroups", EntityType.DOCUMENT,
        new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE));

    @Inject
    private Logger logger;

    /** Provides access to the current execution context. */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private DocumentAccessBridge bridge;

    @Override
    @Nullable
    public DocumentReference getCurrentUser()
    {
        return this.bridge.getCurrentUserReference();
    }

    @Override
    @Nonnull
    public String getType(@Nullable final EntityReference userOrGroup)
    {
        try {
            final XWikiDocument doc = (XWikiDocument) this.bridge.getDocument((DocumentReference) userOrGroup);
            if (doc.getXObject(USER_CLASS) != null) {
                return "user";
            } else if (doc.getXObject(GROUP_CLASS) != null) {
                return "group";
            }
        } catch (Exception ex) {
            this.logger.warn("Failed to determine user type: {}", ex.getMessage(), ex);
        }
        return "unknown";
    }

    @Override
    public String getStringProperty(
        @Nonnull final XWikiDocument doc,
        @Nonnull final DocumentReference classReference,
        @Nonnull final String propertyName)
    {
        try {
            final BaseObject object = doc.getXObject(classReference);
            if (object != null) {
                final String property = object.getStringValue(propertyName);
                return StringUtils.defaultIfBlank(property, null);
            }
        } catch (Exception ex) {
            this.logger.error("Failed to get object property", ex);
        }
        return null;
    }

    @Override
    public void setProperty(
        @Nonnull final XWikiDocument doc,
        @Nonnull final DocumentReference classReference,
        @Nonnull final String propertyName,
        @Nonnull final Object propertyValue) throws Exception
    {
        final XWikiContext xcontext = this.xcontextProvider.get();
        final BaseObject obj = doc.getXObject(classReference, true, xcontext);
        if (obj != null) {
            obj.set(propertyName, propertyValue, xcontext);
            doc.setAuthorReference(getCurrentUser());
            doc.setMetaDataDirty(true);
        }
    }
}
