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
package org.phenotips.solr;

import org.phenotips.ontology.SolrOntologyServiceInitializer;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.script.service.ScriptService;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;

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

    /** Character used in URLs to delimit path segments. */
    private static final String URL_PATH_SEPARATOR = "/";

    /**
     * Object used to mark in the cache that a document doesn't exist, since null means that the cache doesn't contain
     * the requested entry.
     */
    private static final SolrDocument EMPTY_MARKER = new SolrDocument();

    /** Logging helper object. */
    @Inject
    protected Logger logger;

    /** The Solr server instance used. */
    protected SolrServer server;

    @Inject
    protected SolrOntologyServiceInitializer initializer;

    /**
     * Cache for the recently accessed documents; useful since the ontology rarely changes, so a search should always
     * return the same thing.
     */
    protected Cache<SolrDocument> cache;

    /** Cache factory needed for creating the document cache. */
    @Inject
    protected CacheManager cacheFactory;

    @Inject
    @Named("xwikiproperties")
    protected ConfigurationSource configuration;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.initializer.initialize(getName());
            this.server = this.initializer.getServer();
            this.cache = this.cacheFactory.createNewLocalCache(new CacheConfiguration());
        } catch (RuntimeException ex) {
            throw new InitializationException("Invalid URL specified for the Solr server: {}");
        } catch (final CacheException ex) {
            throw new InitializationException("Cannot create cache: " + ex.getMessage());
        }
    }

    /**
     * Get the URL where the Solr server can be reached, without any core name.
     *
     * @return an URL as a String
     */
    protected String getSolrLocation()
    {
        String wikiSolrUrl = this.configuration.getProperty("solr.remote.url", String.class);
        if (StringUtils.isBlank(wikiSolrUrl)) {
            return "http://localhost:8080/solr/";
        }
        return StringUtils.substringBeforeLast(StringUtils.removeEnd(wikiSolrUrl, URL_PATH_SEPARATOR),
            URL_PATH_SEPARATOR) + URL_PATH_SEPARATOR;
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
     * Advanced search using custom search parameters. At least the {@code q} parameter should be set, but any other
     * parameters supported by Solr can be specified in this map.
     *
     * @param searchParameters a map of parameters, the keys should be parameters that Solr understands
     * @return the list of matching documents, empty if there are no matching terms
     */
    public SolrDocumentList customSearch(final Map<String, String> searchParameters)
    {
        MapSolrParams params = new MapSolrParams(searchParameters);
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
            NamedList<Object> newParams = params.toNamedList();
            if (newParams.get(CommonParams.FL) == null) {
                newParams.add(CommonParams.FL, "* score");
            }
            QueryResponse response = this.server.query(MapSolrParams.toSolrParams(newParams));
            SolrDocumentList results = response.getResults();
            if (response.getSpellCheckResponse() != null && !response.getSpellCheckResponse().isCorrectlySpelled()) {
                String suggestedQuery = response.getSpellCheckResponse().getCollatedResult();
                if (StringUtils.isEmpty(suggestedQuery)) {
                    return results;
                }
                Pattern p = Pattern.compile("(\\w++):(\\w++)\\*$", Pattern.CASE_INSENSITIVE);
                Matcher originalStub = p.matcher((String) newParams.get(CommonParams.Q));
                newParams.remove(CommonParams.Q);
                Matcher newStub = p.matcher(suggestedQuery);
                if (originalStub.find() && newStub.find()) {
                    suggestedQuery += ' ' + originalStub.group() + "^1.5 " + originalStub.group(2) + "^1.5";
                    String boostQuery = (String) newParams.get(DisMaxParams.BQ);
                    if (boostQuery != null) {
                        boostQuery += ' ' + boostQuery.replace(originalStub.group(2), newStub.group(2));
                        newParams.remove(DisMaxParams.BQ);
                        newParams.add(DisMaxParams.BQ, boostQuery);
                    }
                }
                newParams.add(CommonParams.Q, suggestedQuery);
                SolrDocumentList spellcheckResults =
                    this.server.query(MapSolrParams.toSolrParams(newParams)).getResults();
                if (results.getMaxScore() < spellcheckResults.getMaxScore()) {
                    results = spellcheckResults;
                }
            }
            return results;
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
                value.replaceAll("[^a-zA-Z0-9 :]", " ").replace(FIELD_VALUE_SEPARATOR, "\\" + FIELD_VALUE_SEPARATOR)
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
        if (StringUtils.isNotBlank(sort)) {
            result.put(CommonParams.SORT, sort);
        }
        result.put("spellcheck", Boolean.toString(true));
        result.put("spellcheck.collate", Boolean.toString(true));
        return result;
    }

    /**
     * Serialize a Map into a String.
     *
     * @param map the map to serialize
     * @return a String serialization of the map
     */
    private String dumpMap(Map<String, ?> map)
    {
        StringBuilder out = new StringBuilder();
        out.append('{');
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            out.append(entry.getKey() + ':' + entry.getValue() + '\n');
        }
        out.append('}');
        return out.toString();
    }
}
