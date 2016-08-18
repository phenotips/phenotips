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

import org.phenotips.rest.Relation;
import org.phenotips.vocabularies.rest.model.Vocabulary;

import org.xwiki.stability.Unstable;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
@Unstable("New API introduced in 1.3")
@Path("/vocabularies/{vocabulary-id}")
@Relation("https://phenotips.org/rel/vocabulary")
public interface VocabularyResource
{
    /**
     * Retrieves the resource used for working with the specified vocabulary.
     *
     * @param vocabularyId the vocabulary identifier, which is also used as a prefix in every term identifier from that
     *            vocabulary, for example {@code HP} or {@code MIM}
     * @return a {@link Vocabulary} representation
     */
    @GET
    Vocabulary getVocabulary(@PathParam("vocabulary-id") String vocabularyId);

    /**
     * Reindex the whole vocabulary, fetching the source from the specified location, or from its
     * {@link Vocabulary#getDefaultSourceLocation() default source location}. This request must come from an
     * administrator.
     *
     * @param vocabularyId the vocabulary to be indexed; will return an error if the vocabulary cannot be resolved
     * @param sourceUrl the URL to be indexed, optional
     * @return a {@link Response} indicating whether the indexing was successful
     */
    @POST
    Response reindex(@PathParam("vocabulary-id") String vocabularyId, @QueryParam("url") String sourceUrl);
}
