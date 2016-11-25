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
import org.phenotips.data.permissions.Owner;
import org.phenotips.entities.PrimaryEntityManager;
import org.phenotips.entities.internal.AbstractPrimaryEntityManager;
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
import org.phenotips.studies.family.groupManagers.PatientsInFamilyManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

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
@Component(roles = {FamilyRepository.class, PrimaryEntityManager.class})
@Named("Family")
@Singleton
public class PhenotipsFamilyRepository extends AbstractPrimaryEntityManager<Family> implements FamilyRepository
{
    private static final String PREFIX = "FAM";

    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> provider;

    @Inject
    private PatientRepository patientRepository;

    @Inject
    private PhenotipsFamilyPermissions familyPermissions;

    @Inject
    private PatientsInFamilyManager pifManager;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private PedigreeProcessor pedigreeConverter;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> referenceResolver;

    /** Fills in missing reference fields with those from the current context document to create a full reference. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> entityReferenceResolver;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Override
    public synchronized boolean deleteFamily(Family family, User updatingUser, boolean deleteAllMembers)
    {
        if (!canDeleteFamily(family, updatingUser, deleteAllMembers, false)) {
            return false;
        }
        if (deleteAllMembers) {
            for (Patient patient : this.pifManager.getMembers(family)) {
                if (!this.patientRepository.delete(patient)) {
                    this.logger.error("Failed to delete patient [{}] - deletion of family [{}] aborted",
                        patient.getId(), family.getId());
                    return false;
                }
            }
        } else if (!this.forceRemoveAllMembers(family, updatingUser)) {
            return false;
        }

        if (!super.delete(family)) {
            this.logger.error("Failed to delete family document [{}].", family.getId());
            return false;
        }

        return true;
    }

    @Override
    public boolean forceRemoveAllMembers(Family family, User updatingUser)
    {
        if (!this.authorizationService.hasAccess(updatingUser, Right.EDIT, family.getDocumentReference())) {
            return false;
        }
        try {
            this.removeAllMembers(family, this.pifManager.getMembers(family));
            // Remove the members without updating family document since we don't care about it as it will
            // be removed anyway

            return true;
        } catch (PTException ex) {
            this.logger.error("Failed to unlink all patients for the family [{}]: {}", family.getId(), ex.getMessage());
            return false;
        }
    }

    @Override
    public Family getFamilyById(String id)
    {
        return super.get(id);
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
        Collection<Family> families = this.pifManager.getGroupsForMember(patient);
        if (families.size() != 1) {
            return null;
        } else {
            return families.iterator().next();
        }
    }

    @Override
    public synchronized void addMember(Family family, Patient patient, User updatingUser) throws PTException
    {
        Collection<Patient> asList = Arrays.asList(patient);

        this.checkValidity(family, asList, updatingUser);
        this.addAllMembers(family, asList);
        this.updateFamilyPermissionsAndSave(family, "added " + patient.getId() + " to the family");
    }

    /*
     * This method should be called after the members of the family changed.
     */
    private void updateFamilyPermissionsAndSave(Family family, String message) {
        XWikiContext context = this.provider.get();
        this.updateFamilyPermissions(family, context, false);
        if (!saveFamilyDocument(family, message, context)) {
            throw new PTInternalErrorException();
        }
    }

    /**
     * This method may be called either as a standalone invocation, or internally as part of family pedigree update.
     * ({@link #checkValidity(Family, List, User)} and family saving is always done outside of this method.
     *
     * Updating permissions is an expensive operation which takes all patients into account, so it shouldn't be done
     * after adding each patient. It should be done by the calling code.
     */
    private void addAllMembers(Family family, Collection<Patient> patients) throws PTException
    {
        if (family == null) {
            throw new PTInvalidFamilyIdException(null);
        }

        Collection<Patient> members = this.pifManager.getMembers(family);

        for (Patient patient : patients) {
            if (patient == null) {
                throw new PTInvalidPatientIdException(null);
            }

            // TODO
            // if (patient.getXDocument() == null) {
            //     throw new PTInvalidPatientIdException(patient.getId());
            // }
            String patientId = patient.getId();
            XWikiDocument patientDocument = getDocument(patient);
            if (patientDocument == null) {
                throw new PTInvalidPatientIdException(patientId);
            }

            // Check if not already a member
            if (members.contains(patient)) {
                this.logger.error("Patient [{}] already a member of the same family, not adding", patientId);
                throw new PTPedigreeContainesSamePatientMultipleTimesException(patientId);
            }
        }

        if (!this.pifManager.addAllMembers(family, patients)) {
            // TODO what if some members could not be added? rollback?
            // It can be implemented either here or in entities, for handling a more general case.
            throw new PTInternalErrorException();
        }

    }

