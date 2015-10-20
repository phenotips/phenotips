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
package org.phenotips.groups;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;
import org.xwiki.users.User;

import java.util.Set;

/**
 * API for manipulating groups.
 *
 * @version $Id$
 * @since 1.0M9
 */
@Unstable
@Role
public interface GroupManager
{
    /**
     * List the groups that this user is a member of.
     *
     * @param user the user whose groups will be retrieved
     * @return an unmodifiable set of groups, empty if the user isn't part of any groups.
     */
    Set<Group> getGroupsForUser(User user);

    /**
     * Retrieves the group identified by the passed name.
     *
     * @param name the name of the group to retrieve, interpreted as a serialized reference to a document
     * @return the corresponding group, if found, or {@code null} if the requested group doesn't exist
     */
    Group getGroup(String name);

    /**
     * Retrieves the group stored in the passed document.
     *
     * @param groupReference the document where the group is stored
     * @return the corresponding group, if found, or {@code null} if the requested group doesn't exist
     */
    Group getGroup(DocumentReference groupReference);

    /**
     * Checks if a user belongs to a group
     *
     * @param user the user to check
     * @param group the group to check whether the user belongs to
     * @return true if the user belongs to the group
     */
    boolean isUserInGroup(User user, Group group);

}
