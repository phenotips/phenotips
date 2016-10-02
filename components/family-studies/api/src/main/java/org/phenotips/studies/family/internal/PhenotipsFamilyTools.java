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
            return this.familyRepository.createFamily(creator);
        }
        throw new SecurityException("User not authorized to create new families");
    }

    @Override
    public Family getFamilyById(String familyId)
    {
        if (familyId == null) {
            return null;
        }
        Family family = this.familyRepository.getFamilyById(familyId);
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
            this.userManager.getCurrentUser(), Right.VIEW, patient.getDocument())) {
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
        if (!this.authorizationService.hasAccess(currentUser, Right.EDIT, patient.getDocument())) {
            return false;
        }

        Family family = this.familyRepository.getFamilyForPatient(patient);
        if (family == null) {
            return false;
        }
        if (!currentUserHasAccessRight(family, Right.EDIT)) {
            return false;
        }

        family.removeMember(patient);
        return true;
    }

    @Override
    public boolean deleteFamily(String familyId, boolean deleteAllMembers)
    {
        if (!currentUserCanDeleteFamily(familyId, deleteAllMembers)) {
            return false;
        }
        Family family = this.familyRepository.getFamilyById(familyId);
        if (family == null) {
            // should not happen if canDeleteFamily(), but check for consistency and in case of race conditions
            return false;
        }
        return family.deleteFamily(deleteAllMembers);
    }

    @Override
    public boolean currentUserCanDeleteFamily(String familyId, boolean deleteAllMembers)
    {
        Family family = this.familyRepository.getFamilyById(familyId);
        if (family == null) {
            return false;
        }
        if (!currentUserHasAccessRight(family, Right.DELETE)) {
            return false;
        }
        if (deleteAllMembers) {
            // check permissions
            User currentUser = this.userManager.getCurrentUser();
            for (Patient patient : family.getMembers()) {
                if (!this.authorizationService.hasAccess(currentUser, Right.DELETE, patient.getDocument())) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean currentUserHasAccessRight(Family family, Right right)
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
}
