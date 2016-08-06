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

import org.phenotips.data.permissions.rest.internal.utils.annotations.Relation;
import org.phenotips.data.permissions.rest.model.CollaboratorRepresentation;

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
public interface CollaboratorResource
{
    /**
     * Retrieves information about a particular collaborator. If the indicated patient record doesn't exist, or if the
     * user sending the request doesn't have the right to view the target patient record, an error is returned.
     *
     * @param patientId internal identifier of a patient record
     * @param collaboratorId fully qualified, internal id of a collaborator (ex. xwiki:XWiki.JohnAdams)
     * @return REST representation of a single collaborator
     */
    @GET
    CollaboratorRepresentation getCollaborator(@PathParam("patient-id") String patientId,
        @PathParam("collaborator-id") String collaboratorId);

    /**
     * Updates the access level of a collaborator. If the indicated patient record doesn't exist, or if the user sending
     * the request doesn't have the right to edit the target patient record, no change is performed and an error is
     * returned.
     *
     * @param json must contain "level" property, with a value which is a valid access level
     * @param patientId internal identifier of a patient record
     * @param collaboratorId fully qualified, internal id of a collaborator (ex. xwiki:XWiki.JohnAdams)
     * @return a status message
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    Response putLevelWithJson(String json, @PathParam("patient-id") String patientId,
        @PathParam("collaborator-id") String collaboratorId);

    /**
     * Updates the access level of a collaborator. If the indicated patient record doesn't exist, or if the user sending
     * the request doesn't have the right to edit the target patient record, no change is performed and an error is
     * returned. The request must contain "level" property, with a value which is a valid access level.
     *
     * @param patientId internal identifier of a patient record
     * @param collaboratorId fully qualified, internal id of a collaborator (ex. xwiki:XWiki.JohnAdams)
     * @return a status message
     */
    @PUT
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Response putLevelWithForm(@PathParam("patient-id") String patientId,
        @PathParam("collaborator-id") String collaboratorId);

    /**
     * Removes a particular collaborator from a patient record. If the indicated patient record doesn't exist, or if the
     * user sending the request doesn't have the right to edit the target patient record, no change is performed and an
     * error is returned.
     *
     * @param patientId internal identifier of a patient record
     * @param collaboratorId fully qualified, internal id of a collaborator (ex. xwiki:XWiki.JohnAdams)
     * @return a status message
     */
    @DELETE
    Response deleteCollaborator(@PathParam("patient-id") String patientId,
        @PathParam("collaborator-id") String collaboratorId);
}
