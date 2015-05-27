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
package org.phenotips.data.indexing;

import org.phenotips.data.Patient;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * API for indexing patient data in a search engine.
 *
 * @version $Id$
 * @since 1.0M8
 */
@Unstable
@Role
public interface PatientIndexer
{
    /**
     * Add (or update) a patient to the index.
     *
     * @param patient the patient to index
     */
    void index(Patient patient);

    /**
     * Delete from the index a patient.
     *
     * @param patient the patient to delete
     */
    void delete(Patient patient);

    /**
     * Reindex all the patients.
     */
    void reindex();
}
