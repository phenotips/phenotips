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

import org.phenotips.data.rest.model.PatientVisibilityRepresentation;

import org.xwiki.component.annotation.Role;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Resource for working with visibility of patient records, where the patient record is identified by its internal
 * identifier.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Role
@Path("/patients/{patient-id}/permissions/visibility")
public interface VisibilityResource
{
    /**
     * Retrieves the {@link Visibility} of a patient identified by `patientId`. If the indicated patient record doesn't
     * exist, or if the user sending the request doesn't have the right to view the target patient record, an error is
     * returned.
     *
     * @param patientId identifier of the patient whose visibility to retrieve
     * @return a representation of {@link Visibility} of the patient
     */
    @GET PatientVisibilityRepresentation getVisibility(@PathParam("patient-id") String patientId);

    /**
     * Updates the visibility of a patient. If the indicated patient record doesn't exist, or if the user sending the
     * request doesn't have the right to edit the target patient record, no change is performed and an error is
     * returned.
     *
     * @param json which must contain "level" parameter, with a value of a visibility level
     * @param patientId whose visibility level is to be updated
     * @return a status message
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON) Response putVisibilityWithJson(String json,
        @PathParam("patient-id") String patientId);

    /**
     * Updates the visibility of a patient. If the indicated patient record doesn't exist, or if the user sending the
     * request doesn't have the right to edit the target patient record, no change is performed and an error is
     * returned. The request must contain a "visibility" parameter, with a value of a visibility level
     *
     * @param patientId whose visibility level is to be updated
     * @return a status message
     */
    @PUT
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED) Response putVisibilityWithForm(
        @PathParam("patient-id") String patientId);
}
