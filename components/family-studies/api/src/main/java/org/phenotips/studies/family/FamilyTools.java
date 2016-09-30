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
package org.phenotips.studies.family;

import org.phenotips.data.Patient;
import org.phenotips.studies.family.exceptions.PTException;

import org.xwiki.component.annotation.Role;
import org.xwiki.security.authorization.Right;

/**
 * TODO: instead of interface make a SecureFamilyRepository extending FamilyRepository
 *       (similar to PatientRepository/SecurepatientRepository)
 *
 * Utility methods for manipulating families using the permissions of the current user.
 *
 * @version $Id$
 * @since 1.4
 */
@Role
public interface FamilyTools
{
    /**
     * Creates an empty family.
     *
     * @return Family object corresponding to the newly created family.
     */
    Family createFamily();

    /**
     * Returns family object, or null if doesn't exist or current user has no rights.
     *
     * @param familyId a PhenotTips family ID
     * @return Family object of the family with the given id, or null if familyId is not valid or current user does not
     *         have permissions to view the family.
     */
    Family getFamilyById(String familyId);

    /**
     * Returns family pedigree. Essentially this is a shortcut for getFamilyById().getPedigree() with a check that
     * family is not null.
     *
     * @param familyId must be a valid family id
     * @return pedigree object or null if no such family, no pedigree or no view rights for the family.
     */
    Pedigree getPedigreeForFamily(String familyId);

    /**
     * Returns a family ID the patient belongs to.
     *
     * @param patientId id of the patient
     * @return Id of the, or null if patient does not belong to the family or current user has no view rights for the
     *         patient.
     */
    Family getFamilyForPatient(String patientId);

    /**
     * Returns patient's pedigree, which is the pedigree of a family that patient belongs to.
     *
     * @param patientId id of the patient
     * @return Id of the, or null if patient does not belong to the family or current user has no view rights for the
     *         patient.
     */
    Pedigree getPedigreeForPatient(String patientId);

    /**
     * Removes a patient from the family, modifying the both the family and patient records to reflect the change.
     *
     * @param patientId of the patient to delete
     * @return true if patient was removed. false if not, for example, if the patient is not associated with a family,
     *         or if current user has no delete rights
     */
    boolean removeMember(String patientId);

    /**
     * Delete family, modifying the both the family and patient records to reflect the change.
     * For the patients which current user can not edit, uses admin user.
     *
     * @param familyId of the family to delete
     * @param deleteAllMembers indicator whether to delete all family member documents as well
     * @return true if successful; false if deletion failed or current user has not enough rights
     */
    boolean deleteFamily(String familyId, boolean deleteAllMembers);

    /**
     * Similar to deleteFamily, but does not delete the family document (unlinkes all
     * patients from the family).
     *
     * It is supposed to be used in the event handler for xwiki remove action, when the document
     * will be removed by the framework itself.
     *
     * @param family the family
     * @return true if successful
     */
    boolean forceRemoveAllMembers(Family family);

    /**
     * Checks if the current user can delete the family (or the family and all the members).
     *
     * @param familyId of the family to delete
     * @param deleteAllMembers indicator whether to check delete permissions on all family member documents as well
     * @return true if successful
     */
    boolean currentUserCanDeleteFamily(String familyId, boolean deleteAllMembers);

    /**
     * Checks if the current user has the given right (VIEW/EDIUT/DELETE) for the given family.
     *
     * @param family The family to check access rights for
     * @param right The right to check for
     * @return true if the right is given, false otherwise (or if family is null)
     */
    boolean currentUserHasAccessRight(Family family, Right right);

    /**
     * Checks of the given user can add the given patient to the given family.
     *
     * @param family the family
     * @param patient patient to check
     * @param throwException when true, an exception with details is thrown additionis not possible
     * @return true if given user has enough rights to add the patient to the family, and if the patient
     *         is not in another family already
     * @throws PTException when throwException is true and the return value would be false. The exception
     *         may help the caller deduct the reason the addition can not be performed
     */
    boolean canAddToFamily(Family family, Patient patient, boolean throwException) throws PTException;

    /**
     * Sets the pedigree for the family, and updates all the corresponding other documents.
     *
     * TODO: it is questionable where this method should be located, given new entities API.
     *
     * @param family the family
     * @param pedigree to set
     * @throws PTException when the family could not be correctly and fully updated using the given pedigree
     */
    void setPedigree(Family family, Pedigree pedigree) throws PTException;
}
