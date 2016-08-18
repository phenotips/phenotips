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

import org.phenotips.rest.Relation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Resource for working with patient records, identified by their given "external" identifier.
 *
 * @version $Id$
 * @since 1.2M5
 */
@Path("/patients/eid/{eid}")
@Relation("https://phenotips.org/rel/patientRecord")
public interface PatientByExternalIdResource
{
    /**
     * Retrieve a patient record, identified by its given "external" identifier, in its JSON representation. If the
     * indicated patient record doesn't exist, or if the user sending the request doesn't have the right to view the
     * target patient record, an error is returned. If multiple records exist with the same given identifier, a list of
     * links to each such record is returned.
     *
     * @param eid the patient's given "external" identifier, see {@link org.phenotips.data.Patient#getExternalId()}
     * @return the JSON representation of the requested patient, or a status message in case of error
     */
    @GET
    Response getPatient(@PathParam("eid") String eid);

    /**
     * Update a patient record, identified by its given "external" identifier, from its JSON representation. If the
     * indicated patient record doesn't exist, or if the user sending the request doesn't have the right to edit the
     * target patient record, no change is performed and an error is returned. If multiple records exist with the same
     * given identifier, no change is performed, and a list of links to each such record is returned. If a field is set
     * in the patient record, but missing in the JSON, then that field is not changed.
     *
     * @param json the JSON representation of the new patient to add
     * @param eid the patient's given "external" identifier, see {@link org.phenotips.data.Patient#getExternalId()}
     * @return a status message
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    Response updatePatient(String json, @PathParam("eid") String eid);

    /**
     * Delete a patient record, identified by its given "external" identifier. If the indicated patient record doesn't
     * exist, or if the user sending the request doesn't have the right to edit the target patient record, no change is
     * performed and an error is returned. If multiple records exist with the same given identifier, no change is
     * performed, and a list of links to each such record is returned.
     *
     * @param eid the patient's given "external" identifier, see {@link org.phenotips.data.Patient#getExternalId()}
     * @return a status message
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    Response deletePatient(@PathParam("eid") String eid);
}
