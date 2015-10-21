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
import org.phenotips.projects.access.DefaultProjectCollaborator;
import org.phenotips.projects.access.ProjectAccessLevel;
import org.phenotips.projects.data.Project;
import org.phenotips.studies.data.Study;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id$
 */
@Component
public class DefaultProject implements Project
{
    private static final String ACCESS_KEY = "access";

    private static final String COLLABORATOR_KEY = "collaborator";

    private static final String TEMPLATE_KEY = "study";

    private static Execution execution;

    private static DocumentReferenceResolver<EntityReference> entityResolver;

    private static EntityReferenceSerializer<String> entitySerializer;

    private static DocumentAccessBridge bridge;

    private static PermissionsManager permissionManager;

    private static DocumentReferenceResolver<String> stringResolver;

    private static ProjectAccessLevel leaderAccessLevel;

    private static ProjectAccessLevel contributorAccessLevel;

    private static Logger logger;

    private String projectId;

    private XWikiDocument projectObject;

    private DocumentReference projectReference;

    static {
        try {
            ComponentManager ccm = ComponentManagerRegistry.getContextComponentManager();
            DefaultProject.execution = ccm.getInstance(Execution.class);
            DefaultProject.entityResolver = ccm.getInstance(DocumentReferenceResolver.TYPE_REFERENCE);
            DefaultProject.entitySerializer = ccm.getInstance(EntityReferenceSerializer.TYPE_STRING);
            DefaultProject.bridge = ccm.getInstance(DocumentAccessBridge.class);
            DefaultProject.permissionManager = ccm.getInstance(PermissionsManager.class);
            DefaultProject.stringResolver = ccm.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
            DefaultProject.leaderAccessLevel = ccm.getInstance(ProjectAccessLevel.class, "leader");
            DefaultProject.contributorAccessLevel = ccm.getInstance(ProjectAccessLevel.class, "contributor");
            DefaultProject.logger = ccm.getInstance(Logger.class);
        } catch (ComponentLookupException e) {
            e.printStackTrace();
        }
    }

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
    public int getNumberOfCollaboratorsUsers() {

        //TODO - this is incorrect!
        return this.getCollaborators().size();
    }

    @Override
    public Collection<Collaborator> getCollaborators()
    {
        List<Collaborator> collaborators = new ArrayList<Collaborator>();

        DocumentReference classReference =
            DefaultProject.entityResolver.resolve(Collaborator.CLASS_REFERENCE, projectReference);
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
                EntityReference userOrGroup = DefaultProject.stringResolver.resolve(collaboratorName, projectReference);
                AccessLevel access = DefaultProject.permissionManager.resolveAccessLevel(accessName);
                collaborators.add(new DefaultProjectCollaborator(userOrGroup, access));
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
                collaborators.add(new DefaultProjectCollaborator(contributorRef, contributorAccessLevel));
            }
        }
        if (leaders != null) {
            for (EntityReference leaderRef : leaders) {
                collaborators.add(new DefaultProjectCollaborator(leaderRef, leaderAccessLevel));
            }
        }

        return this.setCollaborators(collaborators);
    }

    @Override
    public boolean setCollaborators(Collection<Collaborator> collaborators)
    {
        DocumentReference classReference =
            DefaultProject.entityResolver.resolve(Collaborator.CLASS_REFERENCE, projectReference);
        XWikiContext context = getXContext();

        this.projectObject.removeXObjects(classReference);
        try {
            for (Collaborator collaborator : collaborators) {
                BaseObject o = this.projectObject.newXObject(classReference, context);
                o.setStringValue(COLLABORATOR_KEY, DefaultProject.entitySerializer.serialize(collaborator.getUser()));
                o.setStringValue(ACCESS_KEY, collaborator.getAccessLevel().getName());
            }
            context.getWiki().saveDocument(this.projectObject, "Updated collaborators", true, context);
            return true;
        } catch (Exception e) {
            DefaultProject.logger.error("Error in ProjectScriptService.setCollaborators: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public Collection<EntityReference> getTemplates()
    {
        List<EntityReference> templates = new ArrayList<EntityReference>();

        DocumentReference classReference =
            DefaultProject.entityResolver.resolve(Study.CLASS_REFERENCE, projectReference);
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
                EntityReference template = DefaultProject.stringResolver.resolve(templateString);
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
            DefaultProject.entityResolver.resolve(Study.CLASS_REFERENCE, projectReference);

        this.projectObject.removeXObjects(classReference);
        try {
            for (EntityReference template : templates) {
                BaseObject o = this.projectObject.newXObject(classReference, xContext);
                o.setStringValue(TEMPLATE_KEY, DefaultProject.entitySerializer.serialize(template));
            }
            xContext.getWiki().saveDocument(this.projectObject, "Updated templates", true, xContext);
            return true;
        } catch (Exception e) {
            DefaultProject.logger.error("Error in ProjectScriptService.setTempaltes: {}", e.getMessage(), e);
        }
        return false;
    }

    private XWikiContext getXContext()
    {
        return (XWikiContext) DefaultProject.execution.getContext().getProperty("xwikicontext");
    }

    private XWikiDocument getProjectObject()
    {
        DocumentReference reference = DefaultProject.stringResolver.resolve(this.projectId, Project.DEFAULT_DATA_SPACE);
        try {
            return (XWikiDocument) DefaultProject.bridge.getDocument(reference);
        } catch (Exception ex) {
            DefaultProject.logger.warn("Failed to access project with id [{}]: {}", projectId, ex.getMessage(), ex);
        }
        return null;
    }
}
