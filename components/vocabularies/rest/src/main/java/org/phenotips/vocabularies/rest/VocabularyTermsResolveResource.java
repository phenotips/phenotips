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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Resource for resolving multiple vocabulary terms from any valid vocabulary, given a list of their identifiers. Each
 * identifier must have a valid vocabulary prefix of format {@code <vocabulary prefix>:<term id>} (e.g. HP:0000526).
 *
 * @version $Id$
 * @since 1.4
 */
@Path("/vocabularies/terms/fetch")
@Relation("https://phenotips.org/rel/vocabularyTerm")
@ParentResource(VocabulariesResource.class)
public interface VocabularyTermsResolveResource
{
    /**
     * Retrieves a JSON representation of the {@link org.phenotips.vocabulary.VocabularyTerm} objects by resolving the
     * term identifiers using their prefix. The terms that cannot be resolved are ignored. The following request
     * parameters are used:
     *
     * <dl>
     * <dt>term-id</dt>
     * <dd>a list of term IDs that should be resolved</dd>
     * </dl>
     *
     * @return the requested terms that were resolved successfully
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Response resolveTerms();
}
