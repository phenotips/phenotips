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

import org.phenotips.vocabulary.SolrVocabularyResourceManager;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyExtension;
import org.phenotips.vocabulary.VocabularyInputTerm;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;

/**
 * Provides access to the Solr server, with the main purpose of providing access to an indexed vocabulary. There are two
 * ways of accessing items in the vocabulary: getting a single term by its identifier, or searching for terms matching a
 * given query in the Lucene query language.
 *
 * @version $Id$
 * @since 1.2M4 (under different names since 1.0M8)
 */
public abstract class AbstractSolrVocabulary implements Vocabulary, Initializable
{
    /** The name of the ID field. */
    protected static final String ID_FIELD_NAME = "id";

    /**
     * Object used to mark in the cache that a term doesn't exist, since null means that the cache doesn't contain the
     * requested entry.
     */
    private static final VocabularyTerm EMPTY_MARKER = new SolrVocabularyTerm(null, null);

    /** Logging helper object. */
    @Inject
    protected Logger logger;

    /** The object used for initializing server connection and cache. */
    @Inject
    protected SolrVocabularyResourceManager externalServicesAccess;

    /** The extensions that apply to this vocabulary. */
    @Inject
    protected List<VocabularyExtension> extensions;

    @Override
    public void initialize() throws InitializationException
    {
        this.externalServicesAccess.initialize(this.getCoreName());
        this.extensions = filterSupportedExtensions();
    }

    // Dilemma:
    // In an ideal world there should be a getter methods for server and cache instances.
    // However the point of splitting up the server was to lessen the number of imports

    /**
     * Get the name of the Solr "core" to be used by this service instance.
     *
     * @return the simple core name
     */
    protected abstract String getCoreName();

    @Override
    public VocabularyTerm getTerm(String id)
    {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        VocabularyTerm result = this.externalServicesAccess.getTermCache(getCoreName()).get(id);
        if (result == null) {
            SolrQuery query = new SolrQuery(ID_FIELD_NAME + ':' + ClientUtils.escapeQueryChars(id));
            SolrDocumentList allResults = this.search(query);
            if (allResults != null && !allResults.isEmpty()) {
                result = new SolrVocabularyTerm(allResults.get(0), this);
                this.externalServicesAccess.getTermCache(getCoreName()).set(id, result);
            } else {
                this.externalServicesAccess.getTermCache(getCoreName()).set(id, EMPTY_MARKER);
            }
        }
        return (result == EMPTY_MARKER) ? null : result;
    }

    @Override
    public Set<VocabularyTerm> getTerms(Collection<String> ids)
    {
        Map<String, VocabularyTerm> rawResult = new HashMap<>();
        StringBuilder query = new StringBuilder("id:(");
        for (String id : ids) {
            VocabularyTerm cachedTerm = this.externalServicesAccess.getTermCache(getCoreName()).get(id);
            if (cachedTerm != null) {
                if (cachedTerm != EMPTY_MARKER) {
                    rawResult.put(id, cachedTerm);
                }
            } else {
                query.append(ClientUtils.escapeQueryChars(id));
                query.append(' ');
            }
        }
        query.append(')');

        // There's at least one more term not found in the cache
        if (query.length() > 5) {
            for (SolrDocument doc : this.search(new SolrQuery(query.toString()))) {
                VocabularyTerm term = new SolrVocabularyTerm(doc, this);
                rawResult.put(term.getId(), term);
            }
        }

        Set<VocabularyTerm> result = new LinkedHashSet<>();
        for (String id : ids) {
            result.add(rawResult.get(id));
        }
        return result;
    }

    @Override
    public List<VocabularyTerm> search(Map<String, ?> fieldValues)
    {
        return search(fieldValues, null);
    }

    @Override
    public List<VocabularyTerm> search(Map<String, ?> fieldValues, Map<String, String> queryOptions)
    {
        List<VocabularyTerm> result = new LinkedList<>();
        for (SolrDocument doc : this
            .search(SolrQueryUtils.generateQuery(new SolrQuery(generateLuceneQuery(fieldValues)), queryOptions))) {
            result.add(new SolrVocabularyTerm(doc, this));
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
    public int reindex(String sourceUrl)
    {
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
    public long getDistance(VocabularyTerm fromTerm, VocabularyTerm toTerm)
    {
        if (fromTerm == null || toTerm == null) {
            return -1;
        }
        return fromTerm.getDistanceTo(toTerm);
    }

    @Override
    public List<VocabularyTerm> search(String input)
    {
        return search(input, 10, null, null);
    }

    @Override
    public List<VocabularyTerm> search(String input, int maxResults, String sort, String customFilter)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Perform a search, falling back on the suggested spellchecked query if the original query fails to return any
     * results.
     *
     * @param query the Solr query to use, should contain at least a value for the "q" parameter
     * @return the list of matching documents, empty if there are no matching terms
     */
    protected SolrDocumentList search(SolrQuery query)
    {
        try {
            query.setIncludeScore(true);
            this.logger.debug("Extending query [{}] for vocabulary [{}]", query, getCoreName());
            for (VocabularyExtension extension : this.extensions) {
                extension.extendQuery(query, this);
            }
            this.logger.debug("Searching [{}] with query [{}]", getCoreName(), query);
            QueryResponse response = this.externalServicesAccess.getSolrConnection(getCoreName()).query(query);
            SolrDocumentList results = response.getResults();
            if (response.getSpellCheckResponse() != null && !response.getSpellCheckResponse().isCorrectlySpelled()
                && StringUtils.isNotEmpty(response.getSpellCheckResponse().getCollatedResult())) {
                SolrQueryUtils.applySpellcheckSuggestion(query,
                    response.getSpellCheckResponse().getCollatedResult());
                this.logger.debug("Searching [{}] with spellchecked query [{}]", getCoreName(), query);
                SolrDocumentList spellcheckResults =
                    this.externalServicesAccess.getSolrConnection(getCoreName()).query(query).getResults();
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
     * @param query a valid Lucene query as string
     * @return the number of entries matching the query
     */
    protected long count(String query)
    {
        SolrQuery params = new SolrQuery(query);
        params.setStart(0);
        params.setRows(0);
        SolrDocumentList results;
        try {
            this.logger.debug("Counting terms matching [{}] in [{}]", query, getCoreName());
            results = this.externalServicesAccess.getSolrConnection(getCoreName()).query(params).getResults();
            return results.getNumFound();
        } catch (Exception ex) {
            this.logger.error("Failed to count vocabulary terms: {}", ex.getMessage(), ex);
            return -1;
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
            query.append('+');
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

    /**
     * Runs the term given through all the vocabulary extensions we have.
     *
     * @param term the term being processed
     */
    protected void extendTerm(VocabularyInputTerm term)
    {
        for (VocabularyExtension extension : this.extensions) {
            extension.extendTerm(term, this);
        }
    }

    /**
     * Gets only the list of extensions that this vocabulary supports.
     *
     * @return the list of supported extensions, may be empty
     */
    private List<VocabularyExtension> filterSupportedExtensions()
    {
        List<VocabularyExtension> result = new LinkedList<>();
        for (VocabularyExtension extension : this.extensions) {
            if (extension.isVocabularySupported(this)) {
                result.add(extension);
            }
        }
        return result;
    }
}
