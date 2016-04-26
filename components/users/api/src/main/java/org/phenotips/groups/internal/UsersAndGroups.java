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
package org.phenotips.groups.internal;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.omg.CORBA.UNKNOWN;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
@Component(roles = { UsersAndGroups.class })
@Singleton
public class UsersAndGroups
{
    /** Result returned in case getType() result is a user. */
    public static final String USER = "user";

    /** Result returned in case getType() result is a group. */
    public static final String GROUP = "group";

    /** Result returned in case getType() result is unknown. */
    public static final String UNKNOWN = "unknown";

    private static final EntityReference USER_CLASS = new EntityReference("XWikiUsers", EntityType.DOCUMENT,
        new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE));

    private static final EntityReference GROUP_CLASS = new EntityReference("XWikiGroups", EntityType.DOCUMENT,
        new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE));

    @Inject
    private Logger logger;

    @Inject
    private DocumentAccessBridge bridge;

    /**
     * Checks whether a {@link userOrGroup} is an entity that represents a user or a group.
     *
     * @param userOrGroup entity to check
     * @return {@link USER}, @{link GROUP}, or {@link UNKNOWN}
     */
    public String getType(EntityReference userOrGroup)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.bridge.getDocument((DocumentReference) userOrGroup);
            if (doc.getXObject(USER_CLASS) != null) {
                return USER;
            } else if (doc.getXObject(GROUP_CLASS) != null) {
                return GROUP;
            }
        } catch (Exception ex) {
            this.logger.error("Error in getType({})", userOrGroup.getName(), ex.getMessage());
        }
        return UNKNOWN;
    }
}
