package org.phenotips.recordLocking;

import org.phenotips.data.Patient;

/** A component to add, remove and handle locks on patient records. A record lock removes edit rights for a patient
 * from all users. Locks may be added or removed by the patient's managers.
 * @version $Id$
 * @since 1.3
 */
public interface PatientRecordLockManager
{
    /**
     * Places a record lock on the given patient.
     * @param patient The patient to be locked
     * @return  true if successful, false if otherwise
     */
    boolean lockPatientRecord(Patient patient);

    /**
     * Removes the lock from the given patient.
     * @param patient The patient to be unlocked
     * @return true if successful, false if otherwise
     */
    boolean unlockPatientRecord(Patient patient);

    /**
     * Checks if there is a lock on the given patient
     * @param patient The patient to be checked
     * @return True if the patient is locked, false if otherwise
     */
    boolean isLocked(Patient patient);
}
