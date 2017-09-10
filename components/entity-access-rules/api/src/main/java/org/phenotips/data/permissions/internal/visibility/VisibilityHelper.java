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
package org.phenotips.data.permissions.internal.visibility;

import org.phenotips.data.permissions.Visibility;
import org.phenotips.entities.PrimaryEntity;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import java.util.Collection;
import java.util.Iterator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Handles various operations that modify and/or retrieve visibility data for {@link PrimaryEntity} objects.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable
@Role
public interface VisibilityHelper
{
    /**
     * Get the visibility options available, excluding {@link Visibility#isDisabled() disabled} ones.
     *
     * @return a collection of enabled {@link Visibility} visibilities; may be empty if none are enabled
     */
    @Nonnull
    Collection<Visibility> listVisibilityOptions();

    /**
     * Get all visibility options available in the platform, including {@link Visibility#isDisabled() disabled} ones.
     *
     * @return a collection of {@link Visibility} visibilities; may be empty if none are available
     */
    @Nonnull
    Collection<Visibility> listAllVisibilityOptions();

    /**
     * Get the default {@link Visibility} visibility to set for new patient records.
     *
     * @return a configured {@link Visibility visibility} if valid, or default
     *         {@link org.phenotips.data.permissions.internal.visibility.PrivateVisibility}
     */
    @Nonnull
    Visibility getDefaultVisibility();

    /**
     * Get the {@link Visibility} visibility from its {@code name}. Defaults to
     * {@link org.phenotips.data.permissions.internal.visibility.PrivateVisibility} if {@code name} is invalid.
     *
     * @param name the desired visibility name, as string
     * @return the {@link Visibility} associated with the provided {@code name}, will default to
     *         {@link org.phenotips.data.permissions.internal.visibility.PrivateVisibility} if {@code name} is invalid
     */
    @Nonnull
    Visibility resolveVisibility(@Nullable String name);

    boolean setVisibility(@Nonnull PrimaryEntity entity, @Nullable Visibility visibility);

    @Nonnull
    Visibility getVisibility(@Nonnull PrimaryEntity entity);

    /**
     * Receives a collection of {@link PrimaryEntity} and returns a new collection containing only those with
     * {@code visibility >= requiredVisibility}.
     *
     * @param entities a collection of {@link PrimaryEntity}
     * @param requiredVisibility minimum level of visibility required for entities
     * @return a collection containing only those with {@code visibility >= requiredVisibility}; may be empty; preserves
     *         the order of the input collection; if the threshold visibility is {@code null}, the input collection is
     *         returned unaltered
     */
    @Nonnull
    Collection<PrimaryEntity> filterByVisibility(
        @Nullable Collection<PrimaryEntity> entities,
        @Nullable Visibility requiredVisibility);

    /**
     * Receives a collection of {@link PrimaryEntity} and returns an iterator that will filter only those with
     * {@code visibility >= requiredVisibility}.
     *
     * @param entities an iterator over a collection of entities
     * @param requiredVisibility minimum level of visibility required for entities
     * @return an iterator returning only the entities with {@code visibility >= requiredVisibility}; may be empty;
     *         preserves the order of the input iterator; if the threshold visibility is {@code null}, the input is
     *         returned unaltered
     */
    @Nonnull
    Iterator<PrimaryEntity> filterByVisibility(
        @Nullable Iterator<PrimaryEntity> entities,
        @Nullable Visibility requiredVisibility);
}
