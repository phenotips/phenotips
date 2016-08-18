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
import javax.ws.rs.DELETE;
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
 * @since 1.2M5
 */
@Path("/patients/{patient-id}")
@Relation("https://phenotips.org/rel/patientRecord")
@ParentResource(PatientsResource.class)
public interface PatientResource
{
    /**
     * Retrieve a patient record, identified by its internal PhenoTips identifier, in its JSON representation. If the
     * indicated patient record doesn't exist, or if the user sending the request doesn't have the right to view the
     * target patient record, an error is returned.
     *
     * @param id the patient's internal identifier, see {@link org.phenotips.data.Patient#getId()}
     * @return the JSON representation of the requested patient, or a status message in case of error
     */
    @GET
    Response getPatient(@PathParam("patient-id") String id);

    /**
     * Update a patient record, identified by its internal PhenoTips identifier, from its JSON representation. If the
     * indicated patient record doesn't exist, or if the user sending the request doesn't have the right to edit the
     * target patient record, no change is performed and an error is returned. If a field is set in the patient record,
     * but missing in the JSON, then that field is not changed.
     *
     * @param json the JSON representation of the new patient to add
     * @param id the patient's internal identifier, see {@link org.phenotips.data.Patient#getId()}
     * @return a status message
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    Response updatePatient(String json, @PathParam("patient-id") String id);

    /**
     * Delete a patient record, identified by its internal PhenoTips identifier. If the indicated patient record doesn't
     * exist, or if the user sending the request doesn't have the right to delete the target patient record, no change
     * is performed and an error is returned.
     *
     * @param id the patient's internal identifier, see {@link org.phenotips.data.Patient#getId()}
     * @return a status message
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    Response deletePatient(@PathParam("patient-id") String id);
}
