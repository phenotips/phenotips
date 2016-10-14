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
import org.xwiki.users.User;

/**
 * Utility methods for manipulating families. No access right checks are performed.
 *
 * @version $Id$
 * @since 1.4
 */
@Role
public interface FamilyRepository
{
    /**
     * @param patient whose family the function return
     * @return family for which family.isMember(patient) is true
     */
    Family getFamilyForPatient(Patient patient);

    /**
     * @param id of family to return
     * @return family for which family.getId().equals(id) is true
     */
    Family getFamilyById(String id);

    /**
     * Creates a new empty family (owned by the given entity).
     *
     * @param creator an entity (a user or a group) which will be set as the owner for the created {@link Family family}
     * @return new Family object
     */
    Family createFamily(User creator);

    /**
     * Deletes the family: unlinkes all patients, removes family document.
     *
     * @param family the family
     * @param deleteAllMembers if true, also removes all member patients documents
     * @param updatingUser right checks are done for this user
     * @return true if successful
     */
    boolean deleteFamily(Family family, User updatingUser, boolean deleteAllMembers);

    /**
     * Similar to deleteFamily, but does not delete the family document (unlinkes all patients from the family). It is
     * supposed to be used in the event handler for xwiki remove action, when the document will be removed by the
     * framework itself.
     *
     * @param family the family
     * @param updatingUser right checks are done for this user
     * @return true if successful
     */
    boolean forceRemoveAllMembers(Family family, User updatingUser);

    /**
     * Adds a member to the family TODO: it is questionable where this method should be located, given new entities API.
     *
     * @param family family which should get a new member
     * @param patient to add to family
     * @param updatingUser right checks are done for this user
     * @throws PTException in case addition was not successful for any reason (not enough rights, patient already has a
     *             family, etc.)
     */
    void addMember(Family family, Patient patient, User updatingUser) throws PTException;

    /**
     * Removes the given patient form the family. TODO: it is questionable where this method should be located, given
     * new entities API.
     *
     * @param family family which should lose a new member
     * @param patient to remove from family
     * @param updatingUser right checks are done for this user
     * @throws PTException if removal was not successful for anyh reason (not enough rights, patient not a member of
     *             this family, etc.)
     */
    void removeMember(Family family, Patient patient, User updatingUser) throws PTException;

    /**
     * Sets the pedigree for the family, and updates all the corresponding other documents. TODO: it is questionable
     * where this method should be located, given new entities API.
     *
     * @param family the family
     * @param pedigree to set
     * @param updatingUser right checks are done for this user
     * @throws PTException when the family could not be correctly and fully updated using the given pedigree
     */
    void setPedigree(Family family, Pedigree pedigree, User updatingUser) throws PTException;

    /**
     * Checks of the given user can add the given patient to the given family.
     *
     * @param family the family
     * @param patient patient to check
     * @param updatingUser right checks are done for this user
     * @param throwException when true, an exception with details is thrown additionis not possible
     * @return true if given user has enough rights to add the patient to the family, and if the patient is not in
     *         another family already
     * @throws PTException when throwException is true and the return value would be false. The exception may help the
     *             caller deduct the reason the addition can not be performed
     */
    boolean canAddToFamily(Family family, Patient patient, User updatingUser, boolean throwException)
        throws PTException;

    /**
     * Checks if the given user can delete the family (or the family and all the members).
     *
     * @param family of the family to delete
     * @param updatingUser right checks are done for this user
     * @param deleteAllMembers indicator whether to check delete permissions on all family member documents as well
     * @param throwException when true, an exception with details is thrown additionis not possible
     * @return true if successful
     * @throws PTException when throwException is true and the return value would be false. The exception may help the
     *             caller deduct the reason the addition can not be performed
     */
    boolean canDeleteFamily(Family family, User updatingUser, boolean deleteAllMembers, boolean throwException)
        throws PTException;

    /**
     * For every family member, read users and groups that have either view or edit edit access on the patient, then
     * gives the sam elevel of access on the family for those users and groups. After performing this method, if p is a
     * member of the family, and x has level y access on p, x has level y access on the family. The user who is the
     * owner of the family always has full access to the family. access on p, x has edit access of the family. The famly
     * document is saved to disk after permissions are updated.
     *
     * @param family the family
     */
    void updateFamilyPermissions(Family family);
}
