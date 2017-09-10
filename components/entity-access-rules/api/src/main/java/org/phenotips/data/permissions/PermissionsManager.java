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

import org.phenotips.data.Patient;
import org.phenotips.entities.PrimaryEntity;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import java.util.Collection;
import java.util.Iterator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @version $Id$
 * @since 1.0M9
 */
@Unstable
@Role
public interface PermissionsManager
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

    @Nonnull
    Collection<AccessLevel> listAccessLevels();

    @Nullable
    AccessLevel resolveAccessLevel(@Nullable String name);

    /**
     * Gets the {@link EntityAccess} object for the {@code targetPatient} of interest.
     *
     * @param targetPatient the {@link Patient patient} of interest
     * @return the {@link EntityAccess} for {@code targetPatient}
     * @deprecated since 1.4; please use {@link #getEntityAccess(PrimaryEntity)}
     */
    @Deprecated
    EntityAccess getPatientAccess(Patient targetPatient);

    /**
     * Gets the {@link EntityAccess} object for the {@code entity} of interest.
     *
     * @param entity the {@link PrimaryEntity entity} of interest
     * @return the {@link EntityAccess} for {@code entity}
     */
    @Nonnull
    EntityAccess getEntityAccess(@Nonnull PrimaryEntity entity);

    /**
     * Receives a collection of patients and returns a new collection containing only those with
     * {@code visibility >= requiredVisibility}.
     *
     * @param patients a collection of patients
     * @param requiredVisibility minimum level of visibility required for patients
     * @return a collection containing only those with {@code visibility >= requiredVisibility}; may be empty; preserves
     *         the order of the input collection; if the threshold visibility is {@code null}, the input collection is
     *         returned unaltered
     * @since 1.3M2
     * @deprecated since 1.4; please use {@link #filterVisible(Collection, Visibility)} instead
     */
    @Deprecated
    Collection<Patient> filterByVisibility(Collection<Patient> patients, Visibility requiredVisibility);

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
    Collection<PrimaryEntity> filterVisible(
        @Nonnull Collection<PrimaryEntity> entities,
        @Nullable Visibility requiredVisibility);

    /**
     * Receives a collection of patients and returns a only those with {@code visibility >= requiredVisibility}.
     *
     * @param patients an iterator over a collection of patients
     * @param requiredVisibility minimum level of visibility required for patients
     * @return an iterator returning only the patients with {@code visibility >= requiredVisibility}; may be empty;
     *         preserves the order of the input iterator; if the threshold visibility is {@code null}, the input is
     *         returned unaltered
     * @since 1.3M2
     * @deprecated since 1.4; please use {@link #filterVisible(Iterator, Visibility)} instead
     */
    @Deprecated
    Iterator<Patient> filterByVisibility(Iterator<Patient> patients, Visibility requiredVisibility);

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
    Iterator<PrimaryEntity> filterVisible(
        @Nonnull Iterator<PrimaryEntity> entities,
        @Nullable Visibility requiredVisibility);

    /**
     * Fires a right update event to notify interested parties that some permissions have changed. The idea is to fire
     * only one event after a bunch of updates have been performed.
     *
     * @param entityId the {@link PrimaryEntity#getId() identifier} of the affected entity
     */
    void fireRightsUpdateEvent(String entityId);
}
