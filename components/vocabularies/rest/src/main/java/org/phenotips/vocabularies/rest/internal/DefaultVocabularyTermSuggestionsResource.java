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

import org.phenotips.rest.Autolinker;
import org.phenotips.vocabularies.rest.VocabularyTermResource;
import org.phenotips.vocabularies.rest.VocabularyTermSuggestionsResource;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;
import org.xwiki.stability.Unstable;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Default implementation of the {@link org.phenotips.vocabularies.rest.VocabularyTermSuggestionsResource}.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Component
@Named("org.phenotips.vocabularies.rest.internal.DefaultVocabularyTermSuggestionsResource")
@Singleton
@Unstable
public class DefaultVocabularyTermSuggestionsResource extends XWikiResource implements
    VocabularyTermSuggestionsResource
{
    @Inject
    private VocabularyManager vm;

    @Inject
    private Provider<Autolinker> autolinker;

    @Override
    public Response suggest(String vocabularyId, String input, @DefaultValue("10") int maxResults, String sort,
        String customFilter)
    {
        if (StringUtils.isEmpty(input) || StringUtils.isEmpty(vocabularyId)) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        Vocabulary vocabulary = this.vm.getVocabulary(vocabularyId);
        if (vocabulary == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        List<VocabularyTerm> termSuggestions = vocabulary.search(input, maxResults, sort, customFilter);

        JSONObject rep = new JSONObject();
        JSONArray trms = new JSONArray();
        for (VocabularyTerm term : termSuggestions) {
            JSONObject trm = term.toJSON();
            trm.put("links", this.autolinker.get().forSecondaryResource(VocabularyTermResource.class, this.uriInfo));
            trms.put(trm);
        }

        rep.put("rows", trms);
        rep.put("links", this.autolinker.get().forResource(getClass(), this.uriInfo).build());
        return Response.ok(rep, MediaType.APPLICATION_JSON_TYPE).build();
    }
}
