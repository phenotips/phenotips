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
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.rest.XWikiResource;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONObject;

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

    private static final String LIMIT_LABEL = "limit";

    @Inject
    private GenePanelLoader genePanelLoader;

    @Inject
    private Container container;

    @Override
    public Response getGeneCountsFromPhenotypes()
    {
        final Request request = this.container.getRequest();
        final Set<String> present = extractTerms("present-term", request);
        final Set<String> absent = extractTerms("absent-term", request);
        final Set<String> rejected = extractTerms("rejected-gene", request);
        final boolean withMatchCount = Boolean.parseBoolean((String) request.getProperty("with-match-count"));

        // present and absent will be sets with one null value if "empty", hence performing the check here.
        if (CollectionUtils.isEmpty(present) && CollectionUtils.isEmpty(absent)) {
            this.slf4Jlogger.error("No content provided.");
            return Response.status(Response.Status.NO_CONTENT).build();
        }

        final int offset = NumberUtils.toInt((String) request.getProperty(OFFSET_LABEL), 1);
        final int limit = NumberUtils.toInt((String) request.getProperty(LIMIT_LABEL), -1);
        final int reqNo = NumberUtils.toInt((String) request.getProperty(REQ_NO), 0);

        try {
            final JSONObject panelJSON = getPanelData(present, absent, rejected, withMatchCount, offset, limit);
            panelJSON.put(REQ_NO, reqNo);
            panelJSON.put(OFFSET_LABEL, offset);
            return Response.ok(panelJSON, MediaType.APPLICATION_JSON_TYPE).build();
        } catch (final ExecutionException e) {
            this.slf4Jlogger.warn("No content associated with [present-term: {}, absent-term: {}].", present, absent);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (final IndexOutOfBoundsException e) {
            this.slf4Jlogger.error("The requested [{}: {}] is out of bounds.", OFFSET_LABEL, offset);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (final Exception e) {
            this.slf4Jlogger.error("Unexpected exception while generating gene panel JSON: {}", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generates panel data from the provided {@code present terms}, {@code absent terms}, {@code offset}, and
     * {@code limit on the number of results to return}.
     *
     * @param present a set of term identifiers observed to be present
     * @param absent a set of term identifiers observed to be absent
     * @param rejected a set of gene identifiers that were marked as rejected candidate or tested negative
     * @param withMatchCount set to true iff the number of genes available for term should be counted
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
        final boolean withMatchCount,
        final int offset,
        final int limit) throws ExecutionException, IndexOutOfBoundsException
    {
        final PanelData loaderKey = new PanelData(present, absent, rejected, withMatchCount);
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
     * Extracts terms for a {@code propertyLabel property} from the {@code request}.
     *
     * @param propertyLabel the label of the property that is being extracted from {@code request}
     * @param request the {@link Request} object
     * @return an unmodifiable set of terms
     */
    private Set<String> extractTerms(final String propertyLabel, final Request request)
    {
        final Set<String> terms =
            request.getProperties(propertyLabel).stream()
                .filter(Objects::nonNull)
                .map(t -> (String) t)
                .collect(Collectors.toSet());
        return Collections.unmodifiableSet(terms);
    }
}
