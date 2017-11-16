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

import org.phenotips.rest.ParentResource;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

/**
 * Resource for working with multiple family records, identified by their given "external" or "internal" identifiers.
 *
 * @version $Id$
 * @since 1.4M3
 */
@Path("/families/suggest")
@ParentResource(FamiliesResource.class)
public interface FamiliesSuggestionsResource
{

    /**
     * Provides term suggestions for the families, where id or external matching input. If no suggestions are found an
     * empty list is returned.
     *
     * @param input criterion to select families by
     * @param maxResults maximal number of results for each query
     * @param requiredPermission permission a user has to have over each family in the result
     * @param orderField field used for ordering the families, can be one of {@code id} (default) or {@code eid}
     * @param order the sorting order, can be one of {@code asc} (default) or {@code desc}
     * @param returnAsJSON if true, the result is returned as JSON, otherwise as XML
     * @return a list of family records
     */
    @GET
    String suggest(
        @QueryParam("input") String input,
        @QueryParam("maxResults") @DefaultValue("10") int maxResults,
        @QueryParam("requiredPermission") @DefaultValue("view") String requiredPermission,
        @QueryParam("orderField") @DefaultValue("id") String orderField,
        @QueryParam("order") @DefaultValue("asc") String order,
        @QueryParam("returnAsJSON") @DefaultValue("true") boolean returnAsJSON);
}
