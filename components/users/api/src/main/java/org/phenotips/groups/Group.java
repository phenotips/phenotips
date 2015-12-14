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

import org.phenotips.Constants;
import org.phenotips.studies.data.Study;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;
import org.xwiki.users.User;

import java.util.Collection;

/**
 * A group of users.
 *
 * @version $Id$
 * @since 1.0M9
 */
@Unstable
public interface Group
{
    /** The space where groups are stored. */
    EntityReference GROUP_SPACE = new EntityReference("Groups", EntityType.SPACE);

    /** The XClass used for storing work groups. */
    EntityReference CLASS_REFERENCE = new EntityReference("PhenoTipsGroupClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    /**
     * Get a reference to the XDocument where this group is defined.
     *
     * @return a valid document reference
     */
    DocumentReference getReference();

    /**
     * Checks if a user belongs to the group.
     *
     * @param user the user to check
     * @return true if the user belongs to the group
     */
    boolean isUserInGroup(User user);

    /**
     * Returns a collection of all the users under this collaborator. If the collabrator is a user, then
     * getAllUserNames().size()==1 and getUsername().equals(getAllUserNames().get(0)). If the collaborator is a group
     * the collection will contain the users contained in the group and all its sub groups.
     *
     * @return collections of user names
     */
    Collection<String> getAllUserNames();

    /**
     * @return a collection of studies in which the group is involved.
     */
    Collection<Study> getStudies();
}
