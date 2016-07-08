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
package org.phenotips.entities;

import org.phenotips.Constants;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

import java.util.Collection;

/**
 * A group of {@link PrimaryEntity entities}, which in turn is another type of entity. For example, a Project is also a
 * collection of patient records, and a workgroup is a collection of users.
 *
 * @param <E> the type of entities belonging to this group; if more than one type of entities can be part of the group,
 *            then a generic {@code PrimaryEntity} should be used instead
 * @version $Id$
 * @since 1.3M2
 */
@Unstable("New API introduced in 1.3")
public interface PrimaryEntityGroup<E extends PrimaryEntity> extends PrimaryEntity
{
    /** The XClass used for storing membership information. */
    EntityReference GROUP_MEMBERSHIP_CLASS = new EntityReference("GroupMembershipClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    /** The XProperty from {@link #GROUP_MEMBERSHIP_CLASS} used for referencing the group document. */
    String REFERENCE_XPROPERTY = "reference";

    /**
     * @return a reference to an XClass that is supposed to be used by all members of this group, or {@code null} if any
     *         type of entities are allowed for this group.
     */
    EntityReference getMemberType();

    /**
     * List all the members (entities) that are part of this group.
     *
     * @return a collection of Entities, may be empty
     */
    Collection<E> getMembers();

    /**
     * Add a new member to the group.
     *
     * @param member the member to add to the group
     * @return {@code true} if the member was successfully added, or was already a member, {@code false} if the
     *         operation failed
     */
    boolean addMember(E member);

    /**
     * Remove a member from the group.
     *
     * @param member the member to remove from the group
     * @return {@code true} if the member was successfully removed, or if it wasn't a member, {@code false} if the
     *         operation failed
     */
    boolean removeMember(E member);
}
