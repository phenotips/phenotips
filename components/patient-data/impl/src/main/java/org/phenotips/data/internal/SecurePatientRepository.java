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

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import java.util.Collection;
import java.util.LinkedList;

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
@Component
@Named("secure")
@Singleton
public class SecurePatientRepository implements PatientRepository
{
    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Used for checking access rights. */
    @Inject
    private AuthorizationManager access;

    /** Used for obtaining the current user. */
    @Inject
    private DocumentAccessBridge bridge;

    /** Wrapped trusted API, doing the actual work. */
    @Inject
    private PatientRepository internalService;

    /** Fills in missing reference fields with those from the current context document to create a full reference. */
    @Inject
    @Named("current")
    private EntityReferenceResolver<EntityReference> currentResolver;

    @Override
    public Patient getPatientById(String id)
    {
        Patient patient = this.internalService.getPatientById(id);
        return checkAccess(patient, this.bridge.getCurrentUserReference());
    }

    @Override
    public Patient getPatientByExternalId(String externalId)
    {
        Patient patient = this.internalService.getPatientByExternalId(externalId);
        return checkAccess(patient, this.bridge.getCurrentUserReference());
    }

    @Override
    public Patient loadPatientFromDocument(DocumentModelBridge document)
    {
        // If the caller already has access to the document, then it's safe to proceed
        return this.internalService.loadPatientFromDocument(document);
    }

    @Override
    public Patient createNewPatient(DocumentReference creator)
    {
        if (this.access.hasAccess(Right.EDIT, creator,
            this.currentResolver.resolve(Patient.DEFAULT_DATA_SPACE, EntityType.SPACE))) {
            return this.internalService.createNewPatient();
        }
        throw new SecurityException("User not authorized to create new patients");
    }

    @Override
    public synchronized Patient createNewPatient()
    {
        return createNewPatient(this.bridge.getCurrentUserReference());
    }

    @Override
    public Collection<Patient> getAllPatients()
    {
        Collection<Patient> patients = this.internalService.getAllPatients();
        return checkAccess(patients, this.bridge.getCurrentUserReference());
    }

    private Collection<Patient> checkAccess(Collection<Patient> patients, DocumentReference user)
    {
        Collection<Patient> patientsWithAccess = new LinkedList<Patient>();
        for (Patient patient : patients) {
            if (this.access.hasAccess(Right.VIEW, user, patient.getDocument())) {
                patientsWithAccess.add(patient);
            }
        }
        return patients;
    }

    private Patient checkAccess(Patient patient, DocumentReference user) {
        if (patient != null && this.access.hasAccess(Right.VIEW, user, patient.getDocument())) {
            return patient;
        } else if (patient != null) {
            this.logger.warn("Illegal access requested for patient [{}] by user [{}]", patient.getId(), user);
            throw new SecurityException("Unauthorized access");
        }
        return null;
    }
}
