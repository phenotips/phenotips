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

import org.xwiki.rest.resources.RootResource;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

/**
 * Resource for working with multiple patient records, identified by their given "external" or "internal" identifiers.
 *
 * @version $Id$
 * @since 1.4M3
 */
@Path("/patients/suggest")
@Relation("https://phenotips.org/rel/patientRecords")
@ParentResource(RootResource.class)
public interface PatientsSuggestionsResource
{

    /**
     * Provides term suggestions for the patient records, where id or external matching input. If no suggestions are
     * found an empty list is returned.
     *
     * @param input The string which will be used to generate suggestions
     * @param maxResults The maximum number of results to be returned
     * @param requiredPermission permission a user has to have over each patient record in the result
     * @param markFamilyAssociation boolean indicator for adding family info into patient description
     * @param orderField field used for ordering the patients, can be one of {@code id} (default) or {@code eid}
     * @param order the sorting order, can be one of {@code asc} (default) or {@code desc}
     * @param returnAsJSON if true, the result is returned as JSON, otherwise as XML
     * @return a list of patient records
     */
    @GET
    String suggest(
        @QueryParam("input") String input,
        @QueryParam("maxResults") @DefaultValue("10") int maxResults,
        @QueryParam("requiredPermission") @DefaultValue("view") String requiredPermission,
        @QueryParam("markFamilyAssociation") @DefaultValue("false") boolean markFamilyAssociation,
        @QueryParam("orderField") @DefaultValue("id") String orderField,
        @QueryParam("order") @DefaultValue("asc") String order,
        @QueryParam("returnAsJSON") @DefaultValue("true") boolean returnAsJSON);
}
