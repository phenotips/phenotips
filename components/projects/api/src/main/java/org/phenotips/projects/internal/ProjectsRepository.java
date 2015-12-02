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
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.data.permissions.internal.DefaultCollaborator;
import org.phenotips.projects.access.ProjectAccessLevel;
import org.phenotips.projects.data.Project;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
@Component(roles = { ProjectsRepository.class })
@Singleton
public class ProjectsRepository
{
    private static final String ERROR_CREATING_NEW_PROJECT_MESSAGE = "Error creating a new project with id {}";

    @Inject
    private QueryManager qm;

    @Inject
    private Logger logger;

    @Inject
    private UserManager userManager;

    @Inject
    @Named("leader")
    private ProjectAccessLevel leaderAccessLevel;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> entityResolver;

    /**
     * Returns a collection of EntityReferences of all projects.
     *
     * @return collection of EntityReferences to projects
     */
    public Collection<Project> getAllProjects()
    {
        StringBuilder querySb = new StringBuilder();
        querySb.append("from doc.object(PhenoTips.ProjectClass) as prj ");
        querySb.append("where doc.fullName <> 'PhenoTips.ProjectTemplate'");

        Query query = null;
        List<String> queryResults = null;
        try {
            query = this.qm.createQuery(querySb.toString(), Query.XWQL);
            queryResults = query.execute();
        } catch (QueryException e) {
            this.logger.error("Error while performing projects query: [{}] ", e.getMessage());
        }
        Collections.sort(queryResults, String.CASE_INSENSITIVE_ORDER);

        List<Project> projects = new ArrayList<Project>(queryResults.size());
        for (String projectId : queryResults) {
            Project p = new DefaultProject(projectId);
            projects.add(p);
        }
        return projects;
    }

    /**
     * Returns a collection of all projects that the current user has an {@link accessLevel} to.
     *
     * @param accessLevels access levels required for a project
     * @return a collection of all projects that the current user has an {@link accessLevel} to.
     */
    public Collection<Project> getAllProjects(Collection<ProjectAccessLevel> accessLevels)
    {
        User currentUser = this.userManager.getCurrentUser();

        Collection<Project> projects = this.getAllProjects();
        Iterator<Project> projectsIterator = projects.iterator();
        while (projectsIterator.hasNext()) {
            Project p = projectsIterator.next();

            boolean foundAccessLevel = false;

            Collection<Collaborator> collaborators = p.getCollaborators();
            for (Collaborator collaborator : collaborators) {
                if (accessLevels.contains(collaborator.getAccessLevel())
                    && collaborator.isUserIncluded(currentUser)) {
                    foundAccessLevel = true;
                    break;
                }
            }

            if (!foundAccessLevel) {
                projectsIterator.remove();
            }
        }

        return projects;
    }

    /**
     * Creates a new project. Sets current user as a leader in the project.
     *
     * @param projectId id of the new project
     * @return the new project, if successful
     */
    public Project createNewProject(String projectId)
    {
        XWikiContext xContext = getXContext();
        XWiki wiki = xContext.getWiki();

        EntityReference projectRef = new EntityReference(projectId, EntityType.DOCUMENT, Project.DEFAULT_DATA_SPACE);
        XWikiDocument projectDoc = null;
        try {
            projectDoc = wiki.getDocument(projectRef, xContext);
        } catch (XWikiException e) {
            this.logger.error(ERROR_CREATING_NEW_PROJECT_MESSAGE, projectId, e.getMessage());
            return null;
        }
        if (!projectDoc.isNew()) {
            return null;
        }

        try {
            projectDoc.readFromTemplate(this.entityResolver.resolve(Project.TEMPLATE), xContext);
        } catch (XWikiException e) {
            this.logger.error(ERROR_CREATING_NEW_PROJECT_MESSAGE, projectId, e.getMessage());
            return null;
        }

        User currentUser = this.userManager.getCurrentUser();
        projectDoc.setCreatorReference(currentUser.getProfileDocument());

        // Set current user as a leader in the project
        DefaultProject project = new DefaultProject(projectId);
        Collection<Collaborator> collaborators = new ArrayList<Collaborator>();
        collaborators.add(new DefaultCollaborator(currentUser.getProfileDocument(), leaderAccessLevel));
        project.setCollaborators(collaborators);

        return project;
    }

    private XWikiContext getXContext()
    {
        Execution execution = null;
        try {
            execution = ComponentManagerRegistry.getContextComponentManager().getInstance(Execution.class);
        } catch (ComponentLookupException ex) {
            // Should not happen
            return null;
        }
        XWikiContext context = (XWikiContext) execution.getContext().getProperty("xwikicontext");
        return context;
    }
}
