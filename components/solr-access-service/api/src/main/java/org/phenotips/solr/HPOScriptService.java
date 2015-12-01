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
package org.phenotips.solr;

import org.xwiki.component.annotation.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;

/**
 * Provides access to the Solr server, with the main purpose of providing access to the HPO ontology, and secondary
 * purposes of re-indexing the ontology and clearing the index completely. There are two ways of accessing the HPO
 * ontology: getting a single term by its identifier, or searching for terms matching a given query in the Lucene query
 * language.
 *
 * @version $Id$
 */
@Component
@Named("hpo")
@Singleton
public class HPOScriptService extends AbstractSolrScriptService
{
    /**
     * The name of the Alternative ID field, used for older aliases of updated HPO terms.
     */
    protected static final String ALTERNATIVE_ID_FIELD_NAME = "alt_id";

    /**
     * Get the HPO IDs of the specified phenotype and all its ancestors.
     *
     * @param id the HPO identifier to search for, in the {@code HP:1234567} format
     * @return the full set of ancestors-or-self IDs, or an empty set if the requested ID was not found in the index
     */
    @SuppressWarnings("unchecked")
    public Set<String> getAllAncestorsAndSelfIDs(final String id)
    {
        Set<String> results = new HashSet<String>();
        Queue<SolrDocument> nodes = new LinkedList<SolrDocument>();
        SolrDocument crt = this.get(id);
        if (crt == null) {
            return results;
        }
        nodes.add(crt);
        while (!nodes.isEmpty()) {
            crt = nodes.poll();
            results.add(String.valueOf(crt.get(ID_FIELD_NAME)));
            Object rawParents = crt.get("is_a");
            if (rawParents == null) {
                continue;
            }
            List<String> parents;
            if (rawParents instanceof String) {
                parents = Collections.singletonList(String.valueOf(rawParents));
            } else {
                parents = (List<String>) rawParents;
            }
            for (String pid : parents) {
                nodes.add(this.get(StringUtils.substringBefore(pid, " ")));
            }
        }
        return results;
    }

    @Override
    protected String getName()
    {
        return "hpo";
    }

    @Override
    public SolrDocument get(String id)
    {
        SolrDocument result = super.get(id);
        if (result == null) {
            Map<String, String> queryParameters = new HashMap<String, String>();
            queryParameters.put(ALTERNATIVE_ID_FIELD_NAME, id);
            result = get(queryParameters);
        }
        return result;
    }
}
