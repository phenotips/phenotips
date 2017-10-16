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

import org.phenotips.entities.PrimaryEntity;

import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An {@link EntityAccess} object provides convenience methods for accessing and/or modifying ownership, collaborators,
 * visibility, and access level for the {@link #getEntity() entity}.
 *
 * @version $Id$
 * @since 1.0M9
 * @since 1.4; under new name and moved from patient-access-rules
 */
@Unstable
public interface EntityAccess
{
    /**
     * Returns the {@link PrimaryEntity} object.
     *
     * @return the {@link PrimaryEntity} of interest; may be null
     */
    @Nullable
    PrimaryEntity getEntity();

    /**
     * Returns the {@link Owner} for the {@link #getEntity() entity}.
     *
     * @return the {@link Owner} for the {@link #getEntity()}
     */
    @Nullable
    Owner getOwner();

    /**
     * Returns true iff the current user is the owner of {@link #getEntity()}.
     *
     * @return true iff the current user is the owner of {@link #getEntity()}, false otherwise
     */
    boolean isOwner();

    /**
     * Returns true iff {@code user} is the owner of {@link #getEntity()}.
     *
     * @param user the {@link EntityReference user}
     * @return true iff {@code user} is the owner of {@link #getEntity()}, false otherwise
     */
    boolean isOwner(@Nullable EntityReference user);

    /**
     * Sets the owner for {@link #getEntity()} to provided {@code userOrGroup}. Returns true iff the operation was
     * successful.
     *
     * @param userOrGroup the {@link EntityReference} for the user or group that is to be the new owner
     * @return true iff the new {@code userOrGroup user or group} was set successfully, false otherwise
     */
    boolean setOwner(@Nullable EntityReference userOrGroup);

    /**
     * Retrieves the visibility for the {@link #getEntity() entity}.
     *
     * @return the {@link Visibility} object
     */
    @Nonnull
    Visibility getVisibility();

    /**
     * Sets the {@link #getEntity()} visibility to {@code newVisibility}. Returns true iff the operation was successful.
     *
     * @param newVisibility the new visibility for {@link #getEntity()}; may be null
     * @return true iff the {@code newVisibility} was set successfully, false otherwise
     */
    boolean setVisibility(@Nullable Visibility newVisibility);

    /**
     * Retrieves the {@link Collaborator}s associated with the {@link #getEntity() entity}.
     *
     * @return a list of {@link Collaborator} objects associated with {@link #getEntity()}
     */
    @Nonnull
    Collection<Collaborator> getCollaborators();

    /**
     * Sets {@code newCollaborators} as the collaborators for the {@link #getEntity() entity}. Returns true iff the
     * operation was successful.
     *
     * @param newCollaborators a collection of new {@link Collaborator}s
     * @return true iff {@code newCollaborators} were set successfully, false otherwise
     */
    boolean updateCollaborators(@Nullable Collection<Collaborator> newCollaborators);

    /**
     * Sets the {@code user} as a collaborator for {@link #getEntity() entity}, with the specified {@code access} level.
     * Returns true iff the operation was successful.
     *
     * @param user the {@link EntityReference} for the user that will be added as a new collaborator
     * @param access the {@link AccessLevel} for the newly added {@code user}
     * @return true iff {@code user} was added successfully, false otherwise
     */
    boolean addCollaborator(@Nullable EntityReference user, @Nullable AccessLevel access);

    /**
     * Removes the {@code user} from the list of collaborators for {@link #getEntity() entity}.
     *
     * @param user the {@link EntityReference} referring to a collaborator
     * @return true iff the {@code user} was removed successfully, false otherwise
     */
    boolean removeCollaborator(@Nullable EntityReference user);

    /**
     * Removes the {@code collaborator} from the list of collaborators for {@link #getEntity() entity}.
     *
     * @param collaborator the {@link Collaborator} object to be removed
     * @return true iff the {@code collaborator} was removed successfully, false otherwise
     */
    boolean removeCollaborator(@Nullable Collaborator collaborator);

    /**
     * Gets the current user's {@link AccessLevel} to the {@link #getEntity() entity}.
     *
     * @return the {@link AccessLevel} for the current user
     */
    @Nonnull
    AccessLevel getAccessLevel();

    /**
     * Gets the {@link AccessLevel} for {@code user} to the {@link #getEntity() entity}.
     *
     * @param user the {@link EntityReference} denoting the user whose access level we want to check
     * @return the {@link AccessLevel} for the provided {@code user}
     */
    @Nonnull
    AccessLevel getAccessLevel(@Nullable EntityReference user);

    /**
     * Returns true iff the current user has the level of access that is above or equal to {@code access}.
     *
     * @param access the desired {@link AccessLevel}
     * @return true iff the current user has the desired {@code access} level, false otherwise
     */
    boolean hasAccessLevel(@Nullable AccessLevel access);

    /**
     * Returns true iff the {@code user} has the level of access that is above or equal to {@code access}.
     *
     * @param user the {@link EntityReference} for the user whose access level we want to determine
     * @param access the desired {@link AccessLevel}
     * @return true iff the specified {@code user} has the desired {@code access} level, false otherwise
     */
    boolean hasAccessLevel(@Nullable EntityReference user, @Nullable AccessLevel access);
}
