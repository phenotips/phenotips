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

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id$
 */
@Component
@Singleton
public class DefaultEntityAccessHelper implements EntityAccessHelper
{
    private static final String USER_LABEL = "user";

    private static final String GROUP_LABEL = "group";

    private static final String UNKNOWN_LABEL = "unknown";

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
    public DocumentReference getCurrentUser()
    {
        return this.bridge.getCurrentUserReference();
    }

    @Override
    public String getType(EntityReference userOrGroup)
    {
        if (userOrGroup == null) {
            // Guest user
            return USER_LABEL;
        }
        try {
            XWikiDocument doc = (XWikiDocument) this.bridge.getDocument((DocumentReference) userOrGroup);
            if (doc.getXObject(USER_CLASS) != null) {
                return USER_LABEL;
            } else if (doc.getXObject(GROUP_CLASS) != null) {
                return GROUP_LABEL;
            }
        } catch (Exception ex) {
            this.logger.warn("Failed to determine user type: {}", ex.getMessage(), ex);
        }
        return UNKNOWN_LABEL;
    }

    @Override
    public String getStringProperty(XWikiDocument doc, DocumentReference classReference, String propertyName)
    {
        try {
            BaseObject object = doc.getXObject(classReference);
            if (object != null) {
                String property = object.getStringValue(propertyName);
                if (!StringUtils.isEmpty(property)) {
                    return property;
                }
            }
        } catch (Exception ex) {
            this.logger.error("Failed to get object property", ex);
        }
        return null;
    }

    @Override
    public void setProperty(XWikiDocument doc, DocumentReference classReference, String propertyName,
        Object propertyValue)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        BaseObject obj = doc.getXObject(classReference, true, xcontext);
        if (obj != null) {
            obj.set(propertyName, propertyValue, xcontext);
            doc.setAuthorReference(getCurrentUser());
            doc.setMetaDataDirty(true);
        }
    }
}
