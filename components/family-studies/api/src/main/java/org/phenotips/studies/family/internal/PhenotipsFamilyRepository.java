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

import org.phenotips.configuration.RecordConfiguration;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.permissions.Owner;
import org.phenotips.security.authorization.AuthorizationService;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;
import org.phenotips.studies.family.Pedigree;
import org.phenotips.studies.family.PedigreeProcessor;
import org.phenotips.studies.family.exceptions.PTException;
import org.phenotips.studies.family.exceptions.PTInternalErrorException;
import org.phenotips.studies.family.exceptions.PTInvalidFamilyIdException;
import org.phenotips.studies.family.exceptions.PTInvalidPatientIdException;
import org.phenotips.studies.family.exceptions.PTNotEnoughPermissionsOnFamilyException;
import org.phenotips.studies.family.exceptions.PTNotEnoughPermissionsOnPatientException;
import org.phenotips.studies.family.exceptions.PTPatientAlreadyInAnotherFamilyException;
import org.phenotips.studies.family.exceptions.PTPatientNotInFamilyException;
import org.phenotips.studies.family.exceptions.PTPedigreeContainesSamePatientMultipleTimesException;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Provides utility methods for working with family documents and patients.
 *
 * @version $Id$
 * @since 1.4
 */
@Component(roles = FamilyRepository.class)
@Singleton
public class PhenotipsFamilyRepository extends FamilyEntityManager implements FamilyRepository
{
    /** The location where preferences are stored. */
    private static final EntityReference PREFERENCES_LOCATION = new EntityReference("WebHome", EntityType.DOCUMENT,
        Patient.DEFAULT_DATA_SPACE);

    private static final String FAMILY_REFERENCE_FIELD = "reference";

    private static final String OWNER = "owner";

    private static final String IDENTIFIER = "identifier";

    private static final String EXTERNAL_ID = "external_id";

    private static final String AUTOGENERATE_FAMILY_EXTERNAL_ID_LABEL = "autogenerateFamilyExternalId";

    private static final String FAMILY_EXTERNAL_ID_PREFIX_LABEL = "familyExternalIdPrefix";

    @Inject
    private PatientRepository patientRepository;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private PedigreeProcessor pedigreeConverter;

    /** Used for obtaining the current user. */
    @Inject
    private UserManager userManager;

    @Inject
    private EntityReferenceSerializer<String> entitySerializer;

    @Override
    public Family createFamily(User creator)
    {
        return create(creator != null ? creator.getProfileDocument() : null);
    }

