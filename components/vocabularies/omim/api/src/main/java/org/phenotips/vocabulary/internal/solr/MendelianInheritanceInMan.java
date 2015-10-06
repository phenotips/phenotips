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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.params.SpellingParams;

/**
 * Provides access to the Online Mendelian Inheritance in Man (OMIM) ontology. The ontology prefix is {@code MIM}.
 *
 * @version $Id$
 * @since 1.0M8
 */
@Component
@Named("omim")
@Singleton
public class MendelianInheritanceInMan extends AbstractSolrVocabulary
{
    /** The standard name of this ontology, used as a term prefix. */
    public static final String STANDARD_NAME = "MIM";

    @Inject
    @Named("hpo")
    private Vocabulary hpo;

    @Override
    protected String getName()
    {
        return "omim";
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
        Map<String, String> options = this.getStaticSolrParams();
        options.putAll(this.getStaticFieldSolrParams());
        List<VocabularyTerm> result = new LinkedList<>();
        for (SolrDocument doc : this.search(produceDynamicSolrParams(input, maxResults, sort, customFilter), options)) {
            result.add(new SolrVocabularyTerm(doc, this));
        }
        return result;
    }

    @Override
    public Set<String> getAliases()
    {
        Set<String> result = new HashSet<String>();
        result.add(getName());
        result.add(STANDARD_NAME);
        result.add("OMIM");
        return result;
    }

    @Override
    public String getDefaultSourceLocation()
    {
        return OmimSourceParser.OMIM_SOURCE_URL;
    }

    private Map<String, String> getStaticSolrParams()
    {
        Map<String, String> params = new HashMap<>();
        params.put("spellcheck", Boolean.toString(true));
        params.put(SpellingParams.SPELLCHECK_COLLATE, Boolean.toString(true));
        params.put(SpellingParams.SPELLCHECK_COUNT, "100");
        params.put(SpellingParams.SPELLCHECK_MAX_COLLATION_TRIES, "3");
        params.put("lowercaseOperators", Boolean.toString(false));
        params.put("defType", "edismax");
        return params;
    }

    private Map<String, String> getStaticFieldSolrParams()
    {
        Map<String, String> params = new HashMap<>();
        params.put(DisMaxParams.PF, "name^40 nameSpell^100 keywords^6 text^3 textSpell^5");
        params.put(DisMaxParams.QF, "name^20 nameSpell^50 keywords^2 text^1 textSpell^2");
        return params;
    }

    private SolrParams produceDynamicSolrParams(String originalQuery, Integer rows, String sort, String customFq)
    {
        String query = originalQuery.trim();
        ModifiableSolrParams params = new ModifiableSolrParams();
        String escapedQuery = ClientUtils.escapeQueryChars(query);
        params.add(CommonParams.FQ,
            StringUtils.defaultIfBlank(customFq, "-(nameSort:\\** nameSort:\\+* nameSort:\\^*)"));
        params.add(CommonParams.Q, escapedQuery);
        params.add(SpellingParams.SPELLCHECK_Q, query);
        String lastWord = StringUtils.substringAfterLast(escapedQuery, " ");
        if (StringUtils.isBlank(lastWord)) {
            lastWord = escapedQuery;
        }
        lastWord += "*";
        params.add(DisMaxParams.BQ,
            String.format("nameSpell:%1$s^20 keywords:%1$s^2 text:%1$s^1 textSpell:%1$s^2", lastWord));
        params.add(CommonParams.ROWS, rows.toString());
        if (StringUtils.isNotBlank(sort)) {
            params.add(CommonParams.SORT, sort);
        }
        return params;
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
            this.externalServicesAccess.getSolrConnection().add(data);
            this.externalServicesAccess.getSolrConnection().commit();
            this.externalServicesAccess.getTermCache().removeAll();
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
            this.externalServicesAccess.getSolrConnection().deleteByQuery("*:*");
            return 0;
        } catch (SolrServerException ex) {
            this.logger.error("SolrServerException while clearing the Solr index", ex);
        } catch (IOException ex) {
            this.logger.error("IOException while clearing the Solr index", ex);
        }
        return 1;
    }
}
