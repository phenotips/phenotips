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

import org.phenotips.data.permissions.Collaborator;
import org.phenotips.projects.access.ProjectAccessLevel;
import org.phenotips.projects.data.Project;

import org.xwiki.component.annotation.Component;
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

/**
 * @version $Id$
 */
@Component(roles = { ProjectsRepository.class })
@Singleton
public class ProjectsRepository
{
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
     * Returns a collection of projects that are open for viewing by all users.
     *
     * @return a collection of projects that are open for viewing by all users
     */
    public Collection<Project> getAllProjectsOpenForViewing() {
        Collection<Project> projects = this.getAllProjects();
        Iterator<Project> projectIterator = projects.iterator();
        while (projectIterator.hasNext()) {
            Project project = projectIterator.next();
            if (!project.isProjectOpenForViewing()) {
                projectIterator.remove();
            }
        }
        return projects;
    }

    /**
     * Returns a collection of projects that are open for contribution by all users.
     *
     * @return a collection of projects that are open for contribution by all users
     */
    public Collection<Project> getAllProjectsOpenForContribution() {
        Collection<Project> projects = this.getAllProjects();
        Iterator<Project> projectIterator = projects.iterator();
        while (projectIterator.hasNext()) {
            Project project = projectIterator.next();
            if (!project.isProjectOpenForContribution()) {
                projectIterator.remove();
            }
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
}
