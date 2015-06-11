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

import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.params.SpellingParams;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

/**
 * Provides access to the Human Phenotype Ontology (HPO). The ontology prefix is {@code HP}.
 *
 * @version $Id$
 * @since 1.0M8
 */
@Component
@Named("hpo")
@Singleton
public class HumanPhenotypeOntology extends AbstractOBOSolrVocabulary
{
    /** For determining if a query is a an id. */
    private static final Pattern ID_PATTERN = Pattern.compile("^HP:[0-9]+$", Pattern.CASE_INSENSITIVE);

    private VocabularyTerm rootTerm;

    @Override
    protected String getName()
    {
        return "hpo";
    }

    @Override
    public String getDefaultSourceLocation()
    {
        return "http://purl.obolibrary.org/obo/hp.obo";
    }

    @Override
    protected int getSolrDocsPerBatch()
    {
        /* This number should be sufficient to index the whole ontology in one go */
        return 15000;
    }

    @Override
    public Set<String> getAliases()
    {
        Set<String> result = new HashSet<String>();
        result.add(getName());
        result.add("HP");
        result.add("HPO");
        return result;
    }

    private VocabularyTerm getRootTerm()
    {
        if (this.rootTerm == null) {
            this.rootTerm = this.getTerm("HP:0000001");
        }
        return rootTerm;
    }

    @Override
    public List<VocabularyTerm> search(String query, int rows, String sort, String customFq)
    {
        if (StringUtils.isBlank(query)) {
            return Collections.emptyList();
        }
        boolean isId = this.isId(query);
        Map<String, String> options = this.getStaticSolrParams();
        if (!isId) {
            options.putAll(this.getStaticFieldSolrParams());
        }
        List<VocabularyTerm> result = new LinkedList<>();
        for (SolrDocument doc : this.search(produceDynamicSolrParams(query, rows, sort, customFq, isId), options)) {
            result.add(new SolrVocabularyTerm(doc, this));
        }
        return result;
    }

    /**
     * Returns the same JSON as {@link VocabularyTerm#toJson()}, with the exception that the categories are ordered.
     * @param term whose JSON should be returned
     * @return JSON of the `term` with ordered categories
     */
    public JSON termToOrderedJson(VocabularyTerm term)
    {
        JSON rawJson = term.toJson();
        JSONObject json;
        if (!rawJson.isArray()) {
            json = (JSONObject) rawJson;
        } else {
            return rawJson;
        }

        Set<VocabularyTerm> categoriesSet = term.getAncestorsAndSelf();
        List<VocabularyTerm> sortedCategories = this.sortTermsByLevel(categoriesSet);
        List<String> sortedCategoriesIds = new LinkedList<>();
        for (VocabularyTerm category : sortedCategories) {
            sortedCategoriesIds.add(category.getId());
        }
        json.put("term_category", sortedCategoriesIds);
        return json;
    }

    private List<VocabularyTerm> sortTermsByLevel(Collection<VocabularyTerm> terms)
    {
        List<VocabularyTerm> sortedTerms = new LinkedList<>();
        final Map<VocabularyTerm, Long> levelMap = new HashMap<>();
        for (VocabularyTerm term : terms) {
            levelMap.put(term, term.getDistanceTo(this.getRootTerm()));
            sortedTerms.add(term);
        }
        sortedTerms.sort(new Comparator<VocabularyTerm>()
        {
            @Override
            public int compare(VocabularyTerm o1, VocabularyTerm o2)
            {
                if (levelMap.get(o1) > levelMap.get(o2)) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });
        return sortedTerms;
    }

    private Map<String, String> getStaticSolrParams()
    {
        String trueStr = "true";
        Map<String, String> params = new HashMap<>();
        params.put("spellcheck", trueStr);
        params.put(SpellingParams.SPELLCHECK_COLLATE, trueStr);
        params.put(SpellingParams.SPELLCHECK_COUNT, "100");
        params.put(SpellingParams.SPELLCHECK_MAX_COLLATION_TRIES, "3");
        params.put("lowercaseOperators", "false");
        params.put("defType", "edismax");
        return params;
    }

    private Map<String, String> getStaticFieldSolrParams()
    {
        Map<String, String> params = new HashMap<>();
        params.put("pf", "name^20 nameSpell^36 nameExact^100 namePrefix^30 "
            + "synonym^15 synonymSpell^25 synonymExact^70 synonymPrefix^20 "
            + "text^3 textSpell^5");
        params.put("qf",
            "name^10 nameSpell^18 nameStub^5 synonym^6 synonymSpell^10 synonymStub^3 text^1 textSpell^2 textStub^0.5");
        return params;
    }

    private SolrParams produceDynamicSolrParams(String originalQuery, Integer rows, String sort, String customFq,
        boolean isId)
    {
        String query = originalQuery.trim();
        ModifiableSolrParams params = new ModifiableSolrParams();
        String escapedQuery = ClientUtils.escapeQueryChars(query);
        if (isId) {
            params.add(CommonParams.FQ, StringUtils.defaultIfBlank(customFq,
                new MessageFormat("id:{0} alt_id:{0}").format(new String[] { escapedQuery })));
        } else {
            params.add(CommonParams.FQ, StringUtils.defaultIfBlank(customFq, "term_category:HP\\:0000118"));
        }
        params.add(CommonParams.Q, escapedQuery);
        params.add(SpellingParams.SPELLCHECK_Q, query);
        params.add(CommonParams.ROWS, rows.toString());
        if (StringUtils.isNotBlank(sort)) {
            params.add(CommonParams.SORT, sort);
        }
        return params;
    }

    private boolean isId(String query)
    {
        return ID_PATTERN.matcher(query).matches();
    }
}
