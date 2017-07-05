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
package org.phenotips.data;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import java.util.List;

/**
 * A provider of users and organization contacts responsible for patient records.
 *
 * @version $Id$
 * @since 1.3
 */
@Unstable("New API introduced in 1.3")
@Role
public interface PatientContactProvider
{
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
     * @return a list of {@link ContactInfo} objects, nay be empty or {@code null}
     */
    List<ContactInfo> getContacts(Patient patient);
}
