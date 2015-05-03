/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.studies.family;

import org.phenotips.studies.family.internal.StatusResponse;

import org.xwiki.component.annotation.Role;

import com.xpn.xwiki.XWikiException;
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
     * A wrapper around {@link #canAddToFamily(XWikiDocument, String)}.
     *
     * @param familyAnchor resolved to a family document using {@link FamilyUtils#getFamilyDoc(XWikiDocument)}
     * @param patientId {@see #canAddToFamily(XWikiDocument, String)}
     * @return {@see #canAddToFamily}
     * @throws XWikiException one of numerous XWiki exceptions
     */
    StatusResponse canAddToFamily(String familyAnchor, String patientId) throws XWikiException;

    /**
     * Runs several different checks to determine if a patient is eligible to be added as a member to a family document.
     *
     * @param familyDoc to which the patient will be potentially added to
     * @param patientId patient id who will potentially be added to the family
     * @return a non-null {@link StatusResponse}
     * @throws XWikiException one of numerous XWiki exceptions
     */
    StatusResponse canAddToFamily(XWikiDocument familyDoc, String patientId)
        throws XWikiException;

    /**
     * Checks if the current user has edit access to the family.
     *
     * @param familyDoc must not be null
     * @return a {@link StatusResponse} with a status of 200 (ok) or 401 (insufficient permissions)
     */
    StatusResponse checkFamilyAccessWithResponse(XWikiDocument familyDoc);

    /**
     * Checks if the current user has edit access to a patient.
     *
     * @param patientDoc must not be null
     * @return true if has access; false otherwise
     */
    boolean hasPatientEditAccess(XWikiDocument patientDoc);
}
