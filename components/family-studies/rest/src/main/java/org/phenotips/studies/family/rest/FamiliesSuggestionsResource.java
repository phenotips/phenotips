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
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * Resource for searching for families matching an input (family suggest).
 *
 * @version $Id$
 * @since 1.4
 */
@Path("/families/suggest")
@ParentResource(FamiliesResource.class)
public interface FamiliesSuggestionsResource
{
    /**
     * Provides family suggestions as JSON, where the identifier or the external identifier match the input. If no
     * suggestions are found, an empty list is returned.
     *
     * @param input the string which will be used to generate suggestions
     * @param maxResults maximal number of results to be returned
     * @param requiredPermission permission a user has to have over each family in the result
     * @param orderField field used for ordering the families, can be one of {@code id} (default) or {@code eid}
     * @param order the sorting order, can be one of {@code asc} (default) or {@code desc}
     * @return a list of family records
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    String suggestAsJSON(
        @QueryParam("input") String input,
        @QueryParam("maxResults") @DefaultValue("10") int maxResults,
        @QueryParam("requiredPermission") @DefaultValue("view") String requiredPermission,
        @QueryParam("orderField") @DefaultValue("id") String orderField,
        @QueryParam("order") @DefaultValue("asc") String order);

    /**
     * Provides family suggestions as XML, where id or external match the input. If no suggestions are found, an empty
     * list is returned. The format is the one expected by XWiki's global search widget.
     *
     * @param input the string which will be used to generate suggestions
     * @param maxResults maximal number of results to be returned
     * @param requiredPermission permission a user has to have over each family in the result
     * @param orderField field used for ordering the families, can be one of {@code id} (default) or {@code eid}
     * @param order the sorting order, can be one of {@code asc} (default) or {@code desc}
     * @return a list of family records
     */
    @GET
    @Produces(MediaType.APPLICATION_XML)
    String suggestAsXML(
        @QueryParam("input") String input,
        @QueryParam("maxResults") @DefaultValue("10") int maxResults,
        @QueryParam("requiredPermission") @DefaultValue("view") String requiredPermission,
        @QueryParam("orderField") @DefaultValue("id") String orderField,
        @QueryParam("order") @DefaultValue("asc") String order);
}
