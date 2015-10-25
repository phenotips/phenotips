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

import org.phenotips.vocabularies.rest.model.Vocabulary;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * A resource for working with an individual {@link org.phenotips.vocabulary.Vocabulary}.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Path("/vocabularies/{vocabulary}")
public interface VocabularyResource
{
    /**
     * Reindex the whole vocabulary, fetching the source from the specified location. This request must come from an
     * adminstrator.
     *
     * @param url the URL to be indexed
     * @param vocabularyId the vocabulary to be indexed. Will return an error if the vocabulary cannot be resolved.
     * @return A {@link Response} indicating whether the indexing was successful
     */
    @PUT
    Response reindex(@QueryParam("url") String url, @PathParam("vocabulary") String vocabularyId);

    /**
     * Retrieves the resource used for working with the specified individual resource.
     *
     * @param vocabularyId the vocabulary identifier, which is also used as a prefix in every term identifier from that
     *            vocabulary, for example {@code HP} or {@code MIM}
     * @return a {@link Vocabulary} representation
     */
    @GET
    Vocabulary getVocabulary(@PathParam("vocabulary") String vocabularyId);
}
