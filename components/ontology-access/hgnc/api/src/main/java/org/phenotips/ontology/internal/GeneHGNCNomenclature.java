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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.phenotips.ontology.internal;

import org.phenotips.ontology.OntologyService;
import org.phenotips.ontology.OntologyTerm;
import org.phenotips.ontology.internal.solr.AbstractHGNCSolrOntologyService;
import org.phenotips.ontology.internal.solr.SolrOntologyTerm;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.EntryEvictionConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.params.SpellingParams;
import org.slf4j.Logger;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

/**
 * Provides access to the HUGO Gene Nomenclature Committee's GeneNames ontology. The ontology prefix is {@code HGNC}.
 *
 * @version $Id$
 * @since 1.2RC1
 */
@Component
@Named("hgnc")
@Singleton
public class GeneHGNCNomenclature extends AbstractHGNCSolrOntologyService
{ 
    /** For determining if a query is a an id. */
    private static final Pattern ID_PATTERN = Pattern.compile("^HGNC:[0-9]+$", Pattern.CASE_INSENSITIVE);
    
    @Inject
    private Logger logger;

    @Override
    public String getDefaultOntologyLocation()
    {
        return "http://www.genenames.org/cgi-bin/download?col=gd_hgnc_id&col=gd_app_sym&col=gd_app_name"
            + "&col=gd_prev_sym&col=gd_aliases&col=gd_pub_acc_ids&col=gd_pub_eg_id&col=gd_pub_ensembl_id&col="
            +"gd_pub_refseq_ids&col=family.id&col=family.name&col=md_eg_id&col=md_mim_id&col=md_refseq_id&col="
            +"md_prot_id&col=md_ensembl_id&status=Approved&status_opt=2&where=&order_by=gd_app_sym_sort&format="
            +"text&hgnc_dbtag=on&submit=submit";
    }

    @Override
    protected int getSolrDocsPerBatch()
    { 
        return 15000;
    }
    
    @Override
    protected String getName()
    {
        return "hgnc";
    }

    @Override
    public Set<String> getAliases()
    {
        Set<String> result = new HashSet<String>();
        result.add(getName());
        result.add("HGNC");
        return result;
    }
    private Map<String, String> getStaticSolrParams()
    {
        String trueStr = "true";
        Map<String, String> params = new HashMap<>();
        params.put("spellcheck", trueStr);
        params.put(SpellingParams.SPELLCHECK_COLLATE, trueStr);
        params.put(SpellingParams.SPELLCHECK_COUNT, "100");
        params.put(SpellingParams.SPELLCHECK_MAX_COLLATION_TRIES, "3");
        params.put("lowercaseOperators", "false");
        params.put("defType", "edismax");
        return params;
    }

    private Map<String, String> getStaticFieldSolrParams()
    {
        Map<String, String> params = new HashMap<>();
        params.put("pf", "name^20 nameSpell^36 nameExact^100 namePrefix^30 "
            + "synonym^15 synonymSpell^25 synonymExact^70 synonymPrefix^20 "
            + "text^3 textSpell^5");
        params.put("qf",
            "name^10 nameSpell^18 nameStub^5 synonym^6 synonymSpell^10 synonymStub^3 text^1 textSpell^2 textStub^0.5");
        return params;
    }

    private SolrParams produceDynamicSolrParams(String originalQuery, Integer rows, String sort, String customFq,
        boolean isId)
    {
        String query = originalQuery.trim();
        ModifiableSolrParams params = new ModifiableSolrParams();
        String escapedQuery = ClientUtils.escapeQueryChars(query);
        if (isId) {
            params.add(CommonParams.FQ, StringUtils.defaultIfBlank(customFq,
                new MessageFormat("id:{0} alt_id:{0}").format(new String[] { escapedQuery })));
        } else {
            params.add(CommonParams.FQ, StringUtils.defaultIfBlank(customFq, "term_category:HGNC\\:0000118"));
        }
        params.add(CommonParams.Q, escapedQuery);
        params.add(SpellingParams.SPELLCHECK_Q, query);
        params.add(CommonParams.ROWS, rows.toString());
        if (StringUtils.isNotBlank(sort)) {
            params.add(CommonParams.SORT, sort);
        }
        return params;
    }

