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
package org.phenotips.studies.family.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;
import org.phenotips.studies.family.internal2.StatusResponse2;

import org.xwiki.component.annotation.Component;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * In order to remove Validation.CanAddToFamily, the calls to this function need to be redirected to
 * FamilyRepository.CanPatientBeAddedToFamily. However, to call the latter, multiple additional classes need to be used,
 * which increases the complexity level of the classes where the call is performed. This class offers a temporary
 * solution for this problem.
 *
 * @version $Id$
 */
@Component(roles = { CanAllPatientsBeAddedAdapter.class })
@Singleton
public class CanAllPatientsBeAddedAdapter
{
    @Inject
    private FamilyRepository familyRepository;

    @Inject
    private PatientRepository patientRepository;

    /**
     * See {@link Validation.CanAddEveryMember} Checks if all `members` can be added to the `family`.
     *
     * @param xfamily document to which the members will be added
     * @param members which are to be added
     * @return 200 status code if everything is ok, or one of the code that
     *         {@link #canAddToFamily(XWikiDocument, String)} returns
     * @throws XWikiException if {@link #canAddToFamily(XWikiDocument, String)} fails
     */
    public StatusResponse canAddEveryMember(XWikiDocument xfamily, List<String> members)
        throws XWikiException
    {
        String familyId = xfamily.getDocumentReference().getName();
        Family family = this.familyRepository.getFamilyById(familyId);

        if (members != null) {
            for (String patientId : members) {
                Patient patient = this.patientRepository.getPatientById(patientId);
                StatusResponse2 response =
                    this.familyRepository.canPatientBeAddedToFamily(patient, family);
                if (response != StatusResponse2.CAN_BE_ADDED) {
                    StatusResponse individualAccess = new StatusResponse();
                    individualAccess.errorType = response.getErrorType();
                    individualAccess.message = response.getMessage();
                    individualAccess.statusCode = response.getStatusCode();
                    return individualAccess;
                }
            }
        }

        StatusResponse response = new StatusResponse();
        response.statusCode = 200;
        return response;
    }
}
