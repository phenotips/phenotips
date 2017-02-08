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

import org.phenotips.vocabulary.VocabularyExtension;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.stability.Unstable;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;

/**
 * Base class for implementing vocabularies processed from CSV files.
 *
 * @since 1.2RC1
 * @version $Id$
 */
@Unstable
public abstract class AbstractCSVSolrVocabulary extends AbstractSolrVocabulary
{
    protected static final String VERSION_FIELD_NAME = "version";

    protected static final String SYMBOL_EXACT = "symbolExact^100";

    /**
     * The number of documents to be added and committed to Solr at a time.
     *
     * @return a positive integer, or a negative number to disable batching and pushing all terms in one go
     */
    protected abstract int getSolrDocsPerBatch();

    protected abstract Collection<SolrInputDocument> load(URL url);

    @Override
    public int reindex(String sourceUrl)
    {
        int retval = 1;
        try {
            for (VocabularyExtension ext : this.extensions.get()) {
                if (ext.isVocabularySupported(this)) {
                    ext.indexingStarted(this);
                }
            }
            this.clear();
            retval = this.index(sourceUrl);
        } finally {
            for (VocabularyExtension ext : this.extensions.get()) {
                if (ext.isVocabularySupported(this)) {
                    ext.indexingEnded(this);
                }
            }
        }
        return retval;
    }

    /**
     * Add a vocabulary to the index.
     *
     * @param sourceUrl the URL to be indexed
     * @return {@code 0} if the indexing succeeded, {@code 1} if writing to the Solr server failed, {@code 2} if the
     *         specified URL is invalid
     */
    protected int index(String sourceUrl)
    {
        Collection<SolrInputDocument> data = null;
        try {
            data = load(new URL(sourceUrl));
        } catch (MalformedURLException e) {
            return 2;
        }
        if (data == null) {
            return 2;
        }
        try {
            Collection<SolrInputDocument> termBatch = new HashSet<>();
            Iterator<SolrInputDocument> dataIterator = data.iterator();
            int batchCounter = 0;
            while (dataIterator.hasNext()) {
                /* Resetting when the batch fills */
                if (batchCounter == getSolrDocsPerBatch()) {
                    commitTerms(termBatch);
                    termBatch = new HashSet<>();
                    batchCounter = 0;
                }
                SolrInputDocument item = dataIterator.next();
                extendTerm(new SolrVocabularyInputTerm(item, this));
                termBatch.add(item);
                batchCounter++;
            }
            commitTerms(termBatch);
            return 0;
        } catch (SolrServerException ex) {
            this.logger.warn("Failed to index vocabulary: {}", ex.getMessage());
        } catch (IOException ex) {
            this.logger.warn("Failed to communicate with the Solr server while indexing vocabulary: {}",
                ex.getMessage());
        } catch (OutOfMemoryError ex) {
            this.logger.warn("Failed to add terms to the Solr. Ran out of memory. {}", ex.getMessage());
        }
        return 1;
    }

    protected void commitTerms(Collection<SolrInputDocument> batch)
        throws SolrServerException, IOException, OutOfMemoryError
    {
        this.externalServicesAccess.getSolrConnection(getCoreName()).add(batch);
        this.externalServicesAccess.getSolrConnection(getCoreName()).commit();
        this.externalServicesAccess.getTermCache(getCoreName()).removeAll();
    }

    /**
     * Delete all the data in the Solr index.
     *
     * @return {@code 0} if the command was successful, {@code 1} otherwise
     */
    protected int clear()
    {
        try {
            this.externalServicesAccess.getSolrConnection(getCoreName()).deleteByQuery("*:*");
            return 0;
        } catch (SolrServerException ex) {
            this.logger.error("SolrServerException while clearing the Solr index", ex);
        } catch (IOException ex) {
            this.logger.error("IOException while clearing the Solr index", ex);
        }
        return 1;
    }

    protected VocabularyTerm requestTerm(String queryString, String phraseFields)
    {
        QueryResponse response;
        SolrQuery query = new SolrQuery();
        SolrDocumentList termList;
        VocabularyTerm term;
        query.setQuery(queryString);
        query.setRows(1);
        if (phraseFields != null) {
            query.set(DisMaxParams.PF, phraseFields);
        }

        try {
            response = this.externalServicesAccess.getSolrConnection(getCoreName()).query(query);
            termList = response.getResults();

            if (!termList.isEmpty()) {
                term = new SolrVocabularyTerm(termList.get(0), this);
                return term;
            }
        } catch (SolrServerException | SolrException ex) {
            this.logger.warn("Failed to query vocabulary term: {} ", ex.getMessage());
        } catch (IOException ex) {
            this.logger.error("IOException while getting vocabulary term ", ex);
        }
        return null;
    }

    @Override
    public String getVersion()
    {
        QueryResponse response;
        SolrQuery query = new SolrQuery();
        SolrDocumentList termList;
        SolrDocument firstDoc;

        query.setQuery("version:*");
        query.set(CommonParams.ROWS, "1");
        try {
            response = this.externalServicesAccess.getSolrConnection(getCoreName()).query(query);
            termList = response.getResults();

            if (!termList.isEmpty()) {
                firstDoc = termList.get(0);
                return firstDoc.getFieldValue(VERSION_FIELD_NAME).toString();
            }
        } catch (SolrServerException | SolrException ex) {
            this.logger.warn("Failed to query vocabulary version: {}", ex.getMessage());
        } catch (IOException ex) {
            this.logger.error("IOException while getting vocabulary version", ex);
        }
        return null;
    }
}
