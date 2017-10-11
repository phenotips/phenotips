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

import org.phenotips.data.permissions.rest.model.CollaboratorsRepresentation;
import org.phenotips.data.rest.PatientResource;
import org.phenotips.rest.PATCH;
import org.phenotips.rest.ParentResource;
import org.phenotips.rest.RelatedResources;
import org.phenotips.rest.Relation;
import org.phenotips.rest.RequiredAccess;

import org.xwiki.component.annotation.Role;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Resource for working with entity record collaborators, in bulk, where entities are identified by entity record's
 * internal PhenoTips identifier.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Role
@Path("/{entity-type}/{entity-id}/permissions/collaborators")
@Relation("https://phenotips.org/rel/collaborators")
@ParentResource(PermissionsResource.class)
@RelatedResources(PatientResource.class)
public interface CollaboratorsResource
{
    /**
     * Retrieve information about the collaborators. If the indicated entity record doesn't exist, or if the user
     * sending the request doesn't have the right to view the target entity record, an error is returned.
     *
     * @param entityId internal identifier of an entity record
     * @param entityType the type of entity
     * @return REST representation of a collection of collaborators
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RequiredAccess("view")
    CollaboratorsRepresentation getCollaborators(@PathParam("entity-id") String entityId,
        @PathParam("entity-type") String entityType);

    /**
     * Add new collaborators, or update the permission levels of existing collaborators. If the indicated entity record
     * doesn't exist, or if the user sending the request doesn't have the right to manage the target entity record, no
     * change is performed and an error is returned.
     *
     * @param collaborators a list of collaborators to modify, each of which must have {@code id} and {@code level}
     *            properties
     * @param entityId internal identifier of an entity record
     * @param entityType the type of entity
     * @return a status message
     */
    @PATCH
    @RequiredAccess("manage")
    @Consumes(MediaType.APPLICATION_JSON)
    Response addCollaborators(CollaboratorsRepresentation collaborators, @PathParam("entity-id") String entityId,
        @PathParam("entity-type") String entityType);

    /**
     * Add new collaborators, or update the permission levels of existing collaborators. If the indicated entity record
     * doesn't exist, or if the user sending the request doesn't have the right to manage the target entity record, no
     * change is performed and an error is returned. The request data must contain properties "collaborator" and
     * "level". Multiple values are accepted, if an equal number of values is received for both properties, and they are
     * paired in the order that they appear in the request. Data can be sent both in the request body and in the query
     * string, with the latter taking precedence over the former. If an unequal number of parameter values is received,
     * then no change is performed and an error is returned.
     *
     * @param entityId internal identifier of an entity record
     * @param entityType the type of entity
     * @return a status message
     */
    @PATCH
    @RequiredAccess("manage")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Response addCollaborators(@PathParam("entity-id") String entityId, @PathParam("entity-type") String entityType);

    /**
     * Update all collaborators, replacing all previous collaborators. If the indicated entity record doesn't exist, or
     * if the user sending the request doesn't have the right to manage the target entity record, no change is
     * performed and an error is returned.
     *
     * @param collaborators a list of collaborators, each of which must have {@code id} and {@code level} properties
     * @param entityId internal identifier of an entity record
     * @param entityType the type of entity
     * @return a status message
     */
    @PUT
    @RequiredAccess("manage")
    @Consumes(MediaType.APPLICATION_JSON)
    Response setCollaborators(CollaboratorsRepresentation collaborators, @PathParam("entity-id") String entityId,
        @PathParam("entity-type") String entityType);

    /**
     * Delete all collaborators. If the indicated entity record doesn't exist, or if the user sending the request
     * doesn't have the right to manage the target entity record, no change is performed and an error is returned.
     *
     * @param entityId internal identifier of an entity record
     * @param entityType the type of entity
     * @return a status message
     */
    @DELETE
    @RequiredAccess("manage")
    Response deleteAllCollaborators(@PathParam("entity-id") String entityId,
        @PathParam("entity-type") String entityType);
}
