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
package org.phenotips.projects.groupManagers;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.data.permissions.internal.DefaultCollaborator;
import org.phenotips.entities.PrimaryEntityGroupManager;
import org.phenotips.entities.internal.AbstractContainerPrimaryEntityGroupWithParameters;
import org.phenotips.projects.data.Project;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id$
 */
@Component
@Named("Project:Collaborator")
@Singleton
public class CollaboratorsInProjectManager
    extends AbstractContainerPrimaryEntityGroupWithParameters<Project, Collaborator>
    implements PrimaryEntityGroupManager<Project, Collaborator>
{
    /** Type instance for lookup. */
    public static final ParameterizedType TYPE = new DefaultParameterizedType(null, PrimaryEntityGroupManager.class,
            Project.class, Collaborator.class);

    private static final String ACCESS_LEVEL_PARAMETER = "accessLevel";

    /**
     * Public constructor.
     */
    public CollaboratorsInProjectManager()
    {
        super(Project.CLASS_REFERENCE, Collaborator.CLASS_REFERENCE);
    }

    @Override
    protected void setMemberParameters(Collaborator c, BaseObject obj)
    {
        obj.setStringValue(ACCESS_LEVEL_PARAMETER, c.getAccessLevel().getName());
    }

    @Override
    public Collection<Collaborator> getMembers(Project project)
    {
        Collection<Collaborator> result = new LinkedList<>();

        Map<String, Map<String, String>> membersMap = super.getMembersMap(project, Collaborator.CLASS_REFERENCE);
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

    private DocumentReferenceResolver<String> getStringResolver()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager()
                    .getInstance(DocumentReferenceResolver.TYPE_STRING);
        } catch (ComponentLookupException ex) {
            this.logger.error("Failed to access the document reference resolver: {}", ex.getMessage(), ex);
        }
        return null;
    }

    private PermissionsManager getPermissionsManager()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager()
                    .getInstance(PermissionsManager.class, "secure");
        } catch (ComponentLookupException ex) {
            this.logger.error("Failed to access the permission manager: {}", ex.getMessage(), ex);
        }
        return null;
    }
}
