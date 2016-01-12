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

import java.util.List;

import org.json.JSONArray;

/**
 * Used for interacting with several consents (represented as {@link Consent}) at once; for example loading all consents
 * from a patient and converting them to JSON.
 *
 * @version $Id$
 * @since 1.2RC2
 */
@Role
public interface ConsentManager
{
    /**
     * The underlying system should have a configuration of different consents available.
     * @return all the possible consents that can be used in this system
     */
    List<Consent> getSystemConsents();

    /**
     * Same as {@link #loadConsentsFromPatient(Patient)}, for the exception that the patient record lookup happens internally.
     * @param patientId for which a patient record is to be looked up
     * @return same as {@link #loadConsentsFromPatient(Patient)}
     */
    List<Consent> loadConsentsFromPatient(String patientId);

    /**
     * Each patient should implicitly or explicitly have a list of consents with different grant status.
     * @param patient record on which consents are granted (or not granted)
     * @return a list of all consents configured in the system, with their status
     */
    List<Consent> loadConsentsFromPatient(Patient patient);

    /**
     * Determines if the given ids are present in the system, and grants them on the given patient record. All other
     * consents are revoked.
     * @param patient record in which consents will be granted
     * @param consents list of consent ids
     * @return {@link true} if the operation was successful, otherwise {@link false}
     */
    boolean setPatientConsents(Patient patient, Iterable<String> consents);

    /**
     * Grants a specific consent in a patient record.
     *
     * @param patient record in which to grant a consent
     * @param consentId of consent which is to be granted
     * @return {@link true} if the operation was successful, otherwise {@link false}
     */
    boolean grantConsent(Patient patient, String consentId);

    /**
     * Revokes a specific consent in a patient record.
     * @param patient record in which a consent is to be removed
     * @param consentId of consent to be removed
     * @return {@link true} if the operation was successful, otherwise {@link false}
     */
    boolean revokeConsent(Patient patient, String consentId);

    /**
     * A convenience function (which must be static), that allows for conversion of a list of consents into a
     * {@link net.sf.json.JSONArray} containing JSON representations of all consents.
     * @param consents to be converted into JSON representation
     * @return an array of consents in JSON format
     */
    JSONArray toJson(List<Consent> consents);

    /**
     * A convenience function (which must be static), that allows for conversion of JSON representing several consents.
     * @param json which contains representation of consents
     * @return a list of {@link Consent} instances, converted from JSON
     */
    List<Consent> fromJson(JSONArray json);
}
