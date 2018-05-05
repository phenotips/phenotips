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
import org.phenotips.entities.PrimaryEntityConnectionsManager;
import org.phenotips.security.authorization.AuthorizationService;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;
import org.phenotips.studies.family.FamilyTools;
import org.phenotips.studies.family.Pedigree;
import org.phenotips.studies.family.PedigreeProcessor;
import org.phenotips.studies.family.exceptions.PTException;
import org.phenotips.studies.family.exceptions.PTInternalErrorException;
import org.phenotips.studies.family.groupManagers.DefaultPatientsInFamilyManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation of various family tools. All methods assume actions are performed by current user and do
 * corresponding permission checks.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Singleton
public class PhenotipsFamilyTools implements FamilyTools
{
    @Inject
    @Named(DefaultPatientsInFamilyManager.NAME)
    private PrimaryEntityConnectionsManager<Family, Patient> pifManager;

    @Inject
    private FamilyRepository familyRepository;

    @Inject
    private PatientRepository patientRepository;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private UserManager userManager;

    @Inject
    private PedigreeProcessor pedigreeConverter;

    /** Used for checking access rights. */
    @Inject
    private AuthorizationService access;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Logger logger;

    /** Fills in missing reference fields with those from the current context document to create a full reference. */
    @Inject
    @Named("current")
    private EntityReferenceResolver<EntityReference> currentResolver;

    @Override
    public Family createFamily()
    {
        User creator = this.userManager.getCurrentUser();
        if (this.access.hasAccess(creator, Right.EDIT,
            this.currentResolver.resolve(Family.DATA_SPACE, EntityType.SPACE))) {
            return this.familyRepository.create();
        }
        throw new SecurityException("User not authorized to create new families");
    }

    @Override
    public Family getFamilyById(String familyId)
    {
        Family family = this.familyRepository.get(familyId);
        if (family == null) {
            return null;
        }
        if (!currentUserHasAccessRight(family, Right.VIEW)) {
            return null;
        }
        // Note: it is safe to return Family object even if the user has no edit rights for the family
        return family;
    }

    @Override
    public Pedigree getPedigreeForFamily(String familyId)
    {
        Family family = this.getFamilyById(familyId);
        if (family != null) {
            return family.getPedigree();
        }
        return null;
    }

    @Override
    public Family getFamilyForPatient(String patientId)
    {
        Patient patient = this.patientRepository.get(patientId);
        if (patient == null) {
            return null;
        }
        if (!this.authorizationService.hasAccess(
            this.userManager.getCurrentUser(), Right.VIEW, patient.getDocumentReference())) {
            return null;
        }
        Family family = this.familyRepository.getFamilyForPatient(patient);
        if (family == null) {
            return null;
        }
        if (!currentUserHasAccessRight(family, Right.VIEW)) {
            return null;
        }
        return family;
    }

    @Override
    public Pedigree getPedigreeForPatient(String patientId)
    {
        Family family = this.getFamilyForPatient(patientId);
        if (family != null) {
            return family.getPedigree();
        }
        return null;
    }

    @Override
    public boolean removeMember(String patientId)
    {
        User currentUser = this.userManager.getCurrentUser();

        Patient patient = this.patientRepository.get(patientId);
        if (patient == null) {
            return false;
        }
        if (!this.authorizationService.hasAccess(currentUser, Right.EDIT, patient.getDocumentReference())) {
            return false;
        }

        Family family = this.familyRepository.getFamilyForPatient(patient);
        if (family == null || !currentUserHasAccessRight(family, Right.EDIT)) {
            return false;
        }

        try {
            this.pifManager.disconnect(family, patient);
        } catch (PTException ex) {
            return false;
        }

        return true;
    }

    @Override
    public boolean deleteFamily(String familyId, boolean deleteAllMembers)
    {
        Family family = this.familyRepository.get(familyId);
        if (family == null) {
            return false;
        }
        // the access rights checks are done in familyRepository.deleteFamily()
        return this.familyRepository.delete(family, deleteAllMembers);
    }

