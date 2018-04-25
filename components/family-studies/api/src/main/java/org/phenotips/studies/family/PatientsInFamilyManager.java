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
import org.phenotips.entities.PrimaryEntityConnectionsManager;
import org.phenotips.studies.family.exceptions.PTException;

import org.xwiki.component.annotation.Role;
import org.xwiki.users.User;

/**
 * @version $Id$
 */
@Role
public interface PatientsInFamilyManager extends PrimaryEntityConnectionsManager<Family, Patient>
{
    /**
     * Unlinks Similar to deleteFamily, but does not delete the family document (unlinkes all patients from the family).
     * It is supposed to be used in the event handler for xwiki remove action, when the document will be removed by the
     * framework itself.
     *
     * @param family the family
     * @return true if successful
     */
    boolean forceRemoveAllMembers(Family family);

    /**
     * Unlinks all patients from the family. It is supposed to be used in the event handler for xwiki remove action,
     * when the document will be removed by the framework itself.
     *
     * @param family the family
     * @param updatingUser right checks are done for this user
     * @return true if successful
     */
    boolean forceRemoveAllMembers(Family family, User updatingUser);

    /**
     * Sets the pedigree for the family, and updates all the corresponding other documents.
     *
     * @param family the family
     * @param pedigree to set
     * @throws PTException when the family could not be correctly and fully updated using the given pedigree
     */
    void setPedigree(Family family, Pedigree pedigree) throws PTException;

    /**
     * Sets the pedigree for the family, and updates all the corresponding other documents.
     *
     * @param family the family
     * @param pedigree to set
     * @param updatingUser right checks are done for this user
     * @throws PTException when the family could not be correctly and fully updated using the given pedigree
     */
    void setPedigree(Family family, Pedigree pedigree, User updatingUser) throws PTException;
}
