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
import org.phenotips.studies.family.JsonAdapter;
import org.phenotips.studies.family.Pedigree;
import org.phenotips.studies.family.response.JSONResponse;
import org.phenotips.studies.family.response.StatusResponse;

import org.xwiki.component.annotation.Component;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.UserManager;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import net.sf.json.JSONObject;

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
    private JsonAdapter jsonAdapter;

    /**
     * Receives a pedigree in form of a JSONObject and an SVG image to be stored in proband's family.
     *
     * @param documentId an id of a family or of a proband. Used to get a handle of the family to process the pedigree
     *            for. If it's a proband id, the family assocaited with the patient is used. If not family is
     *            associated, a new one is created.
     * @param json (data) part of the pedigree JSON
     * @param image svg part of the pedigree JSON
     * @return {@link JSONResponse} with one of many possible statuses
     */
    public JSONResponse processPedigree(String documentId, JSONObject json, String image)
    {
        Pedigree pedigree = new DefaultPedigree(json, image);

        // checking if documentId is family's id
        Family family = this.familyRepository.getFamilyById(documentId);
        if (family != null) {
            return processPedigree(family, pedigree);
        }

        // checking if documentId is patient's id
        Patient proband = this.patientRepository.getPatientById(documentId);
        if (proband != null) {
            family = this.familyRepository.getFamilyForPatient(proband);
            if (family == null) {
                if (!this.authorizationService.hasAccess(
                        this.userManager.getCurrentUser(), Right.EDIT, proband.getDocument()))
                {
                    return new JSONResponse(StatusResponse.INSUFFICIENT_PERMISSIONS_ON_PATIENT).setMessage(documentId);
                }
                family = this.familyRepository.createFamily();
                family.addMember(proband);
            }
            return processPedigree(family, pedigree);
        }

        // documentId is not family's or patient's.
        return new JSONResponse(StatusResponse.INVALID_PATIENT_ID).setMessage(documentId);
    }

    /*
     * @param family not null
     * @param pedigree not null
     * @return
     */
    private JSONResponse processPedigree(Family family, Pedigree pedigree)
    {
        StatusResponse response;

        // sometimes pedigree passes in family document name as a member
        List<String> newMembers = pedigree.extractIds();
        newMembers.remove(family.getId());

        JSONResponse validityResponse = checkValidity(family, newMembers);
        if (!validityResponse.isValid()) {
            return validityResponse;
        }

        // Update patient data from pedigree's JSON
        response = this.updatePatientsFromJson(pedigree);
        if (!response.isValid()) {
            return new JSONResponse(response);
        }

        family.setPedigree(pedigree);

        List<String> members = family.getMembersIds();

        // Removed members who are no longer in the family
        List<String> patientsToRemove = new LinkedList<>();
        patientsToRemove.addAll(members);
        patientsToRemove.removeAll(newMembers);
        for (String patientId : patientsToRemove) {
            Patient patient = this.patientRepository.getPatientById(patientId);
            family.removeMember(patient);
        }

        // Add new members to family
        List<String> patientsToAdd = new LinkedList<>();
        patientsToAdd.addAll(newMembers);
        patientsToAdd.removeAll(members);
        for (String patientId : patientsToAdd) {
            Patient patient = this.patientRepository.getPatientById(patientId);
            family.addMember(patient);
        }

        family.updatePermissions();

        return new JSONResponse(StatusResponse.OK);
    }

    private JSONResponse checkValidity(Family family, List<String> newMembers)
    {
        JSONResponse jsonResponse = new JSONResponse();

        // Checks that current user has edit permissions on family
        if (!this.authorizationService.hasAccess(
                this.userManager.getCurrentUser(), Right.EDIT, family.getDocumentReference()))
        {
            return jsonResponse.setStatusResponse(StatusResponse.INSUFFICIENT_PERMISSIONS_ON_FAMILY);
        }

        // Edge case - empty list of new members
        if (newMembers.size() < 1) {
            return jsonResponse.setStatusResponse(StatusResponse.FAMILY_HAS_NO_MEMBERS);
        }

        if (this.containsDuplicates(newMembers)) {
            return jsonResponse.setStatusResponse(StatusResponse.DUPLICATE_PATIENT);
        }

        // Check if every member of updatedMembers can be added to the family
        if (newMembers != null) {
            for (String patientId : newMembers) {
                Patient patient = this.patientRepository.getPatientById(patientId);
                JSONResponse response = this.familyRepository.canPatientBeAddedToFamily(patient, family);
                if (!response.isValid()) {
                    return jsonResponse;
                }
            }
        }

        jsonResponse.setStatusResponse(StatusResponse.OK);
        return jsonResponse;
    }

    private StatusResponse updatePatientsFromJson(Pedigree pedigree)
    {
        String idKey = "id";
        try {
            List<JSONObject> patientsJson = this.jsonAdapter.convert(pedigree);

            for (JSONObject singlePatient : patientsJson) {
                if (singlePatient.containsKey(idKey)) {
                    Patient patient = this.patientRepository.getPatientById(singlePatient.getString(idKey));
                    patient.updateFromJSON(singlePatient);
                }
            }
        } catch (Exception ex) {
            return StatusResponse.UNKNOWN_ERROR;
        }

        return StatusResponse.OK;
    }

    private boolean containsDuplicates(List<String> updatedMembers)
    {
        List<String> duplicationCheck = new LinkedList<>();
        duplicationCheck.addAll(updatedMembers);
        for (String member : updatedMembers) {
            duplicationCheck.remove(member);
            if (duplicationCheck.contains(member)) {
                return true;
            }
        }

        return false;
    }

}
