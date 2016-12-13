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
package org.phenotips.vocabulary.internal.solr;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.SpellingParams;

/**
 * Utility class for preparing the queries used by the {@link AbstractSolrVocabulary vocabulary service}.
 *
 * @version $Id$
 * @since 1.2M4 (under a different package since 1.0M8)
 */
public final class SolrQueryUtils
{
    /** Regular expression that checks if the last term in a query is a word stub. */
    private static final Pattern WORD_STUB = Pattern.compile("(\\w++):(\\w++)\\*$", Pattern.CASE_INSENSITIVE);

    private static final String SPELLCHECK = "spellcheck";

    /** Private default constructor, so that this utility class can't be instantiated. */
    private SolrQueryUtils()
    {
        // Nothing to do
    }

    /**
     * Adds extra parameters to a Solr query for better term searches, including custom options. More specifically, adds
     * parameters for requesting the score to be included in the results, for requesting a spellcheck result, and sets
     * the {@code start} and {@code rows} parameters when missing.
     *
     * @param originalParams the original Solr parameters to enhance
     * @param queryOptions extra options to include in the query; these override the default values, but don't override
     *            values already set in the query
     * @return the enhanced parameters
     */
    public static SolrQuery generateQuery(SolrQuery originalParams, Map<String, String> queryOptions)
    {
        if (originalParams == null) {
            return null;
        }
        SolrQuery result = new SolrQuery();

        // Default values
        result.setStart(0);
        result.setRows(1000);
        result.setIncludeScore(true);

        // Add the generic query options
        if (queryOptions != null) {
            for (Map.Entry<String, String> item : queryOptions.entrySet()) {
                result.set(item.getKey(), item.getValue());
            }
        }

        // Add the original query parameters
        for (Map.Entry<String, Object> item : originalParams.toNamedList()) {
            if (item.getValue() != null && item.getValue() instanceof String[]) {
                result.set(item.getKey(), (String[]) item.getValue());
            } else {
                result.set(item.getKey(), String.valueOf(item.getValue()));
            }
        }

        if (result.get(SPELLCHECK) == null) {
            result.set(SPELLCHECK, Boolean.toString(true));
            result.set(SpellingParams.SPELLCHECK_COLLATE, Boolean.toString(true));
        }

        return result;
    }

    /**
     * Replaces the original query in the Solr parameters with the suggested spellchecked query. It also fixes the boost
     * query, if any.
     *
     * @param originalQuery the original Solr query to fix
     * @param suggestedQuery the suggested query string
     * @return new Solr query with the query and boost query fixed
     */
    public static SolrQuery applySpellcheckSuggestion(SolrQuery originalQuery, String suggestedQuery)
    {
        if (originalQuery == null) {
            return null;
        }
        if (StringUtils.isBlank(suggestedQuery)) {
            return originalQuery;
        }
        String newQueryString = suggestedQuery;

        // Since the spelling suggestion might not be that good, also search for the original user input
        if (StringUtils.isNotEmpty(originalQuery.get(SpellingParams.SPELLCHECK_Q))) {
            newQueryString = "(" + originalQuery.getQuery() + ")^10 " + suggestedQuery;
        }

        // Check if the last term in the query is a word stub search which, in case the request comes from a
        // user-triggered search for terms from the UI, is a prefix search for the last typed word
        Matcher originalStub = WORD_STUB.matcher(originalQuery.getQuery());
        Matcher newStub = WORD_STUB.matcher(suggestedQuery);
        if (originalStub.find() && newStub.find() && !StringUtils.equals(originalStub.group(2), newStub.group(2))) {
            // Since word stubs aren't complete words, they may wrongly be "corrected" to a full word that doesn't match
            // what the user started typing; include both the original stub and the "corrected" stub in the query
            newQueryString += ' ' + originalStub.group() + "^1.5";
            // Also fix the boost query
            String boostQuery = originalQuery.get(DisMaxParams.BQ);
            if (StringUtils.isNotEmpty(boostQuery)) {
                originalQuery.add(DisMaxParams.BQ, boostQuery.replace(originalStub.group(2), newStub.group(2)));
            }
        }
        // Replace the query
        originalQuery.setQuery(newQueryString);

        return originalQuery;
    }
}
