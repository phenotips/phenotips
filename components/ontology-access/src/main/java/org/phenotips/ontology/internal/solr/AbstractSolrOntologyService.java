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
package org.phenotips.ontology.internal.solr;

import org.phenotips.ontology.OntologyService;
import org.phenotips.ontology.OntologyTerm;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.slf4j.Logger;

/**
 * Provides access to the Solr server, with the main purpose of providing access to an indexed ontology. There are two
 * ways of accessing items in the ontology: getting a single term by its identifier, or searching for terms matching a
 * given query in the Lucene query language.
 * 
 * @version $Id$
 * @since 1.0M8
 */
public abstract class AbstractSolrOntologyService implements OntologyService, Initializable
{
    /** The name of the ID field. */
    protected static final String ID_FIELD_NAME = "id";

    /**
     * Object used to mark in the cache that a term doesn't exist, since null means that the cache doesn't contain the
     * requested entry.
     */
    private static final OntologyTerm EMPTY_MARKER = new SolrOntologyTerm(null, null);

    /** Logging helper object. */
    @Inject
    protected Logger logger;

    /** The Solr server instance used. */
    protected SolrServer server;

    /**
     * Cache for the recently accessed terms; useful since the ontology rarely changes, so a search should always return
     * the same thing.
     */
    protected Cache<OntologyTerm> cache;

    /** Cache factory needed for creating the term cache. */
    @Inject
    protected CacheManager cacheFactory;

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

    @Override
    public OntologyTerm getTerm(String id)
    {
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set(CommonParams.Q, ID_FIELD_NAME + ':' + ClientUtils.escapeQueryChars(id));
        String cacheKey = SolrQueryUtils.getCacheKey(params);
        OntologyTerm result = this.cache.get(cacheKey);
        if (result == null) {
            SolrDocumentList allResults = this.search(params);
            if (allResults != null && !allResults.isEmpty()) {
                result = new SolrOntologyTerm(allResults.get(0), this);
                this.cache.set(cacheKey, result);
            } else {
                this.cache.set(cacheKey, EMPTY_MARKER);
            }
        }
        return (result == EMPTY_MARKER) ? null : result;
    }

    @Override
    public Set<OntologyTerm> getTerms(Collection<String> ids)
    {
        StringBuilder query = new StringBuilder("id:(");
        for (String id : ids) {
            query.append(ClientUtils.escapeQueryChars(id));
            query.append(' ');
        }
        query.append(')');
        Set<OntologyTerm> result = new HashSet<OntologyTerm>();
        for (SolrDocument doc : this.search(SolrQueryUtils.transformQueryToSolrParams(query.toString()))) {
            result.add(new SolrOntologyTerm(doc, this));
        }
        return result;
    }

    @Override
    public Set<OntologyTerm> search(Map<String, ?> fieldValues)
    {
        Set<OntologyTerm> result = new HashSet<OntologyTerm>();
        for (SolrDocument doc : this
            .search(SolrQueryUtils.transformQueryToSolrParams(generateLuceneQuery(fieldValues)))) {
            result.add(new SolrOntologyTerm(doc, this));
        }
        return result;
    }

    @Override
    public long count(Map<String, ?> fieldValues)
    {
        return count(this.generateLuceneQuery(fieldValues));
    }

    @Override
    public long size()
    {
        return count("*:*");
    }

    @Override
    public void reindex()
    {
        // FIXME Not implemented yet
        throw new UnsupportedOperationException();
    }

    /**
     * Perform a search, falling back on the suggested spellchecked query if the original query fails to return any
     * results.
     * 
     * @param params the Solr parameters to use, should contain at least a value for the "q" parameter
     * @return the list of matching documents, empty if there are no matching terms
     */
    protected SolrDocumentList search(SolrParams params)
    {
        try {
            SolrParams enhancedParams = SolrQueryUtils.enhanceParams(params);
            QueryResponse response = this.server.query(enhancedParams);
            SolrDocumentList results = response.getResults();
            if (response.getSpellCheckResponse() != null && !response.getSpellCheckResponse().isCorrectlySpelled()
                && StringUtils.isNotEmpty(response.getSpellCheckResponse().getCollatedResult())) {
                enhancedParams =
                    SolrQueryUtils.applySpellcheckSuggestion(enhancedParams, response.getSpellCheckResponse()
                        .getCollatedResult());
                SolrDocumentList spellcheckResults = this.server.query(enhancedParams).getResults();
                if (results.getMaxScore() < spellcheckResults.getMaxScore()) {
                    results = spellcheckResults;
                }
            }
            return results;
        } catch (Exception ex) {
            this.logger.error("Failed to search: {}", ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * Get the number of entries that match a specific Lucene query.
     * 
     * @param query a valid the Lucene query as string
     * @return the number of entries matching the query
     */
    protected long count(String query)
    {
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set(CommonParams.Q, query);
        params.set(CommonParams.START, "0");
        params.set(CommonParams.ROWS, "0");
        SolrDocumentList results;
        try {
            results = this.server.query(params).getResults();
            return results.getNumFound();
        } catch (Exception ex) {
            this.logger.error("Failed to count ontology terms: {}", ex.getMessage(), ex);
            return 0;
        }
    }

    /**
     * Generate a Lucene query from a map of parameters, to be used in the "q" parameter for Solr.
     * 
     * @param fieldValues a map with term meta-property values that must be matched by the returned terms; the keys are
     *            property names, like {@code id}, {@code description}, {@code is_a}, and the values can be either a
     *            single value, or a collection of values that can (OR) be matched by the term;
     * @return the String representation of the equivalent Lucene query
     */
    protected String generateLuceneQuery(Map<String, ?> fieldValues)
    {
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, ?> field : fieldValues.entrySet()) {
            query.append("+");
            query.append(ClientUtils.escapeQueryChars(field.getKey()));
            query.append(":(");
            if (Collection.class.isInstance(field.getValue())) {
                for (Object value : (Collection<?>) field.getValue()) {
                    query.append(ClientUtils.escapeQueryChars(String.valueOf(value)));
                    query.append(' ');
                }
            } else {
                query.append(ClientUtils.escapeQueryChars(String.valueOf(field.getValue())));
            }
            query.append(')');
        }
        return query.toString();
    }
}
