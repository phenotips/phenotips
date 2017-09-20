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

import org.phenotips.data.permissions.Collaborator;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.data.permissions.rest.model.CollaboratorRepresentation;
import org.phenotips.data.permissions.rest.model.CollaboratorsRepresentation;
import org.phenotips.data.permissions.rest.model.OwnerRepresentation;
import org.phenotips.data.permissions.rest.model.PrincipalsRepresentation;
import org.phenotips.data.permissions.rest.model.VisibilityRepresentation;
import org.phenotips.entities.PrimaryEntity;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import javax.ws.rs.core.UriInfo;

/**
 * Factory class for generating REST representations of various types of entities, used within the permissions module.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Unstable
@Role
public interface DomainObjectFactory
{
    /**
     * Create the REST representation for a {@link org.phenotips.data.permissions.Owner}'s summary, starting from a
     * {@link PrimaryEntity} instance.
     *
     * @param entity the owner of this entity will be serialized
     * @return a summary of the entity record's owner, or {@code null} if the current user doesn't have access to the
     *         entity or accessing the entity data fails
     */
    OwnerRepresentation createOwnerRepresentation(PrimaryEntity entity);

    /**
     * Create the REST representation for a {@link org.phenotips.data.permissions.Visibility}'s summary, starting from a
     * {@link PrimaryEntity} instance.
     *
     * @param entity whose visibility is of interest
     * @return a summary of the entity record's visibility, or {@code null} if the current user doesn't have access to
     *         the entity or accessing the entity data fails
     */
    VisibilityRepresentation createVisibilityRepresentation(PrimaryEntity entity);

    /**
     * Create the REST representation for a {@link org.phenotips.data.permissions.Visibility}'s summary, starting from a
     * {@link Visibility} instance.
     *
     * @param visibility of interest
     * @return a summary of the visibility, or {@code null} if the visibility is null
     */
    VisibilityRepresentation createVisibilityRepresentation(Visibility visibility);

    /**
     * Create the REST representation for a list of {@link Collaborator}s, starting from a {@link PrimaryEntity}
     * instance.
     *
     * @param entity to whom the the (list of) collaborators that are attached
     * @param uriInfo the URI information for the rest system and the current request
     * @return a summary of each collaborator on the entity record, or {@code null} if the current user doesn't have
     *         access to the entity or accessing the entity data fails.
     */
    CollaboratorsRepresentation createCollaboratorsRepresentation(PrimaryEntity entity, UriInfo uriInfo);

    /**
     * Create the REST representation for summary of a {@link Collaborator} instance, starting from a
     * {@link PrimaryEntity} and {@link Collaborator} instances.
     *
     * @param collaborator that is to be represented
     * @return a summary of the collaborator, or {@code null} if the current user doesn't have access to the entity or
     *         accessing the entity data fails.
     */
    CollaboratorRepresentation createCollaboratorRepresentation(Collaborator collaborator);

    /**
     * Create the REST representation for a list of principals that have access to the {@link PrimaryEntity}.
     *
     * @param entity whose accessers are of interest
     * @param entityType the type of entity
     * @param uriInfo the URI information for the rest system and the current request
     * @return a summary of each principal that has access to the patient record, or {@code null} if the current user
     *         doesn't have access to the patient or accessing the patient data fails.
     */
    PrincipalsRepresentation createPrincipalsRepresentation(PrimaryEntity entity, String entityType, UriInfo uriInfo);
}
