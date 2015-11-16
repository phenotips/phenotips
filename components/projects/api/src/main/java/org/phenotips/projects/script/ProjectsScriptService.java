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

import org.phenotips.projects.access.ProjectAccessLevel;
import org.phenotips.projects.data.Project;
import org.phenotips.projects.internal.DefaultProject;
import org.phenotips.projects.internal.ProjectsRepository;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * @version $Id$
 */

@Component
@Named("projects")
@Singleton
public class ProjectsScriptService implements ScriptService
{
    @Inject
    private ProjectsRepository projectsRepository;

    @Inject
    @Named("contributor")
    private ProjectAccessLevel contributorAccessLevel;

    /**
     * Returns a project by an id.
     * @param projectId id of the project to return
     * @return a project object
     */
    public Project getProjectById(String projectId)
    {
        return new DefaultProject(projectId);
    }

    /**
     * Returns a collection of all projects.
     *
     * @return a collection of all projects
     */
    public Collection<Project> getAllProjects() {
        return this.projectsRepository.getAllProjects();
    }

    /**
     * Returns a collection of all projects that the current user has an ContributorAccessLevel on.
     *
     * @return a collection of all projects that the current user has an ContributorAccessLevel to.
     */
    public Collection<Project> getAllProjectsAsContributor()
    {
        return this.projectsRepository.getAllProjects(contributorAccessLevel);
    }
}
