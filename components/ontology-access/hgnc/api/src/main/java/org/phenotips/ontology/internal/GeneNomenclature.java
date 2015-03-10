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
package org.phenotips.ontology.internal;

import org.phenotips.ontology.OntologyService;
import org.phenotips.ontology.OntologyTerm;

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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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
import org.apache.solr.common.params.CommonParams;
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
 * @since 1.0RC1
 */
@Component
@Named("hgnc")
@Singleton
public class GeneNomenclature implements OntologyService, Initializable
{
    /**
     * Object used to mark in the cache that a term doesn't exist, since null means that the cache doesn't contain the
     * requested entry.
     */
    private static final OntologyTerm EMPTY_MARKER = new JSONOntologyTerm(null, null);

    private static final String RESPONSE_KEY = "response";

    private static final String DATA_KEY = "docs";

    private static final String LABEL_KEY = "symbol";

    private static final String WILDCARD = "*";

    private static final String DEFAULT_OPERATOR = "AND";

    private static final Map<String, String> QUERY_OPERATORS = new HashMap<>();

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    private String baseServiceURL;

    private String searchServiceURL;

    private String infoServiceURL;

    private String fetchServiceURL;

    /** Performs HTTP requests to the remote REST service. */
    private final CloseableHttpClient client = HttpClients.createSystem();

    @Inject
    private Logger logger;

    /**
     * Cache for the recently accessed terms; useful since the ontology rarely changes, so a search should always return
     * the same thing.
     */
    private Cache<OntologyTerm> cache;

    /** Cache for ontology metadata. */
    private Cache<JSONObject> infoCache;

