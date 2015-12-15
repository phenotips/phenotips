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
import org.phenotips.studies.data.Study;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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

    @Inject
    @Named("leader")
    private ProjectAccessLevel leaderAccessLevel;

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
     * Returns a collection of all projects that the current user can contribute to.
     *
     * @return a collection of all projects that the current user can contribute to.
     */
    public Collection<Project> getAllProjectsAsContributor()
    {
        // Both leaders and contributors can contribute to a project
        Set<ProjectAccessLevel> accessLevels = new HashSet<>();
        accessLevels.add(contributorAccessLevel);
        accessLevels.add(leaderAccessLevel);

        return this.projectsRepository.getAllProjects(accessLevels);
    }

    /**
     * Receives a comma separated list of projects ids and returns a collection of ids of all templates associated with
     * them. For example, if t1,t2 are associated with p1 and t2,t3 are associated with p2, the collection returned for
     * the input "p1,p2" would contain t1,t2,t3.
     *
     * @param projects command separated project ids
     * @return collection of templates ids.
     */
    public Collection<Study> getTemplatesForProjects(String projects)
    {
        Set<Study> templates = new HashSet<Study>();
        for (String projectId : projects.split(",")) {
            Project project = this.getProjectById(projectId);
            if (project == null) {
                continue;
            }
            for (Study s : project.getTemplates()) {
                templates.add(s);
            }
        }
        return templates;
    }
}
