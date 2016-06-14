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
package org.phenotips.studies.family.script;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.security.authorization.AuthorizationService;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;
import org.phenotips.studies.family.internal.PedigreeUtils;
import org.phenotips.studies.family.script.response.FamilyInfoJSONResponse;
import org.phenotips.studies.family.script.response.InvalidFamilyIdResponse;
import org.phenotips.studies.family.script.response.InvalidInputJSONResponse;
import org.phenotips.studies.family.script.response.NotEnoughPermissionsOnFamilyResponse;
import org.phenotips.studies.family.script.response.NotEnoughPermissionsOnPatientResponse;
import org.phenotips.studies.family.script.response.PatientHasNoFamilyResponse;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Script service for working with families. All methods assume actions are performed by current user and do
 * corresponding permision checks.
 *
 * @version $Id$
 * @since 1.2RC1
 */
@Component
@Singleton
@Named("pedigrees")
public class PedigreeScriptService implements ScriptService
{
    @Inject
    private FamilyRepository familyRepository;

    @Inject
    private PatientRepository patientRepository;

    @Inject
    private PedigreeUtils pedigreeUtils;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private UserManager userManager;

    /**
     * Gets a family id or patient id. If the id is a patient id, finds the patient's family and returns a family
     * information JSON. If the id is a family id, returns the family information.
     *
     * @param documentId PhenoTips family id or PhenoTips patient id
     * @return JSONResponse with information about the family. Returns null if no family is defined.
     */
    public JSONResponse getFamilyAndPedigree(String documentId)
    {
        Family family = null;

        User currentUser = this.userManager.getCurrentUser();

        Patient patient = this.patientRepository.getPatientById(documentId);
        if (patient != null) {
            if (!this.authorizationService.hasAccess(currentUser, Right.VIEW, patient.getDocument())) {
                return new NotEnoughPermissionsOnPatientResponse(Arrays.asList(documentId), Right.VIEW);
            }
            // id belonged to a patient. Get patient's family
            family = this.familyRepository.getFamilyForPatient(patient);
            if (family == null) {
                return new PatientHasNoFamilyResponse();
            }
        } else {
            // id is not a patient's id. Check if it is a family id
            family = this.familyRepository.getFamilyById(documentId);
            if (family == null) {
                return new InvalidFamilyIdResponse();
            }
        }
        if (!this.authorizationService.hasAccess(currentUser, Right.VIEW, family.getDocumentReference())) {
            return new NotEnoughPermissionsOnFamilyResponse();
        }

        return new FamilyInfoJSONResponse(family);
    }

    /**
     * Checks if a patient can be linked to a family. The id of the patient to link is patientItLinkId. The family is
     * given by documentId: If document is a family id, it is read directly by its id. If it's a patient id, the family
     * is the one associated with the patient.
     *
     * @param familyId a valid family id
     * @param patientToLinkId id of a patient to link to family
     * @return JSONObject see {@link JSONResponse}
     */
    public JSONResponse canPatientBeLinked(String familyId, String patientToLinkId)
    {
        Family family = this.familyRepository.getFamilyById(familyId);
        if (family != null) {
            if (!this.authorizationService.hasAccess(
                this.userManager.getCurrentUser(), Right.EDIT, family.getDocumentReference())) {
                return new NotEnoughPermissionsOnFamilyResponse();
            }
        }
        // else: if family == null mewans linking to new family, which will be creatd by current user and thus
        // current user will have edit rights
        return this.pedigreeUtils.canPatientBeAddedToFamily(family, patientToLinkId, true);
    }

    /**
     * Performs several operations on the passed in data, and eventually saves it into appropriate documents.
     *
     * @param familyId Phenotips family id. If null, a new family is created.
     * @param json part of the pedigree data
     * @param image svg part of the pedigree data
     * @return {@link JSON} with 'error' field set to {@link false} if everything is ok, or {@link false} if a known
     *         error has occurred. In case the linking is invalid, the JSON will also contain 'errorMessage' and
     *         'errorType'
     */
    public JSONResponse savePedigree(String familyId, String json, String image)
    {
        try {
            JSONObject pedigreeJSON = new JSONObject(json);
            return this.pedigreeUtils.savePedigree(familyId, pedigreeJSON, image, true);
        } catch (JSONException ex) {
            return new InvalidInputJSONResponse(json);
        }
    }
}
