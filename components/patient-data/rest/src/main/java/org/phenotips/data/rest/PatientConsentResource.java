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
 * Resource for working with patient records, identified by their internal PhenoTips identifier.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Path("/patients/{patient-id}/consents")
@Relation("https://phenotips.org/rel/consents")
@ParentResource(PatientResource.class)
public interface PatientConsentResource
{
    /**
     * Retrieves a list of all consents for a given patient.
     *
     * @param patientId of a record whose consents to retrieve
     * @return a JSON array of all consents if the user is allowed to do so and such a record exists, otherwise a failed
     * response.
     */
    @GET
    Response getConsents(@PathParam("patient-id") String patientId);

    /**
     * For granting a single consent in a patient record.
     * @param patientId of the record which is to be affected
     * @param id of the consent to grant
     * @return response with code 200 if successful, or a response with a non-successful code
     */
    @PUT
    @Path("/grant")
    @Consumes(MediaType.TEXT_PLAIN)
    Response grantConsent(@PathParam("patient-id") String patientId, String id);

    /**
     * For revoking a single consent in a patient record.
     * @param patientId of the record which is to be affected
     * @param id of the consent to revoke
     * @return response with code 200 if successful, or a response with a non-successful code
     */
    @PUT
    @Path("/revoke")
    @Consumes(MediaType.TEXT_PLAIN)
    Response revokeConsent(@PathParam("patient-id") String patientId, String id);

    /**
     * For setting a set of consents at the same time.
     *
     * Note: for technical reasons both grantConsent() and revokeConsent() block the
     * given patient document for a short time, in a way that all other requests to grant or revoke a consent
     * for the same patient will fail until the document is "unblocked". So if multiple consents should
     * be changed at the same time it is advised to use this method instead of multiple gant/revoke calls.
     *
     * @param patientId of the record which is to be affected
     * @param json a string representing a JSOn array of consent IDs
     * @return response with code 200 if successful, or a response with a non-successful code
     */
    @PUT
    @Path("/assign")
    @Consumes(MediaType.APPLICATION_JSON)
    Response assignConsents(@PathParam("patient-id") String patientId, String json);
}
