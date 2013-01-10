/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package edu.toronto.cs.cidb.solr;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.MapSolrParams;
import org.slf4j.Logger;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.script.service.ScriptService;

/**
 * Provides access to the Solr server, with the main purpose of providing access to an indexed ontology. There are two
 * ways of accessing items in the ontology: getting a single term by its identifier, or searching for terms matching a
 * given query in the Lucene query language.
 * 
 * @version $Id$
 */
public abstract class AbstractSolrScriptService implements ScriptService, Initializable
{
    /**
     * Delimiter between the field name and the searched value used in the Lucene query language.
     */
    protected static final String FIELD_VALUE_SEPARATOR = ":";

    /**
     * The name of the ID field.
     */
    protected static final String ID_FIELD_NAME = "id";

    /** Logging helper object. */
    @Inject
    protected Logger logger;

    /** The Solr server instance used. */
    protected SolrServer server;

    @Inject
    protected CacheManager cacheFactory;

    protected Cache<SolrDocument> cache;

    private static final SolrDocument EMPTY_MARKER = new SolrDocument();

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.server = new HttpSolrServer("http://localhost:8080/solr/" + this.getName() + "/");
            this.cache = this.cacheFactory.createNewLocalCache(new CacheConfiguration());

        } catch (RuntimeException ex) {
            throw new InitializationException("Invalid URL specified for the Solr server: {}");
        } catch (final CacheException ex) {
            throw new InitializationException("Cannot create cache: " + ex.getMessage());

        }
    }

    /**
     * Get the name of the Solr "core" to be used by this service instance.
     * 
     * @return the simple core name
     */
    protected abstract String getName();

    /**
     * Search for terms matching the specified query, using the Lucene query language.
     * 
     * @param queryParameters a Lucene query
     * @return the list of matching documents, empty if there are no matching terms
     */
    public SolrDocumentList search(final String queryParameters)
    {
        MapSolrParams params = new MapSolrParams(getSolrQuery(queryParameters, -1, 0));
        return search(params);
    }

    /**
     * Search for terms matching the specified query, using the Lucene query language.
     * 
     * @param queryParameters a Lucene query
     * @param sort sorting criteria
     * @return the list of matching documents, empty if there are no matching terms
     */
    public SolrDocumentList search(final String queryParameters, final String sort)
    {
        MapSolrParams params = new MapSolrParams(getSolrQuery(queryParameters, sort, -1, 0));
        return search(params);
    }

    /**
     * Search for terms matching the specified query, using the Lucene query language.
     * 
     * @param queryParameters a Lucene query
     * @param rows the number of items to return, or -1 to use the default number of results
     * @param start the number of items to skip, i.e. the index of the first hit to return, 0-based
     * @return the list of matching documents, empty if there are no matching terms
     */
    public SolrDocumentList search(final String queryParameters, final int rows, final int start)
    {
        MapSolrParams params = new MapSolrParams(getSolrQuery(queryParameters, rows, start));
        return search(params);
    }

    /**
     * Search for terms matching the specified query, using the Lucene query language.
     * 
     * @param queryParameters a Lucene query
     * @param sort sorting criteria
     * @param rows the number of items to return, or -1 to use the default number of results
     * @param start the number of items to skip, i.e. the index of the first hit to return, 0-based
     * @return the list of matching documents, empty if there are no matching terms
     */
    public SolrDocumentList search(final String queryParameters, final String sort, final int rows, final int start)
    {
        MapSolrParams params = new MapSolrParams(getSolrQuery(queryParameters, sort, rows, start));
        return search(params);
    }

    /**
     * Search for terms matching the specified query, where the query is specified as a map of field name and keywords.
     * 
     * @param fieldValues the map of values to search for, where each key is the name of an indexed field and the value
     *            is the keywords to match for that field
     * @return the list of matching documents, empty if there are no matching terms
     */
    public SolrDocumentList search(final Map<String, String> fieldValues)
    {
        return search(fieldValues, -1, 0);
    }

    /**
     * Search for terms matching the specified query, where the query is specified as a map of field name and keywords.
     * 
     * @param fieldValues the map of values to search for, where each key is the name of an indexed field and the value
     *            is the keywords to match for that field
     * @param sort sorting criteria
     * @return the list of matching documents, empty if there are no matching terms
     */
    public SolrDocumentList search(final Map<String, String> fieldValues, String sort)
    {
        return search(fieldValues, sort, -1, 0);
    }

    /**
     * Search for terms matching the specified query, where the query is specified as a map of field name and keywords.
     * 
     * @param fieldValues the map of values to search for, where each key is the name of an indexed field and the value
     *            is the keywords to match for that field
     * @param rows the number of items to return, or -1 to use the default number of results
     * @param start the number of items to skip, i.e. the index of the first hit to return, 0-based
     * @return the list of matching documents, empty if there are no matching terms
     */
    public SolrDocumentList search(final Map<String, String> fieldValues, final int rows, final int start)
    {
        MapSolrParams params = new MapSolrParams(getSolrQuery(fieldValues, rows, start));
        return search(params);
    }

    /**
     * Search for terms matching the specified query, where the query is specified as a map of field name and keywords.
     * 
     * @param fieldValues the map of values to search for, where each key is the name of an indexed field and the value
     *            is the keywords to match for that field
     * @param sort sorting criteria
     * @param rows the number of items to return, or -1 to use the default number of results
     * @param start the number of items to skip, i.e. the index of the first hit to return, 0-based
     * @return the list of matching documents, empty if there are no matching terms
     */
    public SolrDocumentList search(final Map<String, String> fieldValues, final String sort, final int rows,
        final int start)
    {
        MapSolrParams params = new MapSolrParams(getSolrQuery(fieldValues, sort, rows, start));
        return search(params);
    }

    /**
     * Get the top hit corresponding to the specified query.
     * 
     * @param fieldValues the map of values to search for, where each key is the name of an indexed field and the value
     *            is the keywords to match for that field
     * @return the top matching document, {@code null} if there were no matches at all
     * @see #search(Map)
     */
    public SolrDocument get(final Map<String, String> fieldValues)
    {
        String cacheKey = dumpMap(fieldValues);
        SolrDocument result = this.cache.get(cacheKey);
        if (result == null) {
            SolrDocumentList all = search(fieldValues, 1, 0);
            if (all != null && !all.isEmpty()) {
                result = all.get(0);
                this.cache.set(cacheKey, result);
            } else {
                this.cache.set(cacheKey, EMPTY_MARKER);
            }
        }
        return (result == EMPTY_MARKER) ? null : result;
    }

    /**
     * Get the document corresponding to the specified term identifier.
     * 
     * @param id the identifier to search for, in the {@code HP:1234567} format (for HPO), or {@code 123456} (for OMIM)
     * @return the matching document, if one was found, or {@code null} otherwise
     */
    public SolrDocument get(final String id)
    {
        Map<String, String> queryParameters = new HashMap<String, String>();
        queryParameters.put(ID_FIELD_NAME, id);
        return get(queryParameters);
    }

    /**
     * Perform a search, falling back on the suggested spellchecked query if the original query fails to return any
     * results.
     * 
     * @param params the Solr parameters to use, should contain at least a value for the "q" parameter; use
     *            {@link #getSolrQuery(String, int, int)} to get the proper parameter expected by this method
     * @return the list of matching documents, empty if there are no matching terms
     */
    private SolrDocumentList search(MapSolrParams params)
    {
        try {
            QueryResponse response = this.server.query(params);
            SolrDocumentList results = response.getResults();
            if (results.size() == 0 && response.getSpellCheckResponse() != null
                && !response.getSpellCheckResponse().isCorrectlySpelled()) {
                String suggestedQuery = response.getSpellCheckResponse().getCollatedResult();
                if (StringUtils.isEmpty(suggestedQuery)) {
                    return new SolrDocumentList();
                }
                // The spellcheck doesn't preserve the identifiers, manually
                // correct this
                suggestedQuery = suggestedQuery.replaceAll("term_category:hip", "term_category:HP");
                MapSolrParams newParams =
                    new MapSolrParams(getSolrQuery(suggestedQuery, params.get(CommonParams.SORT),
                        params.getInt(CommonParams.ROWS, -1), params.getInt(CommonParams.START, 0)));
                return this.server.query(newParams).getResults();
            } else {
                return results;
            }
        } catch (SolrServerException ex) {
            this.logger.error("Failed to search: {}", ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * Create Solr parameters based on the specified search terms. More specifically, concatenates the specified field
     * values into a Lucene query which is used as the "q" parameter, and adds parameters for requesting a spellcheck
     * result.
     * 
     * @param fieldValues the map of values to search for, where each key is the name of an indexed field and the value
     *            is the keywords to match for that field
     * @param rows the number of items to return, or -1 to use the default number of results
     * @param start the number of items to skip, i.e. the index of the first hit to return, 0-based
     * @return a map of Solr query parameter ready to be used for constructing a {@link MapSolrParams} object
     */
    private Map<String, String> getSolrQuery(Map<String, String> fieldValues, int rows, int start)
    {
        return getSolrQuery(fieldValues, "", rows, start);
    }

    /**
     * Create Solr parameters based on the specified search terms. More specifically, concatenates the specified field
     * values into a Lucene query which is used as the "q" parameter, and adds parameters for requesting a spellcheck
     * result.
     * 
     * @param fieldValues the map of values to search for, where each key is the name of an indexed field and the value
     *            is the keywords to match for that field
     * @param sort the sort criteria ("fiel_name order')
     * @param rows the number of items to return, or -1 to use the default number of results
     * @param start the number of items to skip, i.e. the index of the first hit to return, 0-based
     * @return a map of Solr query parameter ready to be used for constructing a {@link MapSolrParams} object
     */
    private Map<String, String> getSolrQuery(Map<String, String> fieldValues, String sort, int rows, int start)
    {
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> field : fieldValues.entrySet()) {
            String value = field.getValue();
            if (value == null) {
                value = "";
            }
            String[] pieces =
                value.replaceAll("[^a-zA-Z0-9 :]/", " ").replace(FIELD_VALUE_SEPARATOR, "\\" + FIELD_VALUE_SEPARATOR)
                    .trim().split("\\s+");
            for (String val : pieces) {
                query.append(field.getKey()).append(FIELD_VALUE_SEPARATOR).append(val).append(" ");
            }
        }
        return getSolrQuery(query.toString().trim(), sort, rows, start);
    }

    /**
     * Convert a Lucene query string into a map of Solr parameters. More specifically, places the input query under the
     * "q" parameter, and adds parameters for requesting a spellcheck result.
     * 
     * @param query the lucene query string to use
     * @param rows the number of items to return, or -1 to use the default number of results
     * @param start the number of items to skip, i.e. the index of the first hit to return, 0-based
     * @return a map of Solr query parameter ready to be used for constructing a {@link MapSolrParams} object
     */
    private Map<String, String> getSolrQuery(String query, int rows, int start)
    {
        return getSolrQuery(query, "", rows, start);
    }

    /**
     * Convert a Lucene query string into a map of Solr parameters. More specifically, places the input query under the
     * "q" parameter, and adds parameters for requesting a spellcheck result.
     * 
     * @param query the lucene query string to use
     * @param sort the sort criteria ("fiel_name order')
     * @param rows the number of items to return, or -1 to use the default number of results
     * @param start the number of items to skip, i.e. the index of the first hit to return, 0-based
     * @return a map of Solr query parameter ready to be used for constructing a {@link MapSolrParams} object
     */
    private Map<String, String> getSolrQuery(String query, String sort, int rows, int start)
    {
        Map<String, String> result = new HashMap<String, String>();
        result.put(CommonParams.START, start + "");
        if (rows > 0) {
            result.put(CommonParams.ROWS, rows + "");
        }
        result.put(CommonParams.Q, query);
        if (!StringUtils.isBlank(sort)) {
            result.put(CommonParams.SORT, sort);
        }
        result.put("spellcheck", Boolean.toString(true));
        result.put("spellcheck.collate", Boolean.toString(true));
        result.put("spellcheck.onlyMorePopular", Boolean.toString(true));
        return result;
    }

    private String dumpMap(Map<String, ? > map)
    {
        StringBuilder out = new StringBuilder();
        out.append('{');
        for (Entry<String, ? > entry : map.entrySet()) {
            out.append(entry.getKey() + ':' + entry.getValue() + '\n');
        }
        out.append('}');
        return out.toString();
    }
}
