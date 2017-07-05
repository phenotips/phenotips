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
import org.phenotips.panels.rest.GenePanelsResource;

import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.rest.XWikiResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * Default implementation of the {@link GenePanelsResource}.
 *
 * @version $Id$
 * @since 1.3
 */
@Component
@Named("org.phenotips.panels.rest.internal.DefaultGenePanelsResourceImpl")
@Singleton
public class DefaultGenePanelsResourceImpl extends XWikiResource implements GenePanelsResource
{
    private static final String REQ_NO = "reqNo";

    private static final String TOTAL_PAGES_LABEL = "totalPages";

    private static final String START_PAGE_LABEL = "startPage";

    private static final String RESULTS_LABEL = "numResults";

    @Inject
    private Logger logger;

    @Inject
    private GenePanelLoader genePanelLoader;

    @Inject
    private Container container;

    @Override
    public Response getGeneCountsFromPhenotypes()
    {
        Request request = this.container.getRequest();
        List<String> presentTerms = new ArrayList<>();
        for (Object t : request.getProperties("present-term")) {
            if (t != null) {
                presentTerms.add((String) t);
            }
        }
        presentTerms = Collections.unmodifiableList(presentTerms);
        List<String> absentTerms = new ArrayList<>();
        for (Object t : request.getProperties("absent-term")) {
            if (t != null) {
                absentTerms.add((String) t);
            }
        }
        absentTerms = Collections.unmodifiableList(absentTerms);

        if (CollectionUtils.isEmpty(presentTerms) && CollectionUtils.isEmpty(absentTerms)) {
            this.logger.error("No content provided.");
            return Response.status(Response.Status.NO_CONTENT).build();
        }

        final int startPage = NumberUtils.toInt((String) request.getProperty(START_PAGE_LABEL), 1);
        final int numResults = NumberUtils.toInt((String) request.getProperty(RESULTS_LABEL), -1);
        final int reqNo = NumberUtils.toInt((String) request.getProperty(REQ_NO), 0);

        try {
            // Try to generate the JSON for the requested subset of data.
            final JSONObject panels = getPageData(this.genePanelLoader.get(presentTerms), startPage, numResults);
            panels.put(REQ_NO, reqNo);
            return Response.ok(panels, MediaType.APPLICATION_JSON_TYPE).build();
        } catch (final ExecutionException e) {
            this.logger.error("No content associated with [present-term: {}, absent-term: {}].", presentTerms,
                absentTerms);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (final IndexOutOfBoundsException e) {
            this.logger.error("The requested [{}: {}] is out of bounds.", START_PAGE_LABEL, startPage);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (final Exception e) {
            this.logger.error("Unexpected exception while generating gene panel JSON: {}", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Returns a subset of {@code genePanel} data as a {@link JSONObject}, given a {@code startPageStr start page} for
     * the data and the {@code numResults number of results} per page.
     *
     * @param genePanel the {@link GenePanel} object containing gene panel data
     * @param startPage an integer denoting the starting page number
     * @param numResults an integer denoting the number of results per page
     * @return a {@link JSONObject} that contains the specified {@code numResults number of results} for given
     *         {@code startPageStr page number}
     * @throws NumberFormatException if {@code startPageStr} or {@code numResults} cannot be converted into integers
     * @throws IndexOutOfBoundsException if the requested {@code startPageStr} is out of bounds
     */
    private JSONObject getPageData(@Nonnull final GenePanel genePanel, final int startPage, final int numResults)
    {
        // If the number of results was not specified, assume that all results were requested.
        if (numResults < 0) {
            return genePanel.toJSON().put(TOTAL_PAGES_LABEL, 1);
        }

        final int count = genePanel.size();
        final int firstItemIndex = determineFirstIndex((startPage - 1) * numResults, count - 1);
        final int resultSubsetSize = determineActualResultSubsetSize(firstItemIndex + numResults, count);
        return getDataSubsetJSON(genePanel, firstItemIndex, resultSubsetSize, numResults);
    }

    /**
     * Return a {@link JSONObject} for {@code genePanel} with data ranging from {@code firstIndex}, inclusive, to
     * {@code actualResultSubsetSize}, exclusive.
     *
     * @param genePanel the {@link GenePanel} object containing gene panel data
     * @param firstIndex the index of the first data item that we are interested in
     * @param actualResultSubsetSize the actual size of the data subset that we are interested in
     * @param requestedNumResults the requested number of results to retrieve
     * @return a new {@link JSONObject} containing just the requested data
     * @throws IndexOutOfBoundsException if {@code firstIndex} is out of bounds
     */
    private JSONObject getDataSubsetJSON(@Nonnull final GenePanel genePanel, final int firstIndex,
        final int actualResultSubsetSize, final int requestedNumResults)
    {
        final int allCount = genePanel.size();
        if (firstIndex == 0 && requestedNumResults >= allCount) {
            return genePanel.toJSON().put(TOTAL_PAGES_LABEL, 1);
        }

        final JSONObject pageJson = genePanel.toJSON(firstIndex, actualResultSubsetSize);
        pageJson.put(TOTAL_PAGES_LABEL, Math.ceil((float) allCount / requestedNumResults));
        return pageJson;
    }

    /**
     * Compares two integers {@code calculated} and {@code lastItem}. If {@code calculated} is greater than
     * {@code lastItem} or less than 0, throws an {@link IndexOutOfBoundsException}.
     *
     * @param calculated the calculated position of the first requested {@link GenePanel} element
     * @param lastItem the actual position of the last element of a {@link GenePanel}
     * @return {@code calculated} iff calculated &gt;= 0 && calculated &lt;= lastItem
     * @throws IndexOutOfBoundsException if calculated &lt; 0 || calculated &gt; lastItem
     */
    private int determineFirstIndex(final int calculated, final int lastItem)
    {
        if (calculated < 0 || calculated > lastItem) {
            throw new IndexOutOfBoundsException();
        }
        return calculated;
    }

    /**
     * Compares two integers, {@code calculated} and {@code lastItem}, and returns the smallest.
     *
     * @param calculated the calculated position of the last requested {@link GenePanel} element
     * @param lastItem the actual position of the last element of a {@link GenePanel}
     * @return the smallest integer
     */
    private int determineActualResultSubsetSize(final int calculated, final int lastItem)
    {
        return calculated <= lastItem ? calculated : lastItem;
    }
}
