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
import org.phenotips.entities.PrimaryEntityManager;
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
public interface FamilyRepository extends PrimaryEntityManager<Family>
{
    /**
     * Returns a Family object for patient. If there's an XWiki family document but no PhenotipsFamily object associated
     * with it in the cache, a new PhenotipsFamily object will be created.
     *
     * @param patient for which to look for a family
     * @return Family if there's an XWiki family document, otherwise null
     */
    Family getFamilyForPatient(Patient patient);

    /**
     * @param id of family to return
     * @return family for which family.getId().equals(id) is true
     * @deprecated use {@link PrimaryEntityManager#get(String)}.
     */
    @Deprecated
    Family getFamilyById(String id);

    /**
     * Deletes the family: unlinks or deletes all patients, then removes family document.
     *
     * @param family the family
     * @param deleteAllMembers if true, also removes all member patients documents
     * @param updatingUser right checks are done for this user
     * @return true if successful
     */
    boolean deleteFamily(Family family, User updatingUser, boolean deleteAllMembers);

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
