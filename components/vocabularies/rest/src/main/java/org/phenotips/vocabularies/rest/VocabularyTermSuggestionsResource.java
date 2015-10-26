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

import org.phenotips.vocabularies.rest.model.VocabularyTerms;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

/**
 * Resource for searching in a vocabulary for terms matching an input (vocabulary suggest).
 *
 * @version $Id$
 * @since 1.3M1
 */
@Path("/vocabularies/terms/{vocabulary}/suggest")
public interface VocabularyTermSuggestionsResource
{
    /**
     * Provides term suggestions for the specified {@link org.phenotips.vocabulary.Vocabulary} as a list of
     * {@link VocabularyTerms}. Request can optionally specify additional filters. If no suggestions are found an empty
     * list is returned.
     *
     * @param vocabularyId The ID of the {@link org.phenotips.vocabulary.Vocabulary} to be used for suggestions. Any
     *            alias of the vocabulary can be used. If no matching vocabulary is found an error is returned to the
     *            user.
     * @param input The string which will be used to generate suggestions
     * @param maxResults The maximum number of results to be returned
     * @param sort an optional sort parameter, in a format that depends on the actual engine that stores the vocabulary;
     *            usually a property name followed by {@code asc} or {@code desc}; Usually empty
     * @param customFilter a custom filter query to further restrict which terms may be returned, in a format that
     *            depends on the actual engine that stores the vocabulary; some vocabularies may not support a filter
     *            query; may be empty
     * @return A {@link VocabularyTerms} representing a list of
     *         {@link org.phenotips.vocabularies.rest.model.VocabularyTerm} suggestions.
     */
    @GET
    VocabularyTerms suggest(@PathParam("vocabulary") String vocabularyId,
        @QueryParam("input") String input,
        @QueryParam("maxResults") @DefaultValue("10") int maxResults,
        @QueryParam("sort") String sort,
        @QueryParam("customFilter") String customFilter);
}
