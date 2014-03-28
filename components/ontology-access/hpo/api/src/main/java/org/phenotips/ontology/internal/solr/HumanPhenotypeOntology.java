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

import org.phenotips.obo2solr.ParameterPreparer;
import org.phenotips.obo2solr.SolrUpdateGenerator;
import org.phenotips.obo2solr.TermData;
import org.phenotips.ontology.OntologyTerm;

import org.xwiki.component.annotation.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

/**
 * Provides access to the Human Phenotype Ontology (HPO). The ontology prefix is {@code HP}.
 *
 * @version $Id$
 * @since 1.0M8
 */
@Component
@Named("hpo")
@Singleton
public class HumanPhenotypeOntology extends AbstractSolrOntologyService
{
    /**
     * The name of the Alternative ID field, used for older aliases of updated HPO terms.
     */
    protected static final String ALTERNATIVE_ID_FIELD_NAME = "alt_id";

    protected static final String VERSION_FIELD_NAME = "version";

    @Override
    protected String getName()
    {
        return "hpo";
    }

    @Override
    public String getDefaultOntologyLocation()
    {
        return "http://purl.obolibrary.org/obo/hp.obo";
    }

    @Override
    public OntologyTerm getTerm(String id)
    {
        OntologyTerm result = super.getTerm(id);
        if (result == null) {
            Map<String, String> queryParameters = new HashMap<String, String>();
            queryParameters.put(ALTERNATIVE_ID_FIELD_NAME, id);
            Set<OntologyTerm> results = search(queryParameters);
            if (results != null && !results.isEmpty()) {
                result = search(queryParameters).iterator().next();
            }
        }
        return result;
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

    @Override
    public int reindex(String ontologyUrl)
    {
        this.clear();
        return this.index(ontologyUrl);
    }

    /**
     * Add an ontology to the index.
     *
     * @param ontologyUrl the address from where to get the ontology file
     * @return {@code 0} if the indexing succeeded, {@code 1} if writing to the Solr server failed, {@code 2} if the
     *         specified URL is invalid
     */
    protected int index(String ontologyUrl)
    {
        String realOntologyUrl = StringUtils.defaultIfBlank(ontologyUrl, getDefaultOntologyLocation());

        SolrUpdateGenerator generator = new SolrUpdateGenerator();
        Map<String, Double> fieldSelection = new HashMap<String, Double>();
        Map<String, TermData> data = generator.transform(realOntologyUrl, fieldSelection);
        if (data == null) {
            return 2;
        }
        Collection<SolrInputDocument> allTerms = new HashSet<SolrInputDocument>();
        for (Map.Entry<String, TermData> item : data.entrySet()) {
            SolrInputDocument doc = new SolrInputDocument();
            for (Map.Entry<String, Collection<String>> property : item.getValue().entrySet()) {
                String name = property.getKey();
                for (String value : property.getValue()) {
                    doc.addField(name, value, ParameterPreparer.DEFAULT_BOOST.floatValue());
                }
            }
            allTerms.add(doc);
        }
        try {
            this.externalServicesAccess.getServer().add(allTerms);
            this.externalServicesAccess.getServer().commit();
            this.externalServicesAccess.getCache().removeAll();
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
    protected int clear()
    {
        try {
            this.externalServicesAccess.getServer().deleteByQuery("*:*");
            return 0;
        } catch (SolrServerException ex) {
            this.logger.error("SolrServerException while clearing the Solr index", ex);
        } catch (IOException ex) {
            this.logger.error("IOException while clearing the Solr index", ex);
        }
        return 1;
    }

    @Override
    public String getVersion()
    {
        QueryResponse response;
        SolrQuery query = new SolrQuery();
        SolrDocumentList termList;
        SolrDocument firstDoc;

        query.setQuery("version:*");
        query.set("rows", "1");
        try {
            response = this.externalServicesAccess.getServer().query(query);
            termList = response.getResults();

            if (!termList.isEmpty()) {
                firstDoc = termList.get(0);
                return firstDoc.getFieldValue(VERSION_FIELD_NAME).toString();
            }
        } catch (SolrServerException ex) {
            this.logger.warn("Failed to query ontology version {}", ex.getMessage());
        }
        return null;
    }
}
