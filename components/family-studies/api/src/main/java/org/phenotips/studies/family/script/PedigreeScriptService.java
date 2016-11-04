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
import org.phenotips.studies.family.FamilyTools;
import org.phenotips.studies.family.Pedigree;
import org.phenotips.studies.family.exceptions.PTException;
import org.phenotips.studies.family.exceptions.PTInvalidFamilyIdException;
import org.phenotips.studies.family.exceptions.PTInvalidPatientIdException;
import org.phenotips.studies.family.exceptions.PTNotEnoughPermissionsOnFamilyException;
import org.phenotips.studies.family.exceptions.PTNotEnoughPermissionsOnPatientException;
import org.phenotips.studies.family.exceptions.PTPatientAlreadyInAnotherFamilyException;
import org.phenotips.studies.family.exceptions.PTPatientNotInFamilyException;
import org.phenotips.studies.family.exceptions.PTPedigreeContainesSamePatientMultipleTimesException;
import org.phenotips.studies.family.internal.DefaultPedigree;
import org.phenotips.studies.family.script.response.AlreadyHasFamilyResponse;
import org.phenotips.studies.family.script.response.FamilyInfoJSONResponse;
import org.phenotips.studies.family.script.response.InternalErrorResponse;
import org.phenotips.studies.family.script.response.InvalidFamilyIdResponse;
import org.phenotips.studies.family.script.response.InvalidInputJSONResponse;
import org.phenotips.studies.family.script.response.InvalidPatientIdResponse;
import org.phenotips.studies.family.script.response.NotEnoughPermissionsOnFamilyResponse;
import org.phenotips.studies.family.script.response.NotEnoughPermissionsOnPatientResponse;
import org.phenotips.studies.family.script.response.PatientContainedMultipleTimesInPedigreeResponse;
import org.phenotips.studies.family.script.response.PatientHasNoFamilyResponse;
import org.phenotips.studies.family.script.response.ValidLinkJSONResponse;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * Script service for working with families. All methods assume actions are performed by current user and do
 * corresponding permission checks.
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
    private PatientRepository patientRepository;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private UserManager userManager;

    @Inject
    private FamilyTools familyTools;

    @Inject
    private Logger logger;

    /**
     * Gets a family id or patient id. If the id is a patient id, finds the patient's family and returns a family
     * information JSON. If the id is a family id, returns the family information.
     *
     * @param documentId PhenoTips family id or PhenoTips patient id
     * @return JSONResponse with information about the family. Returns null if no family is defined.
     */
    public JSONResponse getFamilyAndPedigree(String documentId)
    {
        try {
            Family family = null;

            User currentUser = this.userManager.getCurrentUser();

            Patient patient = this.patientRepository.get(documentId);
            if (patient != null) {
                if (!this.authorizationService.hasAccess(currentUser, Right.VIEW, patient.getDocument())) {
                    return new NotEnoughPermissionsOnPatientResponse(Arrays.asList(documentId), Right.VIEW);
                }
                // id belonged to a patient. Get patient's family
                family = this.familyTools.getFamilyForPatient(documentId);
                if (family == null) {
                    return new PatientHasNoFamilyResponse();
                }
            } else {
                // id is not a patient's id. Check if it is a family id
                family = this.familyTools.getFamilyById(documentId);
                if (family == null) {
                    return new InvalidFamilyIdResponse();
                }
            }
            if (!this.authorizationService.hasAccess(currentUser, Right.VIEW, family.getDocumentReference())) {
                return new NotEnoughPermissionsOnFamilyResponse();
            }
            return new FamilyInfoJSONResponse(family);
        } catch (Exception ex) {
            return this.convertExceptionIntoJSONResponse(ex);
        }
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
        try {
            if (!this.familyTools.familyExists(familyId)) {
                return new InvalidFamilyIdResponse();
            }

            Family family = this.familyTools.getFamilyById(familyId);
            Patient patient = this.patientRepository.get(patientToLinkId);

            // an exception will be thrown in case of any errors
            this.familyTools.canAddToFamily(family, patient, true);

            return new ValidLinkJSONResponse();
        } catch (Exception ex) {
            return this.convertExceptionIntoJSONResponse(ex);
        }
    }

    /**
     * Performs several operations on the passed in data, and eventually saves it into appropriate documents.
     *
     * @param familyId Phenotips family id. If null, a new family is created.
     * @param json part of the pedigree data
     * @param image svg part of the pedigree data
     * @return {@link JSONResponse} with 'error' field set to {@code false} if everything is ok, or {@code true} if an
     *         error has occurred. In case the linking is invalid, the JSON will also contain {@code errorMessage} and
     *         {@code errorType}. In case of success JSONResponse will be of {@link FamilyInfoJSONResponse} type
     */
    public JSONResponse savePedigree(String familyId, String json, String image)
    {
        try {
            if (!this.familyTools.familyExists(familyId)) {
                return new InvalidFamilyIdResponse();
            }

            if (!this.familyTools.currentUserHasAccessRight(familyId, Right.EDIT)) {
                return new NotEnoughPermissionsOnFamilyResponse();
            }

            Family family = this.familyTools.getFamilyById(familyId);

            JSONObject pedigreeJSON;
            try {
                pedigreeJSON = new JSONObject(json);
            } catch (JSONException ex) {
                return new InvalidInputJSONResponse(json);
            }

            Pedigree pedigree = new DefaultPedigree(pedigreeJSON, image);

            this.familyTools.setPedigree(family, pedigree);

            return new FamilyInfoJSONResponse(family);
        } catch (Exception ex) {
            return this.convertExceptionIntoJSONResponse(ex);
        }
    }

    private JSONResponse convertExceptionIntoJSONResponse(Exception ex)
    {
        if (ex instanceof PTException) {

            if (ex instanceof PTInvalidFamilyIdException) {
                return new InvalidFamilyIdResponse();
            }
            if (ex instanceof PTInvalidPatientIdException) {
                return new InvalidPatientIdResponse(((PTInvalidPatientIdException) ex).getWrongId());
            }
            if (ex instanceof PTPatientNotInFamilyException) {
                return new PatientHasNoFamilyResponse();
            }
            if (ex instanceof PTPedigreeContainesSamePatientMultipleTimesException) {
                return new PatientContainedMultipleTimesInPedigreeResponse(
                    ((PTPedigreeContainesSamePatientMultipleTimesException) ex).getPatientId());
            }
            if (ex instanceof PTNotEnoughPermissionsOnFamilyException) {
                return new NotEnoughPermissionsOnFamilyResponse();
            }
            if (ex instanceof PTNotEnoughPermissionsOnPatientException) {
                List<String> patientIdList = new LinkedList<>();
                patientIdList.add(((PTNotEnoughPermissionsOnPatientException) ex).getDocumentId());
                return new NotEnoughPermissionsOnPatientResponse(patientIdList,
                    ((PTNotEnoughPermissionsOnPatientException) ex).getMissingPermission());
            }
            if (ex instanceof PTPatientAlreadyInAnotherFamilyException) {
                String patientId = ((PTPatientAlreadyInAnotherFamilyException) ex).getPatientId();
                String otherFamilyID = ((PTPatientAlreadyInAnotherFamilyException) ex).getOtherFamilyId();
                return new AlreadyHasFamilyResponse(patientId, otherFamilyID);
            }
        }
        // for all other exceptions there are no custom JSON responses
        this.logger.error("Error in PedigreeScriptService: []", ex);
        return new InternalErrorResponse(ex.getMessage());
    }
}
