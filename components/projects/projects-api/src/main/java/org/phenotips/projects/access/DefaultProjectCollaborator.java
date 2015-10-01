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
package org.phenotips.projects.access;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.internal.DefaultCollaborator;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import javax.inject.Inject;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
public class DefaultProjectCollaborator extends DefaultCollaborator
{
    private static final EntityReference USER_CLASS = new EntityReference("XWikiUsers", EntityType.DOCUMENT,
        new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE));

    private static final EntityReference GROUP_CLASS = new EntityReference("XWikiGroups", EntityType.DOCUMENT,
        new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE));

    @Inject
    private static DocumentAccessBridge bridge;

    static {
        try {
            DefaultProjectCollaborator.bridge =
                ComponentManagerRegistry.getContextComponentManager().getInstance(DocumentAccessBridge.class);
        } catch (ComponentLookupException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create a new DefaultProjectCollaborator.
     *
     * @param user that collaborates in the project.
     * @param access access level of collaboration. At the moment only LeaderAccessLevel and ContributorAccessLevel are
     *            expected.
     */
    public DefaultProjectCollaborator(EntityReference user, AccessLevel access)
    {
        super(user, access, null);
    }

    @Override
    public String getType()
    {
        try {
            XWikiDocument doc =
                (XWikiDocument) DefaultProjectCollaborator.bridge.getDocument((DocumentReference) this.getUser());
            if (doc.getXObject(USER_CLASS) != null) {
                return "user";
            } else if (doc.getXObject(GROUP_CLASS) != null) {
                return "group";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "unknown";
    }
}
