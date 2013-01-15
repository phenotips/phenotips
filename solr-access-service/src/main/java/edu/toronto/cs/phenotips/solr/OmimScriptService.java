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
package edu.toronto.cs.phenotips.solr;

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
import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

import edu.toronto.cs.phenotips.obo2solr.maps.CounterMap;
import edu.toronto.cs.phenotips.obo2solr.maps.SumMap;

/**
 * Provides access to the Solr server, with the main purpose of providing access to the HPO ontology, and secondary
 * purposes of re-indexing the ontology and clearing the index completely. There are two ways of accessing the HPO
 * ontology: getting a single term by its identifier, or searching for terms matching a given query in the Lucene query
 * language.
 * 
 * @version $Id$
 */
@Component
@Named("omim")
@Singleton
public class OmimScriptService extends AbstractSolrScriptService
{
    @Inject
    @Named("hpo")
    private ScriptService service;

    @Override
    protected String getName()
    {
        return "omim";
    }

    public List<SearchResult> getDifferentialPhenotypes(Collection<String> phenotypes, int limit)
    {
        Map<String, String> params = new HashMap<String, String>();
        HPOScriptService hpoService = (HPOScriptService) this.service;
        params.put(CommonParams.Q, "symptom:" + StringUtils.join(phenotypes, " symptom:").replaceAll("HP:", "HP\\\\:"));
        params.put(CommonParams.ROWS, "100");
        params.put(CommonParams.START, "0");
        params.put(CommonParams.DEBUG_QUERY, "true");
        params.put(CommonParams.EXPLAIN_STRUCT, "true");

        MapSolrParams solrParams = new MapSolrParams(params);
        QueryResponse response;
        List<SearchResult> result = new LinkedList<SearchResult>();
        try {
            response = this.server.query(solrParams);
        } catch (SolrServerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return result;
        }
        SolrDocumentList matchingDisorders = response.getResults();
        Map< ? , ? > explanations = response.getExplainMap();
        SumMap<String> cummulativeScore = new SumMap<String>();
        CounterMap<String> matchCounter = new CounterMap<String>();
        Set<String> allAncestors = new HashSet<String>();
        for (String phenotype : phenotypes) {
            allAncestors.addAll(hpoService.getAllAncestorsAndSelfIDs(phenotype));
        }
        for (SolrDocument disorder : matchingDisorders) {
            String omimId = (String) disorder.getFieldValue(ID_FIELD_NAME);
            float score = ((SimpleOrderedMap<Float>) explanations.get(omimId)).get("value");
            for (Object hpoId : disorder.getFieldValues("actual_symptom")) {
                if (allAncestors.contains(hpoId)) {
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
                result.add(new SearchResult(hpoId, (String) term.getFieldValue("name"), cummulativeScore.get(hpoId)
                    / (matchCounter.get(hpoId) * matchCounter.get(hpoId))));
            }
            Collections.sort(result);
        }
        return result.subList(0, limit);
    }
}
