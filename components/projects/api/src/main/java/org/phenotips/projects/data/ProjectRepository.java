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
package org.phenotips.projects.data;

import org.phenotips.data.Patient;
import org.phenotips.entities.PrimaryEntityGroupManager;
import org.phenotips.projects.access.ProjectAccessLevel;

import org.xwiki.component.annotation.Role;

import java.util.Collection;
import java.util.List;

/**
 * @version $Id$
 */
@Role
public interface ProjectRepository extends PrimaryEntityGroupManager<Project, Patient>
{
    /**
     * Returns a collection of all projects that the current user has one of {@link accessLevels} to.
     *
     * @param accessLevels a collection of access levels required for a project
     * @return a list of all projects that the current user has one of {@link accessLevels} to.
     */
    Collection<Project> getAll(Collection<ProjectAccessLevel> accessLevels);

    /**
     * @return a collection of all projects that the current user can contribute to.
     */
    Collection<Project> getProjectsCurrentUserCanContributeTo();

    /**
     * @return a collection of all projects that the current user is a leader in.
     */
    Collection<Project> getProjectsWithLeadingRights();

    /**
     * @return a collection of projects that are open for contribution by all users.
     */
    Collection<Project> getAllProjectsOpenForContribution();

    /**
     * Receives a comma separated list of project ids and returns a collection of Project objects.
     *
     * @param projects comma separated list of project ids.
     * @return a collection of Project objects
     */
    Collection<Project> getFromString(String projects);

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
    String getProjectCondition(String baseObjectTable, String propertyTable, List<Project> projects);
}
