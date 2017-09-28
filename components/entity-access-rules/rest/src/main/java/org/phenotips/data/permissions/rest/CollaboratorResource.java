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

import org.phenotips.data.permissions.rest.model.CollaboratorRepresentation;
import org.phenotips.data.rest.PatientResource;
import org.phenotips.rest.ParentResource;
import org.phenotips.rest.RelatedResources;
import org.phenotips.rest.Relation;
import org.phenotips.rest.RequiredAccess;

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
 * Resource for working with entity record collaborators, one at a time. Collaborators are found by both the internal
 * entity identifier and the internal collaborator identifier.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Path("/{entity-type}/{entity-id}/permissions/collaborators/{collaborator-id}")
@Relation("https://phenotips.org/rel/collaborator")
@ParentResource(CollaboratorsResource.class)
@RelatedResources({ PermissionsResource.class, PatientResource.class })
public interface CollaboratorResource
{
    /**
     * Retrieve information about a particular collaborator. If the indicated entity record doesn't exist, or if the
     * indicated principal is not a collaborator, or if the user sending the request doesn't have the right to view the
     * target entity record, an error is returned.
     *
     * @param entityId internal identifier of an entity record
     * @param entityType the type of entity
     * @param collaboratorId internal id of a principal, ideally fully qualified, (ex. {@code xwiki:XWiki.JohnDoe})
     * @return REST representation of a single collaborator
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RequiredAccess("view")
    CollaboratorRepresentation getCollaborator(@PathParam("entity-id") String entityId,
        @PathParam("entity-type") String entityType, @PathParam("collaborator-id") String collaboratorId);

    /**
     * Update the access level of a collaborator. If the targeted principal wasn't a collaborator before, then it is
     * added as one. If the indicated entity record doesn't exist, or if the user sending the request doesn't have the
     * right to edit the target entity record, or if no access level is sent, or if the requested access level isn't
     * valid, then no change is performed and an error is returned.
     *
     * @param collaborator a collaborator representation, must contain the "level" property, with a value which is a
     *            valid access level; all other properties are ignored
     * @param entityId internal identifier of an entity record
     * @param entityType the type of entity
     * @param collaboratorId internal id of a principal, ideally fully qualified, (ex. {@code xwiki:XWiki.JohnDoe})
     * @return a status message
     */
    @PUT
    @RequiredAccess("manage")
    @Consumes(MediaType.APPLICATION_JSON)
    Response setLevel(CollaboratorRepresentation collaborator, @PathParam("entity-id") String entityId,
        @PathParam("entity-type") String entityType, @PathParam("collaborator-id") String collaboratorId);

    /**
     * Update the access level of a collaborator. If the targeted principal wasn't a collaborator before, then it is
     * added as one. If the indicated entity record doesn't exist, or if the user sending the request doesn't have the
     * right to edit the target entity record, or if no access level is sent, or if the requested access level isn't
     * valid, then no change is performed and an error is returned. The request must contain a "level" property, with a
     * value which is a valid access level.
     *
     * @param entityId internal identifier of an entity record
     * @param entityType the type of entity
     * @param collaboratorId internal id of a principal, ideally fully qualified, (ex. {@code xwiki:XWiki.JohnDoe})
     * @return a status message
     */
    @PUT
    @RequiredAccess("manage")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Response setLevel(@PathParam("entity-id") String entityId, @PathParam("entity-type") String entityType,
        @PathParam("collaborator-id") String collaboratorId);

    /**
     * Remove a particular collaborator from an entity record. If the indicated entity record doesn't exist, or if the
     * user sending the request doesn't have the right to edit the target entity record, or if the requested
     * collaborator isn't actually a collaborator, no change is performed and an error is returned.
     *
     * @param entityId internal identifier of an entity record
     * @param entityType the type of entity
     * @param collaboratorId internal id of a principal, ideally fully qualified, (ex. {@code xwiki:XWiki.JohnDoe})
     * @return a status message
     */
    @DELETE
    @RequiredAccess("manage")
    Response deleteCollaborator(@PathParam("entity-id") String entityId, @PathParam("entity-type") String entityType,
        @PathParam("collaborator-id") String collaboratorId);
}
