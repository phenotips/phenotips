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
import javax.naming.NamingException;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import groovy.json.JsonException;
import net.sf.json.JSON;
import net.sf.json.JSONObject;

/**
 * Storage and retrieval.
 */
@Component
public class ProcessingImpl implements Processing
{
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

    public StatusResponse processPatientPedigree(String anchorId, JSONObject json, String image)
        throws XWikiException, NamingException, QueryException
    {
        StatusResponse response = new StatusResponse();
        DocumentReference anchorRef = referenceResolver.resolve(anchorId, Patient.DEFAULT_DATA_SPACE);
        XWikiDocument anchorDoc = familyUtils.getDoc(anchorRef);
        XWikiDocument familyDoc = familyUtils.getFamilyDoc(anchorDoc);

        if (anchorDoc == null) {  // fixme must check for all conditions as in verify linkable
            response.statusCode = 404;
            response.errorType = "invalidId";
            response.message = String.format("The family/patient id %s is invalid", anchorId);
            return response;
        }

        boolean isNew = false;
        List<String> members = new LinkedList<>();
        List<String> updatedMembers = PedigreeUtils.extractIdsFromPedigree(json);
        // sometimes pedigree passes in family document name as a member
        if (familyDoc != null) {
            updatedMembers.remove(familyDoc.getDocumentReference().getName());
        }
        updatedMembers = Collections.unmodifiableList(updatedMembers);
        if (updatedMembers.size() < 1) {
            // the list of members should not be empty.
            response.statusCode = 412;
            response.errorType = "invalidUpdate";
            response.message = "The family has no members. Please specify at least one patient link.";
            return response;
        } else if (familyDoc == null && updatedMembers.size() > 1) {
            // in theory the anchorDoc could be a family document, but at this point it should be a patient document
            familyDoc = familyUtils.createFamilyDoc(anchorDoc, true);
            this.setUnionOfUserPermissions(familyDoc, updatedMembers);
            isNew = true;
        } else if (familyDoc != null) {
            members = familyUtils.getFamilyMembers(familyDoc);
            StatusResponse duplicationStatus = ProcessingImpl.checkForDuplicates(updatedMembers);
            if (duplicationStatus.statusCode != 200) {
                return duplicationStatus;
            }
            members = Collections.unmodifiableList(members);
        }

        if (familyDoc != null) {
            StatusResponse individualAccess = this.canAddEveryMember(familyDoc, updatedMembers);
            if (individualAccess.statusCode != 200) {
                return individualAccess;
            }

            this.updatePatientsFromJson(json);
            // storing first, because pedigree depends on this.
            StatusResponse storingResponse = this.storeFamilyRepresentation(familyDoc, updatedMembers, json, image);
            if (storingResponse.statusCode != 200) {
                return storingResponse;
            }

            if (!isNew) {
                this.setUnionOfUserPermissions(familyDoc, updatedMembers);
                this.removeMembersNotPresent(members, updatedMembers, image);
            }
            this.addNewMembers(members, updatedMembers, familyDoc);
            // remove and add do not take care of modifying the 'members' property
            familyUtils.setFamilyMembers(familyDoc, updatedMembers);
        } else {
            if (!validation.hasPatientEditAccess(anchorDoc)) {
                return validation.createInsufficientPermissionsResponse(anchorId);
            }
            // when saving just a patient's pedigree that does not belong to a family
            XWikiContext context = provider.get();
            PedigreeUtils.storePedigreeWithSave(anchorDoc, json, image, context, context.getWiki());
        }

        response.statusCode = 200;
        return response;
    }

    /** Does not save the family document. */
    private void setUnionOfUserPermissions(XWikiDocument familyDocument, List<String> patientIds) throws XWikiException
    {
        XWikiContext context = provider.get();
        BaseObject rightsObject = familyDocument.getXObject(FamilyUtils.RIGHTS_CLASS);
        Set<String> usersUnion = new HashSet<>();
        Set<String> groupsUnion = new HashSet<>();
        for (String patientId : patientIds) {
            DocumentReference patientRef = patientRepository.getPatientById(patientId).getDocument();
            XWikiDocument patientDoc = familyUtils.getDoc(patientRef);
            List<Set<String>> patientRights = familyUtils.getEntitiesWithEditAccess(patientDoc);
            usersUnion.addAll(patientRights.get(0));
            groupsUnion.addAll(patientRights.get(1));
        }
        rightsObject.set("users", setToString(usersUnion), context);
        rightsObject.set("groups", setToString(groupsUnion), context);
        rightsObject.set("levels", "view,edit", context);
        rightsObject.set("allow", 1, context);
    }

