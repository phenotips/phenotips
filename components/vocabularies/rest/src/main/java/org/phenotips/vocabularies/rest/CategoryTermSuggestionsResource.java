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
package org.phenotips.vocabularies.rest;

import org.phenotips.rest.ParentResource;
import org.phenotips.rest.Relation;
import org.phenotips.vocabulary.VocabularyTerm;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Resource for searching in a category for terms matching an input (vocabulary suggest).
 *
 * @version $Id$
 * @since 1.4
 */
@Path("/vocabularies/categories/{category}/suggest")
@ParentResource(CategoryResource.class)
@Relation("https://phenotips.org/rel/categoryTermSuggest")
public interface CategoryTermSuggestionsResource
{
    /**
     * Provides term suggestions for the specified vocabulary category as a {@link Response}. Request can optionally
     * specify the maximum number of results to return. If no suggestions are found, {@link Response} will not contain
     * any terms.
     *
     * @param category The name of the vocabulary category to be used for suggestions. If no matching category is found,
     *            an error is returned to the user
     * @param input The string which will be used to generate suggestions
     * @param maxResults The maximum number of results to be returned; default is 10
     * @return A {@link Response} object representing a list of {@link VocabularyTerm term} suggestions.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Response suggest(
        @PathParam("category") String category,
        @QueryParam("input") String input,
        @QueryParam("maxResults") @DefaultValue("10") int maxResults);
}
