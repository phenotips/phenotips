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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.SpellingParams;

/**
 * Provides access to the Human Phenotype Ontology (HPO). The vocabulary prefix is {@code HP}.
 *
 * @version $Id$
 * @since 1.0M8
 */
@Component
@Named("hpo")
@Singleton
public class HumanPhenotypeOntology extends AbstractOBOSolrVocabulary
{
    private static final String PHENOTYPE = "phenotype";

    private static final String PHENOTYPE_QUALIFIER = "phenotype-qualifier";

    /** The list of supported categories for this vocabulary. */
    private static final Collection<String> SUPPORTED_CATEGORIES = Arrays.asList(PHENOTYPE, PHENOTYPE_QUALIFIER);

    /** For determining if a query is a an id. */
    private static final Pattern ID_PATTERN = Pattern.compile("^HP:[0-9]+$", Pattern.CASE_INSENSITIVE);

    /** The default filter for phenotype vocabulary searches. */
    private static final String DEFAULT_PHENOTYPE_FILTER = "term_category:HP\\:0000118";

    /** The default filter for phenotype qualifier vocabulary searches. */
    private static final String DEFAULT_QUALIFIER_FILTER = "term_category:HP\\:0012823";

    @Override
    protected String getCoreName()
    {
        return getIdentifier();
    }

    @Override
    public String getDefaultSourceLocation()
    {
        return "https://raw.githubusercontent.com/obophenotype/human-phenotype-ontology/master/hp.obo";
    }

    @Override
    protected int getSolrDocsPerBatch()
    {
        /* This number should be sufficient to index the whole vocabulary in one go */
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
    public Collection<String> getSupportedCategories()
    {
        return Collections.unmodifiableCollection(SUPPORTED_CATEGORIES);
    }

    @Override
    public Set<String> getAliases()
    {
        Set<String> result = new HashSet<>();
        result.add(getIdentifier());
        result.add("HP");
        result.add("HPO");
        return result;
    }

    @Override
    public String getWebsite()
    {
        return "http://human-phenotype-ontology.github.io/";
    }

    @Override
    public String getCitation()
    {
        return "The Human Phenotype Ontology project: linking molecular biology and disease through phenotype data."
            + " Sebastian K\u00f6hler, Sandra C Doelken, Christopher J. Mungall, Sebastian Bauer, Helen V. Firth,"
            + " et al. Nucl. Acids Res. (1 January 2014) 42 (D1): D966-D974 doi:10.1093/nar/gkt1026";
    }

    @Override
    public List<VocabularyTerm> search(String input, int maxResults, String sort, String customFilter)
    {
        if (StringUtils.isBlank(input)) {
            return Collections.emptyList();
        }
        boolean isId = this.isId(input);
        return search(input, maxResults, sort, customFilter, isId);
    }

    @Override
    public List<VocabularyTerm> search(String input, String category, int maxResults, String sort, String customFilter)
    {
        if (StringUtils.isBlank(input)) {
            return Collections.emptyList();
        }
        // If the wrong category was provided for the vocabulary, want to provide an appropriate log message.
        if (!getSupportedCategories().contains(category)) {
            this.logger.warn("The provided category [{}] is not supported by the HPO vocabulary.", category);
            return Collections.emptyList();
        }

        final boolean isId = isId(input);
        final String filter = StringUtils.defaultIfBlank(customFilter, generateDefaultFilter(category, isId));
        return search(input, maxResults, sort, filter, isId);
    }

    /**
     * Suggest the terms that best match the user's input, in a specific sub-category of this vocabulary.
     *
     * @param input the text that the user entered
     * @param maxResults the maximum number of terms to be returned
     * @param sort an optional sort parameter, in a format that depends on the actual engine that stores the vocabulary;
     *            usually a property name followed by {@code asc} or {@code desc}; may be {@code null}
     * @param customFilter an optional custom filter query to further restrict which terms may be returned, in a format
     *            that depends on the actual engine that stores the vocabulary; some vocabularies may not support a
     *            filter query; may be {@code null}
     * @param isId specifies if the {@code input} is an HPO ID
     * @return a list of suggestions, possibly empty.
     * @since 1.1-rc-1
     */
    private List<VocabularyTerm> search(@Nonnull final String input, final int maxResults, final String sort,
        final String customFilter, final boolean isId)
    {
        SolrQuery query = new SolrQuery();
        this.addGlobalQueryParameters(query);
        if (!isId) {
            this.addFieldQueryParameters(query);
        }
        List<VocabularyTerm> result = new LinkedList<>();
        for (SolrDocument doc : this.search(addDynamicQueryParameters(input, maxResults, sort, customFilter, isId,
            query))) {
            result.add(new SolrVocabularyTerm(doc, this));
        }
        return result;
    }

    /**
     * Generates the default filter for the HPO vocabulary given the {@code category vocabulary category} and a boolean
     * {@code isId} specifying if the {@code input} is an ID.
     *
     * @param category the valid vocabulary category
     * @param isId will be true if input is an ID, false otherwise
     * @return the default filter for the query
     */
    private String generateDefaultFilter(@Nonnull final String category, final boolean isId)
    {
        return isId
            ? null
            : PHENOTYPE.equals(category) ? DEFAULT_PHENOTYPE_FILTER : DEFAULT_QUALIFIER_FILTER;
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
        query.set(DisMaxParams.PF, "name^20 nameSpell^36 nameExact^100 namePrefix^30 "
            + "synonym^15 synonymSpell^25 synonymExact^70 synonymPrefix^20 "
            + "text^3 textSpell^5");
        query.set(DisMaxParams.QF,
            "name^10 nameSpell^18 nameStub^5 synonym^6 synonymSpell^10 synonymStub^3 text^1 textSpell^2 textStub^0.5");
        return query;
    }

    private SolrQuery addDynamicQueryParameters(String originalQuery, Integer rows, String sort, String customFq,
        boolean isId, SolrQuery query)
    {
        String queryString = originalQuery.trim();
        String escapedQuery = ClientUtils.escapeQueryChars(queryString);
        if (isId) {
            query.setFilterQueries(StringUtils.defaultIfBlank(customFq,
                new MessageFormat("id:{0} alt_id:{0}").format(new String[] { escapedQuery })));
        } else if (StringUtils.isNotBlank(customFq)) {
            query.setFilterQueries(customFq);
        }
        query.setQuery(escapedQuery);
        query.set(SpellingParams.SPELLCHECK_Q, queryString);
        query.setRows(rows);
        if (StringUtils.isNotBlank(sort)) {
            for (String sortItem : sort.split("\\s*,\\s*")) {
                query.addSort(StringUtils.substringBefore(sortItem, " "),
                    sortItem.endsWith(" desc") || sortItem.startsWith("-") ? ORDER.desc : ORDER.asc);
            }
        }
        return query;
    }

    private boolean isId(String query)
    {
        return ID_PATTERN.matcher(query).matches();
    }
}
