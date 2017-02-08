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

import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.SpellingParams;

/**
 * Provides access to the Online Mendelian Inheritance in Man (OMIM) vocabulary. The vocabulary prefix is {@code MIM}.
 *
 * @version $Id$
 * @since 1.0M8
 */
@Component
@Named("omim")
@Singleton
public class MendelianInheritanceInMan extends AbstractSolrVocabulary
{
    /** The standard name of this vocabulary, used as a term prefix. */
    public static final String STANDARD_NAME = "MIM";

    @Inject
    @Named("hpo")
    private Vocabulary hpo;

    @Override
    protected String getCoreName()
    {
        return getIdentifier();
    }

    @Override
    public VocabularyTerm getTerm(String id)
    {
        VocabularyTerm result = super.getTerm(id);
        if (result == null) {
            String optionalPrefix = STANDARD_NAME + ":";
            if (StringUtils.startsWith(id, optionalPrefix)) {
                result = getTerm(StringUtils.substringAfter(id, optionalPrefix));
            }
        }
        return result;
    }

    @Override
    public List<VocabularyTerm> search(String input, int maxResults, String sort, String customFilter)
    {
        if (StringUtils.isBlank(input)) {
            return Collections.emptyList();
        }
        SolrQuery query = new SolrQuery();
        addFieldQueryParameters(addGlobalQueryParameters(query));
        List<VocabularyTerm> result = new LinkedList<>();
        for (SolrDocument doc : search(addDynamicQueryParameters(input, maxResults, sort, customFilter, query))) {
            result.add(new SolrVocabularyTerm(doc, this));
        }
        return result;
    }

    @Override
    public String getIdentifier()
    {
        return "omim";
    }

    @Override
    public String getName()
    {
        return "Online Mendelian Inheritance in Man (OMIM)";
    }

    @Override
    public Set<String> getAliases()
    {
        Set<String> result = new HashSet<>();
        result.add(getIdentifier());
        result.add(STANDARD_NAME);
        result.add("OMIM");
        return result;
    }

    @Override
    public String getWebsite()
    {
        return "http://www.omim.org/";
    }

    @Override
    public String getCitation()
    {
        return "Online Mendelian Inheritance in Man, OMIM\u00ae. McKusick-Nathans Institute of Genetic Medicine,"
            + " Johns Hopkins University (Baltimore, MD)";
    }

    @Override
    public String getDefaultSourceLocation()
    {
        return OmimSourceParser.OMIM_SOURCE_URL;
    }

    private SolrQuery addGlobalQueryParameters(SolrQuery query)
    {
        query.set("spellcheck", Boolean.toString(true));
        query.set(SpellingParams.SPELLCHECK_COLLATE, Boolean.toString(true));
        query.set(SpellingParams.SPELLCHECK_COUNT, "100");
        query.set(SpellingParams.SPELLCHECK_MAX_COLLATION_TRIES, "3");
        query.set("lowercaseOperators", Boolean.toString(false));
        query.set("defType", "edismax");
        return query;
    }

    private SolrQuery addFieldQueryParameters(SolrQuery query)
    {
        query.set(DisMaxParams.PF, "name^40 nameSpell^70 synonym^15 synonymSpell^25 text^3 textSpell^5");
        query.set(DisMaxParams.QF,
            "name^10 nameSpell^18 nameStub^5 synonym^6 synonymSpell^10 synonymStub^3 text^1 textSpell^2 textStub^0.5");
        return query;
    }

    private SolrQuery addDynamicQueryParameters(String originalQuery, Integer rows, String sort, String customFq,
        SolrQuery query)
    {
        String queryString = originalQuery.trim();
        String escapedQuery = ClientUtils.escapeQueryChars(queryString);
        query.setFilterQueries(StringUtils.defaultIfBlank(customFq, "-(nameSort:\\** nameSort:\\+* nameSort:\\^*)"));
        query.setQuery(escapedQuery);
        query.set(SpellingParams.SPELLCHECK_Q, queryString);
        String lastWord = StringUtils.substringAfterLast(escapedQuery, " ");
        if (StringUtils.isBlank(lastWord)) {
            lastWord = escapedQuery;
        }
        lastWord += "*";
        query.set(DisMaxParams.BQ,
            String.format("nameSpell:%1$s^20 keywords:%1$s^2 text:%1$s^1 textSpell:%1$s^2", lastWord));
        query.setRows(rows);
        if (StringUtils.isNotBlank(sort)) {
            for (String sortItem : sort.split("\\s*,\\s*")) {
                query.addSort(StringUtils.substringBefore(sortItem, " "),
                    sortItem.endsWith(" desc") || sortItem.startsWith("-") ? ORDER.desc : ORDER.asc);
            }
        }
        return query;
    }

    @Override
    public String getVersion()
    {
        SolrQuery query = new SolrQuery();
        query.setQuery("version:*");
        query.set(CommonParams.ROWS, "1");
        try {
            QueryResponse response = this.externalServicesAccess.getSolrConnection(getCoreName()).query(query);
            SolrDocumentList termList = response.getResults();
            if (!termList.isEmpty()) {
                return termList.get(0).getFieldValue("version").toString();
            }
        } catch (SolrServerException | SolrException ex) {
            this.logger.warn("Failed to query vocabulary version: {}", ex.getMessage());
        } catch (IOException ex) {
            this.logger.error("IOException while getting vocabulary version", ex);
        }
        return null;
    }

    @Override
    public synchronized int reindex(String sourceURL)
    {
        try {
            Collection<SolrInputDocument> data = new OmimSourceParser(this.hpo, sourceURL).getData();
            if (data.isEmpty()) {
                return 2;
            }
            if (clear() == 1) {
                return 1;
            }
            this.externalServicesAccess.getSolrConnection(getCoreName()).add(data);
            this.externalServicesAccess.getSolrConnection(getCoreName()).commit();
            this.externalServicesAccess.getTermCache(getCoreName()).removeAll();
        } catch (SolrServerException | IOException ex) {
            this.logger.error("Failed to reindex OMIM: {}", ex.getMessage(), ex);
            return 1;
        }
        return 0;
    }

    /**
     * Delete all the data in the Solr index.
     *
     * @return {@code 0} if the command was successful, {@code 1} otherwise
     */
    private int clear()
    {
        try {
            this.externalServicesAccess.getSolrConnection(getCoreName()).deleteByQuery("*:*");
            return 0;
        } catch (SolrServerException ex) {
            this.logger.error("SolrServerException while clearing the Solr index", ex);
        } catch (IOException ex) {
            this.logger.error("IOException while clearing the Solr index", ex);
        }
        return 1;
    }
}
