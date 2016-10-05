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

import org.phenotips.entities.PrimaryEntityManager;
import org.phenotips.projects.access.ProjectAccessLevel;

import org.xwiki.component.annotation.Role;

import java.util.Collection;

/**
 * @version $Id$
 */
@Role
public interface ProjectRepository extends PrimaryEntityManager<Project>
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
}
