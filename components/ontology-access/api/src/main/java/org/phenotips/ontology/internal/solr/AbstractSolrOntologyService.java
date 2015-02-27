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
import org.phenotips.ontology.SolrOntologyServiceInitializer;

import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
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

    /** The object used for initializing server connection and cache. */
    @Inject
    protected SolrOntologyServiceInitializer externalServicesAccess;

    @Override
    public void initialize() throws InitializationException
    {
        this.externalServicesAccess.initialize(this.getName());
    }

    // Dilemma:
    // In an ideal world there should be a getter methods for server and cache instances.
    // However the point of splitting up the server was to lessen the number of imports

    /**
     * Get the name of the Solr "core" to be used by this service instance.
     *
     * @return the simple core name
     */
    protected abstract String getName();

    @Override
    public OntologyTerm getTerm(String id)
    {
        OntologyTerm result = this.externalServicesAccess.getCache().get(id);
        if (result == null) {
            ModifiableSolrParams params = new ModifiableSolrParams();
            params.set(CommonParams.Q, ID_FIELD_NAME + ':' + ClientUtils.escapeQueryChars(id));
            SolrDocumentList allResults = this.search(params);
            if (allResults != null && !allResults.isEmpty()) {
                result = new SolrOntologyTerm(allResults.get(0), this);
                this.externalServicesAccess.getCache().set(id, result);
            } else {
                this.externalServicesAccess.getCache().set(id, EMPTY_MARKER);
            }
        }
        return (result == EMPTY_MARKER) ? null : result;
    }

    @Override
    public Set<OntologyTerm> getTerms(Collection<String> ids)
    {
        Set<OntologyTerm> result = new LinkedHashSet<OntologyTerm>();
        StringBuilder query = new StringBuilder("id:(");
        for (String id : ids) {
            OntologyTerm cachedTerm = this.externalServicesAccess.getCache().get(id);
            if (cachedTerm != null) {
                if (cachedTerm != EMPTY_MARKER) {
                    result.add(cachedTerm);
                }
            } else {
                query.append(ClientUtils.escapeQueryChars(id));
                query.append(' ');
            }
        }
        query.append(')');

        // There's at least one more term not found in the cache
        if (query.length() > 5) {
            for (SolrDocument doc : this.search(SolrQueryUtils.transformQueryToSolrParams(query.toString()))) {
                result.add(new SolrOntologyTerm(doc, this));
            }
        }
        return result;
    }

    @Override
    public Set<OntologyTerm> search(Map<String, ?> fieldValues)
    {
        return search(fieldValues, null);
    }

    @Override
    public Set<OntologyTerm> search(Map<String, ?> fieldValues, Map<String, String> queryOptions)
    {
        Set<OntologyTerm> result = new LinkedHashSet<OntologyTerm>();
        for (SolrDocument doc : this
            .search(SolrQueryUtils.transformQueryToSolrParams(generateLuceneQuery(fieldValues)), queryOptions)) {
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
    public int reindex(String ontologyUrl)
    {
        // FIXME Not implemented yet
        throw new UnsupportedOperationException();
    }

    @Override
    public String getVersion()
    {
        return null;
    }

    @Override
    public long getDistance(String fromTermId, String toTermId)
    {
        return getDistance(getTerm(fromTermId), getTerm(toTermId));
    }

    @Override
    public long getDistance(OntologyTerm fromTerm, OntologyTerm toTerm)
    {
        if (fromTerm == null || toTerm == null) {
            return -1;
        }
        return fromTerm.getDistanceTo(toTerm);
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
        return search(params, null);
    }

    /**
     * Perform a search, falling back on the suggested spellchecked query if the original query fails to return any
     * results.
     *
     * @param params the Solr parameters to use, should contain at least a value for the "q" parameter
     * @param queryOptions extra options to include in the query; these override the default values, but don't override
     *            values already set in the query
     * @return the list of matching documents, empty if there are no matching terms
     */
    protected SolrDocumentList search(SolrParams params, Map<String, String> queryOptions)
    {
        try {
            SolrParams enhancedParams = SolrQueryUtils.enhanceParams(params, queryOptions);
            QueryResponse response = this.externalServicesAccess.getServer().query(enhancedParams);
            SolrDocumentList results = response.getResults();
            if (response.getSpellCheckResponse() != null && !response.getSpellCheckResponse().isCorrectlySpelled()
                && StringUtils.isNotEmpty(response.getSpellCheckResponse().getCollatedResult())) {
                enhancedParams =
                    SolrQueryUtils.applySpellcheckSuggestion(enhancedParams, response.getSpellCheckResponse()
                        .getCollatedResult());
                SolrDocumentList spellcheckResults =
                    this.externalServicesAccess.getServer().query(enhancedParams).getResults();
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
            results = this.externalServicesAccess.getServer().query(params).getResults();
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
            if (Collection.class.isInstance(field.getValue()) && ((Collection<?>) field.getValue()).isEmpty()) {
                continue;
            }
            query.append("+");
            query.append(ClientUtils.escapeQueryChars(field.getKey()));
            query.append(":(");
            if (Collection.class.isInstance(field.getValue())) {
                for (Object value : (Collection<?>) field.getValue()) {
                    query.append(ClientUtils.escapeQueryChars(String.valueOf(value)));
                    query.append(' ');
                }
            } else {
                String value = String.valueOf(field.getValue());
                if ("*".equals(value)) {
                    query.append(value);
                } else {
                    query.append(ClientUtils.escapeQueryChars(value));
                }
            }
            query.append(')');
        }
        return query.toString();
    }

    @Override
    public Set<OntologyTerm> termSuggest(String query, Integer rows, String sort, String customFq)
    {
        throw new UnsupportedOperationException();
    }
}
