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
package org.phenotips.data;

import org.xwiki.component.annotation.Role;

import java.util.List;

/**
 * An interface for returning a list of patient the current user can view under a specific criterion
 * (e.g. patients that belong to a project that current user has view rights on, patients where current
 * user is a collaborator).
 *
 * For now, all criteria are equivalent in priority.
 *
 * @version $Id$
 */
@Role
public interface PatientListCriterion
{
    /**
     * @return a list of patients that the current user can view, under a specific criterion.
     */
    List<Patient> getAddList();

    /**
     * @return a list of patients that current user should not view, under a specific criterion.
     */
    List<Patient> getRemoveList();
}
