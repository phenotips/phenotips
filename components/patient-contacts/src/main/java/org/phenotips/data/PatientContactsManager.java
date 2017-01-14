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

import java.util.Collection;

/**
 * Information about the users and organization contacts responsible for a patient.
 *
 * @version $Id$
 * @since 1.3M5
 */
@Unstable
@Role
public interface PatientContactsManager
{
    /**
     * Get the number of contacts for the patient.
     *
     * @return the number of contacts
     */
    int size();

    /**
     * Get the highest-priority non-empty contact.
     *
     * @return the highest-priority non-empty {@link ContactInfo}, or {@code null}
     */
    ContactInfo getFirst();

    /**
     * Get all available non-empty contacts.
     *
     * @return a potentially-empty collection of non-empty {@link ContactInfo} objects
     */
    Collection<ContactInfo> getAll();

    /**
     * Get all available email addresses.
     *
     * @return a potentially-empty collection of email addresses from all {@link ContactInfo} objects
     */
    Collection<String> getEmails();
}
