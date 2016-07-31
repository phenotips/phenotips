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
public interface PermissionsManager
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
     * Get the default visibility to set for new patient records.
     *
     * @return a visibility, or {@code null} if none is configured or the configured one isn't valid
     * @since 1.3M2
     */
    Visibility getDefaultVisibility();

    Visibility resolveVisibility(String name);

    Collection<AccessLevel> listAccessLevels();

    AccessLevel resolveAccessLevel(String name);

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
     */
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
     */
    Iterator<Patient> filterByVisibility(Iterator<Patient> patients, Visibility requiredVisibility);
}
