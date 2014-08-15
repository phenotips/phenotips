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
package org.phenotips.solr;

import org.phenotips.obo2solr.ParameterPreparer;
import org.phenotips.obo2solr.SolrUpdateGenerator;
import org.phenotips.obo2solr.TermData;

import org.xwiki.component.annotation.Component;

import java.io.IOException;
import java.util.Collection;
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
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;

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
            @SuppressWarnings("unchecked")
            List<String> parents = (List<String>) crt.get("is_a");
            if (parents == null) {
                continue;
            }
            for (String pid : parents) {
                nodes.add(this.get(StringUtils.substringBefore(pid, " ")));
            }
        }
        return results;
    }

    /**
     * Add an ontology to the index.
     *
     * @param ontologyUrl the address from where to get the ontology file
     * @param fieldList the list of ontology fields to index; comma separated list of field names with an optional boost
     *            separated by a color; for example: {@code id:50,name,def,synonym,is_a:0.1}; if the empty string is
     *            passed, then all fields from the ontology are indexed, using the default boost of 1.0
     * @return {@code 0} if the indexing succeeded, {@code 1} if writing to the Solr server failed, {@code 2} if the
     *         specified URL is invalid
     */
    public int index(String ontologyUrl, String fieldList)
    {
        ParameterPreparer paramPrep = new ParameterPreparer();
        SolrUpdateGenerator generator = new SolrUpdateGenerator();
        Map<String, Double> fieldSelection = paramPrep.getFieldSelection(fieldList);
        Map<String, TermData> data = generator.transform(ontologyUrl, fieldSelection);
        if (data == null) {
            return 2;
        }
        Collection<SolrInputDocument> allTerms = new HashSet<SolrInputDocument>();
        for (Map.Entry<String, TermData> item : data.entrySet()) {
            SolrInputDocument doc = new SolrInputDocument();
            for (Map.Entry<String, Collection<String>> property : item.getValue().entrySet()) {
                String name = property.getKey();
                for (String value : property.getValue()) {
                    doc.addField(name, value, (fieldSelection.get(name) == null ? ParameterPreparer.DEFAULT_BOOST
                        : fieldSelection.get(name)).floatValue());
                }
            }
            allTerms.add(doc);
        }
        try {
            this.server.add(allTerms);
            this.server.commit();
            this.cache.removeAll();
            return 0;
        } catch (SolrServerException ex) {
            this.logger.warn("Failed to index ontology: {}", ex.getMessage());
        } catch (IOException ex) {
            this.logger.warn("Failed to communicate with the Solr server while indexing ontology: {}", ex.getMessage());
        }
        return 1;
    }

    /**
     * Delete all the data in the Solr index.
     *
     * @return {@code 0} if the command was successful, {@code 1} otherwise
     */
    public int clear()
    {
        try {
            this.server.deleteByQuery("*:*");
            this.server.commit();
            this.cache.removeAll();
            return 0;
        } catch (SolrServerException ex) {
            this.logger.error("SolrServerException while clearing the Solr index", ex);
        } catch (IOException ex) {
            this.logger.error("IOException while clearing the Solr index", ex);
        }
        return 1;
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
