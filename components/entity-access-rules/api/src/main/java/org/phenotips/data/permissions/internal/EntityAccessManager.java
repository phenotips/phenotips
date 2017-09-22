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
package org.phenotips.data.permissions.internal;

import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.data.permissions.Owner;
import org.phenotips.entities.PrimaryEntity;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Handles various operations that modify and/or get access data for {@link Owner} and {@link Collaborator} objects.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable
@Role
public interface EntityAccessManager
{
    /**
     * Get the access levels available, excluding un-assignable ones, {@link AccessLevel#isAssignable()}}.
     *
     * @return a collection of assignable {@link AccessLevel} objects, may be empty if none are assignable
     */
    @Nonnull
    Collection<AccessLevel> listAccessLevels();

    /**
     * Get the access levels available, including un-assignable ones, {@link AccessLevel#isAssignable()}}.
     *
     * @return a collection of {@link AccessLevel} objects, may be empty if none are available
     */
    @Nonnull
    Collection<AccessLevel> listAllAccessLevels();

    /**
     * Get the {@link AccessLevel} access level from its {@code name}.
     *
     * @param name the desired access level name, as string
     * @return the {@link AccessLevel} associated with the provided {@code name}, or null if the provided {@code name}
     * is not valid
     */
    @Nonnull
    AccessLevel resolveAccessLevel(@Nullable String name);

    @Nonnull
    AccessLevel getAccessLevel(@Nullable PrimaryEntity entity, @Nullable EntityReference userOrGroup);

    /**
     * Returns true iff {@link EntityAccessHelper#getCurrentUser() the current user} has administrative access level,
     * false otherwise.
     *
     * @param entity the {@link PrimaryEntity entity} of interest
     * @return true iff the current user has administrative access level, false otherwise
     */
    boolean isAdministrator(@Nullable PrimaryEntity entity);

    /**
     * Returns true iff {@code user} has administrative access level, false otherwise or if user is {@code null}.
     *
     * @param entity the {@link PrimaryEntity entity} of interest
     * @param user the {@link DocumentReference user} of interest
     * @return true iff {@code user} has administrative access level, false otherwise or if user is {@code null}.
     */
    boolean isAdministrator(@Nullable PrimaryEntity entity, @Nullable DocumentReference user);

    /**
     * Get the {@link Owner owner} of the {@code entity}.
     *
     * @param entity the {@link PrimaryEntity entity} of interest
     * @return the {@link Owner} owner for the {@code entity}
     */
    @Nullable
    Owner getOwner(@Nullable PrimaryEntity entity);

    /**
     * Sets the {@code userOrGroup owner} for {@code entity} of interest.
     *
     * @param entity the {@link PrimaryEntity entity} of interest
     * @param userOrGroup the new {@link EntityReference owner} for the {@code entity}
     * @return true iff the new owner was set successfully, false otherwise
     */
    boolean setOwner(@Nullable PrimaryEntity entity, @Nullable EntityReference userOrGroup);

    @Nonnull
    Collection<Collaborator> getCollaborators(@Nullable PrimaryEntity entity);

    boolean setCollaborators(@Nullable PrimaryEntity entity, @Nullable Collection<Collaborator> newCollaborators);

    boolean addCollaborator(@Nullable PrimaryEntity entity, @Nullable Collaborator collaborator);

    boolean removeCollaborator(@Nullable PrimaryEntity entity, @Nullable Collaborator collaborator);
}
