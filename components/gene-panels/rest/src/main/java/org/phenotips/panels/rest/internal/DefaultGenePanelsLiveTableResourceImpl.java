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
package org.phenotips.panels.rest.internal;

import org.phenotips.panels.GenePanel;
import org.phenotips.panels.rest.GenePanelsLiveTableResource;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.collections4.CollectionUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * Default implementation of the {@link GenePanelsLiveTableResource}.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("org.phenotips.panels.rest.internal.DefaultGenePanelsLiveTableResourceImpl")
@Singleton
public class DefaultGenePanelsLiveTableResourceImpl extends XWikiResource implements GenePanelsLiveTableResource
{
    private static final String REQ_NO = "reqNo";

    private static final String OFFSET_LABEL = "offset";

    @Inject
    private Logger logger;

    @Inject
    private GenePanelLoader genePanelLoader;

    @Override
    public Response getGeneCountsFromPhenotypes(
        @Nullable final List<String> presentTerms,
        @Nullable final List<String> absentTerms,
        @Nullable final List<String> rejectedGenes,
        final int offset,
        final int limit,
        final int reqNo)
    {
        final Set<String> present = makeTermList(presentTerms);
        final Set<String> absent = makeTermList(absentTerms);
        final Set<String> rejected = makeTermList(rejectedGenes);
        // presentTerms and absentTerms will be lists with one null value if "empty", hence performing the check here.
        if (CollectionUtils.isEmpty(present) && CollectionUtils.isEmpty(absent)) {
            this.logger.error("No content provided.");
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        try {
            final JSONObject panelJSON = getPanelData(present, absent, rejected, offset, limit);
            panelJSON.put(REQ_NO, reqNo);
            panelJSON.put(OFFSET_LABEL, offset);
            return Response.ok(panelJSON, MediaType.APPLICATION_JSON_TYPE).build();
        } catch (final ExecutionException e) {
            this.logger.warn("No content associated with [present-term: {}, absent-term: {}].", present, absent);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (final IndexOutOfBoundsException e) {
            this.logger.error("The requested [{}: {}] is out of bounds.", OFFSET_LABEL, offset);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (final Exception e) {
            this.logger.error("Unexpected exception while generating gene panel JSON: {}", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generates panel data from the provided {@code present terms}, {@code absent terms}, {@code offset}, and
     * {@code limit on the number of results to return}.
     *
     * @param present a set of term identifiers observed to be present
     * @param absent a set of term identifiers observed to be absent
     * @param rejected a set of gene identifiers that were marked as rejected
     * @param offset the offset for the returned data
     * @param limit the limit on the number of results to return
     * @return a {@link JSONObject} that contains the requested gene panel data
     * @throws ExecutionException if a gene panel could not be generated
     * @throws IndexOutOfBoundsException if the specified offset is out of bounds
     */
    private JSONObject getPanelData(
        @Nonnull final Set<String> present,
        @Nonnull final Set<String> absent,
        @Nonnull final Set<String> rejected,
        final int offset,
        final int limit) throws ExecutionException, IndexOutOfBoundsException
    {
        final PanelData loaderKey = new PanelData(present, absent, rejected);
        final GenePanel panel = this.genePanelLoader.get(loaderKey);
        final int panelSize = panel.size();
        if (offset < 1 || offset > panelSize) {
            throw new IndexOutOfBoundsException();
        }

        final int lastIndex = limit < 1 ? panelSize : calculateLastIndex(offset, limit, panelSize);
        // If there is no limit specified, then assume all results after the provided offset were requested.
        return panel.toJSON(offset - 1, lastIndex);
    }

    /**
     * Calculates the last index of returned data based on the provided {@code offset}, {@code limit on data to return},
     * and the actual {@code panelSize size of the gene panel}.
     *
     * @param offset the offset for the returned data
     * @param limit the limit on the number of results to return
     * @param panelSize the actual size of the gene panel
     * @return the last index
     */
    private int calculateLastIndex(final int offset, final int limit, final int panelSize)
    {
        final int calculated = offset + limit - 1;
        return calculated < panelSize ? calculated : panelSize;
    }

    /**
     * Makes an unmodifiable set of terms specified in {@code terms}, with any null values filtered out.
     *
     * @param terms a nullable list of terms, that may contain null values
     * @return an unmodifiable set of terms that does not contain any null values
     */
    private Set<String> makeTermList(@Nullable final List<String> terms)
    {
        final Set<String> nullFiltered = Optional.ofNullable(terms)
            .orElseGet(Collections::emptyList)
            .stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        return Collections.unmodifiableSet(nullFiltered);
    }
}
