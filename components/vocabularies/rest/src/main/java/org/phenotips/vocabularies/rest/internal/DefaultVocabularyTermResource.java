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
package org.phenotips.vocabularies.rest.internal;

import org.phenotips.data.rest.Relations;
import org.phenotips.vocabularies.rest.VocabularyResource;
import org.phenotips.vocabularies.rest.VocabularyTermResource;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

/**
 * Default implementation of the {@link VocabularyTermResource}.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Component
@Named("org.phenotips.vocabularies.rest.internal.DefaultVocabularyTermResource")
@Singleton
public class DefaultVocabularyTermResource extends XWikiResource implements VocabularyTermResource
{
    @Inject
    private VocabularyManager vm;

    @Override
    public Response getTerm(String vocabularyId, String termId)
    {
        if (StringUtils.isEmpty(vocabularyId) || StringUtils.isEmpty(termId)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        Vocabulary vocabulary = this.vm.getVocabulary(vocabularyId);
        if (vocabulary == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        VocabularyTerm term = vocabulary.getTerm(termId);
        if (term == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        JSONObject rep = this.createTermRepresentation(term);
        return Response.ok(rep, MediaType.APPLICATION_JSON_TYPE).build();
    }

    @Override
    public Response resolveTerm(String termId)
    {
        VocabularyTerm term = this.vm.resolveTerm(termId);
        if (term == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        JSONObject rep = this.createTermRepresentation(term);
        return Response.ok(rep, MediaType.APPLICATION_JSON_TYPE).build();
    }

    private JSONObject createTermRepresentation(VocabularyTerm term)
    {
        JSONObject rep = term.toJSON();
        // decorate with links
        JSONObject links = new JSONObject();
        links.accumulate(Relations.SELF, this.uriInfo.getRequestUri().toString());
        links.accumulate(Relations.VOCABULARY, UriBuilder.fromUri(this.uriInfo.getBaseUri())
            .path(VocabularyResource.class)
            .build(term.getVocabulary().getAliases().iterator().next())
            .toString());
        rep.accumulate("links", links);
        return rep;
    }
}