    @Override
    public Set<OntologyTerm> termSuggest(String query, Integer rows, String sort, String customFq)
    {
        if (StringUtils.isBlank(query)) {
            return new HashSet<>();
        }
        boolean isId = this.isId(query);
        Map<String, String> options = this.getStaticSolrParams();
        if (!isId) {
            options.putAll(this.getStaticFieldSolrParams());
        }
        Set<OntologyTerm> result = new LinkedHashSet<OntologyTerm>();
        for (SolrDocument doc : this.search(produceDynamicSolrParams(query, rows, sort, customFq, isId), options)) {
            result.add(new SolrOntologyTerm(doc, this));
        }
        return result;
    }
    

    private boolean isId(String query)
    {
        return ID_PATTERN.matcher(query).matches();
    }
    
    @Override
    public long getDistance(String fromTermId, String toTermId)
    {
        // Flat nomenclature
        return -1;
    }

    @Override
    public long getDistance(OntologyTerm fromTerm, OntologyTerm toTerm)
    {
        // Flat nomenclature
        return -1;
    }
    
    
    
    
    
    
    //--------------------------------------------------------------to re-implement


    @Override
    public Set<OntologyTerm> search(Map<String, ?> fieldValues)
    {
        return search(fieldValues, Collections.<String, String>emptyMap());
    }

    @Override
    public Set<OntologyTerm> search(Map<String, ?> fieldValues, Map<String, String> queryOptions)
    {
        try {
            HttpGet method =
                new HttpGet(this.searchServiceURL + URLEncoder.encode(generateQuery(fieldValues), Consts.UTF_8.name()));
            method.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
            try (CloseableHttpResponse httpResponse = this.client.execute(method)) {
                String response = IOUtils.toString(httpResponse.getEntity().getContent(), Consts.UTF_8);
                JSONObject responseJSON = (JSONObject) JSONSerializer.toJSON(response);
                JSONArray docs = responseJSON.getJSONObject(RESPONSE_KEY).getJSONArray(DATA_KEY);
                if (docs.size() >= 1) {
                    Set<OntologyTerm> result = new LinkedHashSet<>();
                    // The remote service doesn't offer any query control, manually select the right range
                    int start = 0;
                    if (queryOptions.containsKey(CommonParams.START)
                        && StringUtils.isNumeric(queryOptions.get(CommonParams.START)))
                    {
                        start = Math.max(0, Integer.parseInt(queryOptions.get(CommonParams.START)));
                    }
                    int end = docs.size();
                    if (queryOptions.containsKey(CommonParams.ROWS)
                        && StringUtils.isNumeric(queryOptions.get(CommonParams.ROWS)))
                    {
                        end = Math.min(end, start + Integer.parseInt(queryOptions.get(CommonParams.ROWS)));
                    }

                    for (int i = start; i < end; ++i) {
                        result.add(new JSONOntologyTerm(docs.getJSONObject(i), this));
                    }
                    return result;
                    // This is too slow, for the moment only return summaries
                    // return getTerms(ids);
                }
            } catch (IOException | JSONException ex) {
                this.logger.warn("Failed to search gene names: {}", ex.getMessage());
            }
        } catch (UnsupportedEncodingException ex) {
            // This will not happen, UTF-8 is always available
        }
        return Collections.emptySet();
    }

    @Override
    public long count(Map<String, ?> fieldValues)
    {
        try {
            HttpGet method =
                new HttpGet(this.searchServiceURL + URLEncoder.encode(generateQuery(fieldValues), Consts.UTF_8.name()));
            method.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
            try (CloseableHttpResponse httpResponse = this.client.execute(method)) {
                String response = IOUtils.toString(httpResponse.getEntity().getContent(), Consts.UTF_8);
                JSONObject responseJSON = (JSONObject) JSONSerializer.toJSON(response);
                JSONArray docs = responseJSON.getJSONObject(RESPONSE_KEY).getJSONArray(DATA_KEY);
                return docs.size();
            } catch (IOException | JSONException ex) {
                this.logger.warn("Failed to count matching gene names: {}", ex.getMessage());
            }
        } catch (UnsupportedEncodingException ex) {
            // This will not happen, UTF-8 is always available
        }
        return 0;
    }

}