    /** Cache factory needed for creating the term cache. */
    @Inject
    private CacheManager cacheFactory;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.baseServiceURL =
                this.configuration.getProperty("phenotips.ontologies.hgnc.serviceURL", "http://rest.genenames.org/");
            this.searchServiceURL = this.baseServiceURL + "search/";
            this.infoServiceURL = this.baseServiceURL + "info";
            this.fetchServiceURL = this.baseServiceURL + "fetch/";
            this.cache = this.cacheFactory.createNewLocalCache(new CacheConfiguration());
            EntryEvictionConfiguration infoConfig = new LRUEvictionConfiguration(1);
            infoConfig.setTimeToLive(300);
            this.infoCache = this.cacheFactory.createNewLocalCache(new CacheConfiguration(infoConfig));
        } catch (final CacheException ex) {
            throw new InitializationException("Cannot create cache: " + ex.getMessage());
        }
        QUERY_OPERATORS.put("OR", "");
        QUERY_OPERATORS.put(DEFAULT_OPERATOR, DEFAULT_OPERATOR + ' ');
        QUERY_OPERATORS.put("NOT", "-");
    }

    @Override
    public OntologyTerm getTerm(String id)
    {
        OntologyTerm result = this.cache.get(id);
        String safeID;
        if (result == null) {
            try {
                safeID = URLEncoder.encode(id, Consts.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                safeID = id.replaceAll("\\s", "");
                this.logger.warn("Could not find the encoding: {}", Consts.UTF_8.name());
            }
            HttpGet method = new HttpGet(this.fetchServiceURL + "symbol/" + safeID);
            method.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
            try (CloseableHttpResponse httpResponse = this.client.execute(method)) {
                String response = IOUtils.toString(httpResponse.getEntity().getContent(), Consts.UTF_8);
                JSONObject responseJSON = (JSONObject) JSONSerializer.toJSON(response);
                JSONArray docs = responseJSON.getJSONObject(RESPONSE_KEY).getJSONArray(DATA_KEY);
                if (docs.size() == 1) {
                    result = new JSONOntologyTerm(docs.getJSONObject(0), this);
                    this.cache.set(id, result);
                } else {
                    this.cache.set(id, EMPTY_MARKER);
                }
            } catch (IOException | JSONException ex) {
                this.logger.warn("Failed to fetch gene definition: {}", ex.getMessage());
            }
        }
        return (result == EMPTY_MARKER) ? null : result;
    }

    @Override
    public Set<OntologyTerm> getTerms(Collection<String> ids)
    {
        // FIXME Reimplement with a bunch of async connections fired in parallel
        Set<OntologyTerm> result = new LinkedHashSet<>();
        for (String id : ids) {
            OntologyTerm term = getTerm(id);
            if (term != null) {
                result.add(term);
            }
        }
        return result;
    }

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

    @Override
    public Set<String> getAliases()
    {
        Set<String> result = new HashSet<String>();
        result.add("hgnc");
        result.add("HGNC");
        return result;
    }

    @Override
    public long size()
    {
        JSONObject info = getInfo();
        return info.isNullObject() ? -1 : info.getLong("numDoc");
    }

    @Override
    public int reindex(String ontologyUrl)
    {
        // Remote ontology, we cannot reindex, but we can clear the local cache
        this.cache.removeAll();
        return 0;
    }

    @Override
    public String getDefaultOntologyLocation()
    {
        return this.baseServiceURL;
    }

    @Override
    public String getVersion()
    {
        JSONObject info = getInfo();
        return info.isNullObject() ? "" : info.getString("lastModified");
    }

    private JSONObject getInfo()
    {
        JSONObject info = this.infoCache.get("");
        if (info != null) {
            return info;
        }
        HttpGet method = new HttpGet(this.infoServiceURL);
        method.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        try (CloseableHttpResponse httpResponse = this.client.execute(method)) {
            String response = IOUtils.toString(httpResponse.getEntity().getContent(), Consts.UTF_8);
            JSONObject responseJSON = (JSONObject) JSONSerializer.toJSON(response);
            this.infoCache.set("", responseJSON);
            return responseJSON;
        } catch (IOException | JSONException ex) {
            this.logger.warn("Failed to get HGNC information: {}", ex.getMessage());
        }
        return new JSONObject(true);
    }

    /**
     * Generate a Lucene query from a map of parameters, to be used in the "q" parameter for Solr.
     *
     * @param fieldValues a map with term meta-property values that must be matched by the returned terms; the keys are
     * property names, like {@code id}, {@code description}, {@code is_a}, and the values can be either a single value,
     * or a collection of values that can (OR) be matched by the term;
     * @return the String representation of the equivalent Lucene query
     */
    private String generateQuery(Map<String, ?> fieldValues)
    {
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, ?> field : fieldValues.entrySet()) {
            processQueryPart(query, field, true);
        }
        return StringUtils.removeStart(query.toString().trim(), DEFAULT_OPERATOR);
    }

    private StringBuilder processQueryPart(StringBuilder query, Map.Entry<String, ?> field, boolean includeOperator)
    {
        if (Collection.class.isInstance(field.getValue()) && ((Collection<?>) field.getValue()).isEmpty()) {
            return query;
        }
        if (Map.class.isInstance(field.getValue())) {
            if (QUERY_OPERATORS.containsKey(field.getKey())) {
                @SuppressWarnings("unchecked")
                Map.Entry<String, Map<String, ?>> subquery = (Map.Entry<String, Map<String, ?>>) field;
                return processSubquery(query, subquery);
            } else {
                this.logger.warn("Invalid subquery operator: {}", field.getKey());
                return query;
            }
        }
        query.append(' ');
        if (includeOperator) {
            query.append(QUERY_OPERATORS.get(DEFAULT_OPERATOR));
        }

        query.append(ClientUtils.escapeQueryChars(field.getKey()));
        query.append(":(");
        if (Collection.class.isInstance(field.getValue())) {
            for (Object value : (Collection<?>) field.getValue()) {
                String svalue = String.valueOf(value);
                if (svalue.endsWith(WILDCARD)) {
                    svalue = ClientUtils.escapeQueryChars(StringUtils.removeEnd(svalue, WILDCARD)) + WILDCARD;
                } else {
                    svalue = ClientUtils.escapeQueryChars(svalue);
                }
                query.append(svalue);
                query.append(' ');
            }
        } else {
            String svalue = String.valueOf(field.getValue());
            if (svalue.endsWith(WILDCARD)) {
                svalue = ClientUtils.escapeQueryChars(StringUtils.removeEnd(svalue, WILDCARD)) + WILDCARD;
            } else {
                svalue = ClientUtils.escapeQueryChars(svalue);
            }
            query.append(svalue);
        }
        query.append(')');
        return query;
    }

    private StringBuilder processSubquery(StringBuilder query, Map.Entry<String, Map<String, ?>> subquery)
    {
        query.append(' ').append(QUERY_OPERATORS.get(subquery.getKey())).append('(');
        for (Map.Entry<String, ?> field : subquery.getValue().entrySet()) {
            processQueryPart(query, field, false);
        }
        query.append(')');
        return query;
    }

    private static class JSONOntologyTerm implements OntologyTerm
    {
        private JSONObject data;

        private OntologyService ontology;

        public JSONOntologyTerm(JSONObject data, OntologyService ontology)
        {
            this.data = data;
            this.ontology = ontology;
        }

        @Override
        public String getId()
        {
            return this.data.getString(LABEL_KEY);
        }

        @Override
        public String getName()
        {
            return this.data.getString("name");
        }

        @Override
        public String getDescription()
        {
            // No description for gene names
            return "";
        }

        @Override
        public Set<OntologyTerm> getParents()
        {
            return Collections.emptySet();
        }

        @Override
        public Set<OntologyTerm> getAncestors()
        {
            return Collections.emptySet();
        }

        @Override
        public Set<OntologyTerm> getAncestorsAndSelf()
        {
            return Collections.<OntologyTerm>singleton(this);
        }

        @Override
        public long getDistanceTo(OntologyTerm other)
        {
            return -1;
        }

        @Override
        public Object get(String name)
        {
            return this.data.get(name);
        }

        @Override
        public OntologyService getOntology()
        {
            return this.ontology;
        }

        @Override
        public String toString()
        {
            return "HGNC:" + getId();
        }

        @Override
        public JSON toJson()
        {
            JSONObject json = new JSONObject();
            json.put("id", this.getId());
            return json;
        }
    }

    @Override
    public Set<OntologyTerm> termSuggest(String query, Integer rows, String sort, String customFq)
    {
        // ignoring sort and customFq
        String formattedQuery = String.format("%s*", query);
        Map<String, Object> fieldValues = new HashMap<>();
        Map<String, String> queryMap = new HashMap<>();
        Map<String, String> rowsMap = new HashMap<>();
        queryMap.put(LABEL_KEY, formattedQuery);
        queryMap.put("alias_symbol", formattedQuery);
        queryMap.put("prev_symbol", formattedQuery);
        fieldValues.put("status", "Approved");
        fieldValues.put(DEFAULT_OPERATOR, queryMap);
        rowsMap.put("rows", rows.toString());

        return this.search(fieldValues, rowsMap);
    }
}
