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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

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
 * Provides access to searching ethnicity Solr index. Currently the implementation is very basic, but will be extended
 * upon the index becoming a full-fledged ontology.
 *
 * @version $Id$
 * @since 1.0M11
 */
@Component
@Named("ethnicity")
@Singleton
public class EthnicityOntology extends AbstractSolrVocabulary
{
    /** For determining if a query is a an id. */
    private static final Pattern ID_PATTERN = Pattern.compile("^ETHNO:\\w+$", Pattern.CASE_INSENSITIVE);

    /**
     * @param input part of full ethnicity name
     * @return set of strings that are full ethnicity names that match the partial string
     * @deprecated since 1.2M4 use {@link #search(String, int, String, String)} instead
     */
    @Deprecated
    public List<VocabularyTerm> getMatchingEthnicities(String input)
    {
        return search(input, 10, null, null);
    }

    @Override
    public List<VocabularyTerm> search(String input, int maxResults, String sort, String customFilter)
    {
        if (StringUtils.isBlank(input)) {
            return Collections.emptyList();
        }
        boolean isId = this.isId(input);
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

    @Override
    protected String getCoreName()
    {
        return getIdentifier();
    }

    @Override
    public String getIdentifier()
    {
        return "ethnicity";
    }

    @Override
    public String getName()
    {
        return "Ethnicities (non-standard)";
    }

    @Override
    public String getDefaultSourceLocation()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getAliases()
    {
        Set<String> aliases = new HashSet<>();
        aliases.add(getIdentifier());
        aliases.add("ETHNO");
        return aliases;
    }

    @Override
    public String getWebsite()
    {
        return "";
    }

    @Override
    public String getCitation()
    {
        return "";
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
        query.set(DisMaxParams.PF, "name^20 nameSpell^36 nameStub^10");
        query.set(DisMaxParams.QF, "name^10 nameSpell^18 nameStub^5");
        query.set(DisMaxParams.BF, "log(popsize)^10");
        return query;
    }

    private SolrQuery addDynamicQueryParameters(String originalQuery, Integer rows, String sort, String customFq,
        boolean isId, SolrQuery query)
    {
        String queryString = originalQuery.trim();
        String escapedQuery = ClientUtils.escapeQueryChars(queryString);
        if (isId) {
            query.setFilterQueries(new MessageFormat("id:{0}").format(new String[] { escapedQuery }));
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
