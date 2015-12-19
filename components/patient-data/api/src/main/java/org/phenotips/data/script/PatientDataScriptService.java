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
package org.phenotips.data.script;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * API that provides access to patient data.
 *
 * @version $Id$
 * @since 1.0M8
 */
@Unstable
@Component
@Named("patients")
@Singleton
public class PatientDataScriptService implements ScriptService
{
    /** Wrapped trusted API, doing the actual work. */
    @Inject
    @Named("secure")
    private PatientRepository internalService;

    /**
     * Retrieve a {@link Patient patient} by it's PhenoTips identifier.
     *
     * @param id the patient identifier, i.e. the serialized document reference
     * @return the patient data, or {@code null} if the requested patient does not exist, is not a valid patient, or is
     *         not accessible by the current user
     */
    public Patient getPatientById(String id)
    {
        try {
            return this.internalService.getPatientById(id);
        } catch (SecurityException ex) {
            return null;
        }
    }

    /**
     * Retrieve a {@link Patient patient} by it's clinical identifier. Only works if external identifiers are enabled
     * and used.
     *
     * @param externalId the patient's clinical identifier, as set by the patient's reporter
     * @return the patient data, or {@code null} if the requested patient does not exist, is not a valid patient, or is
     *         not accessible by the current user
     */
    public Patient getPatientByExternalId(String externalId)
    {
        try {
            return this.internalService.getPatientByExternalId(externalId);
        } catch (SecurityException ex) {
            return null;
        }
    }

    /**
     * Create and return a new empty patient record.
     *
     * @return the created patient record, or {@code null} if the user does not have the right to create a new patient
     *         record or the creation fails
     */
    public Patient createNewPatient()
    {
        try {
            return this.internalService.createNewPatient();
        } catch (SecurityException ex) {
            return null;
        }
    }
}
