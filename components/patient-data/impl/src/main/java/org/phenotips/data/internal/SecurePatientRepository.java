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

import org.slf4j.Logger;

/**
 * Secure implementation of patient data access service which checks the user's access rights before performing an
 * operation. If the user is authorized, the actual work is done by the default {@link PatientRepository}
 * implementation. If the user is not authorized, a {@link SecurityException} is thrown.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Component(roles = PatientRepository.class)
@Named("secure")
@Singleton
public class SecurePatientRepository implements PatientRepository
{
    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Used for checking access rights. */
    @Inject
    private AuthorizationService access;

    /** Used for obtaining the current user. */
    @Inject
    private UserManager userManager;

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
    public EntityReference getDataSpace()
    {
        return this.internalService.getDataSpace();
    }

    @Override
    public Patient create()
    {
        return create(this.userManager.getCurrentUser());
    }

    @Override
    public synchronized Patient createNewPatient()
    {
        return create();
    }

    @Override
    public Patient create(DocumentReference creator)
    {
        return create(this.userManager.getUser(this.serializer.serialize(creator)));
    }

    private Patient create(User creator)
    {
        if (this.access.hasAccess(creator, Right.EDIT,
            this.currentResolver.resolve(Patient.DEFAULT_DATA_SPACE, EntityType.SPACE))) {
            return this.internalService.create(creator != null ? creator.getProfileDocument() : null);
        }
        throw new SecurityException("User not authorized to create new patients");
    }

    @Override
    public Patient createNewPatient(DocumentReference creator)
    {
        return create(creator);
    }

    @Override
    public Patient get(String id)
    {
        Patient patient = this.internalService.get(id);
        return checkAccess(patient, this.userManager.getCurrentUser());
    }

    @Override
    public Patient getPatientById(String id)
    {
        return get(id);
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
    public Patient getPatientByExternalId(String externalId)
    {
        return getByName(externalId);
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
        return this.internalService.load(document);
    }

    @Override
    public Patient loadPatientFromDocument(DocumentModelBridge document)
    {
        return load(document);
    }

    private Patient checkAccess(Patient patient, User user)
    {
        return checkAccess(Right.VIEW, patient, user);
    }

    private Patient checkAccess(Right right, Patient patient, User user)
    {
        if (patient != null && this.access.hasAccess(user, right, patient.getDocument())) {
            return patient;
        } else if (patient != null) {
            this.logger.warn("Illegal access requested for patient [{}] by user [{}]", patient.getId(), user);
            throw new SecurityException("Unauthorized access");
        }
        return null;
    }

    @Override
    public boolean deletePatient(String id)
    {
        if (this.access.hasAccess(Right.DELETE, this.bridge.getCurrentUserReference(),
            this.currentResolver.resolve(Patient.DEFAULT_DATA_SPACE, EntityType.SPACE))) {
            return this.internalService.deletePatient(id);
        }
        this.logger.warn("Illegal delete action requested for patient [{}] by user [{}]", id,
            this.bridge.getCurrentUserReference());
        throw new SecurityException("User not authorized to delete a patient");
    }
}
