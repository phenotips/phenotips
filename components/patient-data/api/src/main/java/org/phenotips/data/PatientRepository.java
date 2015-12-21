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

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * API that provides access to patient data. No access rights are checked here.
 *
 * @version $Id$
 * @since 1.0M10 (was named PatientData since 1.0M8)
 */
@Unstable
@Role
public interface PatientRepository
{
    /**
     * Retrieve a {@link Patient patient} by it's PhenoTips identifier.
     *
     * @param id the patient identifier, i.e. the serialized document reference
     * @return the patient data, or {@code null} if the requested patient does not exist or is not a valid patient
     */
    Patient getPatientById(String id);

    /**
     * Retrieve a {@link Patient patient} by it's clinical identifier. Only works if external identifiers are enabled
     * and used.
     *
     * @param externalId the patient's clinical identifier, as set by the patient's reporter
     * @return the patient data, or {@code null} if the requested patient does not exist or is not a valid patient
     */
    Patient getPatientByExternalId(String externalId);

    /**
     * Load and return a {@link Patient patient} from the specified document. This method will be removed once the new
     * XWiki model is implemented and the intermediary model bridge is no longer needed. Do not use.
     *
     * @param document the document where the patient data is stored
     * @return the patient data
     * @throws IllegalArgumentException if the provided document doesn't contain a patient record
     */
    Patient loadPatientFromDocument(DocumentModelBridge document);

    /**
     * Create and return a new empty patient record (owned by the currently logged in user).
     *
     * @return the created patient record
     */
    Patient createNewPatient();

    /**
     * Create and return a new empty patient record (owned by the given entity).
     *
     * @param creator a reference to the document representing an entity (a user or a group) which will be set as the
     *            owner for the created {@link Patient patient}.
     * @return the created patient record
     */
    Patient createNewPatient(DocumentReference creator);

    /**
     * Delete a patient record, identified by its internal PhenoTips identifier. If the indicated patient record doesn't
     * exist, or if the user sending the request doesn't have the right to delete the target patient record, no change
     * is performed and an error is returned.
     *
     * @param id the patient's internal identifier, see {@link org.phenotips.data.Patient#getId()}
     * @return true if successful
     */
    boolean deletePatient(String id);
}
