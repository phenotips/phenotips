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

import org.phenotips.data.permissions.rest.model.PermissionsRepresentation;
import org.phenotips.data.rest.PatientResource;
import org.phenotips.rest.PATCH;
import org.phenotips.rest.ParentResource;
import org.phenotips.rest.Relation;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Resource for working with patient record's owners, visibility and collaborators. Patients are identified by patient
 * record's internal PhenoTips identifier.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Path("/patients/{patient-id}/permissions")
@Relation("https://phenotips.org/rel/permissions")
@ParentResource(PatientResource.class)
public interface PermissionsResource
{
    /**
     * Retrieves all permissions: owner, collaborators, visibility. If the indicated patient record doesn't exist, or if
     * the user sending the request doesn't have the right to view the target patient record, an error is returned.
     *
     * @param patientId internal identifier of a patient record
     * @return REST representation of the permissions of a patient record
     */
    @GET
    PermissionsRepresentation getPermissions(@PathParam("patient-id") String patientId);

    /**
     * Overwrites all permissions: owner, collaborators, visibility. All elements must be present in the input JSON. If
     * the indicated patient record doesn't exist, or if the user sending the request doesn't have the right to edit the
     * target patient record, or if either element is missing, no change is performed and an error is returned. To
     * remove all collaborators, an empty {@code "collaborators": {"collaborators": []}} JSON fragment must be sent.
     *
     * @param permissions must contain owner and visibility representations, and a list of collaborator representations
     * @param patientId identifier of the patient whose permissions should be changed
     * @return a status message
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    Response setPermissions(PermissionsRepresentation permissions, @PathParam("patient-id") String patientId);

    /**
     * Update permissions: owner, collaborators, visibility. Not all elements must be present in the input JSON, the
     * missing pieces will be left as-is. The submitted owner and visibility will be updated, but the submitted
     * collaborators will be added to the existing list of collaborators. To remove an individual collaborator, send a
     * {@code DELETE} request to the targeted {@link CollaboratorResource}, or {@code PUT} the full permissions without
     * the collaborators to be removed. If the indicated patient record doesn't exist, or if the user sending the
     * request doesn't have the right to edit the target patient record, no change is performed and an error is
     * returned.
     *
     * @param permissions may contain owner and visibility representations, and a list of collaborator representations
     * @param patientId identifier of the patient whose permissions should be changed
     * @return a status message
     */
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    Response updatePermissions(PermissionsRepresentation permissions, @PathParam("patient-id") String patientId);
}
