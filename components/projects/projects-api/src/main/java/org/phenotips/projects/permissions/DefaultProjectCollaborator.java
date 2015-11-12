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
package org.phenotips.projects.permissions;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.internal.DefaultCollaborator;
import org.phenotips.groups.internal.DefaultGroup;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.users.User;

import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
public class DefaultProjectCollaborator extends DefaultCollaborator implements ProjectCollaborator
{
    private static final EntityReference USER_CLASS = new EntityReference("XWikiUsers", EntityType.DOCUMENT,
        new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE));

    private static final EntityReference GROUP_CLASS = new EntityReference("XWikiGroups", EntityType.DOCUMENT,
        new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE));

    private static DocumentAccessBridge bridge;

    private static Logger logger;

    static {
        try {
            ComponentManager ccm = ComponentManagerRegistry.getContextComponentManager();
            DefaultProjectCollaborator.bridge = ccm.getInstance(DocumentAccessBridge.class);
            DefaultProjectCollaborator.logger = ccm.getInstance(Logger.class);
        } catch (ComponentLookupException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param user collaborator user
     * @param access collaborator user access level
     */
    public DefaultProjectCollaborator(EntityReference user, AccessLevel access)
    {
        super(user, access);
    }

    @Override
    public String getType()
    {
        try {
            EntityReference userOrGroup = getUser();
            XWikiDocument doc =
                (XWikiDocument) DefaultProjectCollaborator.bridge.getDocument((DocumentReference) userOrGroup);
            if (doc.getXObject(USER_CLASS) != null) {
                return "user";
            } else if (doc.getXObject(GROUP_CLASS) != null) {
                return "group";
            }
        } catch (Exception ex) {
            DefaultProjectCollaborator.logger.error("Error getting type of project collaborator: {}", ex.getMessage());
        }
        return "unknown";
    }

    @Override
    public boolean isUserIncluded(User user)
    {
        EntityReference thisUser = this.getUser();
        if (isGroup()) {
            if (!(thisUser instanceof DocumentReference)) {
                return false;
            }
            DefaultGroup group = new DefaultGroup((DocumentReference) thisUser);
            return group.isUserInGroup(user);
        } else {
            return thisUser.equals(user.getProfileDocument());
        }
    }
}
