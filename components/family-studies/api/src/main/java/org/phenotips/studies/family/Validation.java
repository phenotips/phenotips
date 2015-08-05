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
import org.phenotips.studies.family.internal2.StatusResponse2;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;

import java.util.List;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Used for checking if actions, such as adding a certain patient to a family are valid.
 *
 * @version $Id$
 * @since 1.2RC1
 */
@Role
public interface Validation
{
    /**
     * Checks if the current user has edit access to a patient.
     *
     * @param patientId must not be null
     * @return true if has access; false otherwise
     */
    boolean hasPatientEditAccess(String patientId);

    /**
     * Checks if the current user has edit access to a patient.
     *
     * @param patient must not be null
     * @return true if has access; false otherwise
     */
    boolean hasPatientEditAccess(Patient patient);

    /**
     * Checks if the current user has edit access to a patient.
     *
     * @param patient must not be null
     * @param user which will access the patient
     * @return true if has access; false otherwise
     */
    boolean hasPatientEditAccess(Patient patient, User user);

    /**
     * Checks if current user has at least view access to a patient.
     *
     * @param patient must not be null
     * @return true if has access; false otherwise
     */
    boolean hasPatientViewAccess(Patient patient);

    /**
     * Checks if the user has at least view access to a patient.
     *
     * @param patient must not be null
     * @param user which will access the patient
     * @return true if has access; false otherwise
     */
    boolean hasPatientViewAccess(Patient patient, User user);

    /**
     * Checks if all `members` can be added to the `family`.
     *
     * @param family document to which the members will be added
     * @param members which are to be added
     * @return 200 status code if everything is ok, or one of the code that
     *         {@link #canAddToFamily(XWikiDocument, String)} returns
     */
    StatusResponse2 canAddEveryMember(XWikiDocument family, List<String> members);

    /**
     * Checks if a user has an access on a document with permissions.
     *
     * @param document document to check access for
     * @param permissions permissions current user needs to have on the family
     * @return true is user has an access on the family with permissions
     */
    boolean hasAccess(DocumentReference document, String permissions);

    /**
     * Checks if current user has an access on a document with a right.
     *
     * @param document document to check access for
     * @param right the right the current user needs to have on the document
     * @return true is user has an access on the document with the right
     */
    boolean hasAccess(DocumentReference document, Right right);

}
