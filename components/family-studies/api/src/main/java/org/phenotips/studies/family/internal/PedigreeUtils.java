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
import org.phenotips.security.authorization.AuthorizationService;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;
import org.phenotips.studies.family.Pedigree;
import org.phenotips.studies.family.PedigreeProcessor;
import org.phenotips.studies.family.script.JSONResponse;
import org.phenotips.studies.family.script.response.AlreadyHasFamilyResponse;
import org.phenotips.studies.family.script.response.FamilyInfoJSONResponse;
import org.phenotips.studies.family.script.response.InternalErrorResponse;
import org.phenotips.studies.family.script.response.InvalidFamilyIdResponse;
import org.phenotips.studies.family.script.response.InvalidPatientIdResponse;
import org.phenotips.studies.family.script.response.NotEnoughPermissionsOnFamilyResponse;
import org.phenotips.studies.family.script.response.NotEnoughPermissionsOnPatientResponse;
import org.phenotips.studies.family.script.response.OKJSONResponse;
import org.phenotips.studies.family.script.response.PatientContainedMultipleTimesInPedigreeResponse;
import org.phenotips.studies.family.script.response.ValidLinkJSONResponse;

import org.xwiki.component.annotation.Component;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.json.JSONObject;

/**
 * Processes pedigree from UI.
 *
 * @version $Id$
 * @since 1.2RC1
 */
@Component(roles = { PedigreeUtils.class })
@Singleton
public class PedigreeUtils
{
    @Inject
    private PatientRepository patientRepository;

    @Inject
    private FamilyRepository familyRepository;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private UserManager userManager;

    @Inject
    private PedigreeProcessor pedigreeConverter;

    /**
     * Checks if a patient can be linked to a family. The id of the patient to link is patientItLinkId.
     *
     * @param family a valid family object
     * @param patientToLinkId id of a patient to link to family
     * @param useCurrentUser if true, permission checks for the current user are performed
     * @return JSONResponse see {@link JSONResponse}
     */
    public JSONResponse canPatientBeAddedToFamily(Family family, String patientToLinkId, boolean useCurrentUser)
    {
        Patient patient = this.patientRepository.getPatientById(patientToLinkId);
        if (patient == null) {
            return new InvalidPatientIdResponse(patientToLinkId);
        }
        if (useCurrentUser
            && !this.authorizationService.hasAccess(
                this.userManager.getCurrentUser(), Right.EDIT, patient.getDocument())) {
            return new NotEnoughPermissionsOnPatientResponse(Arrays.asList(patientToLinkId), Right.EDIT);
        }
        Family familyForLinkedPatient = familyRepository.getFamilyForPatient(patient);
        if (familyForLinkedPatient != null) {
            if (family == null || !familyForLinkedPatient.getId().equals(family.getId())) {
                return new AlreadyHasFamilyResponse(patientToLinkId, familyForLinkedPatient.getId());
            }
        }
        return new ValidLinkJSONResponse();
    }

    /**
     * Receives a pedigree in form of a JSONObject and an SVG image to be stored in proband's family.
     *
     * @param familyId phenotips family id
     * @param json (data) part of the pedigree JSON
     * @param image svg part of the pedigree JSON
     * @param useCurrentUser if true, checks will be made to make sure current user has enough permissions
     * @return {@link JSONResponse} with one of many possible statuses
     */
    public JSONResponse savePedigree(String familyId, JSONObject json, String image, boolean useCurrentUser)
    {
        Pedigree pedigree = new DefaultPedigree(json, image);
        // saving into a new family
        if (familyId == null || familyId.length() == 0) {
            return processPedigree(this.familyRepository.createFamily(), pedigree, useCurrentUser, true);
        }
        // saving into existing family
        Family family = this.familyRepository.getFamilyById(familyId);
        if (family != null) {
            return processPedigree(family, pedigree, useCurrentUser, false);
        }
        return new InvalidFamilyIdResponse();
    }

