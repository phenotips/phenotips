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
import org.phenotips.rest.PATCH;
import org.phenotips.rest.ParentResource;
import org.phenotips.rest.Relation;
import org.phenotips.rest.RequiredAccess;

import org.xwiki.component.annotation.Role;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Resource for working with patient record's collaborators, in bulk, where patients are identified by patient record's
 * internal PhenoTips identifier.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Role
@Path("/patients/{patient-id}/permissions/collaborators")
@Relation("https://phenotips.org/rel/collaborators")
@ParentResource(PermissionsResource.class)
public interface CollaboratorsResource
{
    /**
     * Retrieves information about the collaborators. If the indicated patient record doesn't exist, or if the user
     * sending the request doesn't have the right to view the target patient record, an error is returned.
     *
     * @param patientId internal identifier of a patient record
     * @return REST representation of a collection of patient record's collaborators
     */
    @GET
    @RequiredAccess("view")
    CollaboratorsRepresentation getCollaborators(@PathParam("patient-id") String patientId);

    /**
     * Adds a new collaborator, or updates the permission level of a collaborator. If the indicated patient record
     * doesn't exist, or if the user sending the request doesn't have the right to edit the target patient record, no
     * change is performed and an error is returned.
     *
     * @param collaborators a list of collaborators to add, each of which must have {@code id} and {@code level}
     *            properties
     * @param patientId internal identifier of a patient record
     * @return a status message
     */
    @PATCH
    @RequiredAccess("manage")
    @Consumes(MediaType.APPLICATION_JSON)
    Response addCollaborators(CollaboratorsRepresentation collaborators, @PathParam("patient-id") String patientId);

    /**
     * Adds a new collaborator, or updates the permission level of a collaborator. If the indicated patient record
     * doesn't exist, or if the user sending the request doesn't have the right to edit the target patient record, no
     * change is performed and an error is returned. The payload of the request must contain properties "collaborator"
     * and "level".
     *
     * @param patientId internal identifier of a patient record
     * @return a status message
     */
    @PATCH
    @RequiredAccess("manage")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Response addCollaborators(@PathParam("patient-id") String patientId);

    /**
     * Updates all collaborators, replacing all previous collaborators. If the indicated patient record doesn't exist,
     * or if the user sending the request doesn't have the right to edit the target patient record, no change is
     * performed and an error is returned.
     *
     * @param collaborators a list of collaborators, each of which must have {@code id} and {@code level} properties
     * @param patientId internal identifier of a patient record
     * @return a status message
     */
    @PUT
    @RequiredAccess("manage")
    @Consumes(MediaType.APPLICATION_JSON)
    Response setCollaborators(CollaboratorsRepresentation collaborators, @PathParam("patient-id") String patientId);

    /**
     * Deletes all collaborators. If the indicated patient record doesn't exist, or if the user sending the request
     * doesn't have the right to edit the target patient record, no change is performed and an error is returned.
     *
     * @param patientId internal identifier of a patient record
     * @return a status message
     */
    @DELETE
    @RequiredAccess("manage")
    Response deleteAllCollaborators(@PathParam("patient-id") String patientId);
}
