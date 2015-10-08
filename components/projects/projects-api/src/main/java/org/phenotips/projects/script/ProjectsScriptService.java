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
package org.phenotips.projects.script;

import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.projects.access.DefaultProjectCollaborator;
import org.phenotips.projects.data.Project;
import org.phenotips.studies.data.Study;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.script.service.ScriptService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id$
 */

@Component
@Named("projects")
@Singleton
public class ProjectsScriptService implements ScriptService
{
    private static final String ACCESS_KEY = "access";

    private static final String COLLABORATOR_KEY = "collaborator";

    private static final String TEMPLATE_KEY = "study";

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> stringResolver;

    @Inject
    private DocumentReferenceResolver<EntityReference> entityResolver;

    @Inject
    private EntityReferenceSerializer<String> entitySerializer;

    @Inject
    private DocumentAccessBridge bridge;

    @Inject
    private Execution execution;

    @Inject
    @Named("leader")
    private AccessLevel leaderAccessLevel;

    @Inject
    @Named("contributor")
    private AccessLevel contributorAccessLevel;

    @Inject
    private PermissionsManager manager;

    @Inject
    private Logger logger;

    /**
     * Returns a collection project collaborators, both leaders and contributors.
     *
     * @param projectId identifier of the project
     * @return a collection of collaborators
     */
    public Collection<Collaborator> getCollaborators(String projectId)
    {
        List<Collaborator> collaborators = new ArrayList<Collaborator>();
        XWikiDocument projectDocument = this.getProjectObject(projectId);
        DocumentReference projectReference = projectDocument.getDocumentReference();
        DocumentReference classReference = this.entityResolver.resolve(Collaborator.CLASS_REFERENCE, projectReference);
        Collection<BaseObject> xCollaborators = projectDocument.getXObjects(classReference);

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
                AccessLevel access = this.manager.resolveAccessLevel(accessName);
                collaborators.add(new DefaultProjectCollaborator(userOrGroup, access));
            }
        }

        return collaborators;
    }

    /**
     * Sets the list of project collaborators.
     *
     * @param projectId identifier of the project
     * @param contributors collection of contributors
     * @param leaders collection of contributors
     * @return true if successful
     */
    public boolean setCollaborators(String projectId, Collection<EntityReference> contributors,
        Collection<EntityReference> leaders)
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

        return this.setCollaborators(projectId, collaborators);
    }

    /**
     * Sets the list of project collaborators.
     *
     * @param projectId identifier of the project
     * @param collaborators collection of contributors
     * @return true if successful
     */
    public boolean setCollaborators(String projectId, Collection<Collaborator> collaborators)
    {
        XWikiDocument projectDocument = this.getProjectObject(projectId);
        DocumentReference projectReference = projectDocument.getDocumentReference();
        DocumentReference classReference = this.entityResolver.resolve(Collaborator.CLASS_REFERENCE, projectReference);
        XWikiContext context = getXContext();

        projectDocument.removeXObjects(classReference);
        try {
            for (Collaborator collaborator : collaborators) {
                BaseObject o = projectDocument.newXObject(classReference, context);
                o.setStringValue(COLLABORATOR_KEY, this.entitySerializer.serialize(collaborator.getUser()));
                o.setStringValue(ACCESS_KEY, collaborator.getAccessLevel().getName());
            }
            context.getWiki().saveDocument(projectDocument, "Updated collaborators", true, context);
            return true;
        } catch (Exception e) {
            this.logger.error("Error in ProjectScriptService.setCollaborators: {}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * Returns a collection templates available for the project.
     *
     * @param projectId identifier of the project
     * @return a collection of templates
     */
    public Collection<EntityReference> getTemplates(String projectId)
    {
        List<EntityReference> templates = new ArrayList<EntityReference>();
        XWikiDocument projectDocument = this.getProjectObject(projectId);
        DocumentReference projectReference = projectDocument.getDocumentReference();
        DocumentReference classReference = this.entityResolver.resolve(Study.CLASS_REFERENCE, projectReference);
        Collection<BaseObject> xTemplates = projectDocument.getXObjects(classReference);

        if (xTemplates != null) {
            for (BaseObject o : xTemplates) {
                if (o == null) {
                    continue;
                }
                String templateString = o.getStringValue(TEMPLATE_KEY);
                if (StringUtils.isBlank(templateString)) {
                    continue;
                }
                EntityReference template = this.stringResolver.resolve(templateString);
                templates.add(template);
            }
        }

        return templates;
    }

    /**
     * Sets the list of templates available for the project.
     *
     * @param projectId identifier of the project
     * @param templates collection of templates
     * @return true if successful
     */
    public boolean setTemplates(String projectId, Collection<EntityReference> templates)
    {
        XWikiDocument projectDocument = this.getProjectObject(projectId);
        DocumentReference projectReference = projectDocument.getDocumentReference();
        XWikiContext xContext = getXContext();
        DocumentReference classReference = this.entityResolver.resolve(Study.CLASS_REFERENCE, projectReference);

        projectDocument.removeXObjects(classReference);
        try {
            for (EntityReference template : templates) {
                BaseObject o = projectDocument.newXObject(classReference, xContext);
                o.setStringValue(TEMPLATE_KEY, this.entitySerializer.serialize(template));
            }
            xContext.getWiki().saveDocument(projectDocument, "Updated templates", true, xContext);
            return true;
        } catch (Exception e) {
            this.logger.error("Error in ProjectScriptService.setTempaltes: {}", e.getMessage(), e);
        }
        return false;
    }

    private XWikiDocument getProjectObject(String projectId)
    {
        DocumentReference reference = this.stringResolver.resolve(projectId, Project.DEFAULT_DATA_SPACE);
        try {
            return (XWikiDocument) this.bridge.getDocument(reference);
        } catch (Exception ex) {
            this.logger.warn("Failed to access project with id [{}]: {}", projectId, ex.getMessage(), ex);
        }
        return null;
    }

    private XWikiContext getXContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }
}
