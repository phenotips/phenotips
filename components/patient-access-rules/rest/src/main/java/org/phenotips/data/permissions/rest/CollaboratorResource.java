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
import org.phenotips.rest.ParentResource;
import org.phenotips.rest.Relation;
import org.phenotips.rest.RequiredAccess;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Resource for working with patient collaborators, one at a time. Collaborators are found by both the internal patient
 * identifier and the internal collaborator identifier.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Path("/patients/{patient-id}/permissions/collaborators/{collaborator-id}")
@Relation("https://phenotips.org/rel/collaborator")
@ParentResource(CollaboratorsResource.class)
public interface CollaboratorResource
{
    /**
     * Retrieves information about a particular collaborator. If the indicated patient record doesn't exist, or if the
     * indicated user is not a collaborator, or if the user sending the request doesn't have the right to view the
     * target patient record, an error is returned.
     *
     * @param patientId internal identifier of a patient record
     * @param collaboratorId internal id of a collaborator, ideally fully qualified, (ex. {@code xwiki:XWiki.JohnDoe})
     * @return REST representation of a single collaborator
     */
    @GET
    @RequiredAccess("view")
    CollaboratorRepresentation getCollaborator(@PathParam("patient-id") String patientId,
        @PathParam("collaborator-id") String collaboratorId);

    /**
     * Updates the access level of a collaborator. If the indicated patient record doesn't exist, or if the user sending
     * the request doesn't have the right to edit the target patient record, or if no access level is sent, or if the
     * requested access level isn't valid, then no change is performed and an error is returned.
     *
     * @param collaborator a collaborator representation, must contain the "level" property, with a value which is a
     *            valid access level; all other properties are ignored
     * @param patientId internal identifier of a patient record
     * @param collaboratorId internal id of a collaborator, ideally fully qualified, (ex. {@code xwiki:XWiki.JohnDoe})
     * @return a status message
     */
    @PUT
    @RequiredAccess("manage")
    @Consumes(MediaType.APPLICATION_JSON)
    Response setLevel(CollaboratorRepresentation collaborator, @PathParam("patient-id") String patientId,
        @PathParam("collaborator-id") String collaboratorId);

    /**
     * Updates the access level of a collaborator. If the indicated patient record doesn't exist, or if the user sending
     * the request doesn't have the right to edit the target patient record, or if no access level is sent, or if the
     * requested access level isn't valid, then no change is performed and an error is returned. The request must
     * contain a "level" property, with a value which is a valid access level.
     *
     * @param patientId internal identifier of a patient record
     * @param collaboratorId internal id of a collaborator, ideally fully qualified, (ex. {@code xwiki:XWiki.JohnDoe})
     * @return a status message
     */
    @PUT
    @RequiredAccess("manage")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Response setLevel(@PathParam("patient-id") String patientId, @PathParam("collaborator-id") String collaboratorId);

    /**
     * Removes a particular collaborator from a patient record. If the indicated patient record doesn't exist, or if the
     * user sending the request doesn't have the right to edit the target patient record, or if the requested
     * collaborator isn't actually a collaborator, no change is performed and an error is returned.
     *
     * @param patientId internal identifier of a patient record
     * @param collaboratorId internal id of a collaborator, ideally fully qualified, (ex. {@code xwiki:XWiki.JohnDoe})
     * @return a status message
     */
    @DELETE
    @RequiredAccess("manage")
    Response deleteCollaborator(@PathParam("patient-id") String patientId,
        @PathParam("collaborator-id") String collaboratorId);
}
