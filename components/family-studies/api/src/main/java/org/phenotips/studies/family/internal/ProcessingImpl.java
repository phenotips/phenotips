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
import org.phenotips.studies.family.FamilyUtils;
import org.phenotips.studies.family.JsonAdapter;
import org.phenotips.studies.family.Processing;
import org.phenotips.studies.family.Validation;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.QueryException;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
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
    private FamilyUtils familyUtils;

    @Inject
    private Provider<XWikiContext> provider;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> referenceResolver;

    @Inject
    private Validation validation;

    @Inject
    private JsonAdapter jsonAdapter;

    @Override
    public StatusResponse processPatientPedigree(String anchorId, JSONObject json, String image)
        throws XWikiException, NamingException, QueryException
    {
        LogicInterDependantVariables variables = new LogicInterDependantVariables();
        variables.json = json;
        variables.image = image;

        variables.anchorId = anchorId;
        variables.anchorRef = this.referenceResolver.resolve(anchorId, Patient.DEFAULT_DATA_SPACE);
        variables.anchorDoc = this.familyUtils.getDoc(variables.anchorRef);
        variables.familyDoc = this.familyUtils.getFamilyDoc(variables.anchorDoc);

        // fixme must check for all conditions as in verify linkable
        if (variables.anchorDoc == null) {
            variables.response.statusCode = 404;
            variables.response.errorType = "invalidId";
            variables.response.message = String.format("The family/patient id %s is invalid", anchorId);
            return variables.response;
        }

        variables.updatedMembers = PedigreeUtils.extractIdsFromPedigree(json);
        // sometimes pedigree passes in family document name as a member
        if (variables.familyDoc != null) {
            variables.updatedMembers.remove(variables.familyDoc.getDocumentReference().getName());
        }
        variables.updatedMembers = Collections.unmodifiableList(variables.updatedMembers);

        variables = this.executePreUpdateLogic(variables);
        if (variables.response.statusCode != 200) {
            return variables.response;
        }

        variables = this.executeSaveUpdateLogic(variables);
        return variables.response;
    }

    private LogicInterDependantVariables executeSaveUpdateLogic(LogicInterDependantVariables variables)
        throws XWikiException
    {
        if (variables.familyDoc != null) {
            StatusResponse individualAccess = this.validation.canAddEveryMember(variables.familyDoc,
                variables.updatedMembers);
            if (individualAccess.statusCode != 200) {
                variables.response = individualAccess;
                return variables;
            }

            StatusResponse updateFromJson = this.updatePatientsFromJson(variables.json);
            if (updateFromJson.statusCode != 200) {
                variables.response = updateFromJson;
                return variables;
            }
            // storing first, because pedigree depends on this.
            StatusResponse storingResponse = this.storeFamilyRepresentation(variables.familyDoc, variables
                .updatedMembers, variables.json, variables.image);
            if (storingResponse.statusCode != 200) {
                variables.response = storingResponse;
                return variables;
            }

            if (!variables.isNew) {
                this.setUnionOfUserPermissions(variables.familyDoc, variables.updatedMembers);
                this.removeMembersNotPresent(variables.members, variables.updatedMembers);
            }
            this.addNewMembers(variables.members, variables.updatedMembers, variables.familyDoc);
            // remove and add do not take care of modifying the 'members' property
            this.familyUtils.setFamilyMembers(variables.familyDoc, variables.updatedMembers);
        } else {
            if (!this.validation.hasPatientEditAccess(variables.anchorDoc.getDocumentReference().getName())) {
                variables.response = StatusResponse.createInsufficientPatientPermissionsResponse(variables.anchorId);
                return variables;
            }
            // when saving just a patient's pedigree that does not belong to a family
            XWikiContext context = this.provider.get();
            PedigreeUtils.storePedigreeWithSave(variables.anchorDoc, variables.json, variables.image, context,
                context.getWiki());
        }
        return variables;
    }

    /**
     * Checks for several conditions, and updates some variables for later use by logic that handles saving/updating
     * family and patient records.
     */
    private LogicInterDependantVariables executePreUpdateLogic(LogicInterDependantVariables variables)
        throws XWikiException, NamingException, QueryException
    {
        if (variables.updatedMembers.size() < 1) {
            // the list of members should not be empty.
            variables.response.statusCode = 412;
            variables.response.errorType = "invalidUpdate";
            variables.response.message = "The family has no members. Please specify at least one patient link.";
            return variables;
        } else if (variables.familyDoc == null && variables.updatedMembers.size() > 1) {
            // in theory the anchorDoc could be a family document, but at this point it should be a patient document
            variables.familyDoc = this.familyUtils.createFamilyDoc(variables.anchorDoc, true);
            this.setUnionOfUserPermissions(variables.familyDoc, variables.updatedMembers);
            variables.isNew = true;
        } else if (variables.familyDoc != null) {
            variables.members = this.familyUtils.getFamilyMembers(variables.familyDoc);
            StatusResponse duplicationStatus = ProcessingImpl.checkForDuplicates(variables.updatedMembers);
            if (duplicationStatus.statusCode != 200) {
                variables.response = duplicationStatus;
                return variables;
            }
            variables.members = Collections.unmodifiableList(variables.members);
        }
        return variables;
    }

    /**
     * Used to pass around variables for logic heavy functions inside
     * {@link #processPatientPedigree(String, JSONObject, String)}.
     */
    private class LogicInterDependantVariables
    {
        protected StatusResponse response = new StatusResponse();

        protected JSONObject json;

        protected String image;

        protected XWikiDocument familyDoc;

        protected XWikiDocument anchorDoc;

        protected DocumentReference anchorRef;

        protected String anchorId;

        protected List<String> updatedMembers = new LinkedList<>();

        protected List<String> members = new LinkedList<>();

        protected boolean isNew;
    }

    /**
     * Does not save the family document.
     *
     * @param familyDocument XWiki family document object.
     * @param patientIds List of PhenoTips patient IDs of patients in the family.
     * @throws XWikiException TODO: review if need to throw on error.
     */
    @Override
    public void setUnionOfUserPermissions(XWikiDocument familyDocument, List<String> patientIds)
        throws XWikiException
    {
        XWikiContext context = this.provider.get();
        BaseObject rightsObject = getDefaultRightsObject(familyDocument);
        if (rightsObject == null) {
            this.logger.error("Could not find a permission object attached to the family document {}",
                familyDocument.getDocumentReference().getName());
            return;
        }

        Set<String> usersUnion = new HashSet<>();
        Set<String> groupsUnion = new HashSet<>();
        for (String patientId : patientIds) {
            DocumentReference patientRef = this.patientRepository.getPatientById(patientId).getDocument();
            XWikiDocument patientDoc = this.familyUtils.getDoc(patientRef);
            List<Set<String>> patientRights = this.familyUtils.getEntitiesWithEditAccess(patientDoc);
            usersUnion.addAll(patientRights.get(0));
            groupsUnion.addAll(patientRights.get(1));
        }
        rightsObject.set("users", setToString(usersUnion), context);
        rightsObject.set("groups", setToString(groupsUnion), context);
        rightsObject.set("allow", 1, context);
    }

    /**
     * A document can have several rights objects.
     *
     * @return XWiki {@link BaseObject} that corresponds to the default rights
     */
    private BaseObject getDefaultRightsObject(XWikiDocument doc)
    {
        List<BaseObject> rights = doc.getXObjects(FamilyUtils.RIGHTS_CLASS);
        for (BaseObject single : rights) {
            if (StringUtils.equalsIgnoreCase(single.getStringValue("levels"), FamilyUtils.DEFAULT_RIGHTS)) {
                return single;
            }
        }
        return null;
    }

    private StatusResponse updatePatientsFromJson(JSON familyContents)
    {
        String idKey = "id";
        StatusResponse response = new StatusResponse();
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
            response.statusCode = 500;
            response.errorType = "unknown";
            response.message = "Could not update patient records";
        }
        return response;
    }

    /**
     * Does not do access checking.
     *
     * @param family
     * @param updatedMembers
     * @param familyContents
     * @param image
     * @return
     * @throws XWikiException
     */
    private StatusResponse storeFamilyRepresentation(XWikiDocument family, List<String> updatedMembers,
        JSON familyContents, String image) throws XWikiException
    {
        XWikiContext context = this.provider.get();
        XWiki wiki = context.getWiki();

        for (String member : updatedMembers) {
            Patient patient = this.patientRepository.getPatientById(member);
            XWikiDocument patientDoc = wiki.getDocument(patient.getDocument(), context);
            PedigreeUtils.storePedigreeWithSave(patientDoc, familyContents, image, context, wiki);
        }
        StatusResponse familyResponse = this.validation.checkFamilyAccessWithResponse(family);
        if (familyResponse.statusCode == 200) {
            PedigreeUtils.storePedigreeWithSave(family, familyContents, image, context, wiki);
        }
        return familyResponse;
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
            PedigreeUtils.Pedigree pedigree = PedigreeUtils.getPedigree(patientDoc);
            if (pedigree != null && !pedigree.isEmpty()) {
                /* Should not prevent saving the document */
                try {
                    JSONObject strippedPedigree =
                        this.stripIdsFromPedigree(pedigree, patientDoc.getDocumentReference().getName());
                    String image = SvgUpdater.removeLinks(pedigree.getImage(), id);
                    PedigreeUtils.storePedigree(patientDoc, strippedPedigree, image, context);
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
    private JSONObject stripIdsFromPedigree(PedigreeUtils.Pedigree pedigree, String patientId)
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

    private static String setToString(Set<String> set)
    {
        String finalString = "";
        for (String item : set) {
            if (StringUtils.isNotBlank(item)) {
                finalString += item + ",";
            }
        }
        return finalString;
    }

    private static StatusResponse checkForDuplicates(List<String> updatedMembers)
    {
        StatusResponse response = new StatusResponse();
        List<String> duplicationCheck = new LinkedList<>();
        duplicationCheck.addAll(updatedMembers);
        for (String member : updatedMembers) {
            duplicationCheck.remove(member);
            if (duplicationCheck.contains(member)) {
                response.statusCode = 400;
                response.errorType = "duplicate";
                response.message = String.format("There is a duplicate link for patient %s", member);
                return response;
            }
        }

        response.statusCode = 200;
        return response;
    }
}
