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

import org.phenotips.data.permissions.events.EntityRightsUpdatedEvent;
import org.phenotips.data.permissions.events.EntityRightsUpdatedEvent.RightsUpdateEventType;
import org.phenotips.data.permissions.events.EntityStudyUpdatedEvent;
import org.phenotips.entities.PrimaryEntity;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A manager for entity permissions. Provides convenience methods for listing visibility options and access levels.
 * Allows to retrieve and resolve visibilities, and access levels; provides methods for filtering entities.
 *
 * @version $Id$
 * @since 1.0M9
 * @since 1.4, under new name and moved from patient-access-rules
 */
@Unstable
@Role
public interface EntityPermissionsManager
{
    /**
     * Get the visibility options available, excluding {@link Visibility#isDisabled() disabled} ones.
     *
     * @return a collection of enabled visibilities, may be empty if none are enabled
     */
    @Nonnull
    Collection<Visibility> listVisibilityOptions();

    /**
     * Get all visibility options available in the platform, including {@link Visibility#isDisabled() disabled} ones.
     *
     * @return a collection of visibilities, may be empty if none are available
     */
    @Nonnull
    Collection<Visibility> listAllVisibilityOptions();

    /**
     * Get the default visibility to set for new entity records.
     *
     * @return a visibility, or private if none is configured or the configured one isn't valid
     */
    @Nonnull
    Visibility getDefaultVisibility();

    /**
     * Resolves the provided visibility {@code name} to its associated {@link Visibility} object. Will resolve to
     * {@link org.phenotips.data.permissions.internal.visibility.PrivateVisibility} if provided {@code name} is not
     * valid.
     *
     * @param name the name of the {@link Visibility} object of interest
     * @return the {@link Visibility} object associated with the provided {@code name}
     */
    @Nonnull
    Visibility resolveVisibility(@Nullable String name);

    /**
     * Lists all {@link AccessLevel#isAssignable() assignable} access levels.
     *
     * @return a collection of {@link AccessLevel#isAssignable() assignable} {@link AccessLevel} objects
     */
    @Nonnull
    Collection<AccessLevel> listAccessLevels();

    /**
     * Lists all access levels.
     *
     * @return a collection of all {@link AccessLevel} objects, which may or may not be
     *         {@link AccessLevel#isAssignable() assignable}
     */
    @Nonnull
    Collection<AccessLevel> listAllAccessLevels();

    /**
     * Resolves the provided access {@code name} to the associated {@link AccessLevel} object; will resolve to
     * {@link org.phenotips.data.permissions.internal.access.NoAccessLevel} if {@code name} is invalid.
     *
     * @param name the name of the {@link AccessLevel} of interest
     * @return the {@link AccessLevel} object associated with the provided {@code name}
     */
    @Nonnull
    AccessLevel resolveAccessLevel(@Nullable String name);

    /**
     * Returns the {@link EntityAccess} object for the {@code targetEntity}.
     *
     * @param targetEntity the {@link PrimaryEntity} of interest
     * @return the {@link EntityAccess} for {@code targetEntity}
     */
    @Nonnull
    EntityAccess getEntityAccess(@Nullable PrimaryEntity targetEntity);

    /**
     * Receives a collection of entities and returns a new collection containing only those with
     * {@code visibility >= requiredVisibility}.
     *
     * @param entities a collection of entities
     * @param requiredVisibility minimum level of visibility required for entities
     * @return a collection containing only those with {@code visibility >= requiredVisibility}; may be empty; preserves
     *         the order of the input collection; if the threshold visibility is {@code null}, the input collection is
     *         returned unaltered
     */
    @Nonnull
    Collection<? extends PrimaryEntity> filterByVisibility(
        @Nullable Collection<? extends PrimaryEntity> entities,
        @Nullable Visibility requiredVisibility);

    /**
     * Receives a collection of entities and returns a only those with {@code visibility >= requiredVisibility}.
     *
     * @param entities an iterator over a collection of entities
     * @param requiredVisibility minimum level of visibility required for entities
     * @return an iterator returning only the entities with {@code visibility >= requiredVisibility}; may be empty;
     *         preserves the order of the input iterator; if the threshold visibility is {@code null}, the input is
     *         returned unaltered
     */
    @Nonnull
    Iterator<? extends PrimaryEntity> filterByVisibility(
        @Nullable Iterator<? extends PrimaryEntity> entities,
        @Nullable Visibility requiredVisibility);

    /**
     * Fires a right update event to notify interested parties that ALL permissions have changed. The idea is to fire
     * only one event after a bunch of updates have been performed.
     *
     * @param entityId the {@link PrimaryEntity#getId() identifier} of the affected entity
     */
    void fireRightsUpdateEvent(@Nonnull String entityId);

    /**
     * Fires a {@link EntityRightsUpdatedEvent} to notify interested parties that some permissions have changed. The
     * idea is to fire only one event after a particular update(s) has been performed.
     *
     * @param eventTypes the types of this event, a list of {@link RightsUpdateEventType}s
     * @param entityId the {@link PrimaryEntity#getId() identifier} of the affected entity
     */
    void fireRightsUpdateEvent(@Nonnull List<RightsUpdateEventType> eventTypes, @Nonnull String entityId);

    /**
     * Fires a {@link EntityStudyUpdatedEvent} to notify interested parties that entity has been assigned to a new
     * study.
     *
     * @param entityId the {@link PrimaryEntity#getId() identifier} of the affected entity
     * @param studyId the new study identifier
     */
    void fireStudyUpdateEvent(@Nonnull String entityId, @Nonnull String studyId);
}
