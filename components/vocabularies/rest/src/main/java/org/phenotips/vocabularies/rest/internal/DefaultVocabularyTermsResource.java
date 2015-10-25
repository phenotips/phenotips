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
import org.phenotips.vocabularies.rest.DomainObjectFactory;
import org.phenotips.vocabularies.rest.VocabularyResource;
import org.phenotips.vocabularies.rest.VocabularyTermResource;
import org.phenotips.vocabularies.rest.VocabularyTermsResource;
import org.phenotips.vocabularies.rest.model.Link;
import org.phenotips.vocabularies.rest.model.VocabularyTerms;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;
import org.xwiki.stability.Unstable;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.StringUtils;

/**
 * Default implementation of the {@link org.phenotips.vocabularies.rest.VocabularyTermsResource}.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Component
@Named("org.phenotips.vocabularies.rest.internal.DefaultVocabularyTermsResource")
@Singleton
@Unstable
public class DefaultVocabularyTermsResource extends XWikiResource implements VocabularyTermsResource
{
    @Inject
    private VocabularyManager vm;

    @Inject
    private DomainObjectFactory objectFactory;

    @Override
    public VocabularyTerms suggest(String vocabularyId, String input, @DefaultValue("10") int maxResults, String sort,
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

        List<org.phenotips.vocabularies.rest.model.VocabularyTerm> termReps = new ArrayList<>();
        for (VocabularyTerm term : termSuggestions) {
            org.phenotips.vocabularies.rest.model.VocabularyTerm termRep =
                this.objectFactory.createVocabularyTermRepresentation(term);
            List<Link> links = new ArrayList<>();
            links.add(new Link().withRel(Relations.VOCABULARY_TERM)
                .withHref(UriBuilder.fromUri(this.uriInfo.getBaseUri()).path(VocabularyTermResource.class)
                    .path(VocabularyTermResource.class, "getTerm")
                    .build(vocabularyId, term.getId())
                    .toString()));
            links.add(new Link().withRel(Relations.VOCABULARY)
                .withHref(UriBuilder.fromUri(this.uriInfo.getBaseUri()).path(VocabularyResource.class)
                    .build(vocabularyId)
                    .toString()));
            termRep.withLinks(links);
            termReps.add(termRep);
        }
        VocabularyTerms result = new VocabularyTerms().withVocabularyTerms(termReps);
        result.withLinks(new Link().withRel(Relations.SELF).withHref(this.uriInfo.getRequestUri().toString()));
        return result;
    }
}
