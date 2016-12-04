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

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
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
     * Convert a Lucene query string into Solr parameters. More specifically, places the input query under the "q"
     * parameter.
     *
     * @param query the lucene query string to use
     * @return the obtained parameters
     */
    public static SolrParams transformQueryToSolrParams(String query)
    {
        ModifiableSolrParams result = new ModifiableSolrParams();
        result.add(CommonParams.Q, query);
        return result;
    }

    /**
     * Adds extra parameters to a Solr query for better term searches. More specifically, adds parameters for requesting
     * the score to be included in the results, for requesting a spellcheck result, and sets the {@code start} and
     * {@code rows} parameters when missing.
     *
     * @param originalParams the original Solr parameters to enhance
     * @return the enhanced parameters
     */
    public static SolrParams enhanceParams(SolrParams originalParams)
    {
        return enhanceParams(originalParams, null);
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
    public static SolrParams enhanceParams(SolrParams originalParams, Map<String, String> queryOptions)
    {
        if (originalParams == null) {
            return null;
        }
        ModifiableSolrParams newParams = new ModifiableSolrParams();
        newParams.set(CommonParams.START, "0");
        newParams.set(CommonParams.ROWS, "1000");
        newParams.set(CommonParams.FL, "* score");
        if (queryOptions != null) {
            for (Map.Entry<String, String> item : queryOptions.entrySet()) {
                newParams.set(item.getKey(), item.getValue());
            }
        }
        for (Map.Entry<String, Object> item : originalParams.toNamedList()) {
            if (item.getValue() != null && item.getValue() instanceof String[]) {
                newParams.set(item.getKey(), (String[]) item.getValue());
            } else {
                newParams.set(item.getKey(), String.valueOf(item.getValue()));
            }
        }
        if (newParams.get(SPELLCHECK) == null) {
            newParams.set(SPELLCHECK, Boolean.toString(true));
            newParams.set(SpellingParams.SPELLCHECK_COLLATE, Boolean.toString(true));
        }
        return newParams;
    }

    /**
     * Replaces the original query in the Solr parameters with the suggested spellchecked query. It also fixes the boost
     * query, if any.
     *
     * @param originalParams the original Solr parameters to fix
     * @param suggestedQuery the suggested query
     * @return new Solr parameters with the query and boost query fixed
     */
    public static SolrParams applySpellcheckSuggestion(SolrParams originalParams, String suggestedQuery)
    {
        if (originalParams == null) {
            return null;
        }
        if (StringUtils.isBlank(suggestedQuery)) {
            return originalParams;
        }
        ModifiableSolrParams newParams = new ModifiableSolrParams(originalParams);
        String newQuery = suggestedQuery;

        // Since the spelling suggestion might not be that good, also search for the original user input
        if (StringUtils.isNotEmpty(originalParams.get(SpellingParams.SPELLCHECK_Q))) {
            newQuery = originalParams.get(CommonParams.Q) + "^10 " + suggestedQuery;
        }

        // Check if the last term in the query is a word stub search which, in case the request comes from a
        // user-triggered search for terms from the UI, is a prefix search for the last typed word
        Matcher originalStub = WORD_STUB.matcher(newParams.get(CommonParams.Q));
        Matcher newStub = WORD_STUB.matcher(suggestedQuery);
        if (originalStub.find() && newStub.find() && !StringUtils.equals(originalStub.group(2), newStub.group(2))) {
            // Since word stubs aren't complete words, they may wrongly be "corrected" to a full word that doesn't match
            // what the user started typing; include both the original stub and the "corrected" stub in the query
            newQuery += ' ' + originalStub.group() + "^1.5";
            // Also fix the boost query
            String boostQuery = newParams.get(DisMaxParams.BQ);
            if (StringUtils.isNotEmpty(boostQuery)) {
                newParams.add(DisMaxParams.BQ, boostQuery.replace(originalStub.group(2), newStub.group(2)));
            }
        }
        // Replace the query
        newParams.set(CommonParams.Q, newQuery);

        return newParams;
    }

    /**
     * Serialize Solr parameters into a String.
     *
     * @param params the parameters to serialize
     * @return a String serialization of the parameters
     */
    public static String getCacheKey(SolrParams params)
    {
        StringBuilder out = new StringBuilder();
        out.append('{');
        Iterator<String> parameterNames = params.getParameterNamesIterator();
        while (parameterNames.hasNext()) {
            String parameter = parameterNames.next();
            out.append(parameter).append(":[").append(StringUtils.join(params.getParams(parameter), ", "))
                .append("]\n");
        }
        out.append('}');
        return out.toString();
    }
}
