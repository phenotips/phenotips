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
import org.phenotips.studies.family.FamilyUtils;
import org.phenotips.studies.family.JsonAdapter;
import org.phenotips.studies.family.Processing;
import org.phenotips.studies.family.Validation;
import org.phenotips.studies.family.internal2.Pedigree;
import org.phenotips.studies.family.internal2.StatusResponse2;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.QueryException;
import org.xwiki.security.authorization.Right;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.naming.NamingException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

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
    private Logger logger;

    @Inject
    private PatientRepository patientRepository;

    @Inject
    private FamilyRepository familyRepository;

    @Inject
    private FamilyUtils familyUtils;

    @Inject
    private Provider<XWikiContext> provider;

    @Inject
    private Validation validation;

    @Inject
    private JsonAdapter jsonAdapter;

    @Override
    public StatusResponse2 processPatientPedigree(String patientId, JSONObject json, String image)
        throws XWikiException, NamingException, QueryException
    {
        LogicInterDependantVariables variables = new LogicInterDependantVariables();
        variables.json = json;
        variables.image = image;

        // Get proband
        variables.proband = this.patientRepository.getPatientById(patientId);
        if (variables.proband == null) {
            return StatusResponse2.INVALID_PATIENT_ID.setMessage(patientId);
        }

        // Get proband's family
        variables.family = this.familyRepository.getFamilyForPatient(variables.proband);

        // Get list of new members in pedigree/family
        variables.updatedMembers = PedigreeUtils.extractIdsFromPedigree(json);
        if (variables.family != null) {
            // sometimes pedigree passes in family document name as a member
            variables.updatedMembers.remove(variables.family.getId());
        }
        variables.updatedMembers = Collections.unmodifiableList(variables.updatedMembers);

        // Edge case - empty list of new members
        if (variables.updatedMembers.size() < 1) {
            return StatusResponse2.FAMILY_HAS_NO_MEMBERS;
        }

        // Edge case - proband with no family. Create a new one.
        if (variables.family == null) {
            if (!this.validation.hasPatientEditAccess(patientId)) {
                return StatusResponse2.INSUFFICIENT_PERMISSIONS_ON_PATIENT.setMessage(patientId);
            }
            variables.family = this.familyRepository.createFamily();
            variables.family.addMember(variables.proband);
            variables.isNew = true;
        }

        // Checks that current user has edit permissions on family
        if (!this.validation.hasAccess(variables.family.getDocumentReference(), Right.EDIT))
        {
            return StatusResponse2.INSUFFICIENT_PERMISSIONS_ON_FAMILY;
        }

        variables.anchorRef = variables.proband.getDocument();
        variables.anchorDoc = this.familyUtils.getDoc(variables.anchorRef);
        variables.familyDoc = this.familyUtils.getFamilyDoc(variables.anchorDoc);

        variables.members = variables.family.getMembers();

        StatusResponse2 duplicationStatus = ProcessingImpl.checkForDuplicates(variables.updatedMembers);
        if (!duplicationStatus.isValid()) {
            return duplicationStatus;
        }

        variables = this.executeSaveUpdateLogic(variables);

        variables.family.updatePermissions();

        return variables.response;
    }

    private LogicInterDependantVariables executeSaveUpdateLogic(LogicInterDependantVariables variables)
        throws XWikiException
    {
        StatusResponse2 response;

        // Check if every member of updatedMembers can be added to the family
        if (variables.updatedMembers != null) {
            for (String patientId : variables.updatedMembers) {
                Patient patient = this.patientRepository.getPatientById(patientId);
                response = this.familyRepository.canPatientBeAddedToFamily(patient, variables.family);
                if (!response.isValid()) {
                    variables.response = response;
                    return variables;
                }
            }
        }

        StatusResponse2 updateFromJson = this.updatePatientsFromJson(variables.json);
        if (!updateFromJson.isValid()) {
            variables.response = updateFromJson;
            return variables;
        }
        // storing first, because pedigree depends on this.
        Pedigree pedigree = new Pedigree(variables.json, variables.image);
        variables.family.setPedigree(pedigree);

        if (!variables.isNew) {
            this.removeMembersNotPresent(variables.members, variables.updatedMembers);
        }
        this.addNewMembers(variables.members, variables.updatedMembers, variables.familyDoc);
        // remove and add do not take care of modifying the 'members' property
        this.familyUtils.setFamilyMembers(variables.familyDoc, variables.updatedMembers);

        return variables;
    }

    /**
     * Used to pass around variables for logic heavy functions inside
     * {@link #processPatientPedigree(String, JSONObject, String)}.
     */
    private class LogicInterDependantVariables
    {
        protected StatusResponse2 response;

        protected JSONObject json;

        protected String image;

        protected XWikiDocument familyDoc;

        protected XWikiDocument anchorDoc;

        protected Family family;

        protected Patient proband;

        protected DocumentReference anchorRef;

        protected List<String> updatedMembers = new LinkedList<>();

        protected List<String> members = new LinkedList<>();

        protected boolean isNew;
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

    /**
     * Removes records from the family that are no longer in the updated family structure.
     */
    private void removeMembersNotPresent(List<String> currentMembers, List<String> updatedMembers)
        throws XWikiException
    {
        List<String> toRemove = new LinkedList<>();
        toRemove.addAll(currentMembers);
        toRemove.removeAll(updatedMembers);
        if (!toRemove.isEmpty()) {
            XWikiContext context = this.provider.get();
            XWiki wiki = context.getWiki();
            for (String oldMemberId : toRemove) {
                this.removeMember(oldMemberId, wiki, context);
            }
        }
    }

    @Override
    public void removeMember(String id, XWiki wiki, XWikiContext context) throws XWikiException
    {
        Patient patient = this.patientRepository.getPatientById(id);
        XWikiDocument patientDoc = null;
        BaseObject familyRefObj = null;
        if (patient != null) {
            patientDoc = wiki.getDocument(patient.getDocument(), context);
            familyRefObj = patientDoc.getXObject(FamilyUtils.FAMILY_REFERENCE);
        }
        if (familyRefObj != null) {
            patientDoc.removeXObject(familyRefObj);
            Pedigree pedigree = PedigreeUtils.getPedigreeForPatient(patient);
            if (pedigree != null && !pedigree.isEmpty()) {
                /* Should not prevent saving the document */
                try {
                    JSONObject strippedPedigree =
                        this.stripIdsFromPedigree(pedigree, patientDoc.getDocumentReference().getName());
                    String image = SvgUpdater.removeLinks(pedigree.getImage(), id);
                    PedigreeUtils.storePedigree(patientDoc, strippedPedigree, image, context);

                    // TODO: Where is the storePedigree() for family?
                    // Remove storePedigree for patient.

                } catch (Exception ex) {
                    this.logger.error("Could not modify patients pedigree while removing from a family. {}",
                        ex.getMessage());
                }
            }
            wiki.saveDocument(patientDoc, context);
        }
    }

    @Override
    public void removeMember(String id)
    {
        XWikiContext context = this.provider.get();
        try {
            this.removeMember(id, context.getWiki(), context);
        } catch (Exception ex) {
            this.logger.error("Could not remove patient {} from their family. {}", id, ex.getMessage());
        }
    }

    /**
     * Strips out all linked ids from a pedigree.
     *
     * @return null if the pedigree data is empty
     */
    private JSONObject stripIdsFromPedigree(Pedigree pedigree, String patientId)
    {
        if (pedigree != null && !pedigree.isEmpty()) {
            List<JSONObject> patientProperties =
                PedigreeUtils.extractPatientJSONPropertiesFromPedigree(pedigree.getData());
            for (JSONObject properties : patientProperties) {
                if (properties.get(PATIENT_LINK_JSON_KEY) != null && !StringUtils
                    .equalsIgnoreCase(properties.get(PATIENT_LINK_JSON_KEY).toString(), patientId)) {
                    properties.remove(PATIENT_LINK_JSON_KEY);
                }
            }
            return pedigree.getData();
        } else {
            return new JSONObject();
        }
    }

    private void addNewMembers(List<String> currentMembers, List<String> updatedMembers, XWikiDocument familyDoc)
        throws XWikiException
    {
        List<String> newMembers = new LinkedList<>();
        newMembers.addAll(updatedMembers);
        newMembers.removeAll(currentMembers);
        if (!newMembers.isEmpty()) {
            XWikiContext context = this.provider.get();
            XWiki wiki = context.getWiki();
            for (String newMember : newMembers) {
                Patient patient = this.patientRepository.getPatientById(newMember);
                if (patient != null) {
                    XWikiDocument patientDoc = wiki.getDocument(patient.getDocument(), context);
                    this.familyUtils.setFamilyReference(patientDoc, familyDoc, context);
                    wiki.saveDocument(patientDoc, context);
                }
            }
        }
    }

    private static StatusResponse2 checkForDuplicates(List<String> updatedMembers)
    {
        List<String> duplicationCheck = new LinkedList<>();
        duplicationCheck.addAll(updatedMembers);
        for (String member : updatedMembers) {
            duplicationCheck.remove(member);
            if (duplicationCheck.contains(member)) {
                return StatusResponse2.DUPLICATE_PATIENT.setMessage(member);
            }
        }

        return StatusResponse2.OK;
    }
}
