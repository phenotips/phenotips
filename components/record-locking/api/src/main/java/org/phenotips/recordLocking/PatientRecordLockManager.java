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
package org.phenotips.recordLocking;

import org.phenotips.data.Patient;

import org.xwiki.component.annotation.Role;

/**
 * A component to add, remove and handle locks on patient records. A record lock removes edit rights for a patient from
 * all users. Locks may be added or removed by the patient's managers.
 *
 * @version $Id$
 * @since 1.2M5
 */
@Role
public interface PatientRecordLockManager
{
    /**
     * Places a record lock on the given patient.
     *
     * @param patient The patient to be locked
     * @return true if successful, false if otherwise
     */
    boolean lockPatientRecord(Patient patient);

    /**
     * Removes the lock from the given patient.
     *
     * @param patient The patient to be unlocked
     * @return true if successful, false if otherwise
     */
    boolean unlockPatientRecord(Patient patient);

    /**
     * Checks if there is a lock on the given patient.
     *
     * @param patient The patient to be checked
     * @return True if the patient is locked, false if otherwise
     */
    boolean isLocked(Patient patient);
}
