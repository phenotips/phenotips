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

import org.phenotips.obo2solr.maps.CounterMap;
import org.phenotips.obo2solr.maps.SumMap;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.util.SimpleOrderedMap;

/**
 * Provides access to the Solr server, with the main purpose of providing access to the OMIM ontology.
 * 
 * @version $Id$
 */
@Component
@Named("omim")
@Singleton
public class OmimScriptService extends AbstractSolrScriptService
{
    /** Provides access to the HPO ontology, for converting IDs into names and for getting all term ancestors. */
    @Inject
    @Named("hpo")
    private ScriptService service;

    @Override
    protected String getName()
    {
        return "omim";
    }

    /**
     * Compute a list of phenotypes to investigate, which maximize the probability of getting more accurate automatic
     * diagnosis suggestions.
     *
     * @param phenotypes the list of already selected phenotypes
     * @param limit the maximum number of phenotypes to return
     * @return a list of phenotype suggestions
     * @deprecated use {@link #getDifferentialPhenotypes(Collection, Collection, int)} which also has support for
     *             negative phenotypes
     */
    @Deprecated
    public List<SuggestedPhenotype> getDifferentialPhenotypes(Collection<String> phenotypes, int limit)
    {
        return getDifferentialPhenotypes(phenotypes, Collections.<String> emptyList(), limit);
    }

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
        HPOScriptService hpoService = (HPOScriptService) this.service;
        QueryResponse response;
        List<SuggestedPhenotype> result = new LinkedList<SuggestedPhenotype>();
        try {
            response = this.server.query(prepareParams(phenotypes, nphenotypes));
        } catch (SolrServerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return result;
        }
        SolrDocumentList matchingDisorders = response.getResults();
        Map<?, ?> explanations = response.getExplainMap();
        SumMap<String> cummulativeScore = new SumMap<String>();
        CounterMap<String> matchCounter = new CounterMap<String>();
        Set<String> allAncestors = new HashSet<String>();
        for (String phenotype : phenotypes) {
            allAncestors.addAll(hpoService.getAllAncestorsAndSelfIDs(phenotype));
        }
        for (SolrDocument disorder : matchingDisorders) {
            String omimId = (String) disorder.getFieldValue(ID_FIELD_NAME);
            @SuppressWarnings("unchecked")
            SimpleOrderedMap<Float> omimTerm = (SimpleOrderedMap<Float>) explanations.get(omimId);
            float score = omimTerm.get("value");
            for (Object hpoId : disorder.getFieldValues("actual_symptom")) {
                if (allAncestors.contains(hpoId) || nphenotypes.contains(hpoId)) {
                    continue;
                }
                cummulativeScore.addTo((String) hpoId, (double) score);
                matchCounter.addTo((String) hpoId);
            }
        }
        if (matchCounter.getMinValue() <= matchingDisorders.size() / 2) {
            for (String hpoId : cummulativeScore.keySet()) {
                SolrDocument term = hpoService.get(hpoId);
                if (term == null) {
                    continue;
                }
                result.add(new SuggestedPhenotype(hpoId, (String) term.getFieldValue("name"),
                    cummulativeScore.get(hpoId) / (matchCounter.get(hpoId) * matchCounter.get(hpoId))));
            }
            Collections.sort(result);
        }
        return result.subList(0, limit);
    }

    /**
     * Prepare the map of parameters that can be passed to a Solr query, in order to get a list of diseases matching the
     * selected positive and negative phenotypes.
     *
     * @param phenotypes the list of already selected phenotypes
     * @param nphenotypes phenotypes that are not observed in the patient
     * @return the computed Solr query parameters
     */
    private MapSolrParams prepareParams(Collection<String> phenotypes, Collection<String> nphenotypes)
    {
        Map<String, String> params = new HashMap<String, String>();
        String q = "symptom:" + StringUtils.join(phenotypes, " symptom:");
        if (nphenotypes.size() > 0) {
            q += "  not_symptom:" + StringUtils.join(nphenotypes, " not_symptom:");
        }
        q += " -nameSort:\\** -nameSort:\\+* -nameSort:\\^*";
        params.put(CommonParams.Q, q.replaceAll("HP:", "HP\\\\:"));
        params.put(CommonParams.ROWS, "100");
        params.put(CommonParams.START, "0");
        params.put(CommonParams.DEBUG_QUERY, Boolean.toString(true));
        params.put(CommonParams.EXPLAIN_STRUCT, Boolean.toString(true));

        return new MapSolrParams(params);
    }
}
