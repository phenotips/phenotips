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
import org.phenotips.projects.access.ProjectAccessLevel;
import org.phenotips.projects.data.Project;
import org.phenotips.studies.data.Study;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id$
 */
public class DefaultProject implements Project
{
    private static final String ACCESS_KEY = "access";

    private static final String COLLABORATOR_KEY = "collaborator";

    private static final String TEMPLATE_KEY = "study";

    private String projectId;

    private XWikiDocument projectObject;

    private DocumentReference projectReference;

    /**
     * Basic constructor.
     *
     * @param projectId if of the project
     */
    public DefaultProject(String projectId)
    {
        this.projectId = projectId;
        this.projectObject = this.getProjectObject();
        this.projectReference = this.projectObject.getDocumentReference();
    }

    @Override
    public String getName() {
        return this.projectReference.getName();
    }

    @Override
    public String getFullName() {
        return projectId;
    }

    @Override
    public String getDescription() {
        // TODO
        return "";
    }

    @Override
    public int getNumberOfCollaboratorsUsers()
    {
        Set<String> usersList = new HashSet<String>();
        for (Collaborator collaborator : this.getCollaborators()) {
            usersList.addAll(collaborator.getAllUserNames());
        }
        return usersList.size();
    }

    @Override
    public Collection<Collaborator> getCollaborators()
    {
        List<Collaborator> collaborators = new ArrayList<Collaborator>();

        DocumentReference classReference =
            this.getEntityResolver().resolve(Collaborator.CLASS_REFERENCE, projectReference);
        Collection<BaseObject> xCollaborators = projectObject.getXObjects(classReference);

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
                EntityReference userOrGroup = this.getStringResolver().resolve(collaboratorName, projectReference);
                AccessLevel access = this.getPermissionsManager().resolveAccessLevel(accessName);
                collaborators.add(new DefaultCollaborator(userOrGroup, access));
            }
        }

        return collaborators;
    }

    @Override
    public boolean setCollaborators(Collection<EntityReference> contributors, Collection<EntityReference> leaders)
    {
        // Convert EntityReference lists to Collaborators
        Collection<Collaborator> collaborators = new ArrayList<Collaborator>();
        if (contributors != null) {
            for (EntityReference contributorRef : contributors) {
                collaborators.add(new DefaultCollaborator(contributorRef, this.getContributorAccessLevel()));
            }
        }
        if (leaders != null) {
            for (EntityReference leaderRef : leaders) {
                collaborators.add(new DefaultCollaborator(leaderRef, this.getLeaderAccessLevel()));
            }
        }

        return this.setCollaborators(collaborators);
    }

    @Override
    public boolean setCollaborators(Collection<Collaborator> collaborators)
    {
        DocumentReference classReference =
            this.getEntityResolver().resolve(Collaborator.CLASS_REFERENCE, projectReference);
        XWikiContext context = getXContext();

        this.projectObject.removeXObjects(classReference);
        try {
            for (Collaborator collaborator : collaborators) {
                BaseObject o = this.projectObject.newXObject(classReference, context);
                o.setStringValue(COLLABORATOR_KEY, this.getEntitySerializer().serialize(collaborator.getUser()));
                o.setStringValue(ACCESS_KEY, collaborator.getAccessLevel().getName());
            }
            context.getWiki().saveDocument(this.projectObject, "Updated collaborators", true, context);
            return true;
        } catch (Exception e) {
            this.getLogger().error("Error in ProjectScriptService.setCollaborators: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public Collection<EntityReference> getTemplates()
    {
        List<EntityReference> templates = new ArrayList<EntityReference>();

        DocumentReference classReference =
            this.getEntityResolver().resolve(Study.CLASS_REFERENCE, projectReference);
        Collection<BaseObject> xTemplates = this.projectObject.getXObjects(classReference);

        if (xTemplates != null) {
            for (BaseObject o : xTemplates) {
                if (o == null) {
                    continue;
                }
                String templateString = o.getStringValue(TEMPLATE_KEY);
                if (StringUtils.isBlank(templateString)) {
                    continue;
                }
                EntityReference template = this.getStringResolver().resolve(templateString);
                templates.add(template);
            }
        }

        return templates;
    }

    @Override
    public boolean setTemplates(Collection<EntityReference> templates)
    {
        XWikiContext xContext = getXContext();
        DocumentReference classReference =
            this.getEntityResolver().resolve(Study.CLASS_REFERENCE, projectReference);

        this.projectObject.removeXObjects(classReference);
        try {
            for (EntityReference template : templates) {
                BaseObject o = this.projectObject.newXObject(classReference, xContext);
                o.setStringValue(TEMPLATE_KEY, this.getEntitySerializer().serialize(template));
            }
            xContext.getWiki().saveDocument(this.projectObject, "Updated templates", true, xContext);
            return true;
        } catch (Exception e) {
            this.getLogger().error("Error in ProjectScriptService.setTempaltes: {}", e.getMessage(), e);
        }
        return false;
    }

    private XWikiContext getXContext()
    {
        Execution execution;
        try {
            execution = ComponentManagerRegistry.getContextComponentManager().getInstance(Execution.class);
            return (XWikiContext) execution.getContext().getProperty("xwikicontext");
        } catch (ComponentLookupException e) {
            // Should not happen
        }
        return null;
    }

    private XWikiDocument getProjectObject()
    {
        DocumentReference reference = this.getStringResolver().resolve(this.projectId, Project.DEFAULT_DATA_SPACE);
        try {
            return (XWikiDocument) this.getBridge().getDocument(reference);
        } catch (Exception ex) {
            this.getLogger().warn("Failed to access project with id [{}]: {}", projectId, ex.getMessage(), ex);
        }
        return null;
    }

    private DocumentReferenceResolver<EntityReference> getEntityResolver() {
        try {
            return ComponentManagerRegistry.getContextComponentManager()
                    .getInstance(DocumentReferenceResolver.TYPE_REFERENCE);
        } catch (ComponentLookupException e) {
            // Should not happen
        }
        return null;
    }

    private EntityReferenceSerializer<String> getEntitySerializer() {
        try {
            return ComponentManagerRegistry.getContextComponentManager()
                .getInstance(EntityReferenceSerializer.TYPE_STRING);
        } catch (ComponentLookupException e) {
            // Should not happen
        }
        return null;
    }

    private DocumentAccessBridge getBridge() {
        try {
            return ComponentManagerRegistry.getContextComponentManager()
                .getInstance(DocumentAccessBridge.class);
        } catch (ComponentLookupException e) {
            // Should not happen
        }
        return null;
    }

    private PermissionsManager getPermissionsManager() {
        try {
            return ComponentManagerRegistry.getContextComponentManager()
                .getInstance(PermissionsManager.class);
        } catch (ComponentLookupException e) {
            // Should not happen
        }
        return null;
    }

    private DocumentReferenceResolver<String> getStringResolver() {
        try {
            return ComponentManagerRegistry.getContextComponentManager()
                .getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        } catch (ComponentLookupException e) {
            // Should not happen
        }
        return null;
    }

    private ProjectAccessLevel getLeaderAccessLevel() {
        try {
            return ComponentManagerRegistry.getContextComponentManager()
                .getInstance(ProjectAccessLevel.class, "leader");
        } catch (ComponentLookupException e) {
            // Should not happen
        }
        return null;
    }

    private ProjectAccessLevel getContributorAccessLevel() {
        try {
            return ComponentManagerRegistry.getContextComponentManager()
                .getInstance(ProjectAccessLevel.class, "contributor");
        } catch (ComponentLookupException e) {
            // Should not happen
        }
        return null;
    }

    private Logger getLogger() {
        try {
            return ComponentManagerRegistry.getContextComponentManager()
                .getInstance(Logger.class);
        } catch (ComponentLookupException e) {
            // Should not happen
        }
        return null;
    }
}
