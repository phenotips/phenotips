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
import org.phenotips.vocabularies.rest.VocabularyTermResolveResource;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONObject;

/**
 * Default implementation of the {@link VocabularyTermResolveResource}.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Component
@Named("org.phenotips.vocabularies.rest.internal.DefaultVocabularyTermResolveResource")
@Singleton
public class DefaultVocabularyTermResolveResource extends XWikiResource implements VocabularyTermResolveResource
{
    @Inject
    private VocabularyManager vm;

    @Inject
    private Provider<Autolinker> autolinker;

    @Override
    public Response resolveTerm(String termId)
    {
        VocabularyTerm term = this.vm.resolveTerm(termId);
        if (term == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        JSONObject rep = term.toJSON();
        rep.put("links", this.autolinker.get().forResource(getClass(), this.uriInfo)
            .withExtraParameters("vocabulary-id", term.getVocabulary().getIdentifier()).build());
        return Response.ok(rep, MediaType.APPLICATION_JSON_TYPE).build();
    }
}