    @Override
    public boolean forceRemoveAllMembers(Family family)
    {
        return this.pifManager.disconnectAll(family);
    }

    @Override
    public boolean currentUserCanDeleteFamily(String familyId, boolean deleteAllMembers)
    {
        Family family = this.familyRepository.get(familyId);
        if (family == null) {
            return false;
        }
        return this.familyRepository.canDeleteFamily(
            family, this.userManager.getCurrentUser(), deleteAllMembers, false);
    }

    @Override
    public boolean familyExists(String familyId)
    {
        return this.familyRepository.get(familyId) != null;
    }

    @Override
    public boolean currentUserHasAccessRight(String familyId, Right right)
    {
        Family family = this.familyRepository.get(familyId);
        if (family == null) {
            return false;
        }
        return this.currentUserHasAccessRight(family, right);
    }

    private boolean currentUserHasAccessRight(Family family, Right right)
    {
        if (family == null) {
            return false;
        }
        if (!this.authorizationService.hasAccess(
            this.userManager.getCurrentUser(), right, family.getDocumentReference())) {
            return false;
        }
        return true;
    }

    @Override
    public void setPedigree(Family family, Pedigree pedigree) throws PTException
    {
        User currentUser = this.userManager.getCurrentUser();
        // note: whenever available, internal versions of helper methods are used which modify the
        // family document but do not save it to disk
        Collection<Patient> oldMembers = this.pifManager.getAllConnections(family);

        Collection<Patient> currentMembers =
            pedigree.extractIds().stream().map(id -> this.patientRepository.get(id)).collect(Collectors.toList());

        // Add new members to family
        List<Patient> patientsToAdd = new LinkedList<>();
        patientsToAdd.addAll(currentMembers);
        patientsToAdd.removeAll(oldMembers);

        this.pifManager.connectAll(family, patientsToAdd);

        // update patient data from pedigree's JSON
        // (no links to families are set at this point, only patient dat ais updated)
        this.updatePatientsFromJson(pedigree, currentUser);

        boolean firstPedigree = (family.getPedigree() == null);

        this.setPedigreeObject(family, pedigree);

        // Removed members who are no longer in the family
        List<Patient> patientsToRemove = new LinkedList<>();
        patientsToRemove.addAll(oldMembers);
        patientsToRemove.removeAll(currentMembers);

        this.pifManager.disconnectAll(family, patientsToRemove);

        if (firstPedigree && StringUtils.isEmpty(family.getExternalId())) {
            // default family identifier to proband last name - only on first pedigree creation
            // and only if no extrenal id is already (manully) defined
            String lastName = pedigree.getProbandPatientLastName();
            if (lastName != null) {
                this.setFamilyExternalId(lastName, family);
            }
        }
    }

    @Override
    public boolean canAddToFamily(Family family, Patient patient, boolean throwException) throws PTException
    {
        return this.familyRepository.canAddToFamily(family, patient,
            this.userManager.getCurrentUser(), throwException);
    }

    private void updatePatientsFromJson(Pedigree pedigree, User updatingUser)
    {
        String idKey = "id";
        try {
            List<JSONObject> patientsJson = this.pedigreeConverter.convert(pedigree);

            for (JSONObject singlePatient : patientsJson) {
                if (singlePatient.has(idKey)) {
                    Patient patient = this.patientRepository.get(singlePatient.getString(idKey));
                    if (!this.authorizationService.hasAccess(updatingUser, Right.EDIT,
                        patient.getDocumentReference())) {
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

    private void setFamilyExternalId(String externalId, Family family)
    {
        XWikiContext context = this.xcontextProvider.get();
        BaseObject familyObject = family.getXDocument().getXObject(Family.CLASS_REFERENCE);
        familyObject.set("external_id", externalId, context);
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

    private boolean setPedigreeObject(Family family, Pedigree pedigree)
    {
        XWikiContext context = this.xcontextProvider.get();
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
}
