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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Resource for working with multiple patient records, identified by their given "external" or "internal" identifiers.
 *
 * @version $Id$
 * @since 1.3M5
 */
@Path("/patients/fetch")
@Relation("https://phenotips.org/rel/patientRecords")
@ParentResource(PatientsResource.class)
public interface PatientsFetchResource
{
    /**
     * Retrieve multiple patient records, identified by their given "external" or "internal" identifiers, in their JSON
     * representation. If any of the indicated patient records don't exist, or if the user sending the request doesn't
     * have the right to view any of the target patient records, they are excluded from the search results.
     *
     * @return JSON representations of the requested patients, or a status message in case of error
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Response fetchPatients();
}
