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
import org.phenotips.vocabularies.rest.VocabularyTermsResolveResource;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.rest.XWikiResource;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Default implementation of the {@link VocabularyTermsResolveResource}.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("org.phenotips.vocabularies.rest.internal.DefaultVocabularyTermsResolveResource")
@Singleton
public class DefaultVocabularyTermsResolveResource extends XWikiResource implements VocabularyTermsResolveResource
{
    private static final String ROWS = "rows";

    private static final String LINKS = "links";

    private static final String TERM_ID = "term-id";

    private static final String COLON = ":";

    @Inject
    private VocabularyManager vm;

    /** XWiki request container. */
    @Inject
    private Container container;

    @Inject
    private Provider<Autolinker> autolinker;

    @Override
    public Response resolveTerms()
    {
        final Request request = this.container.getRequest();
        final List<Object> termIds = request.getProperties(TERM_ID);

        if (CollectionUtils.isEmpty(termIds)) {
            this.slf4Jlogger.info("No content provided.");
            return Response.status(Response.Status.NO_CONTENT).build();
        }

        this.slf4Jlogger.debug("Retrieving terms with IDs: [{}]", termIds);
        final JSONObject rep = new JSONObject()
            .put(ROWS, this.createRows(termIds))
            .put(LINKS, this.autolinker.get().forResource(getClass(), this.uriInfo).build());
        return Response.ok(rep, MediaType.APPLICATION_JSON_TYPE).build();
    }

    /**
     * Builds a {@link JSONArray} with the data retrieved for the provided {@code termIds}.
     *
     * @param termIds a {@link List} of term identifiers of interest
     * @return a {@link JSONArray} with data for {@code termIds}
     */
    @Nonnull
    private JSONArray createRows(@Nonnull final List<Object> termIds)
    {
        final JSONArray termsJson = new JSONArray();
        termIds.stream()
            // Remove any null identifiers
            .filter(Objects::nonNull)
            // Get a tuple of term prefix to term
            .map(t -> Pair.of(StringUtils.substringBefore((String) t, COLON), (String) t))
            // Keep only those terms where a valid prefix is specified
            .filter(this::prefixIsSpecified)
            // Group by term prefix
            .collect(Collectors.groupingBy(Pair::getLeft, Collectors.mapping(Pair::getRight, Collectors.toSet())))
            // Look through each vocabulary prefix -> term IDs set
            .entrySet().stream()
            // Look up terms, and get a stream of JSONObject for each
            .map(es -> this.getTerms(es.getKey(), es.getValue()))
            // Remove any null streams for when a valid vocabulary was not found
            .filter(Objects::nonNull)
            // Flatten the streams
            .flatMap(t -> t)
            // Put each term into the terms JSONArray
            .forEach(termsJson::put);
        return termsJson;
    }

    /**
     * Takes in a {@code prefixTermPair} and returns true iff a valid prefix was specified.
     *
     * @param prefixTermPair a {@link Pair} of prefix -> term ID
     * @return true iff a prefix is specified, false otherwise
     */
    private boolean prefixIsSpecified(@Nonnull final Pair<String, String> prefixTermPair)
    {
        if (prefixTermPair.getLeft().equals(prefixTermPair.getRight())) {
            this.slf4Jlogger.warn("Term [{}] does not begin with a valid prefix", prefixTermPair.getRight());
            return false;
        }
        return true;
    }

    /**
     * Given the {@code vocabularyPrefix}, tries to retrieve data for provided {@code termIds}.
     *
     * @param vocabPrefix a vocabulary prefix (e.g. HP)
     * @param termIds a {@link Set} of term identifiers, as strings
     * @return a {@link Stream} of {@link JSONObject} for each specified term ID
     */
    @Nullable
    private Stream<JSONObject> getTerms(@Nonnull final String vocabPrefix, @Nonnull final Set<String> termIds)
    {
        // Try to get the vocabulary by prefix.
        final Vocabulary vocabulary = this.vm.getVocabulary(vocabPrefix);
        if (vocabulary == null) {
            // If no matching vocabulary can be found, log a warning.
            this.slf4Jlogger.warn("Could not resolve terms [{}]. No matching vocabulary found.", termIds);
            return null;
        }
        // Return a stream of JSONObject for the retrieved terms
        return vocabulary.getTerms(termIds).stream().map(this::getTermJsonWithLinks);
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
