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
import org.phenotips.vocabularies.rest.CategoryTermSuggestionsResource;
import org.phenotips.vocabularies.rest.DomainObjectFactory;
import org.phenotips.vocabularies.rest.VocabularyResource;
import org.phenotips.vocabularies.rest.VocabularyTermResource;
import org.phenotips.vocabularies.rest.model.VocabularyTermSummary;
import org.phenotips.vocabularies.rest.model.VocabularyTerms;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

/**
 * Default implementation for {@link CategoryTermSuggestionsResource} using XWiki's support for REST resources.
 *
 * @version $Id $
 * @since 1.4M1
 */
@Component
@Named("org.phenotips.vocabularies.rest.internal.DefaultCategoryTermSuggestionsResource")
@Singleton
public class DefaultCategoryTermSuggestionsResource extends XWikiResource implements CategoryTermSuggestionsResource
{
    private static final String CATEGORY_LABEL = "category";

    private static final String TERM_ID_LABEL = "term-id";

    @Inject
    private Logger logger;

    @Inject
    private VocabularyManager vm;

    @Inject
    private DomainObjectFactory objectFactory;

    @Inject
    private Provider<Autolinker> autolinker;

    @Override
    public VocabularyTerms suggest(@Nonnull final String category, @Nonnull final String input, int maxResults)
    {
        // Both input and category must be provided.
        if (StringUtils.isBlank(input) || StringUtils.isBlank(category)) {
            this.logger.error("Both input and category must be provided.");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        // Check if the requested vocabulary category is supported.
        if (!this.vm.getAvailableCategories().contains(category)) {
            this.logger.error("The requested vocabulary category [{}] does not exist.", category);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        // Search for terms matching input in vocabularies associated with provided category.
        final List<VocabularyTerm> terms = this.vm.search(input, category, maxResults);
        final List<VocabularyTermSummary> termReps = new ArrayList<>();
        // For each of the retrieved terms, create a term summary object, add links and other relevant data.
        for (final VocabularyTerm term : terms) {
            final VocabularyTermSummary termRep = this.objectFactory.createVocabularyTermRepresentation(term);
            termRep.withLinks(this.autolinker.get().forSecondaryResource(VocabularyTermResource.class, this.uriInfo)
                .withActionableResources(VocabularyResource.class)
                .withExtraParameters(CATEGORY_LABEL, category)
                .withExtraParameters(TERM_ID_LABEL, term.getId())
                .build());
            termReps.add(termRep);
        }
        final VocabularyTerms result = new VocabularyTerms().withVocabularyTerms(termReps);
        result.withLinks(this.autolinker.get().forResource(getClass(), this.uriInfo).build());
        return result;
    }
}
