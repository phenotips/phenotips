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

import org.phenotips.panels.rest.GenePanelsLoadingCache;
import org.phenotips.panels.rest.GenePanelsResource;

import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.rest.XWikiResource;

import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.google.common.base.Joiner;

/**
 * Default implementation of the {@link GenePanelsResource}.
 *
 * @version $Id$
 * @since 1.3M5
 */
@Component
@Named("org.phenotips.panels.rest.internal.DefaultGenePanelsResourceImpl")
@Singleton
public class DefaultGenePanelsResourceImpl extends XWikiResource implements GenePanelsResource
{
    private static final String REQ_NO = "reqNo";

    private static final String GENES_LABEL = "genes";

    private static final String SIZE_LABEL = "size";

    private static final String TOTAL_PAGES_LABEL = "totalPages";

    private static final String START_PAGE_LABEL = "startPage";

    private static final String NUMBER_RESULTS_LABEL = "numResults";

    @Inject
    private Logger logger;

    @Inject
    private Container container;

    @Inject
    private GenePanelsLoadingCache loadingCache;

    @Override
    public Response getGeneCountsFromPhenotypes()
    {
        final Request request = this.container.getRequest();
        @SuppressWarnings("unchecked")
        final List<String> termIds = (List<String>) (List<?>) request.getProperties("id");
        final Object startPage = request.getProperty(START_PAGE_LABEL);
        final Object numResults = request.getProperty(NUMBER_RESULTS_LABEL);
        final Object reqNo = request.getProperty(REQ_NO);

        try {
            final JSONObject panels = getPageData(loadingCache.getCache().get(Joiner.on(",").skipNulls().join(termIds)),
                startPage, numResults);
            panels.put(REQ_NO, reqNo);
            return Response.ok(panels, MediaType.APPLICATION_JSON_TYPE).build();
        } catch (final ExecutionException e) {
            this.logger.warn("No content associated with [{}]", termIds);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (final NumberFormatException e) {
            this.logger.warn("{} and {} parameters should be integers.", START_PAGE_LABEL, NUMBER_RESULTS_LABEL);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (final IndexOutOfBoundsException e) {
            this.logger.warn("The requested start page index [{}] is out of bounds.", startPage);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    /**
     * Gets the JSON and processes it to contain the requested number of results for the page.
     *
     * @param jsonObject the {@link JSONObject} containing gene panels data
     * @param startPageStr the page number from which to send data
     * @param numResultsStr the number of results per page
     * @return a {@link JSONObject} that contains the data only for the specified page
     */
    private JSONObject getPageData(final JSONObject jsonObject, final Object startPageStr, final Object numResultsStr)
    {
        // If the start page or the number of results were not specified, assume that all results were requested.
        if (startPageStr == null || numResultsStr == null) {
            jsonObject.put(TOTAL_PAGES_LABEL, 1);
            return jsonObject;
        }
        // Converts the starting page and the number of results requested objects to integers.
        // Will throw NumberFormatException if input cannot be converted to an integer.
        final int startPage = Integer.parseInt((String) startPageStr);
        final int numResults = Integer.parseInt((String) numResultsStr);

        final int count = jsonObject.getInt(SIZE_LABEL);
        final int startItem = determineFirstIndex((startPage - 1) * numResults, count - 1);
        final int endItem = determineLastIndex(startItem + numResults - 1, count - 1);
        return getAmendedJSON(jsonObject, startItem, endItem, numResults);
    }

    /**
     * Return a new JSONObject that contains only the specified amount of data. Gene JSON objects are not deep copies.
     *
     * @param jsonObject the {@link JSONObject} containing all gene data
     * @param startItem the index of the first gene JSON object that we are interested in
     * @param endItem the index of the last gene JSON object that we are interested in
     * @param numResults the total number of results that we are retrieving
     * @return a new {@link JSONObject} containing just the requested data
     */
    private JSONObject getAmendedJSON(final JSONObject jsonObject, final int startItem, final int endItem,
        final int numResults)
    {
        final int allCount = jsonObject.getInt(SIZE_LABEL);
        if (startItem == 0 && numResults >= allCount) {
            jsonObject.put(TOTAL_PAGES_LABEL, 1);
            return jsonObject;
        }
        // The original data.
        final JSONArray allGenes = jsonObject.getJSONArray(GENES_LABEL);
        // Create new JSON objects for the sub-data.
        final JSONObject pageJson = new JSONObject();
        final JSONArray pageGenes = new JSONArray();
        // Obtain only the genes of interest.
        for (int i = startItem; i <= endItem; i++) {
            pageGenes.put(allGenes.getJSONObject(i));
        }

        pageJson.put(SIZE_LABEL, pageGenes.length());
        pageJson.put(TOTAL_PAGES_LABEL, Math.ceil((float) allCount / numResults));
        pageJson.put(GENES_LABEL, pageGenes);
        return pageJson;
    }

    /**
     * Compares two integers. If calculated is greater than lastItem, throws an {@link IndexOutOfBoundsException}.
     *
     * @param calculated the calculated position of the first requested element of results {@link JSONArray}
     * @param lastItem the actual position of the last element of the results {@link JSONArray}
     * @return the calculated position of the first requested element iff it is not greater than the last item
     */
    private int determineFirstIndex(final int calculated, final int lastItem)
    {
        if (calculated > lastItem) {
            throw new IndexOutOfBoundsException();
        }
        return calculated;
    }

    /**
     * Compares two integers and returns the smallest.
     *
     * @param calculated the calculated position of the last requested element of results {@link JSONArray}
     * @param lastItem the actual position of the last element of the results {@link JSONArray}
     * @return the smallest integer
     */
    private int determineLastIndex(final int calculated, final int lastItem)
    {
        return calculated <= lastItem ? calculated : lastItem;
    }
}
