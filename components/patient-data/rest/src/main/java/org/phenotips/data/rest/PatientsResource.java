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

import org.phenotips.data.rest.model.Patients;
import org.phenotips.rest.ParentResource;
import org.phenotips.rest.Relation;
import org.phenotips.rest.RequiredAccess;

import org.xwiki.rest.resources.RootResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Root resource for working with patient records.
 *
 * @version $Id$
 * @since 1.2M5
 */
@Path("/patients")
@Relation("https://phenotips.org/rel/patientRecordsRepository")
@ParentResource(RootResource.class)
public interface PatientsResource
{
    /**
     * Import a new patient from its JSON representation.
     *
     * @param json the JSON representation of the new patient to add
     * @return the location of the newly created patient if the operation was successful, or an error report otherwise
     */
    @Consumes(MediaType.APPLICATION_JSON)
    @POST
    @RequiredAccess("edit")
    Response addPatient(String json);

    /**
     * @param start for large result set paging, the index of the first patient to display in the returned page
     * @param number for large result set paging, how many patients to display in the returned page
     * @param orderField field used for ordering the patients, can be one of {@code id} (default) or {@code eid}
     * @param order the sorting order, can be one of {@code asc} (default) or {@code desc}
     * @return a list of patient records
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RequiredAccess("view")
    Patients listPatients(
        @QueryParam("start") @DefaultValue("0") Integer start,
        @QueryParam("number") @DefaultValue("30") Integer number,
        @QueryParam("orderField") @DefaultValue("id") String orderField,
        @QueryParam("order") @DefaultValue("asc") String order);
}
