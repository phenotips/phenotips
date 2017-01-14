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
package org.phenotips.data.internal;

import org.phenotips.data.ContactInfo;
import org.phenotips.data.Patient;

import org.xwiki.component.annotation.Role;

import java.util.List;

/**
 * A provider of users and organization contacts responsible for patient records.
 *
 * @version $Id$
 * @since 1.3M5
 */
@Role
public interface PatientContactProvider extends Comparable<PatientContactProvider>
{
    /**
     * Get the name of this contact provider.
     *
     * @return the String name of the contact provider (should be unique across providers)
     */
    String getName();

    /**
     * Get the relative priority of the contact provider ({@code 0} is the highest priority and {@code 1000} is the
     * lowest).
     *
     * @return the priority of this contact relative to others
     */
    int getPriority();

    /**
     * Get the contact information for the given patient.
     *
     * @param patient the patient
     * @return a list of {@link ContactInfo} object, or {@code null}
     */
    List<ContactInfo> getContacts(Patient patient);
}