    /*
     * @param family not null
     * @param pedigree not null
     * @return
     */
    private synchronized JSONResponse processPedigree(Family family, Pedigree pedigree, boolean useCurrentUser,
        boolean updateIDFromProband)
    {
        List<String> oldMembers = family.getMembersIds();

        List<String> currentMembers = pedigree.extractIds();

        // Add new members to family
        List<String> patientsToAdd = new LinkedList<>();
        patientsToAdd.addAll(currentMembers);
        patientsToAdd.removeAll(oldMembers);

        JSONResponse validity = checkValidity(family, patientsToAdd, useCurrentUser);
        if (validity.isErrorResponse()) {
            return validity;
        }

        // Update patient data from pedigree's JSON
        JSONResponse result = this.updatePatientsFromJson(pedigree, useCurrentUser);
        if (result.isErrorResponse()) {
            return result;
        }

        family.setPedigree(pedigree);

        if (updateIDFromProband) {
            // default family identifier to proband last name
            this.updateFamilyExternalId(family);
        }

        // Removed members who are no longer in the family
        List<String> patientsToRemove = new LinkedList<>();
        patientsToRemove.addAll(oldMembers);
        patientsToRemove.removeAll(currentMembers);
        for (String patientId : patientsToRemove) {
            Patient patient = this.patientRepository.get(patientId);
            family.removeMember(patient);
        }

        for (String patientId : patientsToAdd) {
            Patient patient = this.patientRepository.get(patientId);
            family.addMember(patient);
        }

        family.updatePermissions();

        return new FamilyInfoJSONResponse(family);
    }

    private JSONResponse checkValidity(Family family, List<String> newMembers, boolean useCurrentUser)
    {
        // Checks that current user has edit permissions on family
        if (useCurrentUser && !this.authorizationService.hasAccess(
            this.userManager.getCurrentUser(), Right.EDIT, family.getDocumentReference()))
        {
            return new NotEnoughPermissionsOnFamilyResponse();
        }

        // TODO: do we care?
        // Edge case - empty list of new members
        // if (newMembers.size() < 1) {
        // return ...
        // }

        String duplicateID = this.findDuplicate(newMembers);
        if (duplicateID != null) {
            return new PatientContainedMultipleTimesInPedigreeResponse(duplicateID);
        }

        // Check if every member of updatedMembers can be added to the family
        if (newMembers != null) {
            for (String patientId : newMembers) {
                JSONResponse response = canPatientBeAddedToFamily(family, patientId, useCurrentUser);
                if (response.isErrorResponse()) {
                    return response;
                }
            }
        }
        return new OKJSONResponse();
    }

    private JSONResponse updatePatientsFromJson(Pedigree pedigree, boolean useCurrentUser)
    {
        User currentUser = this.userManager.getCurrentUser();
        String idKey = "id";
        try {
            List<JSONObject> patientsJson = this.pedigreeConverter.convert(pedigree);

            for (JSONObject singlePatient : patientsJson) {
                if (singlePatient.has(idKey)) {
                    Patient patient = this.patientRepository.get(singlePatient.getString(idKey));
                    if (useCurrentUser
                        && !this.authorizationService.hasAccess(currentUser, Right.EDIT, patient.getDocument())) {
                        // skip patients the current user does not have edit rights for
                        continue;
                    }
                    patient.updateFromJSON(singlePatient);
                }
            }
        } catch (Exception ex) {
            return new InternalErrorResponse(ex.getMessage());
        }
        return new OKJSONResponse();
    }

    private String findDuplicate(List<String> updatedMembers)
    {
        List<String> duplicationCheck = new LinkedList<>();
        duplicationCheck.addAll(updatedMembers);
        for (String member : updatedMembers) {
            duplicationCheck.remove(member);
            if (duplicationCheck.contains(member)) {
                return member;
            }
        }

        return null;
    }

    private void updateFamilyExternalId(Family family)
    {
        String probandId = family.getProbandId();
        if (probandId != null) {
            Patient patient = this.patientRepository.get(probandId);
            if (patient != null) {
                String lastName = patient.<String>getData("patientName").get("last_name");
                if (!lastName.isEmpty()) {
                    family.setExternalId(lastName);
                }
            }
        }
    }

}
