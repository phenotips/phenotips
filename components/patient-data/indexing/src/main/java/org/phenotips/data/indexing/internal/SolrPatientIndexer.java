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
package org.phenotips.data.indexing.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.Feature;
import org.phenotips.data.indexing.PatientIndexer;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;

/**
 * Indexes patients in a local Solr core.
 * 
 * @version $Id$
 * @since 1.0M8
 */
@Component
@Singleton
public class SolrPatientIndexer implements PatientIndexer, Initializable
{
    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** The Solr server instance used. */
    private SolrServer server;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.server = new HttpSolrServer("http://localhost:8080/solr/patients/");
        } catch (RuntimeException ex) {
            throw new InitializationException("Invalid URL specified for the Solr server: {}");
        }
    }

    @Override
    public void index(Patient patient)
    {
        SolrInputDocument input = new SolrInputDocument();
        input.setField("document", patient.getDocument().toString());
        String reporter = "";
        if (patient.getReporter() != null) {
            reporter = patient.getReporter().toString();
        }
        input.setField("reporter", reporter);

        for (Feature phenotype : patient.getFeatures()) {
            input.addField((phenotype.isPresent() ? "" : "negative_") + phenotype.getType(), phenotype.getId());
        }
        try {
            this.server.add(input);
        } catch (SolrServerException ex) {
            this.logger.warn("Failed to perform Solr search: {}", ex.getMessage());
        } catch (IOException ex) {
            this.logger.warn("Error occurred while performing Solr search: {}", ex.getMessage());
        }
    }

    @Override
    public void delete(Patient patient)
    {
        try {
            this.server.deleteByQuery("document:" + patient.getDocument());
            this.server.commit();
        } catch (SolrServerException ex) {
            this.logger.warn("Failed to delete from Solr: {}", ex.getMessage());
        } catch (IOException ex) {
            this.logger.warn("Error occurred while deleting Solr documents: {}", ex.getMessage());
        }
    }

    @Override
    public void reindex()
    {
        // FIXME Not implemented yet
        throw new UnsupportedOperationException();
    }
}
