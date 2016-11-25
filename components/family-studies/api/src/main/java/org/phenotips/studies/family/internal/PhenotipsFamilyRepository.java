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
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
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
 * Provides utility methods for working with family documents and patients.
 *
 * @version $Id$
 * @since 1.4
 */
@Component(roles = {FamilyRepository.class, PrimaryEntityManager.class})
@Named("Family")
@Singleton
public class PhenotipsFamilyRepository extends FamilyEntityManager implements FamilyRepository
{
    private static final String FAMILY_REFERENCE_FIELD = "reference";

    private static final String OWNER = "owner";

    private static final String IDENTIFIER = "identifier";

    @Inject
    private PatientRepository patientRepository;

    @Inject
    private PatientsInFamilyManager pifManager;

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
        String patientId = patient.getId();

        this.checkValidity(family, Arrays.asList(patientId), updatingUser);
        this.addAllMembers(family, Arrays.asList(patient));
        this.updateFamilyPermissionsAndSave(family, "added " + patientId + " to the family");
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
            XWikiContext context = this.xcontextProvider.get();
            XWikiDocument patientDocument = patient.getXDocument();
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
        this.checkIfPatientCanBeRemovedFromFamily(family, patient, updatingUser);
        this.removeAllMembers(family, Arrays.asList(patient));
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
            XWikiContext context = this.xcontextProvider.get();
            XWikiDocument patientDocument = patient.getXDocument();
            if (patientDocument == null) {
                throw new PTInvalidPatientIdException(patientId);
            }

            if (!members.contains(patient)) {
                this.logger.error("Can't remove patient [{}] from family [{}]: patient not a member of the family",
                    patientId, family.getId());
                throw new PTPatientNotInFamilyException(patientId);
            }
        }

        // Remove patient from family's members list
        members.remove(patientLinkString(patient));
        BaseObject familyObject = family.getXDocument().getXObject(Family.CLASS_REFERENCE);
        familyObject.set(PhenotipsFamily.FAMILY_MEMBERS_FIELD, members, context);

        if (!batchUpdate) {
            if (!saveFamilyDocument(family, "removed " + patientId + " from the family", context)) {
                throw new PTInternalErrorException();

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

    // TODO Change to work with Collection<Patient>
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
        List<String> patientIdsToAdd = new LinkedList<>();
        patientIdsToAdd.addAll(currentMembers);
        patientIdsToAdd.removeAll(oldMembers);

        this.checkValidity(family, patientIdsToAdd, updatingUser);

        XWikiContext context = this.xcontextProvider.get();
        context.setUserReference(updatingUser == null ? null : updatingUser.getProfileDocument());

        // update patient data from pedigree's JSON
        // (no links to families are set at this point, only patient dat ais updated)
        this.updatePatientsFromJson(pedigree, updatingUser);

        boolean firstPedigree = (family.getPedigree() == null);

        this.setPedigreeObject(family, pedigree, context);

        // Removed members who are no longer in the family
        Collection<String> patientIdsToRemove = new LinkedList<>();
        patientIdsToRemove.addAll(oldMembers);
        patientIdsToRemove.removeAll(currentMembers);

        Collection<Patient> patientsToRemove = new ArrayList<>(patientIdsToRemove.size());
        for (String patientId : patientIdsToRemove) {
            Patient patient = this.patientRepository.get(patientId);
            this.checkIfPatientCanBeRemovedFromFamily(family, patient, updatingUser);
            patientsToRemove.add(patient);
        }
        this.removeAllMembers(family, patientsToRemove);

        List<Patient> patientsToAdd = new ArrayList<>(patientIdsToAdd.size());
        for (String patientId : patientIdsToAdd) {
            patientsToAdd.add(this.patientRepository.get(patientId));
        }
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

    // TODO change to work with Collection<Patient> newMembers
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

    @Override
    public EntityReference getDataSpace()
    {
        return Family.DATA_SPACE;
    }
}
