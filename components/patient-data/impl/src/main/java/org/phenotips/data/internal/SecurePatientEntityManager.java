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
package org.phenotips.data.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.entities.PrimaryEntityManager;
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Secure implementation of patient data access service using XWiki as the storage backend.
 *
 * @version $Id$
 * @since 1.4
 */
@Named("Patient/secure")
@Component(roles = {PrimaryEntityManager.class})
@Singleton
public class SecurePatientEntityManager extends PatientEntityManager
{
    /** Used for obtaining the current user. */
    @Inject
    private UserManager userManager;

    /** Used for checking access rights. */
    @Inject
    private AuthorizationService access;

    /** Wrapped trusted API, doing the actual work. */
    @Inject
    private PatientRepository internalService;

    /** Fills in missing reference fields with those from the current context document to create a full reference. */
    @Inject
    @Named("current")
    private EntityReferenceResolver<EntityReference> currentResolver;

    /** Serializes references to strings. */
    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Override
    public Patient create()
    {
        return create(this.userManager.getCurrentUser());
    }

    @Override
    public Patient create(DocumentReference creator)
    {
        return create(this.userManager.getUser(this.serializer.serialize(creator)));
    }

    @Override
    public Patient get(String id)
    {
        Patient patient = this.internalService.get(id);
        return checkAccess(patient, this.userManager.getCurrentUser());
    }

    @Override
    public Patient get(DocumentReference reference)
    {
        Patient patient = this.internalService.get(reference);
        return checkAccess(patient, this.userManager.getCurrentUser());
    }

    @Override
    public Patient getByName(String name)
    {
        Patient patient = this.internalService.getByName(name);
        return checkAccess(patient, this.userManager.getCurrentUser());
    }

    @Override
    public Iterator<Patient> getAll()
    {
        Iterator<Patient> patientsIterator = this.internalService.getAll();
        return new SecurePatientIterator(patientsIterator, this.access, this.userManager.getCurrentUser());
    }

    @Override
    public boolean delete(Patient patient)
    {
        if (checkAccess(Right.DELETE, patient, this.userManager.getCurrentUser()) != null) {
            this.internalService.delete(patient);
        }
        return false;
    }

    @Override
    public Patient load(DocumentModelBridge document) throws IllegalArgumentException
    {
        // If the caller already has access to the document, then it's safe to proceed
        return createSecurePatient(this.internalService.load(document));
    }

    /**
     * Creates a new patient iff {@code creator} has {@link Right#EDIT edit rights}.
     *
     * @param creator the {@link User} trying to create a new {@link Patient}
     * @return a new {@link SecurePatient}
     * @throws SecurityException if {@code creator} is not authorized to create new patients
     */
    private Patient create(User creator)
    {
        if (this.access.hasAccess(creator, Right.EDIT,
            this.currentResolver.resolve(Patient.DEFAULT_DATA_SPACE, EntityType.SPACE))) {
            Patient patient = this.internalService.create(creator != null ? creator.getProfileDocument() : null);
            return createSecurePatient(patient);
        }
        throw new SecurityException("User not authorized to create new patients");
    }

    /**
     * Returns a secure implementation of {@link Patient} if {@code user} has {@link Right#VIEW view right} for the
     * provided {@code patient}. Returns null if {@code patient} is null.
     *
     * @param patient the {@link Patient} of interest
     * @param user the {@link User} requesting {@link Right#VIEW view access} to {@code patient}
     * @return a {@link SecurePatient} for {@code patient}
     * @throws SecurityException if {@code user} does not have {@link Right#VIEW} for {@code patient}
     */
    private Patient checkAccess(Patient patient, User user)
    {
        return checkAccess(Right.VIEW, patient, user);
    }

    /**
     * Returns a secure implementation of {@link Patient} if {@code user} has the required {@code right} for interacting
     * with the provided {@code patient}. Returns null if {@code patient} is null.
     *
     * @param right the required {@link Right} that the user needs to have in order to interact with the patient
     * @param patient the {@link Patient} of interest
     * @param user the {@link User} requesting access to {@code patient}
     * @return a {@link SecurePatient} for {@code patient}
     * @throws SecurityException if {@code user} does not have the required {@code right} for {@code patient}
     */
    private Patient checkAccess(Right right, Patient patient, User user)
    {
        if (patient != null && this.access.hasAccess(user, right, patient.getDocumentReference())) {
            return createSecurePatient(patient);
        } else if (patient != null) {
            this.logger.warn("Illegal access requested for patient [{}] by user [{}]", patient.getId(), user);
            throw new SecurityException("Unauthorized access");
        }
        return null;
    }

    /**
     * Returns a SecurePatient wrapper around a given patient.
     *
     * TODO: Modify testing/change this method? The method is added and is made protected
     *       for the sole purpose of simplifying testing and mocking:
     *       - it is hard to test what patient is returned by the iterator if
     *         "new SecurePatient(patient)" is used directly, since constructor can't be mocked,
     *         and correctly mocking all dependencies to actually mock patient creation is hard.
     *       - another "correct" way is to use some kind of SecurePatient factory, but static methods
     *         can't be mocked as well, and a non-static method does not make sense
     *
     * @param patient a patient object
     * @return a SecurePatient wrapper around the given patient
     */
    protected SecurePatient createSecurePatient(Patient patient)
    {
        if (patient == null) {
            return null;
        }
        return new SecurePatient(patient);
    }
}