    private StatusResponse canAddEveryMember(XWikiDocument family, List<String> updatedMembers) throws XWikiException
    {
        StatusResponse defaultResponse = new StatusResponse();
        defaultResponse.statusCode = 200;

        for (String member : updatedMembers) {
            StatusResponse patientResponse = validation.canAddToFamily(family, member);
            if (patientResponse.statusCode != 200) {
                return patientResponse;
            }
        }
        return defaultResponse;
    }

    private void updatePatientsFromJson(JSON familyContents) throws JsonException {
        JSONObject familyContentsObject = JSONObject.fromObject(familyContents);
        List<JSONObject> patientsJson = jsonAdapter.convert(familyContentsObject);

        for (JSONObject singlePatient : patientsJson) {
            if (singlePatient.containsKey("id")) {
                Patient patient = patientRepository.getPatientById(singlePatient.getString("id"));
                patient.updateFromJSON(singlePatient);
            }
        }
    }

    /**
     * Does not do access checking.
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
        XWikiContext context = provider.get();
        XWiki wiki = context.getWiki();

        for (String member : updatedMembers) {
            Patient patient = patientRepository.getPatientById(member);
            XWikiDocument patientDoc = wiki.getDocument(patient.getDocument(), context);
            PedigreeUtils.storePedigreeWithSave(patientDoc, familyContents, image, context, wiki);
        }
        StatusResponse familyResponse = validation.checkFamilyAccessWithResponse(family);
        if (familyResponse.statusCode == 200) {
            PedigreeUtils.storePedigreeWithSave(family, familyContents, image, context, wiki);
        }
        return familyResponse;
    }

    /**
     * Removes records from the family that are no longer in the updated family structure.
     */
    private void removeMembersNotPresent(List<String> currentMembers, List<String> updatedMembers, String image) throws XWikiException
    {
        List<String> toRemove = new LinkedList<>();
        toRemove.addAll(currentMembers);
        toRemove.removeAll(updatedMembers);
        if (!toRemove.isEmpty()) {
            XWikiContext context = provider.get();
            XWiki wiki = context.getWiki();
            for (String oldMemberId : toRemove) {
                Patient patient = patientRepository.getPatientById(oldMemberId);
                if (patient != null) {
                    XWikiDocument patientDoc = wiki.getDocument(patient.getDocument(), context);
                    BaseObject familyRefObj = patientDoc.getXObject(FamilyUtils.FAMILY_REFERENCE);
                    if (familyRefObj != null) {
                        patientDoc.removeXObject(familyRefObj);
                        PedigreeUtils.Pedigree pedigree = PedigreeUtils.getPedigree(patientDoc);
                        if (pedigree != null) {
                            JSONObject strippedPedigree =
                                this.stripIdsFromPedigree(pedigree, patientDoc.getDocumentReference().getName());
                            image = SvgUpdater.removeLinks(pedigree.image, oldMemberId);
                            PedigreeUtils.storePedigree(patientDoc, strippedPedigree, image, context);
                        }
                        wiki.saveDocument(patientDoc, context);
                    }
                }
            }
        }
    }

    /** Strips out all linked ids from a pedigree. */
    private JSONObject stripIdsFromPedigree(PedigreeUtils.Pedigree pedigree, String patientId)
    {
        if (pedigree != null) {
            List<JSONObject> patientProperties = PedigreeUtils.extractPatientJSONPropertiesFromPedigree(pedigree.data);
            for (JSONObject properties : patientProperties) {
                if (properties.get(PATIENT_LINK_JSON_KEY) != null && !StringUtils
                    .equalsIgnoreCase(properties.get(PATIENT_LINK_JSON_KEY).toString(), patientId))
                {
                    properties.remove(PATIENT_LINK_JSON_KEY);
                }
            }
            return pedigree.data;
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
            XWikiContext context = provider.get();
            XWiki wiki = context.getWiki();
            for (String newMember : newMembers) {
                Patient patient = patientRepository.getPatientById(newMember);
                if (patient != null) {
                    XWikiDocument patientDoc = wiki.getDocument(patient.getDocument(), context);
                    familyUtils.setFamilyReference(patientDoc, familyDoc, context);
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
