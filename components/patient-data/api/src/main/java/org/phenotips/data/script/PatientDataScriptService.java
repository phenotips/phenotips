/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.data.script;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * API that provides access to patient data.
 *
 * @version $Id$
 * @since 1.0M8
 */
@Unstable
@Component
@Named("patients")
@Singleton
public class PatientDataScriptService implements ScriptService
{
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

    /**
     * Retrieve a {@link Patient patient} by it's PhenoTips identifier.
     *
     * @param id the patient identifier, i.e. the serialized document reference
     * @return the patient data, or {@code null} if the requested patient does not exist, is not a valid patient, or is
     *         not accessible by the current user
     */
    public Patient getPatientById(String id)
    {
        Patient patient = this.internalService.getPatientById(id);
        if (patient != null
            && this.access.hasAccess(Right.VIEW, this.bridge.getCurrentUserReference(), patient.getDocument())) {
            return patient;
        }
        return null;
    }

    /**
     * Retrieve a {@link Patient patient} by it's clinical identifier. Only works if external identifiers are enabled
     * and used.
     *
     * @param externalId the patient's clinical identifier, as set by the patient's reporter
     * @return the patient data, or {@code null} if the requested patient does not exist, is not a valid patient, or is
     *         not accessible by the current user
     */
    public Patient getPatientByExternalId(String externalId)
    {
        Patient patient = this.internalService.getPatientByExternalId(externalId);
        if (patient != null
            && this.access.hasAccess(Right.VIEW, this.bridge.getCurrentUserReference(), patient.getDocument())) {
            return patient;
        }
        return null;
    }

    /**
     * Create and return a new empty patient record.
     *
     * @return the created patient record, or {@code null} if the user does not have the right to create a new patient
     *         record or the creation fails
     */
    public Patient createNewPatient()
    {
        if (this.access.hasAccess(Right.EDIT, this.bridge.getCurrentUserReference(),
            this.currentResolver.resolve(Patient.DEFAULT_DATA_SPACE, EntityType.SPACE))) {
            return this.internalService.createNewPatient();
        }
        return null;
    }
}
