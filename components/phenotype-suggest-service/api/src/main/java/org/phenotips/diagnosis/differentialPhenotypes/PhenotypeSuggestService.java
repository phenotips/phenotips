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
package org.phenotips.diagnosis.differentialPhenotypes;

import org.phenotips.obo2solr.maps.CounterMap;
import org.phenotips.obo2solr.maps.SumMap;
import org.phenotips.vocabulary.SolrVocabularyResourceManager;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import org.apache.solr.common.util.SimpleOrderedMap;
import org.slf4j.Logger;

/**
 * Suggests phenotypes to investigate, based on an existing list of already selected positive and negative phenotypes.
 *
 * @version $Id$
 */
@Component
@Named("phenotypeSuggest")
@Singleton
public class PhenotypeSuggestService implements ScriptService
{
    /** Provides access to the HPO ontology, for converting IDs into names and for getting all term ancestors. */
    @Inject
    @Named("hpo")
    private Vocabulary hpo;

    /** Provides access to the OMIM ontology. */
    @Inject
    @Named("omim")
    private Vocabulary omim;

    @Inject
    private SolrVocabularyResourceManager solrManager;

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /**
     * Compute a list of phenotypes to investigate, which maximize the probability of getting more accurate automatic
     * diagnosis suggestions.
     *
     * @param phenotypes the list of already selected phenotypes
     * @param nphenotypes phenotypes that are not observed in the patient
     * @param limit the maximum number of phenotypes to return
     * @return a list of phenotype suggestions
     */
    public List<SuggestedPhenotype> getDifferentialPhenotypes(Collection<String> phenotypes,
        Collection<String> nphenotypes, int limit)
    {
        QueryResponse response;
        List<SuggestedPhenotype> result = new LinkedList<>();
        try {
            response = this.solrManager.getSolrConnection(this.omim).query(prepareParams(phenotypes, nphenotypes));
        } catch (SolrServerException | IOException ex) {
            this.logger.warn("Failed to query OMIM index: {}", ex.getMessage());
            return result;
        }
        SolrDocumentList matchingDisorders = response.getResults();
        Map<?, ?> explanations = response.getExplainMap();
        SumMap<String> cummulativeScore = new SumMap<>();
        CounterMap<String> matchCounter = new CounterMap<>();
        Set<String> allAncestors = new HashSet<>();
        for (String phenotype : phenotypes) {
            allAncestors.addAll(this.getAllAncestorsAndSelfIDs(phenotype));
        }
        for (SolrDocument disorder : matchingDisorders) {
            String omimId = (String) disorder.getFieldValue("id");
            @SuppressWarnings("unchecked")
            SimpleOrderedMap<Float> omimTerm = (SimpleOrderedMap<Float>) explanations.get(omimId);
            float score = omimTerm.get("value");
            for (Object hpoId : disorder.getFieldValues("actual_symptom")) {
                if (allAncestors.contains(hpoId) || nphenotypes.contains(hpoId)
                    || !this.getAllAncestorsAndSelfIDs((String) hpoId).contains("HP:0000118")) {
                    continue;
                }
                cummulativeScore.addTo((String) hpoId, (double) score);
                matchCounter.addTo((String) hpoId);
            }
        }
        if (matchCounter.getMinValue() <= matchingDisorders.size() / 2) {
            for (String hpoId : cummulativeScore.keySet()) {
                VocabularyTerm term = this.hpo.getTerm(hpoId);
                if (term == null) {
                    continue;
                }
                result.add(new SuggestedPhenotype(hpoId, (String) term.get("name"),
                    cummulativeScore.get(hpoId) / (matchCounter.get(hpoId) * matchCounter.get(hpoId))));
            }
            Collections.sort(result);
        }
        return result.subList(0, Math.min(limit, result.size()));
    }

    /**
     * Prepare the map of parameters that can be passed to a Solr query, in order to get a list of diseases matching the
     * selected positive and negative phenotypes.
     *
     * @param phenotypes the list of already selected phenotypes
     * @param nphenotypes phenotypes that are not observed in the patient
     * @return the computed Solr query parameters
     */
    private SolrQuery prepareParams(Collection<String> phenotypes, Collection<String> nphenotypes)
    {
        SolrQuery result = new SolrQuery();
        String q = "symptom:" + StringUtils.join(phenotypes, " symptom:");
        if (!nphenotypes.isEmpty()) {
            q += "  not_symptom:" + StringUtils.join(nphenotypes, " not_symptom:");
        }
        q += " -nameSort:\\** -nameSort:\\+* -nameSort:\\^*";
        result.set(CommonParams.Q, q.replaceAll("HP:", "HP\\\\:"));
        result.set(CommonParams.ROWS, "100");
        result.set(CommonParams.START, "0");
        result.set(CommonParams.DEBUG_QUERY, Boolean.toString(true));
        result.set(CommonParams.EXPLAIN_STRUCT, Boolean.toString(true));

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
        VocabularyTerm crt = this.hpo.getTerm(id);
        Set<VocabularyTerm> ancenstors = crt.getAncestorsAndSelf();
        for (VocabularyTerm term : ancenstors) {
            parents.add(term.getId());
        }
        return parents;
    }
}
