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
import org.phenotips.vocabularies.rest.VocabularyTermsResource;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.rest.XWikiResource;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.collections4.CollectionUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Default implementation of the {@link VocabularyTermsResource}.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("org.phenotips.vocabularies.rest.internal.DefaultVocabularyTermsResource")
@Singleton
public class DefaultVocabularyTermsResource extends XWikiResource implements VocabularyTermsResource
{
    private static final String ROWS = "rows";

    private static final String LINKS = "links";

    private static final String TERM_ID = "term-id";

    @Inject
    private VocabularyManager vm;

    /** XWiki request container. */
    @Inject
    private Container container;

    @Inject
    private Provider<Autolinker> autolinker;

    @Override
    public Response getTerms(@Nonnull final String vocabularyId)
    {
        // Try to find the requested vocabulary.
        final Vocabulary vocabulary = this.vm.getVocabulary(vocabularyId);
        if (vocabulary == null) {
            this.slf4Jlogger.error("The requested vocabulary [{}] was not found", vocabularyId);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        final Request request = this.container.getRequest();
        final List<Object> termIds = request.getProperties(TERM_ID);
        // This may have nulls, so remove them.
        termIds.removeIf(Objects::isNull);

        if (CollectionUtils.isEmpty(termIds)) {
            this.slf4Jlogger.info("No content provided.");
            return Response.status(Response.Status.NO_CONTENT).build();
        }

        this.slf4Jlogger.debug("Retrieving terms with IDs: [{}]", termIds);
        final JSONObject rep = new JSONObject()
            .put(ROWS, this.createRows(vocabulary, termIds))
            .put(LINKS, this.autolinker.get().forResource(getClass(), this.uriInfo).build());
        return Response.ok(rep, MediaType.APPLICATION_JSON_TYPE).build();
    }

    /**
     * Builds a {@link JSONArray} with the data retrieved for the provided {@code termIds}.
     *
     * @param vocabulary the {@link Vocabulary} from which term data will be queried
     * @param termIds a {@link List} of term identifiers of interest
     * @return a {@link JSONArray} with data for {@code termIds}
     */
    @Nonnull
    private JSONArray createRows(@Nonnull final Vocabulary vocabulary, @Nonnull final List<Object> termIds)
    {
        final JSONArray termsJson = new JSONArray();
        // Try to retrieve the vocabulary terms
        vocabulary.getTerms((List<String>) (List<?>) termIds).stream()
            // Get the JSONObject representation of each term
            .map(this::getTermJsonWithLinks)
            // Insert the term into the JSONArray
            .forEach(termsJson::put);
        return termsJson;
    }

    /**
     * Retrieves the {@link JSONObject} for the provided {@code term} with all the relevant links.
     *
     * @param term the {@link VocabularyTerm} of interest
     * @return the {@link JSONObject} representation of the {@code term} with links
     */
    @Nonnull
    private JSONObject getTermJsonWithLinks(@Nonnull final VocabularyTerm term)
    {
        return term.toJSON()
            .put(LINKS, this.autolinker.get().forSecondaryResource(VocabularyTermResource.class, this.uriInfo).build());
    }
}
