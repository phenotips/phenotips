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
package org.phenotips.data.permissions;

import java.util.Collection;

import org.phenotips.Constants;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;
import org.xwiki.users.User;

/**
 * A collaborator on a patient record, either a user or a group that has been granted a specific {@link AccessLevel
 * access level}.
 *
 * @version $Id$
 * @since 1.0M9
 */
@Unstable
public interface Collaborator
{
    /** The XClass used to store collaborators in the patient record. */
    EntityReference CLASS_REFERENCE = new EntityReference("CollaboratorClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    String getType();

    /**
     * @return true if collaborator is of type user.
     */
    boolean isUser();

    /**
     * @return true if collaborator if of type group.
     */
    boolean isGroup();

    /**
     * The user or group that has been set as collaborator.
     *
     * @return a reference to the user's or group's profile
     */
    EntityReference getUser();

    /**
     * The username or group name.
     *
     * @return the name of the document holding the user or group (just the name without the space or instance name)
     */
    String getUsername();

    /**
     * The access that has been granted to this collaborator.
     *
     * @return an access level, must not be null.
     */
    AccessLevel getAccessLevel();

    /**
     * Checks if {@link user} is included in this collaborator. If this.isUser() then a simple comparison is made. If
     * this.isGroup() then a check is made if the user is in the group.
     *
     * @param user to check if included in collaborator
     * @return true if the user is included in collaborator
     */
    boolean isUserIncluded(User user);

    /**
     * Returns a collection of all the users under this collaborator. If the collabrator is a user, then
     * getAllUserNames().size()==1 and getUsername().equals(getAllUserNames().get(0)). If the collaborator is a group
     * the collection will contain the users contained in the group and all its sub groups.
     *
     * @return collections of user names
     */
    Collection<String> getAllUserNames();
}
