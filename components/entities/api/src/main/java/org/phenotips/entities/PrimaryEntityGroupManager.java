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

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

import java.util.Collection;

/**
 * The property of a group containing members, both of type {@link PrimaryEntity}. For example, a Project is a group
 * of patients.
 *
 * @param <G> the type of the group containing the members
 * @param <E> the type of entities belonging to this group; if more than one type of entities can be part of the group,
 *            then a generic {@code PrimaryEntity} should be used instead
 * @version $Id$
 * @since 1.3M2
 */
@Unstable("New API introduced in 1.3")
@Role
public interface PrimaryEntityGroupManager<G extends PrimaryEntity, E extends PrimaryEntity>
{
    /** The XProperty used for referencing the group document. */
    String REFERENCE_XPROPERTY = "reference";

    /** The XProperty used to save the class of contained members. */
    String CLASS_XPROPERTY = "class";

    /**
     * Adds a new member to a group.
     *
     * @param group to add member to
     * @param member the member to add to the group
     * @return {@code true} if the member was successfully added, or was already a member, {@code false} if the
     *         operation failed
     */
    boolean addMember(G group, E member);

    /**
     * Add all members in a collection to a group.
     *
     * @param group to add members too
     * @param members the members to add to the group
     * @return {@code true} if the members were successfully added, {@code false} if the operation failed
     */
    boolean addAllMembers(G group, Collection<E> members);

    /**
     * Add all members with given ids in a collection to a group.
     *
     * @param group to add members too
     * @param memberIds ids of the members to add to the group
     * @return {@code true} if the members were successfully added, {@code false} if the operation failed
     */
    boolean addAllMembersById(G group, Collection<String> memberIds);

    /**
     * Removes a member from the group.
     *
     * @param group to remove member from
     * @param member the member to remove from the group
     * @return {@code true} if the member was successfully removed, or if it wasn't a member, {@code false} if the
     *         operation failed
     */
    boolean removeMember(G group, E member);

    /**
     * Removes all members from a group.
     *
     * @param group to remove members from
     * @return true if successful
     */
    boolean removeAllMembers(G group);

    /**
     * Removes all members in {@code members} from {@code group}.
     *
     * @param group to remove members from
     * @param members a collection of members to remove
     * @return true if successful
     */
    boolean removeAllMembers(G group, Collection<E> members);

    /**
     * Removed the member {@code member} from all the groups it is a member of.
     *
     * @param member to remove from the groups
     * @return true if successful
     */
    boolean removeFromAllGroups(E member);

    /**
     * Add the member {@code member} to all the groups in {@code groups}.
     *
     * @param member to add to the groups
     * @param groups a collection of groups to add the member to
     * @return true if successful
     */
    boolean addToAllGroups(E member, Collection<G> groups);

    /**
     * Lists all the members (entities) that are part of a group.
     *
     * @param group to get members from
     * @return a collection of Entities, may be empty
     */
    Collection<E> getMembers(G group);

    /**
     * Lists all the members (entities) of a given type that are part of a group.
     *
     * @param group to get members from
     * @param type a reference to an XClass to filter members by; if {@code null}, all members are returned, regardless
     *            of type
     * @return a collection of Entities, may be empty
     */
    Collection<E> getMembersOfType(G group, EntityReference type);

    /**
     * Retrieves groups that have the specified entity as their member, in a random order.
     *
     * @param member the member of the returned groups
     * @return a collection of groups, may be empty
     */
    Collection<G> getGroupsForMember(E member);

    /**
     * Checks if {@code member} is a member of {@code group}.
     *
     * @param group for checking
     * @param member for checking
     * @return true if {@code member} is a member of {@code group}
     */
    boolean isMember(G group, E member);
}