    @Override
    public synchronized void removeMember(Family family, Patient patient, User updatingUser) throws PTException
    {
        List<Patient> asList = Arrays.asList(patient);
        this.checkIfPatientsCanBeRemovedFromFamily(family, asList, updatingUser);
        this.removeAllMembers(family, asList);
        this.updateFamilyPermissionsAndSave(family, "removed " + patient.getId() + " from the family");
    }

    /*
     * Calls to {@link #checkIfPatientCanBeRemovedFromFamily} and {@link #updateFamilyPermissionsAndSave}
     * before and after, respectively, are the responsibility of the caller.
     */
    private void removeAllMembers(Family family, Collection<Patient> patients)
        throws PTException
    {
        if (family == null) {
            throw new PTInvalidFamilyIdException(null);
        }

        Collection<Patient> members = this.pifManager.getMembers(family);

        for (Patient patient : patients) {
            if (patient == null) {
                throw new PTInvalidPatientIdException(null);
            }

            // TODO
            // if (patient.getXDocument() == null) {
            //    throw new PTInvalidPatientIdException(patientId);
            // }
            String patientId = patient.getId();
            XWikiContext context = this.provider.get();
            XWikiDocument patientDocument = getDocument(patient);
            if (patientDocument == null) {
                throw new PTInvalidPatientIdException(patientId);
            }

            if (!members.contains(patient)) {
                this.logger.error("Can't remove patient [{}] from family [{}]: patient not a member of the family",
                    patientId, family.getId());
                throw new PTPatientNotInFamilyException(patientId);
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
        }

        if (!this.pifManager.removeAllMembers(family, patients)) {
            throw new PTInternalErrorException();
        }
    }

    @Override
    public synchronized void updateFamilyPermissions(Family family)
    {
        XWikiContext context = this.provider.get();
        this.updateFamilyPermissions(family, context, true);
    }

    private void updateFamilyPermissions(Family family, XWikiContext context, boolean saveXwikiDocument)
    {
        this.familyPermissions.updatePermissions(family, context);
        if (saveXwikiDocument) {
            this.saveFamilyDocument(family, "updated permissions", context);
        }
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
                for (Patient patient : this.pifManager.getMembers(family)) {
                    if (!this.authorizationService.hasAccess(updatingUser, Right.DELETE, patient.getDocument())) {
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
        if (!this.authorizationService.hasAccess(updatingUser, Right.EDIT, patient.getDocument())) {
            throw new PTNotEnoughPermissionsOnPatientException(Right.EDIT, patient.getId());
        }
        // check for logical problems: patient in another family
        Family familyForLinkedPatient = this.getFamilyForPatient(patient);
        if (familyForLinkedPatient != null && !familyForLinkedPatient.getId().equals(family.getId())) {
            throw new PTPatientAlreadyInAnotherFamilyException(patient.getId(), familyForLinkedPatient.getId());
        }
    }

    private void checkIfPatientsCanBeRemovedFromFamily(Family family, Collection<Patient> patients, User updatingUser)
        throws PTException
    {
        for (Patient patient : patients) {
            // check rights
            if (!this.authorizationService.hasAccess(updatingUser, Right.EDIT, family.getDocumentReference())) {
                throw new PTNotEnoughPermissionsOnFamilyException(Right.EDIT, family.getId());
            }
            if (!this.authorizationService.hasAccess(updatingUser, Right.EDIT, patient.getDocument())) {
                throw new PTNotEnoughPermissionsOnPatientException(Right.EDIT, patient.getId());
            }
        }
    }

    @Override
    public synchronized void setPedigree(Family family, Pedigree pedigree, User updatingUser) throws PTException
    {
        // note: whenever available, internal versions of helper methods are used which modify the
        // family document but do not save it to disk
        Collection<Patient> oldMembers = this.pifManager.getMembers(family);

        Collection<Patient> currentMembers = new ArrayList<>();
        for (String id : pedigree.extractIds()) {
            currentMembers.add(this.patientRepository.get(id));
        }

        // Add new members to family
        List<Patient> patientsToAdd = new LinkedList<>();
        patientsToAdd.addAll(currentMembers);
        patientsToAdd.removeAll(oldMembers);

        this.checkValidity(family, patientsToAdd, updatingUser);

        // update patient data from pedigree's JSON
        // (no links to families are set at this point, only patient dat ais updated)
        this.updatePatientsFromJson(pedigree, updatingUser);

        boolean firstPedigree = (family.getPedigree() == null);

        XWikiContext context = this.provider.get();

        this.setPedigreeObject(family, pedigree, context);

        // Removed members who are no longer in the family
        List<Patient> patientsToRemove = new LinkedList<>();
        patientsToRemove.addAll(oldMembers);
        patientsToRemove.removeAll(currentMembers);

        this.checkIfPatientsCanBeRemovedFromFamily(family, patientsToRemove, updatingUser);
        this.removeAllMembers(family, patientsToRemove);

        this.addAllMembers(family, patientsToAdd);

        if (firstPedigree && StringUtils.isEmpty(family.getExternalId())) {
            // default family identifier to proband last name - only on first pedigree creation
            // and only if no extrenal id is already (manully) defined
            String lastName = pedigree.getProbandPatientLastName();
            if (lastName != null) {
                this.setFamilyExternalId(lastName, family, context);
            }
        }

        updateFamilyPermissionsAndSave(family, "Updated family from saved pedigree");
    }

    private void checkValidity(Family family, Collection<Patient> newMembers, User updatingUser) throws PTException
    {
        // Checks that current user has edit permissions on family
        if (!this.authorizationService.hasAccess(updatingUser, Right.EDIT, family.getDocumentReference())) {
            throw new PTNotEnoughPermissionsOnFamilyException(Right.EDIT, family.getId());
        }

        Patient duplicatePatient = this.findDuplicate(newMembers);
        if (duplicatePatient != null) {
            throw new PTPedigreeContainesSamePatientMultipleTimesException(duplicatePatient.getId());
        }

        // Check if every new member can be added to the family
        if (newMembers != null) {
            for (Patient patient : newMembers) {
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
                    if (!this.authorizationService.hasAccess(updatingUser, Right.EDIT, patient.getDocument())) {
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

    private Patient findDuplicate(Collection<Patient> updatedMembers)
    {
        List<Patient> duplicationCheck = new LinkedList<>();
        duplicationCheck.addAll(updatedMembers);
        for (Patient member : updatedMembers) {
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
                familyClassObject.setStringValue("proband_id", (patient == null) ? "" : patient.getDocument()
                    .toString());
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

    private synchronized boolean saveFamilyDocument(Family family, String documentHistoryComment, XWikiContext context)
    {
        try {
            context.getWiki().saveDocument(family.getXDocument(), documentHistoryComment, context);
        } catch (XWikiException e) {
            this.logger.error("Error saving family [{}] document for commit {}: [{}]",
                family.getId(), documentHistoryComment, e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    protected String getIdPrefix()
    {
        return PREFIX;
    }

    @Override
    public synchronized Family create(DocumentReference creator)
    {
        Family newFamily = null;

        try {
            XWikiContext context = this.xcontextProvider.get();
            newFamily = super.create(creator);
            int familyID = Integer.parseInt(newFamily.getDocumentReference().getName().replaceAll("\\D++", ""));

            // TODO newFamily.getXDocument();
            XWikiDocument familyXDocument = (XWikiDocument) this.bridge.getDocument(newFamily.getDocumentReference());

            BaseObject ownerObject = familyXDocument.newXObject(Owner.CLASS_REFERENCE, context);

            // FIXME is this the right way to do that?
            String ownerString = creator == null ? "" : this.entityReferenceSerializer.serialize(creator);
            ownerObject.set("owner", ownerString, context);

            BaseObject familyObject = familyXDocument.getXObject(Family.CLASS_REFERENCE);
            familyObject.set("identifier", familyID, context);

            context.getWiki().saveDocument(familyXDocument, context);
        } catch (Exception e) {
            this.logger.error("Could not create a new family document: {}", e.getMessage());
        }

        return newFamily;
    }

    private XWikiDocument getDocument(Patient patient)
    {
        try {
            DocumentReference document = patient.getDocument();
            XWikiDocument patientDocument = getDocument(document);
            return patientDocument;
        } catch (XWikiException ex) {
            this.logger.error("Can't get patient document for patient [{}]: []", patient.getId(), ex);
            return null;
        }
    }

    private XWikiDocument getDocument(EntityReference docRef) throws XWikiException
    {
        XWikiContext context = this.provider.get();
        XWiki wiki = context.getWiki();
        return wiki.getDocument(docRef, context);
    }

    @Override
    public EntityReference getDataSpace()
    {
        return Family.DATA_SPACE;
    }
}
