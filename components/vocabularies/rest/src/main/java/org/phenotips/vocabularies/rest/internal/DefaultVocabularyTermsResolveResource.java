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
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.rest.XWikiResource;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
            this.slf4Jlogger.error("No content provided.");
            return Response.status(Response.Status.NO_CONTENT).build();
        }

        this.slf4Jlogger.debug("Retrieving terms with IDs: [{}]", termIds);
        // The JSONArray that will contain the vocabulary terms as JSONObjects.
        final JSONArray termsJson = new JSONArray();
        termIds.stream()
            // Remove any null identifiers
            .filter(Objects::nonNull)
            // Try to resolve each identifier to a vocabulary term
            .map(this::resolveTerm)
            // Remove any nulls for terms that could not be resolved
            .filter(Objects::nonNull)
            // Get the term JSONObject with links
            .map(this::getTermJsonWithLinks)
            // And put each term in the terms JSONArray
            .forEach(termsJson::put);
        final JSONObject rep = new JSONObject()
            .put(ROWS, termsJson)
            .put(LINKS, this.autolinker.get().forResource(getClass(), this.uriInfo).build());
        return Response.ok(rep, MediaType.APPLICATION_JSON_TYPE).build();
    }

    /**
     * Tries to resolve the provided {@code termId} to a corresponding {@link VocabularyTerm}.
     *
     * @param termId the identifier for the {@link VocabularyTerm} of interest
     * @return the corresponding {@link VocabularyTerm}, or {@code null} if no such term exists
     */
    @Nullable
    private VocabularyTerm resolveTerm(@Nonnull final Object termId)
    {
        final VocabularyTerm term = this.vm.resolveTerm((String) termId);
        if (term == null) {
            // Since we're ignoring terms that cannot be resolved, log a warning.
            this.slf4Jlogger.warn("Could not resolve vocabulary term: [{}]", termId);
        }
        return term;
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
