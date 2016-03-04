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
import java.util.Set;

import org.json.JSONArray;

/**
 * Used for interacting with several consents (represented as {@link Consent}) at once; for example loading all consents
 * from a patient and converting them to JSON.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Unstable
@Role
public interface ConsentManager
{
    /**
     * The underlying system should have a configuration of different consents available.
     * @return all the possible consents that can be used in this system
     */
    Set<Consent> getSystemConsents();

    /**
     * Checks if passed in consent id is actually configured (in the system).
     * @param consentId of consent which is to be tested
     * @return {@link true} if consentId is a valid consent configure din the system
     */
    boolean isValidConsentId(String consentId);

    /**
     * Returns the list of all consents configured in the system, with "granted"/"not granted" status
     * for the given patient for each consent.
     *
     * @param patient record on which consents are granted (or not granted)
     * @return All the consents configured in the system with granted (for the patient) status set for each.
     *         Returns null if patientId is not a valid id.
     */
    Set<Consent> getAllConsentsForPatient(Patient patient);

    /**
     * Same as {@link #getAllConsentsForPatient(Patient)}, for the exception that the patient record
     * lookup happens internally.
     *
     * @param patientId for which a patient record is to be looked up
     * @return same as {@link #getAllConsentsForPatient(Patient)}. Returns null if patientId is not a valid id.
     */
    Set<Consent> getAllConsentsForPatient(String patientId);

    /**
     * Returns the list of consents configured in the system but NOT granted for the patient.
     *
     * @param patient record on which consents are checked
     * @return a list of consents granted for the patient. If patient is null then null is returned.
     */
    Set<Consent> getMissingConsentsForPatient(Patient patient);

    /**
     * Same as {@link #getMissingConsentsForPatient(Patient)}, for the exception that the patient record
     * lookup happens internally.
     *
     * @param patientId for which a patient record is to be looked up
     * @return same as {@link #getMissingConsentsForPatient(Patient)}. Returns null if patientId is not a valid id.
     */
    Set<Consent> getMissingConsentsForPatient(String patientId);

    /**
     * Determines if the given ids are present in the system, and grants them on the given patient record. All other
     * consents are revoked.
     * @param patient record in which consents will be granted
     * @param consents list of consent ids
     * @return {@link true} if the operation was successful, otherwise {@link false}
     */
    boolean setPatientConsents(Patient patient, Iterable<String> consents);

    /**
     * Checks if a specific consent is given for the patient record. If consentId is not configured
     * in the system returns {@link false} regardless of patient consent status.
     *
     * @param patient record in which to test consent
     * @param consentId of consent which is to be checked
     * @return {@link true} if the consent was granted for the patient, otherwise {@link false}
     */
    boolean hasConsent(Patient patient, String consentId);

    /**
     * Same as {@link #hasConsent(Patient,String)}, for the exception that the patient record
     * lookup happens internally.
     *
     * @param patientId for which a patient record is to be looked up
     * @param consentId of consent which is to be checked
     * @return {@link true} if the consent was granted for the patient, otherwise {@link false}
     */
    boolean hasConsent(String patientId, String consentId);

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
     * A convenience function that allows for conversion of a list of consents into a
     * {@link org.json.JSONArray} containing JSON representations of all consents.
     * @param consents to be converted into JSON representation
     * @return a JSON array of consents, each consent represented by a JSONObject
     */
    JSONArray toJSON(Collection<Consent> consents);

    /**
     * A convenience function that allows for conversion of JSON representing several consents.
     * @param consentsJSON array which contains a list of consents
     * @return a set of {@link Consent} instances, converted from JSON
     */
    Set<Consent> fromJSON(JSONArray consentsJSON);
}