    @Override
    public synchronized Family create(final DocumentReference creator)
    {
        try {
            final XWikiContext context = this.xcontextProvider.get();
            final Family family = super.create(creator);
            final XWikiDocument doc = family.getXDocument();

            // Adding owner reference to family
            doc.newXObject(Owner.CLASS_REFERENCE, context).set(OWNER,
                creator == null ? StringUtils.EMPTY : this.entitySerializer.serialize(creator), context);
            // Adding identifier to family
            doc.getXObject(Family.CLASS_REFERENCE).setLongValue(IDENTIFIER,
                Integer.parseInt(family.getId().replaceAll("\\D++", StringUtils.EMPTY)));
            // Adding external identifier to family if configured to be autogenerated
            if (autogenerateFamilyExternalID()) {
                String prefix = getFamilyExternalIDPrefix();
                doc.getXObject(Family.CLASS_REFERENCE).setStringValue(EXTERNAL_ID, prefix
                    + family.getId().replaceAll("[^0-9]", StringUtils.EMPTY));
            }
            context.getWiki().saveDocument(doc, context);
            return family;
        } catch (Exception ex) {
            this.logger.warn("Failed to create family: {}", ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    public synchronized boolean deleteFamily(Family family, User updatingUser, boolean deleteAllMembers)
    {
        return delete(family, deleteAllMembers);
    }

    @Override
    public synchronized boolean delete(final Family family)
    {
        return delete(family, false);
    }

    @Override
    public synchronized boolean delete(final Family family, boolean deleteAllMembers)
    {
        // TODO: Should there be a SecureFamilyRepository to perform these checks (similar to SecurePatientRepository)?
        final User currentUser = this.userManager.getCurrentUser();
        if (!canDeleteFamily(family, currentUser, deleteAllMembers, false)) {
            return false;
        }

        if (deleteAllMembers) {
            for (Patient patient : family.getMembers()) {
                if (!this.patientRepository.delete(patient)) {
                    this.logger.error("Failed to delete patient [{}] - deletion of family [{}] aborted",
                        patient.getId(), family.getId());
                    return false;
                }
            }
        } else if (!this.forceRemoveAllMembers(family, currentUser)) {
            return false;
        }

        return super.delete(family);
    }

    @Override
    public boolean forceRemoveAllMembers(Family family, User updatingUser)
    {
        if (!this.authorizationService.hasAccess(updatingUser, Right.EDIT, family.getDocumentReference())) {
            return false;
        }
        try {
            for (Patient patient : family.getMembers()) {
                // remove the member without updating family document (use "batch mode")
                // since we don't care about it as it will be removed anyway
                this.removeMember(family, patient, updatingUser, true);
            }
            return true;
        } catch (PTException ex) {
            this.logger.error("Failed to unlink all patients for the family [{}]: {}", family.getId(), ex.getMessage());
            return false;
        }
    }

    @Override
    public Family getFamilyById(String id)
    {
        return get(id);
    }

    /**
     * Returns a Family object for patient. If there's an XWiki family document but no PhenotipsFamily object associated
     * with it in the cache, a new PhenotipsFamily object will be created.
     *
     * @param patient for which to look for a family
     * @return Family if there's an XWiki family document, otherwise null
     */
    @Override
    public Family getFamilyForPatient(Patient patient)
    {
        if (patient == null) {
            return null;
        }
        String patientId = patient.getId();
        XWikiDocument patientDocument = patient.getXDocument();
        if (patientDocument == null) {
            return null;
        }

        DocumentReference familyReference = getFamilyReference(patientDocument);
        if (familyReference == null) {
            this.logger.debug("Family not found for patient [{}]", patientId);
            return null;
        }

        try {
            XWikiDocument document = getDocument(familyReference);
            return new PhenotipsFamily(document);
        } catch (XWikiException e) {
            this.logger.error("Can't find family document for patient [{}]", patient.getId());
            return null;
        }
    }

    @Override
    public synchronized void addMember(Family family, Patient patient, User updatingUser) throws PTException
    {
        this.addMember(family, patient, updatingUser, false);
    }

    /**
     * This method may be called either as a standalone invocation, or internally as part of family pedigree update. The
     * latter invocation may add multiple patients (a "batch update")
     */
    private void addMember(Family family, Patient patient, User updatingUser, boolean batchUpdate) throws PTException
    {
        if (family == null) {
            throw new PTInvalidFamilyIdException(null);
        }
        if (patient == null) {
            throw new PTInvalidPatientIdException(null);
        }
        if (!batchUpdate) {
            // when called as part of a batch update all permissions have already been checked;
            // otherwise perform the check, which may throw some exceptiuon in case of problems
            this.checkIfPatientCanBeAddedToFamily(family, patient, updatingUser);
        }

        String patientId = patient.getId();
        XWikiContext context = this.xcontextProvider.get();
        XWikiDocument patientDocument = patient.getXDocument();
        if (patientDocument == null) {
            throw new PTInvalidPatientIdException(patientId);
        }

        // Check if not already a member
        List<String> members = family.getMembersIds();
        if (members.contains(patientLinkString(patient))) {
            this.logger.error("Patient [{}] already a member of the same family, not adding", patientId);
            throw new PTPedigreeContainesSamePatientMultipleTimesException(patientId);
        }

        if (!this.setFamilyReference(patientDocument, family.getXDocument(), context)) {
            throw new PTInternalErrorException();
        }
        if (!savePatientDocument(patientDocument, "added to family " + family.getId(), context)) {
            throw new PTInternalErrorException();
        }

        // Add member to the list of family members
        members.add(patientLinkString(patient));
        BaseObject familyObject = family.getXDocument().getXObject(Family.CLASS_REFERENCE);
        familyObject.set(PhenotipsFamily.FAMILY_MEMBERS_FIELD, members, context);

        // only save family document if this add() is not performed as a part of a batch update
        if (!batchUpdate) {
            if (!saveFamilyDocument(family, "added " + patientId + " to the family", context)) {
                throw new PTInternalErrorException();
            }
        }
    }

    @Override
    public synchronized void removeMember(Family family, Patient patient, User updatingUser) throws PTException
    {
        this.removeMember(family, patient, updatingUser, false);
    }

    private void removeMember(Family family, Patient patient, User updatingUser, boolean batchUpdate)
        throws PTException
    {
        if (family == null) {
            throw new PTInvalidFamilyIdException(null);
        }
        if (patient == null) {
            throw new PTInvalidPatientIdException(null);
        }
        if (!batchUpdate) {
            // when called as part of a batch update all permissions have already been checked;
            // otherwise perform the check, which may throw some exceptiuon in case of problems
            this.checkIfPatientCanBeRemovedFromFamily(family, patient, updatingUser);
        }

        String patientId = patient.getId();
        XWikiContext context = this.xcontextProvider.get();
        XWikiDocument patientDocument = patient.getXDocument();
        if (patientDocument == null) {
            throw new PTInvalidPatientIdException(patientId);
        }

        List<String> members = family.getMembersIds();
        if (!members.contains(patientLinkString(patient))) {
            this.logger.error("Can't remove patient [{}] from framily [{}]: patient not a member of the family",
                patientId, family.getId());
            throw new PTPatientNotInFamilyException(patientId);
        }

        // Remove reference to a family from patient document
        if (!this.removeFamilyReference(patientDocument)) {
            throw new PTInternalErrorException();
        }
        if (!savePatientDocument(patientDocument, "removed from family", context)) {
            throw new PTInternalErrorException();
        }

        // Remove patient from the pedigree
        Pedigree pedigree = family.getPedigree();
        if (pedigree != null) {
            pedigree.removeLink(patientId);
            if (!this.setPedigreeObject(family, pedigree, context)) {
                this.logger.error("Could not remove patient [{}] from pedigree from the family [{}]",
                    patientId, family.getId());
                throw new PTInternalErrorException();
            }
        }

        // Remove patient from family's members list
        members.remove(patientLinkString(patient));
        BaseObject familyObject = family.getXDocument().getXObject(Family.CLASS_REFERENCE);
        familyObject.set(PhenotipsFamily.FAMILY_MEMBERS_FIELD, members, context);

        if (!batchUpdate) {
            if (!saveFamilyDocument(family, "removed " + patientId + " from the family", context)) {
                throw new PTInternalErrorException();
            }
        }
    }

    /**
     * Returns string as stored in the family members list.
     */
    private String patientLinkString(Patient patient)
    {
        return patient.getId();
    }

    @Override
    public boolean canAddToFamily(Family family, Patient patient, User updatingUser, boolean throwException)
        throws PTException
    {
        try {
            if (family == null) {
                if (throwException) {
                    throw new PTInvalidFamilyIdException(null);
                }
                return false;
            }
            if (patient == null) {
                if (throwException) {
                    throw new PTInvalidPatientIdException(null);
                }
                return false;
            }
            this.checkIfPatientCanBeAddedToFamily(family, patient, updatingUser);
            return true;
        } catch (PTException ex) {
            if (throwException) {
                throw ex;
            }
            return false;
        }
    }

    @Override
    public boolean canDeleteFamily(Family family, User updatingUser,
        boolean deleteAllMembers, boolean throwException) throws PTException
    {
        try {
            if (family == null) {
                if (throwException) {
                    throw new PTInvalidFamilyIdException(null);
                }
                return false;
            }
            if (!this.authorizationService.hasAccess(updatingUser, Right.DELETE, family.getDocumentReference())) {
                throw new PTNotEnoughPermissionsOnFamilyException(Right.DELETE, family.getId());
            }
            if (deleteAllMembers) {
                // check permissions on all patients
                for (Patient patient : family.getMembers()) {
                    if (!this.authorizationService.hasAccess(
                        updatingUser, Right.DELETE, patient.getDocumentReference())) {
                        throw new PTNotEnoughPermissionsOnPatientException(Right.DELETE, patient.getId());
                    }
                }
            }
            return true;
        } catch (PTException ex) {
            if (throwException) {
                throw ex;
            }
            return false;
        }
    }

    private void checkIfPatientCanBeAddedToFamily(Family family, Patient patient, User updatingUser) throws PTException
    {
        // check rights
        if (!this.authorizationService.hasAccess(updatingUser, Right.EDIT, family.getDocumentReference())) {
            throw new PTNotEnoughPermissionsOnFamilyException(Right.EDIT, family.getId());
        }
        if (!this.authorizationService.hasAccess(updatingUser, Right.EDIT, patient.getDocumentReference())) {
            throw new PTNotEnoughPermissionsOnPatientException(Right.EDIT, patient.getId());
        }
        // check for logical problems: patient in another family
        Family familyForLinkedPatient = this.getFamilyForPatient(patient);
        if (familyForLinkedPatient != null && !familyForLinkedPatient.getId().equals(family.getId())) {
            throw new PTPatientAlreadyInAnotherFamilyException(patient.getId(), familyForLinkedPatient.getId());
        }
    }

    private void checkIfPatientCanBeRemovedFromFamily(Family family, Patient patient, User updatingUser)
        throws PTException
    {
        // check rights
        if (!this.authorizationService.hasAccess(updatingUser, Right.EDIT, family.getDocumentReference())) {
            throw new PTNotEnoughPermissionsOnFamilyException(Right.EDIT, family.getId());
        }
        if (!this.authorizationService.hasAccess(updatingUser, Right.EDIT, patient.getDocumentReference())) {
            throw new PTNotEnoughPermissionsOnPatientException(Right.EDIT, patient.getId());
        }
    }

    @Override
    public synchronized void setPedigree(Family family, Pedigree pedigree, User updatingUser) throws PTException
    {
        // note: whenever available, internal versions of helper methods are used which modify the
        // family document but do not save it to disk
        List<String> oldMembers = family.getMembersIds();

        List<String> currentMembers = pedigree.extractIds();

        // Add new members to family
        List<String> patientsToAdd = new LinkedList<>();
        patientsToAdd.addAll(currentMembers);
        patientsToAdd.removeAll(oldMembers);

        this.checkValidity(family, patientsToAdd, updatingUser);

        XWikiContext context = this.xcontextProvider.get();
        context.setUserReference(updatingUser == null ? null : updatingUser.getProfileDocument());

        // update patient data from pedigree's JSON
        // (no links to families are set at this point, only patient dat ais updated)
        this.updatePatientsFromJson(pedigree, updatingUser);

        boolean firstPedigree = (family.getPedigree() == null);

        this.setPedigreeObject(family, pedigree, context);

        // Removed members who are no longer in the family
        List<String> patientsToRemove = new LinkedList<>();
        patientsToRemove.addAll(oldMembers);
        patientsToRemove.removeAll(currentMembers);
        for (String patientId : patientsToRemove) {
            Patient patient = this.patientRepository.get(patientId);
            // remove the memebr and update patient document, but don't write family document to disk yet
            // and don't update permisisons (that will be done once afdter all patients are added/removed)
            this.removeMember(family, patient, updatingUser, true);
        }

        for (String patientId : patientsToAdd) {
            Patient patient = this.patientRepository.get(patientId);
            this.addMember(family, patient, updatingUser, true);
        }

        if (firstPedigree && StringUtils.isEmpty(family.getExternalId())) {
            // default family identifier to proband last name - only on first pedigree creation
            // and only if no extrenal id is already (manully) defined
            String lastName = pedigree.getProbandPatientLastName();
            if (lastName != null) {
                this.setFamilyExternalId(lastName, family, context);
            }
        }

        if (!this.saveFamilyDocument(family, "Updated family from saved pedigree", context)) {
            throw new PTInternalErrorException();
        }
    }

    private void checkValidity(Family family, List<String> newMembers, User updatingUser) throws PTException
    {
        // Checks that current user has edit permissions on family
        if (!this.authorizationService.hasAccess(updatingUser, Right.EDIT, family.getDocumentReference())) {
            throw new PTNotEnoughPermissionsOnFamilyException(Right.EDIT, family.getId());
        }

        String duplicateID = this.findDuplicate(newMembers);
        if (duplicateID != null) {
            throw new PTPedigreeContainesSamePatientMultipleTimesException(duplicateID);
        }

        // Check if every new member can be added to the family
        if (newMembers != null) {
            for (String patientId : newMembers) {
                Patient patient = this.patientRepository.get(patientId);
                checkIfPatientCanBeAddedToFamily(family, patient, updatingUser);
            }
        }
    }

    private void updatePatientsFromJson(Pedigree pedigree, User updatingUser)
    {
        String idKey = "id";
        try {
            List<JSONObject> patientsJson = this.pedigreeConverter.convert(pedigree);

            for (JSONObject singlePatient : patientsJson) {
                if (singlePatient.has(idKey)) {
                    Patient patient = this.patientRepository.get(singlePatient.getString(idKey));
                    if (!this.authorizationService.hasAccess(
                        updatingUser, Right.EDIT, patient.getDocumentReference())) {
                        // skip patients the current user does not have edit rights for
                        continue;
                    }
                    patient.updateFromJSON(singlePatient);
                }
            }
        } catch (Exception ex) {
            throw new PTInternalErrorException();
        }
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

    private boolean setPedigreeObject(Family family, Pedigree pedigree, XWikiContext context)
    {
        if (pedigree == null) {
            this.logger.error("Can not set NULL pedigree for family [{}]", family.getId());
            return false;
        }

        BaseObject pedigreeObject = family.getXDocument().getXObject(Pedigree.CLASS_REFERENCE);
        pedigreeObject.set(Pedigree.IMAGE, ((pedigree == null) ? "" : pedigree.getImage(null)), context);
        pedigreeObject.set(Pedigree.DATA, ((pedigree == null) ? "" : pedigree.getData().toString()), context);

        // update proband ID every time pedigree is changed
        BaseObject familyClassObject = family.getXDocument().getXObject(Family.CLASS_REFERENCE);
        if (familyClassObject != null) {
            String probandId = pedigree.getProbandId();
            if (!StringUtils.isEmpty(probandId)) {
                Patient patient = this.patientRepository.get(probandId);
                familyClassObject.setStringValue("proband_id",
                    (patient == null) ? "" : patient.getDocumentReference().toString());
            } else {
                familyClassObject.setStringValue("proband_id", "");
            }
        }

        return true;
    }

    private void setFamilyExternalId(String externalId, Family family, XWikiContext context)
    {
        BaseObject familyObject = family.getXDocument().getXObject(Family.CLASS_REFERENCE);
        familyObject.set("external_id", externalId, context);
    }

    private boolean savePatientDocument(XWikiDocument patientDocument, String documentHistoryComment,
        XWikiContext context)
    {
        try {
            patientDocument.setAuthorReference(context.getUserReference());
            context.getWiki().saveDocument(patientDocument, documentHistoryComment, context);
        } catch (XWikiException e) {
            this.logger.error("Error saving patient [{}] document for commit {}: [{}]",
                patientDocument.getId(), documentHistoryComment, e.getMessage());
            return false;
        }
        return true;
    }

    private synchronized boolean saveFamilyDocument(Family family, String documentHistoryComment, XWikiContext context)
    {
        try {
            family.getXDocument().setAuthorReference(context.getUserReference());
            context.getWiki().saveDocument(family.getXDocument(), documentHistoryComment, context);
        } catch (XWikiException e) {
            this.logger.error("Error saving family [{}] document for commit {}: [{}]",
                family.getId(), documentHistoryComment, e.getMessage());
            return false;
        }
        return true;
    }

    /*
     * returns a reference to a family document from an XWiki patient document.
     */
    private DocumentReference getFamilyReference(XWikiDocument patientDocument)
    {
        BaseObject familyObject = patientDocument.getXObject(Family.REFERENCE_CLASS_REFERENCE);
        if (familyObject == null) {
            return null;
        }

        String familyDocName = familyObject.getStringValue(FAMILY_REFERENCE_FIELD);
        if (StringUtils.isBlank(familyDocName)) {
            return null;
        }

        DocumentReference familyReference = this.stringResolver.resolve(familyDocName, Family.DATA_SPACE);

        return familyReference;
    }

    /**
     * Sets the reference to the family document in the patient document.
     *
     * @param patientDoc to set the family reference
     * @param familyDoc family of the patient
     * @param context context
     * @return true if no problems, false in case of any error/sexception
     */
    private boolean setFamilyReference(XWikiDocument patientDoc, XWikiDocument familyDoc, XWikiContext context)
    {
        try {
            BaseObject pointer = patientDoc.getXObject(Family.REFERENCE_CLASS_REFERENCE, true, context);
            pointer.set(FAMILY_REFERENCE_FIELD, familyDoc.getDocumentReference().toString(), context);
            return true;
        } catch (Exception ex) {
            this.logger.error("Could not add patient [{}] to family. Error setting family reference: []",
                patientDoc.getId(), ex);
            return false;
        }
    }

    /**
     * Removes a family reference from a patient.
     *
     * @param patientDoc to set the family reference
     * @return true if successful
     */
    private boolean removeFamilyReference(XWikiDocument patientDoc)
    {
        try {
            BaseObject pointer = patientDoc.getXObject(Family.REFERENCE_CLASS_REFERENCE);
            if (pointer != null) {
                return patientDoc.removeXObject(pointer);
            }
            return true;
        } catch (Exception ex) {
            this.logger.error("Could not remove patient [{}] from family. Error removing family reference: []",
                patientDoc.getId(), ex);
            return false;
        }
    }

    /*
     * Returns the largest family identifier id
     */
    @Override
    protected long getLastUsedId()
    {
        this.logger.debug("getLastUsedId()");

        long crtMaxID = 0;
        try {
            Query q = this.qm.createQuery("select family.identifier "
                + "from     Document doc, "
                + "         doc.object(PhenoTips.FamilyClass) as family "
                + "where    family.identifier is not null "
                + "order by family.identifier desc", Query.XWQL).setLimit(1);
            List<Long> crtMaxIDList = q.execute();
            if (crtMaxIDList.size() > 0 && crtMaxIDList.get(0) != null) {
                crtMaxID = crtMaxIDList.get(0);
            }
            crtMaxID = Math.max(crtMaxID, 0);
        } catch (QueryException ex) {
            this.logger.warn("Failed to get the last used identifier: {}", ex.getMessage());
        }
        return crtMaxID;
    }

    private XWikiDocument getDocument(EntityReference docRef) throws XWikiException
    {
        XWikiContext context = this.xcontextProvider.get();
        XWiki wiki = context.getWiki();
        return wiki.getDocument(docRef, context);
    }

    private boolean autogenerateFamilyExternalID()
    {
        BaseObject settings = getGlobalConfigurationObject();
        if (settings != null) {
            return settings.getIntValue(AUTOGENERATE_FAMILY_EXTERNAL_ID_LABEL) == 1;
        }
        return false;
    }

    private String getFamilyExternalIDPrefix()
    {
        BaseObject settings = getGlobalConfigurationObject();
        if (settings != null) {
            return settings.getStringValue(FAMILY_EXTERNAL_ID_PREFIX_LABEL);
        }
        return "";
    }

    private BaseObject getGlobalConfigurationObject()
    {
        try {
            XWikiContext context = this.xcontextProvider.get();
            return context.getWiki().getDocument(PREFERENCES_LOCATION, context)
                .getXObject(RecordConfiguration.GLOBAL_PREFERENCES_CLASS);
        } catch (XWikiException ex) {
            this.logger.warn("Failed to read preferences: {}", ex.getMessage());
        }
        return null;
    }
}
