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

/**
 * @version $Id$
 * @since 1.0M9
 */
@Unstable
@Role
@Deprecated
public interface PermissionsManager
{
    /**
     * Get the visibility options available, excluding {@link Visibility#isDisabled() disabled} ones.
     *
     * @return a collection of enabled visibilities, may be empty if none are enabled
     * @deprecated since 1.4; use {@link EntityPermissionsManager#listVisibilityOptions()} instead
     */
    @Deprecated
    Collection<Visibility> listVisibilityOptions();

    /**
     * Get all visibility options available in the platform, including {@link Visibility#isDisabled() disabled} ones.
     *
     * @return a collection of visibilities, may be empty if none are available
     * @since 1.3M2
     * @deprecated since 1.4; use {@link EntityPermissionsManager#listAllVisibilityOptions()} instead
     */
    @Deprecated
    Collection<Visibility> listAllVisibilityOptions();

    /**
     * Get the default visibility to set for new patient records.
     *
     * @return a visibility, or {@code null} if none is configured or the configured one isn't valid
     * @since 1.3M2
     * @deprecated since 1.4; use {@link EntityPermissionsManager#getDefaultVisibility()} instead
     */
    @Deprecated
    Visibility getDefaultVisibility();

    /**
     * Resolves visibility from its name.
     *
     * @param name the name of the visibility
     * @return the {@link Visibility} associated with provided {@code name}
     * @deprecated since 1.4; use {@link EntityPermissionsManager#resolveVisibility(String)} instead
     */
    @Deprecated
    Visibility resolveVisibility(String name);

    /**
     * Lists all available access levels.
     *
     * @return a collection of available {@link AccessLevel} objects
     * @deprecated since 1.4; use {@link EntityPermissionsManager#listAccessLevels()} instead
     */
    @Deprecated
    Collection<AccessLevel> listAccessLevels();

    /**
     * Resolves the access level from its name.
     *
     * @param name the name of the {@link AccessLevel}
     * @return the {@link AccessLevel} associated with the provided {@code name}
     * @deprecated since 1.4; use {@link EntityPermissionsManager#resolveAccessLevel(String)} instead
     */
    @Deprecated
    AccessLevel resolveAccessLevel(String name);

    /**
     * Gets the {@link PatientAccess} for the {@code targetPatient}.
     * @param targetPatient the {@link Patient} of interest
     * @return the {@link PatientAccess} for {@code targetPatient}
     * @deprecated since 1.4; use {@link EntityPermissionsManager#getEntityAccess(PrimaryEntity)} instead
     */
    @Deprecated
    PatientAccess getPatientAccess(Patient targetPatient);

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
     * @deprecated since 1.4; use {@link EntityPermissionsManager#filterByVisibility(Collection, Visibility)} instead
     */
    @Deprecated
    Collection<Patient> filterByVisibility(Collection<Patient> patients, Visibility requiredVisibility);

    /**
     * Receives a collection of patients and returns a only those with {@code visibility >= requiredVisibility}.
     *
     * @param patients an iterator over a collection of patients
     * @param requiredVisibility minimum level of visibility required for patients
     * @return an iterator returning only the patients with {@code visibility >= requiredVisibility}; may be empty;
     *         preserves the order of the input iterator; if the threshold visibility is {@code null}, the input is
     *         returned unaltered
     * @since 1.3M2
     * @deprecated since 1.4; use {@link EntityPermissionsManager#filterByVisibility(Iterator, Visibility)} instead
     */
    @Deprecated
    Iterator<Patient> filterByVisibility(Iterator<Patient> patients, Visibility requiredVisibility);

    /**
     * Fires a right update event to notify interested parties that some permissions have changed. The idea is to fire
     * only one event after a bunch of updates have been performed.
     *
     * @param patientId the {@link Patient#getId() identifier} of the affected patient
     * @deprecated since 1.4; use {@link EntityPermissionsManager#fireRightsUpdateEvent(String)} instead
     */
    @Deprecated
    void fireRightsUpdateEvent(String patientId);
}
