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
import org.phenotips.templates.data.Template;
import org.phenotips.templates.internal.DefaultTemplate;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.ListProperty;

/**
 * @version $Id$
 */
public class DefaultProject implements Project
{
    private static final String ACCESS_KEY = "access";

    private static final String COLLABORATOR_KEY = "collaborator";

    private static final String TEMPLATE_FIELD_NAME = "templates";

    private static final String OPEN_FOR_CONTRIBUTION_KEY = "openProjectForContribution";

    private static final String OPEN_FOR_VIEWING_KEY = "openProjectForViewing";

    private String projectId;

    private XWikiDocument projectObject;

    private DocumentReference projectReference;

    /**
     * Basic constructor.
     *
     * @param projectObject xwiki object of project
     */
    public DefaultProject(XWikiDocument projectObject) {
        this.projectObject = projectObject;
        this.projectReference = this.projectObject.getDocumentReference();
        this.projectId = this.projectReference.getName();
    }

    @Override
    public DocumentReference getReference()
    {
        return projectReference;
    }

    @Override
    public String getId() {
        return this.projectId;
    }

    @Override
    public String getName() {
        return this.projectReference.getName();
    }

    @Override
    public String getFullName() {
        return projectReference.toString();
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
    public Collection<Template> getTemplates()
    {
        BaseObject xObject = this.projectObject.getXObject(Project.CLASS_REFERENCE);
        if (xObject == null) {
            return Collections.emptyList();
        }

        ListProperty templatesXList = null;
        try {
            templatesXList = (ListProperty) xObject.get(TEMPLATE_FIELD_NAME);
        } catch (Exception e) {
            this.getLogger().error("Error reading property {} from project {}.",
                TEMPLATE_FIELD_NAME, this.projectId, e.getMessage());
        }
        if (templatesXList == null) {
            return Collections.emptyList();
        }

        List<String> templatesList = templatesXList.getList();
        List<Template> templates = new ArrayList<Template>();
        if (templatesList != null) {
            for (String templateString : templatesList) {
                if (StringUtils.isBlank(templateString)) {
                    continue;
                }
                Template s = new DefaultTemplate(templateString);
                templates.add(s);
            }
        }

        return templates;
    }

    @Override
    public boolean setTemplates(Collection<EntityReference> templates)
    {
        XWikiContext xContext = getXContext();
        List<String> templatesList = new ArrayList<String>();
        for (EntityReference template : templates) {
            templatesList.add(template.toString());
        }

        try {
            BaseObject xObject = this.projectObject.getXObject(Project.CLASS_REFERENCE);
            xObject.set(TEMPLATE_FIELD_NAME, templatesList, xContext);
            xContext.getWiki().saveDocument(this.projectObject, "Updated templates", true, xContext);
            return true;
        } catch (Exception e) {
            this.getLogger().error("Error in ProjectScriptService.setTempaltes: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public AccessLevel getCurrentUserAccessLevel()
    {
        AccessLevel highestAccessLevel = null;
        User currentUser = this.getUserManager().getCurrentUser();
        Collection<Collaborator> collaborators = this.getCollaborators();
        for (Collaborator c : collaborators) {
            if (c.isUserIncluded(currentUser)) {
                AccessLevel accessLevel = c.getAccessLevel();
                if (highestAccessLevel == null || accessLevel.compareTo(highestAccessLevel) >= 0) {
                    highestAccessLevel = accessLevel;
                }
            }
        }
        return highestAccessLevel;
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

    private UserManager getUserManager()
    {
        try {
            return ComponentManagerRegistry.getContextComponentManager()
                .getInstance(UserManager.class);
        } catch (ComponentLookupException e) {
            // Should not happen
        }
        return null;
    }

    @Override
    public boolean isProjectOpenForViewing() {
        BaseObject xObject = this.projectObject.getXObject(Project.CLASS_REFERENCE);
        int openIntValue = xObject.getIntValue(DefaultProject.OPEN_FOR_VIEWING_KEY);
        return openIntValue == 1;
    }

    @Override
    public boolean isProjectOpenForContribution() {
        BaseObject xObject = this.projectObject.getXObject(Project.CLASS_REFERENCE);
        int openIntValue = xObject.getIntValue(DefaultProject.OPEN_FOR_CONTRIBUTION_KEY);
        return openIntValue == 1;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DefaultProject)) {
            return false;
        }

        DefaultProject otherProject = (DefaultProject) obj;
        return this.projectId.equals(otherProject.projectId);
    }

    @Override
    public int hashCode() {
        return this.projectId.hashCode();
    }
}
