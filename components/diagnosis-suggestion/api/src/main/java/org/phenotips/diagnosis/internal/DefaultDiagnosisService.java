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
package org.phenotips.diagnosis.internal;

import org.phenotips.diagnosis.DiagnosisService;
import org.phenotips.vocabulary.SolrVocabularyResourceManager;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.filter.annotation.Name;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.slf4j.Logger;

/**
 * An implementation of {@link DiagnosisService} using the builtin vocabularies
 *
 * @since 1.5M1
 * @version $Id$
 */
@Singleton
@Component
public class DefaultDiagnosisService implements DiagnosisService
{
    @Inject
    private Logger logger;

    @Inject
    private VocabularyManager vocabulary;

    /** Direct access to the omim vocabulary */
    @Inject
    @Named("omim")
    private Vocabulary omim;

    @Inject
    private SolrVocabularyResourceManager solrManager;

    @Override
    public List<VocabularyTerm> getDiagnosis(List<String> phenotypes, List<String> nonstandardPhenotypes, int limit)
    {
        // TODO: use the `nonstandardPhenotypes` argument
        // TODO: switch to search across vocabulary.getVocabularies("diagnosis")

        List<VocabularyTerm> result = new LinkedList<>();

        Set<String> allAncestors = new HashSet<>();
        for (String phenotype : phenotypes) {
            allAncestors.addAll(this.getAllAncestorsAndSelfIDs(phenotype));
        }
        if (allAncestors.isEmpty()) {
            return result;
        }

        QueryResponse response;
        try {
            response = this.solrManager.getSolrConnection(this.omim).query(prepareParams(allAncestors,
                Collections.emptyList(), limit));
        } catch (SolrServerException | IOException ex) {
            this.logger.warn("Failed to query OMIM index: {}", ex.getMessage());
            return result;
        }

        SolrDocumentList matchingDisorders = response.getResults();
        for (SolrDocument doc : matchingDisorders) {
            String termId = (String) doc.getFieldValue("id");
            VocabularyTerm term = this.omim.getTerm(termId);
            if (term == null) {
                continue;
            }
            result.add(term);
        }
        return result;
    }

    /**
     * Prepare the map of parameters that can be passed to a Solr query, in order to get a list of diseases matching the
     * selected positive and negative phenotypes.
     *
     * @param phenotypes the list of already selected phenotypes
     * @param nphenotypes phenotypes that are not observed in the patient
     * @param limit the maximum number of results to return
     * @return the computed Solr query parameters
     */
    private SolrQuery prepareParams(Collection<String> phenotypes, Collection<String> nphenotypes, int limit)
    {
        SolrQuery result = new SolrQuery();
        String q = "symptom:" + StringUtils.join(phenotypes, " symptom:");
        if (!nphenotypes.isEmpty()) {
            q += "  not_symptom:" + StringUtils.join(nphenotypes, " not_symptom:");
        }
        q += " -nameSort:\\** -nameSort:\\+* -nameSort:\\^*";
        result.set(CommonParams.Q, q.replaceAll("HP:", "HP\\\\:"));
        result.set(CommonParams.ROWS, limit);
        result.set(CommonParams.START, "0");

        return result;
    }

    /**
     * Get the HPO IDs of the specified phenotype and all its ancestors.
     *
     * @param id the HPO identifier to search for, in the {@code HP:1234567} format
     * @return the full set of ancestors-or-self IDs, or an empty set if the requested ID was not found in the index
     */
    public Set<String> getAllAncestorsAndSelfIDs(final String id)
    {
        Set<String> parents = new HashSet<>();
        VocabularyTerm crt = this.vocabulary.resolveTerm(id);
        Set<VocabularyTerm> ancestors = crt.getAncestorsAndSelf();
        for (VocabularyTerm term : ancestors) {
            parents.add(term.getId());
        }
        return parents;
    }
}
