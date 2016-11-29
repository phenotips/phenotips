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
package org.phenotips.studies.family.groupManagers;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.entities.PrimaryEntityGroupManager;
import org.phenotips.entities.internal.AbstractExternalPrimaryEntityGroupManager;
import org.phenotips.security.authorization.AuthorizationService;
import org.phenotips.studies.family.Family;
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
import org.phenotips.studies.family.internal.PhenotipsFamilyPermissions;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
 * @version $Id$
 */
@Component(roles = { PrimaryEntityGroupManager.class, PatientsInFamilyManager.class })
@Singleton
public class PatientsInFamilyManager
    extends AbstractExternalPrimaryEntityGroupManager<Family, Patient>
    implements PrimaryEntityGroupManager<Family, Patient>
{
    @Inject
    private UserManager userManager;

    @Inject
    private PatientRepository patientRepository;

    @Inject
    private PedigreeProcessor pedigreeConverter;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private PhenotipsFamilyPermissions familyPermissions;

    protected PatientsInFamilyManager(EntityReference groupEntityReference, EntityReference memberEntityReference)
    {
        super(groupEntityReference, memberEntityReference);
    }

    @Override
    public boolean addMember(Family family, Patient patient) throws PTException
    {
        Collection<Patient> asList = Arrays.asList(patient);
        return this.addAllMembers(family, asList);
    }

    @Override
    public boolean removeMember(Family family, Patient patient)
    {
        Collection<Patient> asList = Arrays.asList(patient);
        return this.removeAllMembers(family, asList);
    }

    @Override
    public boolean addAllMembers(Family family, Collection<Patient> patients) throws PTException
    {
        return this.addAllMembers(family, patients, this.userManager.getCurrentUser());
    }

    @Override
    public boolean removeAllMembers(Family family, Collection<Patient> patients)
    {
        return this.removeAllMembers(family, patients, this.userManager.getCurrentUser());
    }

    /*
     * Adds members to the family.
     *
     * Note: the synchronization could be limited to addAllMembersInternal and updateFamilyPermissionsAndSave, because
     * checkValidity is read-only.
     *
     * @param family family which should get a new member
     * @param patients to add to family
     * @param updatingUser right checks are done for this user
     * @throws PTException in case addition was not successful for any reason (not enough rights, patient already has a
     *             family, etc.)
     * @return true if successful
     */
    private synchronized boolean addAllMembers(Family family, Collection<Patient> patients, User updatingUser)
        throws PTException
    {
        this.checkValidity(family, patients, updatingUser);
        this.addAllMembersInternal(family, patients);
        this.updateFamilyPermissionsAndSave(family, "added " + this.allPatientsString(patients) + " to the family");

        return true;
    }

    /*
     * Removes all given patients from the family.
     *
     * See note about synchronization in addAllMembers(Family, Collection, User).
     *
     * @param family family which should lose a new member
     * @param patients to remove from family
     * @param updatingUser right checks are done for this user
     * @throws PTException if removal was not successful for any reason (not enough rights, patient not a member of
     *             this family, etc.)
     * @return true if successful
     */
    private synchronized boolean removeAllMembers(Family family, Collection<Patient> patients, User updatingUser)
        throws PTException
    {
        this.checkIfPatientsCanBeRemovedFromFamily(family, patients, updatingUser);
        this.removeAllMembersInternal(family, patients);
        this.updateFamilyPermissionsAndSave(family, "removed " + this.allPatientsString(patients) + " from the family");

        return true;
    }

    /**
     * Unlinks Similar to deleteFamily, but does not delete the family document (unlinkes all patients from
     * the family). It is supposed to be used in the event handler for xwiki remove action, when the document will be
     * removed by the framework itself.
     *
     * @param family the family
     * @return true if successful
     */
    public boolean forceRemoveAllMembers(Family family)
    {
        return this.forceRemoveAllMembers(family, this.userManager.getCurrentUser());
    }

    /**
     * Unlinks all patients from the family. It is supposed to be used in the event handler for xwiki remove action,
     * when the document will be removed by the framework itself.
     *
     * @param family the family
     * @param updatingUser right checks are done for this user
     * @return true if successful
     */
    public boolean forceRemoveAllMembers(Family family, User updatingUser)
    {
        if (!this.authorizationService.hasAccess(updatingUser, Right.EDIT, family.getDocumentReference())) {
            return false;
        }
        try {
            this.removeAllMembers(family, this.getMembers(family), updatingUser);
            // Remove the members without updating family document since we don't care about it as it will
            // be removed anyway

            return true;
        } catch (PTException ex) {
            this.logger.error("Failed to unlink all patients for the family [{}]: {}", family.getId(), ex.getMessage());
            return false;
        }
    }

    /**
     * Sets the pedigree for the family, and updates all the corresponding other documents.
     *
     * @param family the family
     * @param pedigree to set
     * @throws PTException when the family could not be correctly and fully updated using the given pedigree
     */
    public void setPedigree(Family family, Pedigree pedigree) throws PTException
    {
        this.setPedigree(family, pedigree, this.userManager.getCurrentUser());
    }

    /**
     * Sets the pedigree for the family, and updates all the corresponding other documents.
     *
     * @param family the family
     * @param pedigree to set
     * @param updatingUser right checks are done for this user
     * @throws PTException when the family could not be correctly and fully updated using the given pedigree
     */
    public synchronized void setPedigree(Family family, Pedigree pedigree, User updatingUser) throws PTException
    {
        // note: whenever available, internal versions of helper methods are used which modify the
        // family document but do not save it to disk
        Collection<Patient> oldMembers = this.getMembers(family);

        Collection<Patient> currentMembers = new ArrayList<>();
        for (String id : pedigree.extractIds()) {
            currentMembers.add(this.patientRepository.get(id));
        }

        // Add new members to family
        List<Patient> patientsToAdd = new LinkedList<>();
        patientsToAdd.addAll(currentMembers);
        patientsToAdd.removeAll(oldMembers);

        this.checkValidity(family, patientsToAdd, updatingUser);
        this.addAllMembersInternal(family, patientsToAdd);

        // update patient data from pedigree's JSON
        // (no links to families are set at this point, only patient dat ais updated)
        this.updatePatientsFromJson(pedigree, updatingUser);

        boolean firstPedigree = (family.getPedigree() == null);

        XWikiContext context = this.getXContext();

        this.setPedigreeObject(family, pedigree, context);

        // Removed members who are no longer in the family
        List<Patient> patientsToRemove = new LinkedList<>();
        patientsToRemove.addAll(oldMembers);
        patientsToRemove.removeAll(currentMembers);

        this.checkIfPatientsCanBeRemovedFromFamily(family, patientsToRemove, updatingUser);
        this.removeAllMembers(family, patientsToRemove);

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

    private boolean setPedigreeObject(Family family, Pedigree pedigree, XWikiContext context) {
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
                        (patient == null) ? "" : patient.getDocument().toString());
            } else {
                familyClassObject.setStringValue("proband_id", "");
            }
        }

        return true;
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

    /*
     * This method should be called after the members of the family changed.
     */
    private void updateFamilyPermissionsAndSave(Family family, String message)
    {
        XWikiContext context = this.getXContext();
        this.updateFamilyPermissions(family, context, false);
        if (!saveFamilyDocument(family, message, context)) {
            throw new PTInternalErrorException();
        }
    }

    /**
     * For every family member, read users and groups that have either view or edit edit access on the patient, then
     * gives the sam elevel of access on the family for those users and groups. After performing this method, if p is a
     * member of the family, and x has level y access on p, x has level y access on the family. The user who is the
     * owner of the family always has full access to the family. access on p, x has edit access of the family. The famly
     * document is saved to disk after permissions are updated.
     *
     * @param family the family
     */
    public synchronized void updateFamilyPermissions(Family family)
    {
        XWikiContext context = this.getXContext();
        this.updateFamilyPermissions(family, context, true);
    }

    private void updateFamilyPermissions(Family family, XWikiContext context, boolean saveXwikiDocument)
    {
        this.familyPermissions.updatePermissions(family, context);
        if (saveXwikiDocument) {
            this.saveFamilyDocument(family, "updated permissions", context);
        }
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

    /**
     * This method may be called either as a standalone invocation, or
     * internally as part of family pedigree update.
     * ({@link #checkValidity(Family, List, User)} and family saving is always
     * done outside of this method.
     *
     * Updating permissions is an expensive operation which takes all patients
     * into account, so it shouldn't be done after adding each patient. It
     * should be done by the calling code.
     *
     * Note: This method is not synchronized. At the time of writing (refactoring) this code, this method is called
     * from the public and synchronized {@link #addAllMembers(Family, Collection, User)}
     * {@link #setPedigree(Family, Pedigree, User)}.
     */
    private void addAllMembersInternal(Family family, Collection<Patient> patients) throws PTException
    {
        if (family == null) {
            throw new PTInvalidFamilyIdException(null);
        }

        Collection<Patient> members = super.getMembers(family);

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

        if (!super.addAllMembers(family, patients)) {
            // TODO what if some members could not be added? rollback?
            // It can be implemented either here or in entities, for handling a more general case.
            throw new PTInternalErrorException();
        }
    }

    /*
     * Calls to {@link #checkIfPatientCanBeRemovedFromFamily} and {@link #updateFamilyPermissionsAndSave} before and
     * after, respectively, are the responsibility of the caller.
     *
     * See note about synchronization in addAllMembersInternal().
     */
    private void removeAllMembersInternal(Family family, Collection<Patient> patients) throws PTException
    {
        if (family == null) {
            throw new PTInvalidFamilyIdException(null);
        }

        Collection<Patient> members = this.getMembers(family);

        for (Patient patient : patients) {
            if (patient == null) {
                throw new PTInvalidPatientIdException(null);
            }

            // TODO
            // if (patient.getXDocument() == null) {
            // throw new PTInvalidPatientIdException(patientId);
            // }
            String patientId = patient.getId();
            XWikiContext context = this.getXContext();
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
                    this.logger.error("Could not remove patient [{}] from pedigree from the family [{}]", patientId,
                            family.getId());
                    throw new PTInternalErrorException();
                }
            }
        }

        if (!super.removeAllMembers(family, patients)) {
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

    private void checkIfPatientCanBeAddedToFamily(Family family, Patient patient, User updatingUser)
            throws PTException {
        // check rights
        if (!this.authorizationService.hasAccess(updatingUser, Right.EDIT, family.getDocumentReference())) {
            throw new PTNotEnoughPermissionsOnFamilyException(Right.EDIT, family.getId());
        }
        if (!this.authorizationService.hasAccess(updatingUser, Right.EDIT, patient.getDocument())) {
            throw new PTNotEnoughPermissionsOnPatientException(Right.EDIT, patient.getId());
        }
        // check for logical problems: patient in another family
        Collection<Family> families = this.getGroupsForMember(patient);
        if (families.size() == 1) {
            Family familyForLinkedPatient = families.iterator().next();
            if (familyForLinkedPatient != null && !familyForLinkedPatient.getId().equals(family.getId())) {
                throw new PTPatientAlreadyInAnotherFamilyException(patient.getId(), familyForLinkedPatient.getId());
            }
        }
    }

    private void setFamilyExternalId(String externalId, Family family, XWikiContext context) {
        BaseObject familyObject = family.getXDocument().getXObject(Family.CLASS_REFERENCE);
        familyObject.set("external_id", externalId, context);
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

    private String allPatientsString(Collection<Patient> patients)
    {
        StringBuilder sb = new StringBuilder();
        for (Patient patient : patients) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(patient.getId());
        }
        sb.insert(0, "[");
        sb.append("]");
        return sb.toString();
    }

    // TODO remove when there's patient.getXDocument()
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

    // TODO remove when there's patient.getXDocument()
    private XWikiDocument getDocument(EntityReference docRef) throws XWikiException
    {
        XWikiContext context = this.getXContext();
        XWiki wiki = context.getWiki();
        return wiki.getDocument(docRef, context);
    }
}
