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
import org.phenotips.studies.family.JsonAdapter;
import org.phenotips.studies.family.Pedigree;
import org.phenotips.studies.family.Processing;
import org.phenotips.studies.family.Validation;
import org.phenotips.studies.family.response.JSONResponse;
import org.phenotips.studies.family.response.StatusResponse;

import org.xwiki.component.annotation.Component;
import org.xwiki.security.authorization.Right;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.xpn.xwiki.XWikiException;

import net.sf.json.JSONObject;

/**
 * Storage and retrieval.
 *
 * @version $Id$
 * @since 1.2RC1
 */
@Component
@Singleton
public class ProcessingImpl implements Processing
{
    @Inject
    private PatientRepository patientRepository;

    @Inject
    private FamilyRepository familyRepository;

    @Inject
    private Validation validation;

    @Inject
    private JsonAdapter jsonAdapter;

    @Override
    public JSONResponse processPatientPedigree(String patientId, JSONObject json, String image)
        throws XWikiException
    {
        Pedigree pedigree = new Pedigree(json, image);

        // Get proband
        Patient proband = this.patientRepository.getPatientById(patientId);
        if (proband == null) {
            return new JSONResponse(StatusResponse.INVALID_PATIENT_ID).setMessage(patientId);
        }

        // Get proband's family
        Family family = this.familyRepository.getFamilyForPatient(proband);

        // Get list of new members in pedigree/family
        List<String> newMembers = pedigree.extractIds();
        if (family != null) {
            // sometimes pedigree passes in family document name as a member
            newMembers.remove(family.getId());
        }

        // Edge case - proband with no family. Create a new one.
        if (family == null) {
            if (!this.validation.hasPatientEditAccess(patientId)) {
                return new JSONResponse(StatusResponse.INSUFFICIENT_PERMISSIONS_ON_PATIENT).setMessage(patientId);
            }
            family = this.familyRepository.createFamily();
            family.addMember(proband);
        }

        JSONResponse response = checkValidity(family, newMembers);
        if (!response.isValid()) {
            return response;
        }

        return this.processPatientPedigree(family, pedigree, newMembers);
    }

    private JSONResponse checkValidity(Family family, List<String> newMembers)
    {
        JSONResponse jsonResponse = new JSONResponse();

        // Checks that current user has edit permissions on family
        if (!this.validation.hasAccess(family.getDocumentReference(), Right.EDIT))
        {
            return jsonResponse.setStatusResponse(StatusResponse.INSUFFICIENT_PERMISSIONS_ON_FAMILY);
        }

        // Edge case - empty list of new members
        if (newMembers.size() < 1) {
            return jsonResponse.setStatusResponse(StatusResponse.FAMILY_HAS_NO_MEMBERS);
        }

        if (ProcessingImpl.containsDuplicates(newMembers)) {
            return jsonResponse.setStatusResponse(StatusResponse.DUPLICATE_PATIENT);
        }

        // Check if every member of updatedMembers can be added to the family
        if (newMembers != null) {
            for (String patientId : newMembers) {
                Patient patient = this.patientRepository.getPatientById(patientId);
                StatusResponse response = this.familyRepository.canPatientBeAddedToFamily(patient, family);
                if (!response.isValid()) {
                    jsonResponse.setStatusResponse(response);
                    jsonResponse.setMessage(patientId, family.getId());
                    return jsonResponse;
                }
            }
        }

        jsonResponse.setStatusResponse(StatusResponse.OK);
        return jsonResponse;
    }

    private JSONResponse processPatientPedigree(Family family, Pedigree pedigree, List<String> newMembers)
        throws XWikiException
    {
        StatusResponse response;

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

    private static boolean containsDuplicates(List<String> updatedMembers)
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
