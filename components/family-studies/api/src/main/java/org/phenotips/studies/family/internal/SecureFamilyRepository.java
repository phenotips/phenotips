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
import org.phenotips.security.authorization.AuthorizationService;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;
import org.phenotips.studies.family.exceptions.PTException;
import org.phenotips.studies.family.exceptions.PTInvalidFamilyIdException;
import org.phenotips.studies.family.exceptions.PTInvalidPatientIdException;
import org.phenotips.studies.family.exceptions.PTNotEnoughPermissionsOnFamilyException;
import org.phenotips.studies.family.exceptions.PTNotEnoughPermissionsOnPatientException;
import org.phenotips.studies.family.exceptions.PTPatientAlreadyInAnotherFamilyException;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Provides utility methods for working with family documents and patients.
 *
 * @version $Id$
 * @since 1.4
 */
@Component(roles = FamilyRepository.class)
@Named("secure")
@Singleton
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
public class SecureFamilyRepository extends PhenotipsFamilyRepository implements FamilyRepository
{
    @Inject
    private AuthorizationService authorizationService;

    /** Used for obtaining the current user. */
    @Inject
    private UserManager userManager;

    /** Fills in missing reference fields with those from the current context document to create a full reference. */
    @Inject
    @Named("current")
    private EntityReferenceResolver<EntityReference> currentResolver;

    /** Serializes references to strings. */
    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Override
    public synchronized Family create(final DocumentReference creator)
    {
        User user = this.userManager.getUser(this.serializer.serialize(creator));
        if (this.authorizationService.hasAccess(user, Right.EDIT,
            this.currentResolver.resolve(Family.DATA_SPACE, EntityType.SPACE))) {
            return createSecureFamily(super.create(creator));
        }
        throw new SecurityException("User not authorized to create new families");
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
        final User currentUser = this.userManager.getCurrentUser();
        if (!canDeleteFamily(family, currentUser, deleteAllMembers, false)) {
            return false;
        }

        return super.delete(family);
    }

    @Override
    public Family getFamilyForPatient(Patient patient)
    {
        Family family = super.getFamilyForPatient(patient);
        if (family == null) {
            return null;
        }
        User currentUser = this.userManager.getCurrentUser();
        if (this.authorizationService.hasAccess(currentUser, Right.VIEW, family.getDocumentReference())) {
            return createSecureFamily(family);
        }
        return null;
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

    /**
     * Returns a SecureFamily wrapper around a given family.
     *
     * @param family a Family object
     * @return a SecureFamily wrapper around the given family
     */
    protected SecureFamily createSecureFamily(Family family)
    {
        if (family == null) {
            return null;
        }
        return new SecureFamily(family);
    }
}
