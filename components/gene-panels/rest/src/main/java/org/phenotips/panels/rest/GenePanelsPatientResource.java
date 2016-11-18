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
package org.phenotips.panels.rest;

import org.xwiki.stability.Unstable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Patient resource for working with gene panels.
 *
 * @version $Id$
 * @since 1.3M5
 */
@Unstable("New API introduced in 1.3")
@Path("/panels/{id}")
public interface GenePanelsPatientResource
{
    /**
     * Retrieves a JSON representation of genes associated with HPO terms for with a patient of interest, as
     * well as the counts for each gene.
     *
     * @param patientId the ID of the patient of interest
     * @return gene counts data if successful, an error code otherwise
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Response getPatientGeneCounts(@PathParam("id") final String patientId);
}
