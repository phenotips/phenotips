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

import org.phenotips.data.Feature;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.indexing.PatientIndexer;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.util.ClientUtils;
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
    /** Character used in URLs to delimit path segments. */
    private static final String URL_PATH_SEPARATOR = "/";

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** The Solr server instance used. */
    private SolrServer server;

    /** Provides access to the configuration, where the Solr location is configured. */
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    /** Allows querying for patients. */
    @Inject
    private QueryManager qm;

    /** Provides access to patients. */
    @Inject
    private PatientRepository patientRepository;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.server = new HttpSolrServer(this.getSolrLocation() + "patients/");
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
            this.server.deleteByQuery("document:" + ClientUtils.escapeQueryChars(patient.getDocument().toString()));
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
        try {
            List<String> patientDocs =
                this.qm.createQuery("from doc.object(PhenoTips.PatientClass) as patient", Query.XWQL).execute();
            this.server.deleteByQuery("*:*");
            for (String patientDoc : patientDocs) {
                this.index(this.patientRepository.getPatientById(patientDoc));
            }
            this.server.commit();
        } catch (SolrServerException ex) {
            this.logger.warn("Failed to reindex patients: {}", ex.getMessage());
        } catch (IOException ex) {
            this.logger.warn("Error occurred while reindexing patients: {}", ex.getMessage());
        } catch (QueryException ex) {
            this.logger.warn("Failed to search patients for reindexing: {}", ex.getMessage());
        }
    }

    /**
     * Get the URL where the Solr server can be reached, without any core name.
     * 
     * @return an URL as a String
     */
    protected String getSolrLocation()
    {
        String wikiSolrUrl = this.configuration.getProperty("solr.remote.url", String.class);
        if (StringUtils.isBlank(wikiSolrUrl)) {
            return "http://localhost:8080/solr/";
        }
        return StringUtils.substringBeforeLast(StringUtils.removeEnd(wikiSolrUrl, URL_PATH_SEPARATOR),
            URL_PATH_SEPARATOR) + URL_PATH_SEPARATOR;
    }
}
