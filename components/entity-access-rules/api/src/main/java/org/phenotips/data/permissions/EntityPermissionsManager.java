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

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import java.util.Collection;
import java.util.Iterator;

/**
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
    Collection<Visibility> listVisibilityOptions();

    /**
     * Get all visibility options available in the platform, including {@link Visibility#isDisabled() disabled} ones.
     *
     * @return a collection of visibilities, may be empty if none are available
     * @since 1.3M2
     */
    Collection<Visibility> listAllVisibilityOptions();

    /**
     * Get the default visibility to set for new entity records.
     *
     * @return a visibility, or {@code null} if none is configured or the configured one isn't valid
     * @since 1.3M2
     */
    Visibility getDefaultVisibility();

    Visibility resolveVisibility(String name);

    Collection<AccessLevel> listAccessLevels();

    AccessLevel resolveAccessLevel(String name);

    EntityAccess getEntityAccess(PrimaryEntity targetEntity);

    /**
     * Receives a collection of entities and returns a new collection containing only those with
     * {@code visibility >= requiredVisibility}.
     *
     * @param entities a collection of entities
     * @param requiredVisibility minimum level of visibility required for entities
     * @return a collection containing only those with {@code visibility >= requiredVisibility}; may be empty; preserves
     *         the order of the input collection; if the threshold visibility is {@code null}, the input collection is
     *         returned unaltered
     * @since 1.3M2
     */
    Collection<? extends PrimaryEntity> filterByVisibility(Collection<? extends PrimaryEntity> entities,
        Visibility requiredVisibility);

    /**
     * Receives a collection of entities and returns a only those with {@code visibility >= requiredVisibility}.
     *
     * @param entities an iterator over a collection of entities
     * @param requiredVisibility minimum level of visibility required for entities
     * @return an iterator returning only the entities with {@code visibility >= requiredVisibility}; may be empty;
     *         preserves the order of the input iterator; if the threshold visibility is {@code null}, the input is
     *         returned unaltered
     * @since 1.3M2
     */
    Iterator<? extends PrimaryEntity> filterByVisibility(Iterator<? extends PrimaryEntity> entities,
        Visibility requiredVisibility);

    /**
     * Fires a right update event to notify interested parties that some permissions have changed. The idea is to fire
     * only one event after a bunch of updates have been performed.
     *
     * @param entityId the {@link PrimaryEntity#getId() identifier} of the affected entity
     */
    void fireRightsUpdateEvent(String entityId);
}
