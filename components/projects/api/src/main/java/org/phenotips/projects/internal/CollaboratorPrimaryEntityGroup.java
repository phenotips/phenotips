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
package org.phenotips.projects.internal;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.data.permissions.internal.DefaultCollaborator;
import org.phenotips.entities.internal.AbstractPrimaryEntityGroupWithParameters;
import org.phenotips.projects.data.Project;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import org.json.JSONObject;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id $
 */
public class CollaboratorPrimaryEntityGroup extends AbstractPrimaryEntityGroupWithParameters<Collaborator>
{
    private static final Collection<String> TYPES;

    private static final String ACCESS_LEVEL_PARAMETER = "accessLevel";

    static {
        String[] asArray = new String[] {"XWiki.XWikiUsers", "PhenoTips.PhenoTipsGroupClass"};
        TYPES = Arrays.asList(asArray);
    }

    /**
     * public constructor.
     *
     * @param document project's document
     */
    protected CollaboratorPrimaryEntityGroup(XWikiDocument document)
    {
        // There is no manager for Collaborator.
        super(document);
    }

    /**
     * Replaces collaborators with a new collection.
     *
     * @param collaborators a collection of collaborators
     * @return true if successful
     */
    public boolean setMembers(Collection<Collaborator> collaborators)
    {
        Collection<Collaborator> existingCollaborators = this.getMembers();
        for (Collaborator c : existingCollaborators) {
            this.removeMember(c);
        }

        for (Collaborator c : collaborators) {
            this.addMember(c);
        }

        return true;
    }

    @Override
    public Collection<Collaborator> getMembers()
    {
        Collection<Collaborator> result = new LinkedList<>();

        Map<String, Map<String, String>> membersMap = super.getMembersMap(TYPES);
        for (String userOrGroupName : membersMap.keySet()) {
            Map<String, String> params = membersMap.get(userOrGroupName);
            String accessLevelName = params.get(ACCESS_LEVEL_PARAMETER);

            if (accessLevelName == null) {
                continue;
            }

            DocumentReference userOrGroup = this.getStringResolver().resolve(userOrGroupName);
            AccessLevel accessLevel = this.getPermissionsManager().resolveAccessLevel(accessLevelName);
            Collaborator c = new DefaultCollaborator(userOrGroup, accessLevel);

            result.add(c);
        }

        return result;
    }

    @Override
    protected void setMemberParameters(Collaborator c, BaseObject obj)
    {
        obj.setStringValue(ACCESS_LEVEL_PARAMETER, c.getAccessLevel().getName());
    }

    @Override
    public EntityReference getMemberType()
    {
        return Collaborator.CLASS_REFERENCE;
    }

    @Override
    public EntityReference getType()
    {
        return Project.CLASS_REFERENCE;
    }

    @Override
    public void updateFromJSON(JSONObject json)
    {
        throw new UnsupportedOperationException();
    }

    private DocumentReferenceResolver<String> getStringResolver()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager()
                    .getInstance(DocumentReferenceResolver.TYPE_STRING);
        } catch (ComponentLookupException ex) {
            this.logger.error("Failed to access the query manager: {}", ex.getMessage(), ex);
        }
        return null;
    }

    private PermissionsManager getPermissionsManager()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager()
                    .getInstance(PermissionsManager.class, "secure");
        } catch (ComponentLookupException ex) {
            this.logger.error("Failed to access the query manager: {}", ex.getMessage(), ex);
        }
        return null;
    }
}
