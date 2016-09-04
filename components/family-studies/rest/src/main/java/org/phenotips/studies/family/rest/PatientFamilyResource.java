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
package org.phenotips.studies.family.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 * Resource for working with patient families, identified by patient's internal PhenoTips identifier.
 *
 * @version $Id$
 * @since 1.2M5
 */
@Path("/patients/{id}/family")
public interface PatientFamilyResource
{
    /**
     * Retrieve family data in its JSON representation for the given patient record, identified by its internal
     * PhenoTips identifier. If the indicated patient record doesn't exist, or if the user sending the request
     * doesn't have the right to view the target patient record, an error is returned.
     *
     * @param id the patient's internal identifier, see {@link org.phenotips.data.Patient#getId()}
     * @return the JSON representation of the requested patient's family, or a status message in case of error
     */
    @GET
    Response getFamily(@PathParam("id") String id);
}
