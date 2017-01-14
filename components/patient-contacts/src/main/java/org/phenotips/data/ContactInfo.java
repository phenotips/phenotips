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

import java.util.List;

import org.json.JSONObject;

/**
 * The information about a contact person/organization for a patient record.
 *
 * @version $Id$
 * @since 1.3M5
 */
public interface ContactInfo
{
    /**
     * Return whether or not any contact info has been populated.
     *
     * @return {@code true} if no field is populated, {@code false} otherwise
     */
    boolean isEmpty();

    /**
     * Get the user id for the contact, for instance if the owner is a local PhenoTips user.
     *
     * @return the (potentially-null) user id
     */
    String getUserId();

    /**
     * Get the contact's full name.
     *
     * @return the (potentially-null) full name of the contact
     */
    String getName();

    /**
     * Get the contact's institution.
     *
     * @return the (potentially-null) institution of the contact
     */
    String getInstitution();

    /**
     * Get the contact's institution.
     *
     * @return the list of the contact's emails
     */
    List<String> getEmails();

    /**
     * Get the contact's URL.
     *
     * @return the (potentially-null) URL for the contact
     */
    String getUrl();

    /**
     * Get the contact details in JSON format.
     *
     * @return a JSONObject serialization of the contact details
     */
    JSONObject toJSON();
}
