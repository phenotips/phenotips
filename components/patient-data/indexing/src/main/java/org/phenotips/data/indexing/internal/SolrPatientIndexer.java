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
import org.phenotips.data.Gene;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.indexing.PatientIndexer;
import org.phenotips.data.permissions.EntityPermissionsManager;
import org.phenotips.vocabulary.SolrCoreContainerHandler;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
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
    private static final String GENES_KEY = "genes";

    private static final String SOLR_FIELD_CANDIDATE_GENES = "candidate_genes";

    private static final String SOLR_GENE_STATUS_FIELD_POSTFIX = "_genes";

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
    private EntityPermissionsManager permissions;

    /** Provides access to the HPO ontology. */
    @Inject
    @Named("hpo")
    private Vocabulary ontologyService;

    @Inject
    private EntityReferenceSerializer<String> referenceSerializer;

    @Override
    public void initialize() throws InitializationException
    {
        this.server = new EmbeddedSolrServer(this.cores.getContainer(), "patients");
    }

    @Override
    public void index(Patient patient)
    {
        this.internalIndex(patient, true);
    }

    private void internalIndex(Patient patient, boolean commit)
    {
        SolrInputDocument input = new SolrInputDocument();
        input.setField("document", this.referenceSerializer.serialize(patient.getDocumentReference()));
        String reporter = "";
        if (patient.getReporter() != null) {
            reporter = patient.getReporter().toString();
        }
        input.setField("reporter", reporter);

        // Index direct phenotypes and extended ancestor sets
        for (Feature phenotype : patient.getFeatures()) {
            String presence = (phenotype.isPresent() ? "" : "negative_");
            String fieldName = presence + phenotype.getType();
            String ancestorFieldName = "extended_" + presence + "phenotype";

            String termId = phenotype.getId();
            if (StringUtils.isNotBlank(termId)) {
                input.addField(fieldName, termId);
                // Add ancestors of the term
                VocabularyTerm term = this.ontologyService.getTerm(termId);
                if (term != null) {
                    for (VocabularyTerm ancestor : term.getAncestorsAndSelf()) {
                        input.addField(ancestorFieldName, ancestor.getId());
                    }
                }
            }
        }

        input.setField("visibility", this.permissions.getEntityAccess(patient).getVisibility().getName());
        input.setField("accessLevel", this.permissions.getEntityAccess(patient).getVisibility().getPermissiveness());

        addGenes(input, patient);

        try {
            this.server.add(input);
            if (commit) {
                this.server.commit();
            }
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
            this.server.deleteByQuery("document:"
                + ClientUtils.escapeQueryChars(this.referenceSerializer.serialize(patient.getDocumentReference())));
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
                this.internalIndex(this.patientRepository.get(patientDoc), false);
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

    private void addGenes(SolrInputDocument input, Patient patient)
    {
        PatientData<Gene> data = patient.getData(GENES_KEY);
        if (data == null) {
            return;
        }

        if (data == null || data.size() == 0) {
            return;
        }
        for (Gene gene : data) {
            String name = gene.getId();
            if (StringUtils.isBlank(name)) {
                continue;
            }

            String status = gene.getStatus();
            String field = null;
            // Index genes with empty or null status as candidates
            if (StringUtils.isBlank(status)) {
                field = SOLR_FIELD_CANDIDATE_GENES;
            } else {
                field = status + SOLR_GENE_STATUS_FIELD_POSTFIX;
            }

            input.addField(field, name);
        }
    }
}
