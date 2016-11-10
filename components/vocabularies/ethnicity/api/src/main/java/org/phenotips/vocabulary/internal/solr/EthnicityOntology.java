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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.params.CommonParams;

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
        Map<String, String> searchMap = new HashMap<>();
        Map<String, String> optionsMap = new HashMap<>();
        searchMap.put("nameGram", input);

        // Order by population size:
        if (StringUtils.isBlank(sort)) {
            searchMap.put("_val_", "popsize");
        } else {
            optionsMap.put(CommonParams.SORT, sort);
        }

        optionsMap.put(CommonParams.ROWS, Integer.toString(maxResults));

        return search(searchMap, optionsMap);
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
}
