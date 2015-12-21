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
package org.phenotips.studies.family.script;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.security.authorization.AuthorizationService;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;
import org.phenotips.studies.family.Pedigree;
import org.phenotips.studies.family.internal.export.XWikiFamilyExport;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Script service for working with families. All methods assume actions are performed by current user and do
 * corresponding permision checks.
 *
 * @version $Id$
 * @since 1.2RC1
 */
@Component
@Singleton
@Named("families")
public class FamilyScriptService implements ScriptService
{
    @Inject
    private FamilyRepository familyRepository;

    @Inject
    private PatientRepository patientRepository;

    @Inject
    private XWikiFamilyExport familyExport;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private UserManager userManager;

    /**
     * Creates an empty family.
     *
     * @return Family object corresponding to the newly created family.
     */
    public Family createFamily()
    {
        return this.familyRepository.createFamily();
    }

    /**
     * Returns family object, or null if doesn't exist or current user has no rights.
     *
     * @param familyId a PhenotTips family ID
     * @return Family object of the family with the given id, or null if familyId is not valid or
     * current user does not have permissions to view the family.
     */
    public Family getFamilyById(String familyId)
    {
        if (familyId == null) {
            return null;
        }
        Family family = this.familyRepository.getFamilyById(familyId);
        if (family == null) {
            return null;
        }
        if (!this.authorizationService.hasAccess(
                this.userManager.getCurrentUser(), Right.VIEW, family.getDocumentReference())) {
            return null;
        }
        // Note: it is safe to return Family object even if the user has no edit rights for the family
        return family;
    }

    /**
     * Returns family pedigree.
     * Essentially this is a shortcut for getFamilyById().getPedigree() with a check that family is not null.
     *
     * @param familyId must be a valid family id
     * @return pedigree object or null if no such family, no pedigree or no view rights for the family.
     */
    public Pedigree getPedigreeForFamily(String familyId)
    {
        Family family = this.getFamilyById(familyId);
        if (family != null) {
            return family.getPedigree();
        }
        return null;
    }

    /**
     * Returns a family ID the patient belongs to.
     *
     * @param patientId id of the patient
     * @return Id of the, or null if patient does not belong to the family or current user
     * has no view rights for the patient.
     */
    public Family getFamilyForPatient(String patientId)
    {
        Patient patient = this.patientRepository.getPatientById(patientId);
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
        if (!this.authorizationService.hasAccess(
                this.userManager.getCurrentUser(), Right.VIEW, family.getDocumentReference())) {
            return null;
        }
        return family;
    }

    /**
     * Returns patient's pedigree, which is the pedigree of a family that patient belongs to.
     *
     * @param patientId id of the patient
     * @return Id of the, or null if patient does not belong to the family or current user
     * has no view rights for the patient.
     */
    public Pedigree getPedigreeForPatient(String patientId)
    {
        Family family = this.getFamilyForPatient(patientId);
        if (family != null) {
            return family.getPedigree();
        }
        return null;
    }

    /**
     * return a JSON object with a list of families that fit the input search criterion.
     *
     * @param input beginning of string of family
     * @param resultsLimit maximum number of results
     * @param requiredPermissions only families on which the user has requiredPermissions will be returned
     * @param returnAsJSON if true, the result is returned as JSON, otherwise as HTML
     * @return list of families
     */
    public String searchFamilies(String input, int resultsLimit, String requiredPermissions, boolean returnAsJSON)
    {
        return this.familyExport.searchFamilies(input, resultsLimit, requiredPermissions, returnAsJSON);
    }

    /**
     * Delete family, modifying the both the family and patient records to reflect the change.
     *
     * @param familyId of the family to delete
     * @param deleteAllMembers indicator whether to delete all family member documents as well
     * @return true if successful
     */
    public boolean deleteFamily(String familyId, boolean deleteAllMembers)
    {
        if (!canDeleteFamily(familyId, deleteAllMembers)) {
            return false;
        }
        Family family = this.familyRepository.getFamilyById(familyId);
        if (family == null) {
            // should not happen if canDeleteFamily(), but check for consistency and in case of race conditions
            return false;
        }
        return family.deleteFamily(deleteAllMembers);
    }

    /**
     * Checks if the current user can delete the family (or the family and all the members).
     *
     * @param familyId of the family to delete
     * @param deleteAllMembers indicator whether to check delete permisions on all family member documents as well
     * @return true if successful
     */
    boolean canDeleteFamily(String familyId, boolean deleteAllMembers)
    {
        User currentUser = this.userManager.getCurrentUser();

        Family family = this.familyRepository.getFamilyById(familyId);
        if (family == null) {
            return false;
        }
        if (!this.authorizationService.hasAccess(currentUser, Right.DELETE, family.getDocumentReference())) {
            return false;
        }
        if (deleteAllMembers) {
            // check permissions
            for (Patient patient : family.getMembers()) {
                if (!this.authorizationService.hasAccess(currentUser, Right.DELETE, patient.getDocument())) {
                    return false;
                }
            }
        }
        return true;
    }
}
