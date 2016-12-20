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
package org.phenotips.recordLocking.script;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.recordLocking.PatientRecordLockManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.httpclient.HttpStatus;

/**
 * A service to add or remove a lock object to a record. The lock object will remove edit rights for all users on the
 * document.
 *
 * @version $Id$
 * @since 1.2M5
 */
@Component
@Named("recordLocking")
@Singleton
public class RecordLockingService implements ScriptService
{
    @Inject
    private PatientRecordLockManager lockManager;

    /**
     * Since Patients coming from scripts are secured and don't allow access to {@code getXDocument()}, we must
     * re-retrieve patients from the default patient repository.
     */
    @Inject
    private PatientRepository pr;

    /**
     * Locks the patient record.
     *
     * @param patient The patient to be locked
     * @return A {@link HttpStatus} indicating the status of the request.
     */
    public int lockPatient(Patient patient)
    {
        if (patient == null) {
            return HttpStatus.SC_BAD_REQUEST;
        }
        return this.lockManager.lockPatientRecord(this.pr.get(patient.getDocumentReference())) ? HttpStatus.SC_OK
            : HttpStatus.SC_BAD_REQUEST;
    }

    /**
     * Unlocks the patient record.
     *
     * @param patient The patient to be unlocked
     * @return A {@link HttpStatus} indicating the status of the request.
     */
    public int unlockPatient(Patient patient)
    {
        if (patient == null) {
            return HttpStatus.SC_BAD_REQUEST;
        }
        return this.lockManager.unlockPatientRecord(this.pr.get(patient.getDocumentReference())) ? HttpStatus.SC_OK
            : HttpStatus.SC_BAD_REQUEST;
    }

    /**
     * Locks the patient record.
     *
     * @param patientID The id of the patient to be locked
     * @return A {@link HttpStatus} indicating the status of the request.
     */
    public int lockPatient(String patientID)
    {
        Patient patient = this.pr.get(patientID);
        return patient == null ? HttpStatus.SC_BAD_REQUEST : this.lockPatient(patient);
    }

    /**
     * Unlocks the patient record.
     *
     * @param patientID The id of the patient to be unlocked
     * @return A {@link HttpStatus} indicating the status of the request.
     */
    public int unlockPatient(String patientID)
    {
        Patient patient = this.pr.get(patientID);
        return patient == null ? HttpStatus.SC_BAD_REQUEST : this.unlockPatient(patient);
    }

    /**
     * Checks if a patient is currently locked.
     *
     * @param patient The patient to be checked.
     * @return {@code true} if locked
     */
    public boolean isLocked(Patient patient)
    {
        if (patient == null) {
            return false;
        }
        return this.lockManager.isLocked(this.pr.get(patient.getDocumentReference()));
    }
}
