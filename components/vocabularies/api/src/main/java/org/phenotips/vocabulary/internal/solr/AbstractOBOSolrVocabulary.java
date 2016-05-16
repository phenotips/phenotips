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

import org.phenotips.obo2solr.ParameterPreparer;
import org.phenotips.obo2solr.SolrUpdateGenerator;
import org.phenotips.obo2solr.TermData;
import org.phenotips.vocabulary.VocabularyTerm;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;

/**
 * Ontologies processed from OBO files share much of the processing code.
 *
 * @version $Id$
 * @since 1.2M4 (under different names since 1.1)
 */
public abstract class AbstractOBOSolrVocabulary extends AbstractSolrVocabulary
{
    /**
     * The name of the Alternative ID field, used for older aliases of updated HPO terms.
     */
    protected static final String ALTERNATIVE_ID_FIELD_NAME = "alt_id";

    protected static final String VERSION_FIELD_NAME = "version";

    /** The number of documents to be added and committed to Solr at a time. */
    protected abstract int getSolrDocsPerBatch();

    @Override
    public VocabularyTerm getTerm(String id)
    {
        VocabularyTerm result = super.getTerm(id);
        if (result == null) {
            Map<String, String> queryParameters = new HashMap<>();
            queryParameters.put(ALTERNATIVE_ID_FIELD_NAME, id);
            List<VocabularyTerm> results = search(queryParameters);
            if (results != null && !results.isEmpty()) {
                result = search(queryParameters).iterator().next();
            }
        }
        return result;
    }

    @Override
    public int reindex(String sourceUrl)
    {
        this.clear();
        return this.index(sourceUrl);
    }

    /**
     * Add a vocabulary to the index.
     *
     * @param sourceUrl the address from where to get the vocabulary source file
     * @return {@code 0} if the indexing succeeded, {@code 1} if writing to the Solr server failed, {@code 2} if the
     *         specified URL is invalid
     */
    protected int index(String sourceUrl)
    {
        String realOntologyUrl = StringUtils.defaultIfBlank(sourceUrl, getDefaultSourceLocation());

        SolrUpdateGenerator generator = new SolrUpdateGenerator();
        Map<String, Double> fieldSelection = new HashMap<String, Double>();
        Map<String, TermData> data = generator.transform(realOntologyUrl, fieldSelection);
        if (data == null || data.isEmpty()) {
            return 2;
        }
        try {
            Collection<SolrInputDocument> termBatch = new HashSet<SolrInputDocument>();
            Iterator<Map.Entry<String, TermData>> dataIterator = data.entrySet().iterator();
            int batchCounter = 0;
            while (dataIterator.hasNext()) {
                /* Resetting when the batch fills */
                if (batchCounter == getSolrDocsPerBatch()) {
                    commitTerms(termBatch);
                    termBatch = new HashSet<>();
                    batchCounter = 0;
                }
                Map.Entry<String, TermData> item = dataIterator.next();
                SolrInputDocument doc = new SolrInputDocument();
                for (Map.Entry<String, Collection<String>> property : item.getValue().entrySet()) {
                    String name = property.getKey();
                    for (String value : property.getValue()) {
                        doc.addField(name, value, ParameterPreparer.DEFAULT_BOOST.floatValue());
                    }
                }
                termBatch.add(doc);
                batchCounter++;
            }
            commitTerms(termBatch);
            return 0;
        } catch (SolrServerException ex) {
            this.logger.warn("Failed to index ontology: {}", ex.getMessage());
        } catch (IOException ex) {
            this.logger.warn("Failed to communicate with the Solr server while indexing ontology: {}", ex.getMessage());
        } catch (OutOfMemoryError ex) {
            this.logger.warn("Failed to add terms to the Solr. Ran out of memory. {}", ex.getMessage());
        }
        return 1;
    }

    protected void commitTerms(Collection<SolrInputDocument> batch)
        throws SolrServerException, IOException, OutOfMemoryError
    {
        this.externalServicesAccess.getSolrConnection().add(batch);
        this.externalServicesAccess.getSolrConnection().commit();
        this.externalServicesAccess.getTermCache().removeAll();
    }

    /**
     * Delete all the data in the Solr index.
     *
     * @return {@code 0} if the command was successful, {@code 1} otherwise
     */
    protected int clear()
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
            response = this.externalServicesAccess.getSolrConnection().query(query);
            termList = response.getResults();

            if (!termList.isEmpty()) {
                firstDoc = termList.get(0);
                return firstDoc.getFieldValue(VERSION_FIELD_NAME).toString();
            }
        } catch (SolrServerException | SolrException | IOException ex) {
            this.logger.warn("Failed to query ontology version: {}", ex.getMessage());
        }
        return null;
    }
}
