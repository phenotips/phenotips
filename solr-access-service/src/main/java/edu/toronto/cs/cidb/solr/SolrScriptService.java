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

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.MapSolrParams;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.script.service.ScriptService;

import edu.toronto.cs.cidb.obo2solr.ParameterPreparer;
import edu.toronto.cs.cidb.obo2solr.SolrUpdateGenerator;
import edu.toronto.cs.cidb.obo2solr.TermData;

/**
 * Provides access to the Solr server, with the main purpose of providing access to the HPO ontology, and secondary
 * purposes of re-indexing the ontology and clearing the index completely. There are two ways of accessing the HPO
 * ontology: getting a single term by its identifier, or searching for terms matching a given query in the Lucene query
 * language.
 * 
 * @version $Id$
 */
@Component
@Named("solr")
@Singleton
public class SolrScriptService implements ScriptService, Initializable
{
    /**
     * Delimiter between the field name and the searched value used in the Lucene query language.
     */
    private static final String FIELD_VALUE_SEPARATOR = ":";

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** The Solr server instance used. */
    private SolrServer server;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.server = new CommonsHttpSolrServer("http://localhost:8080/solr/");
        } catch (MalformedURLException ex) {
            throw new InitializationException("Invalid URL specified for the Solr server: {}");
        }
    }

    /**
     * Search for HPO terms matching the specified query, using the Lucene query language.
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
     * Search for HPO terms matching the specified query, using the Lucene query language.
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
     * Search for HPO terms matching the specified query, using the Lucene query language.
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
     * Search for HPO terms matching the specified query, using the Lucene query language.
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
     * Search for HPO terms matching the specified query, where the query is specified as a map of field name and
     * keywords.
     * 
     * @param fieldValues the map of values to search for, where each key is the name of an indexed field and the value
     *        is the keywords to match for that field
     * @return the list of matching documents, empty if there are no matching terms
     */
    public SolrDocumentList search(final Map<String, String> fieldValues)
    {
        return search(fieldValues, -1, 0);
    }

    /**
     * Search for HPO terms matching the specified query, where the query is specified as a map of field name and
     * keywords.
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
     * Search for HPO terms matching the specified query, where the query is specified as a map of field name and
     * keywords.
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
     * Search for HPO terms matching the specified query, where the query is specified as a map of field name and
     * keywords.
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
     *        is the keywords to match for that field
     * @return the top matching document, {@code null} if there were no matches at all
     * @see #search(Map)
     */
    public SolrDocument get(final Map<String, String> fieldValues)
    {
        SolrDocumentList all = search(fieldValues, 1, 0);
        if (!all.isEmpty()) {
            return all.get(0);
        }
        return null;
    }

    /**
     * Get the document corresponding to the specified HPO identifier.
     * 
     * @param id the HPO identifier to search for, in the {@code HP:1234567} format
     * @return the matching document, if one was found, or {@code null} otherwise
     */
    public SolrDocument get(final String id)
    {
        Map<String, String> queryParameters = new HashMap<String, String>();
        queryParameters.put("id", id);
        SolrDocumentList all = search(queryParameters, 1, 0);
        if (!all.isEmpty()) {
            return all.get(0);
        }
        return null;
    }

    /**
     * Delete all the data in the Solr index.
     * 
     * @return {@code 0} if the command was successful, {@code 1} otherwise
     */
    public int clear()
    {
        try {
            this.server.deleteByQuery("*:*");
            this.server.commit();
            return 0;
        } catch (SolrServerException ex) {
            this.logger.error("SolrServerException while clearing the Solr index", ex);
        } catch (IOException ex) {
            this.logger.error("IOException while clearing the Solr index", ex);
        }
        return 1;
    }

    /**
     * Add an ontology to the index.
     * 
     * @param ontologyUrl the address from where to get the ontology file
     * @param fieldList the list of ontology fields to index; comma separated list of field names with an optional boost
     *        separated by a color; for example: {@code id:50,name,def,synonym,is_a:0.1}; if the empty string is passed,
     *        then all fields from the ontology are indexed, using the default boost of 1.0
     * @return {@code 0} if the indexing succeeded, {@code 1} if writing to the Solr server failed, {@code 2} if the
     *         specified URL is invalid
     */
    public int index(String ontologyUrl, String fieldList)
    {
        ParameterPreparer paramPrep = new ParameterPreparer();
        SolrUpdateGenerator generator = new SolrUpdateGenerator();
        Map<String, Double> fieldSelection = paramPrep.getFieldSelection(fieldList);
        Map<String, TermData> data = generator.transform(ontologyUrl, fieldSelection);
        if (data == null) {
            return 2;
        }
        Collection<SolrInputDocument> allTerms = new HashSet<SolrInputDocument>();
        for (Map.Entry<String, TermData> item : data.entrySet()) {
            SolrInputDocument doc = new SolrInputDocument();
            for (Map.Entry<String, Collection<String>> property : item.getValue().entrySet()) {
                String name = property.getKey();
                for (String value : property.getValue()) {
                    doc.addField(name, value, (fieldSelection.get(name) == null ? ParameterPreparer.DEFAULT_BOOST
                        : fieldSelection.get(name)).floatValue());
                }
            }
            allTerms.add(doc);
        }
        try {
            this.server.add(allTerms);
            this.server.commit();
            return 0;
        } catch (SolrServerException ex) {
            this.logger.warn("Failed to index ontology: {}", ex.getMessage());
        } catch (IOException ex) {
            this.logger.warn("Failed to communicate with the Solr server while indexing ontology: {}", ex.getMessage());
        }
        return 1;
    }

    /**
     * Perform a search, falling back on the suggested spellchecked query if the original query fails to return any
     * results.
     * 
     * @param params the Solr parameters to use, should contain at least a value for the "q" parameter; use
     *        {@link #getSolrQuery(String, int, int)} to get the proper parameter expected by this method
     * @return the list of matching documents, empty if there are no matching terms
     */
    private SolrDocumentList search(MapSolrParams params)
    {
        try {
            QueryResponse response = this.server.query(params);
            SolrDocumentList results = response.getResults();
            if (results.size() == 0 && !response.getSpellCheckResponse().isCorrectlySpelled()) {
                String suggestedQuery = response.getSpellCheckResponse().getCollatedResult();
                // The spellcheck doesn't preserve the identifiers, manually
                // correct this
                suggestedQuery = suggestedQuery.replaceAll("term_category:hip", "term_category:HP");
                MapSolrParams newParams =
                    new MapSolrParams(getSolrQuery(suggestedQuery, params.get(CommonParams.SORT), params.getInt(
                        CommonParams.ROWS, -1), params.getInt(CommonParams.START, 0)));
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
     *        is the keywords to match for that field
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
            String[] pieces = value.replaceAll("[^a-zA-Z0-9 :]/", " ")
                .replace(FIELD_VALUE_SEPARATOR, "\\" + FIELD_VALUE_SEPARATOR).trim().split("\\s+");
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
}
