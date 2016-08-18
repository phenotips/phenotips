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

import org.xwiki.stability.Unstable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 * Resource for working with individual {@link org.phenotips.vocabulary.VocabularyTerm} when the containing vocabulary
 * is not known.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Unstable("New API introduced in 1.3")
@Path("/vocabularies/terms/{term-id}")
@Relation("https://phenotips.org/rel/vocabularyTerm")
public interface VocabularyTermResolveResource
{
    /**
     * Retrieves a JSON representation of the {@link org.phenotips.vocabulary.VocabularyTerm} by resolving the term
     * using its prefix.
     *
     * @param termId the term identifier, in the format {@code <vocabulary prefix>:<term id>}, for example
     *            {@code HP:0002066}
     * @return the requested term, or an error if the term could not be resolved
     */
    @GET
    Response resolveTerm(@PathParam("term-id") String termId);
}
