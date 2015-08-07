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
import org.phenotips.studies.family.Processing;
import org.phenotips.studies.family.Validation;
import org.phenotips.studies.family.internal2.Pedigree;
import org.phenotips.studies.family.internal2.StatusResponse2;

import org.xwiki.component.annotation.Component;
import org.xwiki.security.authorization.Right;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.xpn.xwiki.XWikiException;

import net.sf.json.JSON;
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
    public StatusResponse2 processPatientPedigree(String patientId, JSONObject json, String image)
        throws XWikiException
    {
        LogicInterDependantVariables variables = new LogicInterDependantVariables();
        Pedigree pedigree = new Pedigree(json, image);

        // Get proband
        variables.proband = this.patientRepository.getPatientById(patientId);
        if (variables.proband == null) {
            return StatusResponse2.INVALID_PATIENT_ID.setMessage(patientId);
        }

        // Get proband's family
        variables.family = this.familyRepository.getFamilyForPatient(variables.proband);

        // Get list of new members in pedigree/family
        List<String> newMembers = pedigree.extractIds();
        if (variables.family != null) {
            // sometimes pedigree passes in family document name as a member
            newMembers.remove(variables.family.getId());
        }

        // Edge case - empty list of new members
        if (newMembers.size() < 1) {
            return StatusResponse2.FAMILY_HAS_NO_MEMBERS;
        }

        if (ProcessingImpl.containsDuplicates(newMembers)) {
            return StatusResponse2.DUPLICATE_PATIENT;
        }

        // Edge case - proband with no family. Create a new one.
        if (variables.family == null) {
            if (!this.validation.hasPatientEditAccess(patientId)) {
                return StatusResponse2.INSUFFICIENT_PERMISSIONS_ON_PATIENT.setMessage(patientId);
            }
            variables.family = this.familyRepository.createFamily();
            variables.family.addMember(variables.proband);
        }

        // Checks that current user has edit permissions on family
        if (!this.validation.hasAccess(variables.family.getDocumentReference(), Right.EDIT))
        {
            return StatusResponse2.INSUFFICIENT_PERMISSIONS_ON_FAMILY;
        }

        variables = this.executeSaveUpdateLogic(variables, pedigree, newMembers);

        variables.family.updatePermissions();

        return variables.response;
    }

    private LogicInterDependantVariables executeSaveUpdateLogic(LogicInterDependantVariables variables,
        Pedigree pedigree, List<String> newMembers) throws XWikiException
    {
        StatusResponse2 response;


        // Check if every member of updatedMembers can be added to the family
        if (newMembers != null) {
            for (String patientId : newMembers) {
                Patient patient = this.patientRepository.getPatientById(patientId);
                response = this.familyRepository.canPatientBeAddedToFamily(patient, variables.family);
                if (!response.isValid()) {
                    variables.response = response;
                    return variables;
                }
            }
        }

        // Update patient data from pedigree's JSON
        StatusResponse2 updateFromJson = this.updatePatientsFromJson(pedigree.getData());
        if (!updateFromJson.isValid()) {
            variables.response = updateFromJson;
            return variables;
        }

        List<String> members = variables.family.getMembers();

        // storing first, because pedigree depends on this.
        variables.family.setPedigree(pedigree);

        // Removed members who are no longer in the family
        List<String> patientsToRemove = new LinkedList<>();
        patientsToRemove.addAll(members);
        patientsToRemove.removeAll(newMembers);
        for (String patientId : patientsToRemove) {
            Patient patient = this.patientRepository.getPatientById(patientId);
            variables.family.removeMember(patient);
        }

        // Add new members to family
        List<String> patientsToAdd = new LinkedList<>();
        patientsToRemove.addAll(newMembers);
        patientsToRemove.removeAll(members);
        for (String patientId : patientsToAdd) {
            Patient patient = this.patientRepository.getPatientById(patientId);
            variables.family.addMember(patient);
        }

        return variables;
    }

    /**
     * Used to pass around variables for logic heavy functions inside
     * {@link #processPatientPedigree(String, JSONObject, String)}.
     */
    private class LogicInterDependantVariables
    {
        protected StatusResponse2 response;

        protected Family family;

        protected Patient proband;
    }

    private StatusResponse2 updatePatientsFromJson(JSON familyContents)
    {
        String idKey = "id";
        try {
            JSONObject familyContentsObject = JSONObject.fromObject(familyContents);
            List<JSONObject> patientsJson = this.jsonAdapter.convert(familyContentsObject);

            for (JSONObject singlePatient : patientsJson) {
                if (singlePatient.containsKey(idKey)) {
                    Patient patient = this.patientRepository.getPatientById(singlePatient.getString(idKey));
                    patient.updateFromJSON(singlePatient);
                }
            }
        } catch (Exception ex) {
            return StatusResponse2.UNKNOWN_ERROR;
        }

        return StatusResponse2.OK;
    }

    private static boolean containsDuplicates(List<String> updatedMembers) {
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
