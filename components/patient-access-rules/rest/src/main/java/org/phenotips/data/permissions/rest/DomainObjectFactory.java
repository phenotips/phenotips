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
package org.phenotips.data.permissions.rest;

import org.phenotips.data.Patient;
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.data.rest.model.CollaboratorRepresentation;
import org.phenotips.data.rest.model.CollaboratorsRepresentation;
import org.phenotips.data.rest.model.PatientVisibilityRepresentation;
import org.phenotips.data.rest.model.UserSummary;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import javax.ws.rs.core.UriInfo;

/**
 * Factory class for generating REST representations of various types of entities, used within the permissions module.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Unstable
@Role
public interface DomainObjectFactory
{
    /**
     * Create the REST representation for a {@link Owner}'s summary, starting from a {@link Patient} instance.
     *
     * @param patient the owner of this patient will be serialized
     * @return a summary of the patient record's owner, or {@code null} if the current user doesn't have access to the
     * patient or accessing the patient data fails
     */
    UserSummary createOwnerRepresentation(Patient patient);

    /**
     * Create the REST representation for a {@link org.phenotips.data.permissions.Visibility}'s summary, starting from a
     * {@link Patient} instance.
     *
     * @param patient whose visibility is of interest
     * @return a summary of the patient record's visibility, or {@code null} if the current user doesn't have access to
     * the patient or accessing the patient data fails
     */
    PatientVisibilityRepresentation createPatientVisibilityRepresentation(Patient patient);

    /**
     * Create the REST representation for a list of {@link Collaborators}, starting from a {@link Patient} instance.
     *
     * @param patient the (list of) collaborators that are attached to this patient record
     * @param uriInfo the URI information for the rest system and the current request
     * @return a summary of each collaborator on the patient record, or {@code null} if the current user doesn't have
     * access to the patient or accessing the patient data fails.
     */
    CollaboratorsRepresentation createCollaboratorsRepresentation(Patient patient, UriInfo uriInfo);

    /**
     * Create the REST representation for summary of a {@link Collaborator} instance, starting from a {@link Patient}
     * and {@link Collaborator} instances.
     *
     * @param patient to whom the collaborator is attached
     * @param collaborator that is to be represented
     * @return a summary of the collaborator, or {@code null} if the current user doesn't have access to the patient or
     * accessing the patient data fails.
     */
    CollaboratorRepresentation createCollaboratorRepresentation(Patient patient, Collaborator collaborator);
}
