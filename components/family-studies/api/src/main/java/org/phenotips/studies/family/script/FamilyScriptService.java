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
import org.phenotips.entities.PrimaryEntityConnectionsManager;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;
import org.phenotips.studies.family.Pedigree;
import org.phenotips.studies.family.groupManagers.DefaultPatientsInFamilyManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Script service for working with families. All methods assume actions are performed by current user and do
 * corresponding permission checks.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Singleton
@Named("families")
public class FamilyScriptService implements ScriptService
{
    @Inject
    @Named("secure")
    private FamilyRepository familyRepository;

    @Inject
    @Named("secure")
    private PatientRepository patientRepository;

    @Inject
    @Named(DefaultPatientsInFamilyManager.NAME)
    private PrimaryEntityConnectionsManager<Family, Patient> pifManager;

    @Inject
    private UserManager userManager;

    /**
     * Creates an empty family.
     *
     * @return Family object corresponding to the newly created family.
     */
    public Family create()
    {
        return this.familyRepository.create();
    }

    /**
     * Returns family object, or null if doesn't exist or current user has no rights.
     *
     * @param id a PhenotTips family ID
     * @return Family object of the family with the given id, or null if familyId is not valid or current user does not
     *         have permissions to view the family.
     */
    public Family get(String id)
    {
        return this.familyRepository.get(id);
    }

    /**
     * Returns family object, or null if doesn't exist or current user has no rights.
     *
     * @param familyId a PhenotTips family ID
     * @return Family object of the family with the given id, or null if familyId is not valid or current user does not
     *         have permissions to view the family.
     */
    public Family getFamilyById(String familyId)
    {
        return get(familyId);
    }

    /**
     * Returns family pedigree. Essentially this is a shortcut for getFamilyById().getPedigree() with a check that
     * family is not null.
     *
     * @param familyId must be a valid family id
     * @return pedigree object or null if no such family, no pedigree or no view rights for the family.
     */
    public Pedigree getPedigreeForFamily(String familyId)
    {
        Family family = this.familyRepository.get(familyId);
        if (family != null) {
            return family.getPedigree();
        }
        return null;
    }

    /**
     * Returns a family ID the patient belongs to.
     *
     * @param patientId id of the patient
     * @return Id of the, or null if patient does not belong to the family or current user has no view rights for the
     *         patient.
     */
    public Family getFamilyForPatient(String patientId)
    {
        Patient patient = this.patientRepository.get(patientId);
        if (patient != null) {
            return this.familyRepository.getFamilyForPatient(patient);
        }
        return null;
    }

    /**
     * Returns patient's pedigree, which is the pedigree of a family that patient belongs to.
     *
     * @param patientId id of the patient
     * @return Id of the, or null if patient does not belong to the family or current user has no view rights for the
     *         patient.
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
     * Removes a patient from the family, modifying the both the family and patient records to reflect the change.
     *
     * @param patientId of the patient to delete
     * @return true if patient was removed. false if not, for example, if the patient is not associated with a family,
     *         or if current user has no delete rights
     */
    public boolean removeMember(String patientId)
    {
        Family family = this.getFamilyForPatient(patientId);
        Patient patient = this.patientRepository.get(patientId);
        if (family == null || patient == null) {
            return false;
        }
        return this.pifManager.disconnect(family, patient);
    }

    /**
     * Deletes a family record, modifying all member patient records to reflect the change. No patient records are
     * deleted.
     *
     * @param family the family record to delete
     * @return {@code true} if successful; {@code false} if the user does not have the right to delete the family record
     *         or the deletion fails
     */
    public boolean delete(Family family)
    {
        return delete(family, false);
    }

    /**
     * Delete a family record, modifying the both the family and patient records to reflect the change.
     *
     * @param family the family to delete
     * @param deleteAllMembers indicator whether to delete all family member documents as well
     * @return true if successful; false if deletion failed or current user has not enough rights
     */
    public boolean delete(Family family, boolean deleteAllMembers)
    {
        return this.familyRepository.delete(family, deleteAllMembers);
    }

    /**
     * Delete family, modifying the both the family and patient records to reflect the change.
     *
     * @param familyId of the family to delete
     * @param deleteAllMembers indicator whether to delete all family member documents as well
     * @return true if successful; false if deletion failed or current user has not enough rights
     */
    public boolean delete(String familyId, boolean deleteAllMembers)
    {
        Family family = this.get(familyId);
        return this.delete(family, deleteAllMembers);
    }

    /**
     * Checks if the current user can delete the family (or the family and all the members).
     *
     * @param familyId of the family to delete
     * @param deleteAllMembers indicator whether to check delete permissions on all family member documents as well
     * @return true if successful
     */
    public boolean canDeleteFamily(String familyId, boolean deleteAllMembers)
    {
        Family family = this.get(familyId);
        User user = this.userManager.getCurrentUser();
        return this.familyRepository.canDeleteFamily(family, user, deleteAllMembers, false);
    }
}
