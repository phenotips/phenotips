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
import org.phenotips.security.authorization.AuthorizationService;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;
import org.phenotips.studies.family.FamilyTools;
import org.phenotips.studies.family.Pedigree;
import org.phenotips.studies.family.exceptions.PTException;
import org.phenotips.studies.family.groupManagers.PatientsInFamilyManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

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
    private PatientsInFamilyManager pifManager;

    @Inject
    private FamilyRepository familyRepository;

    @Inject
    private PatientRepository patientRepository;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private UserManager userManager;

    /** Used for checking access rights. */
    @Inject
    private AuthorizationService access;

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
            this.pifManager.removeMember(family, patient);
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
        return this.pifManager.forceRemoveAllMembers(family);
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
        this.pifManager.setPedigree(family, pedigree);
    }

    @Override
    public boolean canAddToFamily(Family family, Patient patient, boolean throwException) throws PTException
    {
        return this.familyRepository.canAddToFamily(family, patient,
            this.userManager.getCurrentUser(), throwException);
    }
}
