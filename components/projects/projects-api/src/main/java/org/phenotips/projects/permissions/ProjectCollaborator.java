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
package org.phenotips.projects.permissions;

import org.phenotips.data.permissions.Collaborator;

import org.xwiki.users.User;

/**
 * @version $Id$
 */
public interface ProjectCollaborator extends Collaborator
{

    /**
     * Checks if {@link user} is included in this collaborator. If this.isUser() then a simple comparison is made. If
     * this.isGroup() then a check is made if the user is in the group.
     *
     * @param user to check if included in collaborator
     * @return true if the user is included in collaborator
     */
    boolean isUserIncluded(User user);
}
