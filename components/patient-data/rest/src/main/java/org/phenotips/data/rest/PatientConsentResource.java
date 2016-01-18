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
package org.phenotips.data.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Resource for working with patient records, identified by their internal PhenoTips identifier.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Path("/patients/{patient_id}/consents")
public interface PatientConsentResource
{
    /**
     * Retrieves a list of all consents for a given patient.
     *
     * @param patientId of a record whose consents to retrieve
     * @return a JSON array of all consents if the user is allowed to do so and such a record exists, otherwise a failed
     * response.
     */
    @GET Response getConsents(@PathParam("patient_id") String patientId);

    /**
     * For granting a single consent in a patient record.
     * @param patientId of the record which is to be affected
     * @param id of the consent to grant
     * @return response with code 200 if successful, or a response with a non-successful code
     */
    @PUT
    @Path("/grant")
    @Consumes(MediaType.TEXT_PLAIN) Response grantConsent(@PathParam("patient_id") String patientId, String id);

    /**
     * For revoking a single consent in a patient record.
     * @param patientId of the record which is to be affected
     * @param id of the consent to revoke
     * @return response with code 200 if successful, or a response with a non-successful code
     */
    @PUT
    @Path("/revoke")
    @Consumes(MediaType.TEXT_PLAIN) Response revokeConsent(@PathParam("patient_id") String patientId, String id);
}
