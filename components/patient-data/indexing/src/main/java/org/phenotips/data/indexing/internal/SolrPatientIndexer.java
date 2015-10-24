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
package org.phenotips.data.indexing.internal;

import org.phenotips.data.Feature;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.indexing.PatientIndexer;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.vocabulary.SolrCoreContainerHandler;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
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
    /** Logging helper object. */
    @Inject
    private Logger logger;

    @Inject
    private SolrCoreContainerHandler cores;

    /** The Solr server instance used. */
    private SolrClient server;

    /** Allows querying for patients. */
    @Inject
    private QueryManager qm;

    /** Provides access to patients. */
    @Inject
    private PatientRepository patientRepository;

    @Inject
    private PermissionsManager permissions;

    @Inject
    @Named("hpo")
    private Vocabulary ontologyService;

    @Override
    public void initialize() throws InitializationException
    {
        this.server = new EmbeddedSolrServer(this.cores.getContainer(), "patients");
    }

    @Override
    public void index(Patient patient)
    {
        SolrInputDocument input = new SolrInputDocument();
        Set<String> allAncestors = new HashSet<String>();
        input.setField("document", patient.getDocument().toString());
        String reporter = "";
        if (patient.getReporter() != null) {
            reporter = patient.getReporter().toString();
        }
        input.setField("reporter", reporter);

        for (Feature phenotype : patient.getFeatures()) {
            input.addField((phenotype.isPresent() ? "" : "negative_") + phenotype.getType(), phenotype.getId());
            VocabularyTerm properTerm = this.ontologyService.getTerm(phenotype.getId());
            if (properTerm != null) {
                Set<VocabularyTerm> parents = properTerm.getAncestorsAndSelf();
                for (VocabularyTerm term : parents) {
                    allAncestors.add(term.getId());
                }
            }
        }

        Set<String> solved = new HashSet<String>();
        Set<String> candidate = new HashSet<String>();
        Set<String> rejected = new HashSet<String>();
        PatientData<Map<String, String>> allGenes = patient.getData("genes");
        setGenes(allGenes, solved, candidate, rejected);

        input.setField("visibility", this.permissions.getPatientAccess(patient).getVisibility().getName());
        input.setField("accessLevel", this.permissions.getPatientAccess(patient).getVisibility().getPermissiveness());
        input.setField("phenotype_ancestors", allAncestors);
        input.setField("solved_genes", solved);
        input.setField("candidate_genes", candidate);
        input.setField("rejected_genes", rejected);
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

    private void setGenes(PatientData<Map<String, String>> allGenes, Set<String> solved, Set<String> candidate,
        Set<String> rejected)
    {
        if (allGenes != null && allGenes.isIndexed()) {
            for (Map<String, String> gene : allGenes) {
                String name = gene.get("gene");
                String status = gene.get("status");
                switch (status) {
                    case "solved":
                        solved.add(name);
                        break;
                    case "candidate":
                        candidate.add(name);
                        break;
                    case "rejected":
                        rejected.add(name);
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
