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

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
@Component(roles = { ProjectsRepository.class })
@Singleton
public class ProjectsRepository
{
    private static final String OR = " or ";

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
    @Named("contributor")
    private ProjectAccessLevel contributorAccessLevel;

    @Inject
    private DocumentReferenceResolver<String> stringResolver;

    @Inject
    private DocumentAccessBridge bridge;

    /**
     * @return a list of all projects
     */
    public List<Project> getAllProjects()
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

        List<Project> projects = new ArrayList<Project>(queryResults.size());
        for (String projectId : queryResults) {
            Project p = this.getProjectById(projectId);
            projects.add(p);
        }
        return projects;
    }

    /**
     * @return a list of all projects that the current user can contribute to.
     */
    public List<Project> getProjectsCurrentUserCanContributeTo()
    {
        Set<ProjectAccessLevel> accessLevels = new HashSet<>();
        accessLevels.add(contributorAccessLevel);
        accessLevels.add(leaderAccessLevel);
        List<Project> projects = this.getAllProjects(accessLevels);

        for (Project p : this.getAllProjectsOpenForContribution()) {
            if (!projects.contains(p)) {
                projects.add(p);
            }
        }
        return projects;
    }

    /**
     * @return a list of all projects that the current user is a leader in.
     */
    public List<Project> getProjectsWithLeadingRights()
    {
        Set<ProjectAccessLevel> accessLevels = new HashSet<>();
        accessLevels.add(leaderAccessLevel);
        return this.getAllProjects(accessLevels);
    }

    /**
     * Returns a list of projects that are open for contribution by all users.
     *
     * @return a list of projects that are open for contribution by all users
     */
    public List<Project> getAllProjectsOpenForContribution() {
        List<Project> projects = this.getAllProjects();
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
     * Returns a list of all projects that the current user has an
     * {@link accessLevel} to.
     *
     * @param accessLevels
     *            a collection of access levels required for a project
     * @return a list of all projects that the current user has an
     *         {@link accessLevel} to.
     */
    public List<Project> getAllProjects(Collection<ProjectAccessLevel> accessLevels)
    {
        User currentUser = this.userManager.getCurrentUser();

        List<Project> projects = this.getAllProjects();
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
     * Returns an existing project by its id. If no project is found, returns null.
     *
     * @param projectId id of project to return
     * @return a project object with project.getId().equals(projectId)
     */
    public Project getProjectById(String projectId) {
        DocumentReference reference = this.stringResolver.resolve(projectId, Project.DEFAULT_DATA_SPACE);
        try {
            XWikiDocument xDoc = (XWikiDocument) this.bridge.getDocument(reference);
            if (xDoc != null && xDoc.getXObject(Project.CLASS_REFERENCE) != null) {
                return new DefaultProject(xDoc);
            }
        } catch (Exception ex) {
            this.logger.warn("Failed to access project with id [{}]: {}", projectId, ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * Returns a condition for an HQL patients query that selects all patients that belong to any project in
     * {@link projects}. This is used, for example, in counting the number of cases for a projects (projects would
     * contain only one project), in showing shared data for user (projects would contains all projects that current
     * user is a contributor in).
     *
     * @param baseObjectTable name of BaseObject in query
     * @param propertyTable name of StringProperty in query
     * @param projects list of projects to show patients for
     * @return HQL condition
     */
    public String getProjectCondition(String baseObjectTable, String propertyTable, List<Project> projects)
    {
        String propertyField = new StringBuffer().append("lower(").append(propertyTable).append(".value)").toString();

        StringBuilder sb = new StringBuilder();
        sb.append(" ").append(baseObjectTable).append(".className='PhenoTips.ProjectBindingClass' and (");

        String[] likes = { "'%s;%%'", "'%%;%s'", "'%%;%s;%%'" };
        boolean firstProject = true;
        for (Project p : projects) {
            String projectName = p.getFullName().toLowerCase();
            if (firstProject) {
                firstProject = false;
            } else {
                sb.append(OR);
            }
            for (String l : likes) {
                sb.append(propertyField).append(" like ").append(String.format(l, projectName)).append(OR);
            }
            sb.append(propertyField).append(" = '").append(projectName).append("' ");
        }
        sb.append(") ");

        return sb.toString();
    }

}
