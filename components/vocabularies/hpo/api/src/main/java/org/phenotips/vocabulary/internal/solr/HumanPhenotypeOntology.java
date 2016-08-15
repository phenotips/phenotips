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
import org.xwiki.localization.LocalizationContext;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.params.SpellingParams;

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

    /**
     * The localization context to determine the current locale.
     */
    @Inject
    private LocalizationContext localizationContext;

    @Override
    protected String getCoreName()
    {
        return getIdentifier();
    }

    @Override
    public String getDefaultSourceLocation()
    {
        return "https://compbio.charite.de/jenkins/job/hpo/lastStableBuild/artifact/hp/hp.obo";
    }

    @Override
    protected int getSolrDocsPerBatch()
    {
        /* This number should be sufficient to index the whole ontology in one go */
        return 15000;
    }

    @Override
    public String getIdentifier()
    {
        return "hpo";
    }

    @Override
    public String getName()
    {
        return "The Human Phenotype Ontology (HPO)";
    }

    @Override
    public Set<String> getAliases()
    {
        Set<String> result = new HashSet<String>();
        result.add(getIdentifier());
        result.add("HP");
        result.add("HPO");
        return result;
    }

    @Override
    public List<VocabularyTerm> search(String input, int maxResults, String sort, String customFilter)
    {
        if (StringUtils.isBlank(input)) {
            return Collections.emptyList();
        }
        boolean isId = this.isId(input);
        Map<String, String> options = this.getStaticSolrParams();
        if (!isId) {
            options.putAll(this.getStaticFieldSolrParams());
        }
        List<VocabularyTerm> result = new LinkedList<>();
        for (SolrDocument doc : this.search(produceDynamicSolrParams(input, maxResults, sort, customFilter, isId),
            options)) {
            result.add(new SolrVocabularyTerm(doc, this));
        }
        return result;
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
        /* We need to make sure everything degrades gracefully, so if no locale or
         * language is set to english anyway, just use the old fields */
        String lang = null;
        Locale currentLocale = localizationContext.getCurrentLocale();
        if (currentLocale != null) {
            lang = localizationContext.getCurrentLocale().getLanguage();
            if ("en".equals(lang)) {
                lang = null;
            } else if (lang != null) {
                lang = "_" + lang;
            }
        }
        Map<String, String> params = new HashMap<>();
        Formatter f = new Formatter();
        f.format("name^20 nameSpell^36 nameExact^100 namePrefix^30 ");
        f.format("synonym^15 synonymSpell^25 synonymExact^70 ");
        f.format("synonymPrefix^20 text^3 textSpell^5 ");
        if (lang != null) {
            f.format("name%1$s^60 nameSpell%1$s^108 nameExact%1$s^300 namePrefix%1$s^90 ", lang);
            f.format("synonym%1$s^45 synonymSpell%1$s^75 synonymExact%1$s^210 ", lang);
            f.format("synonymPrefix%1$s^60 text%1$s^9 textSpell%1$s^15 ", lang);
        }
        params.put(DisMaxParams.PF, f.toString());
        f = new Formatter();
        f.format("name^10 nameSpell^18 nameStub^5 synonym^6 ");
        f.format("synonymSpell^10 synonymStub^3 text^1 textSpell^2 ");
        f.format("textStub^0.5 ");
        if (lang != null) {
            f.format("name%1$s^30 nameSpell%1$s^54 nameStub%1$s^15 synonym%1$s^18 ", lang);
            f.format("synonymSpell%1$s^30 synonymStub%1$s^9 text%1$s^3 textSpell%1$s^6 ", lang);
            f.format("textStub%1$s^1.5 ", lang);
        }
        params.put(DisMaxParams.QF, f.toString());
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
