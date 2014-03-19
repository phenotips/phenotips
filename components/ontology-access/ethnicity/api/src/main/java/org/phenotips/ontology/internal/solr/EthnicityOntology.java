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

import org.phenotips.ontology.OntologyTerm;

import org.xwiki.component.annotation.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

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
public class EthnicityOntology extends AbstractSolrOntologyService
{
    /**
     * @param stringSearch part of full ethnicity name
     * @return set of strings that are full ethnicity names that match the partial string
     */
    public Set<OntologyTerm> getMatchingEthnicities(String stringSearch)
    {
        Map<String, String> searchMap = new HashMap<String, String>();
        searchMap.put("nameGram", stringSearch);
        // Order by population size:
        searchMap.put("_val_", "popsize");

        Map<String, String> optionsMap = new HashMap<String, String>();
        optionsMap.put(CommonParams.ROWS, "10");

        return search(searchMap, optionsMap);
    }

    @Override
    protected String getName()
    {
        return "ethnicity";
    }

    @Override
    public String getDefaultOntologyLocation()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getAliases()
    {
        Set<String> aliases = new HashSet<String>();
        aliases.add(getName());
        aliases.add("ETHNO");
        return aliases;
    }
}
