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

import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.data.permissions.internal.DefaultCollaborator;
import org.phenotips.projects.access.ProjectAccessLevel;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Handles setting and getting of templates and collaborators in a project.
 *
 * @version $Id$
 */
@Component(roles = { DefaultProjectHelper.class })
@Singleton
public class DefaultProjectHelper
{
    private static final String ACCESS_KEY = "access";

    private static final String COLLABORATOR_KEY = "collaborator";

    @Inject
    @Named("observer")
    private ProjectAccessLevel observerAccessLevel;

    @Inject
    @Named("leader")
    private ProjectAccessLevel leaderAccessLevel;

    @Inject
    @Named("contributor")
    private ProjectAccessLevel contributorAccessLevel;

    @Inject
    private EntityReferenceSerializer<String> entitySerializer;

    @Inject
    private DocumentReferenceResolver<EntityReference> entityResolver;

    @Inject
    private Logger logger;

    @Inject
    private PermissionsManager permissionsManager;

    @Inject
    private DocumentReferenceResolver<String> stringResolver;

    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * Sets the list of project collaborators.
     *
     * @param projectObject xwiki object of project
     * @param collaborators collection of contributors
     * @return true if successful
     */
    public boolean setCollaborators(XWikiDocument projectObject, Collection<Collaborator> collaborators)
    {
        DocumentReference projectReference = projectObject.getDocumentReference();
        DocumentReference classReference =
            this.entityResolver.resolve(Collaborator.CLASS_REFERENCE, projectReference);
        XWikiContext context = this.contextProvider.get();

        projectObject.removeXObjects(classReference);
        try {
            for (Collaborator collaborator : collaborators) {
                BaseObject o = projectObject.newXObject(classReference, context);
                o.setStringValue(COLLABORATOR_KEY, this.entitySerializer.serialize(collaborator.getUser()));
                o.setStringValue(ACCESS_KEY, collaborator.getAccessLevel().getName());
            }
            context.getWiki().saveDocument(projectObject, "Updated collaborators", true, context);
            return true;
        } catch (Exception e) {
            this.logger.error("Error in ProjectScriptService.setCollaborators: {}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * Returns a collection project collaborators, both leaders and contributors.
     *
     * @param projectObject xwiki object of project
     * @return a collection of collaborators
     */
    public Collection<Collaborator> getCollaborators(XWikiDocument projectObject)
    {
        DocumentReference projectReference = projectObject.getDocumentReference();
        List<Collaborator> collaborators = new ArrayList<Collaborator>();

        DocumentReference classReference =
            this.entityResolver.resolve(Collaborator.CLASS_REFERENCE, projectReference);
        Collection<BaseObject> xCollaborators = projectObject.getXObjects(classReference);
        if (xCollaborators == null) {
            return Collections.emptyList();
        }

        if (xCollaborators != null) {
            for (BaseObject o : xCollaborators) {
                if (o == null) {
                    continue;
                }
                String collaboratorName = o.getStringValue(COLLABORATOR_KEY);
                String accessName = o.getStringValue(ACCESS_KEY);
                if (StringUtils.isBlank(collaboratorName) || StringUtils.isBlank(accessName)) {
                    continue;
                }
                EntityReference userOrGroup = this.stringResolver.resolve(collaboratorName, projectReference);
                AccessLevel access = this.permissionsManager.resolveAccessLevel(accessName);
                collaborators.add(new DefaultCollaborator(userOrGroup, access));
            }
        }

        return collaborators;
    }
}
