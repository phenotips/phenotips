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
package org.phenotips.ontology.internal.solr;

import org.phenotips.obo2solr.ParameterPreparer;
import org.phenotips.obo2solr.TermData;
import org.phenotips.ontology.OntologyTerm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

/**
 * Ontologies processed from HGNC file share much of the processing code.
 *
 * @since 1.2RC1
 * @version $Id$
 */
public abstract class AbstractHGNCSolrOntologyService extends AbstractSolrOntologyService
{
    /**
     * The name of the Alternative ID field, used for older aliases of updated HPO terms.
     */
    protected static final String ALTERNATIVE_ID_FIELD_NAME = "alt_id";

    protected static final String VERSION_FIELD_NAME = "version";

    protected static final String SIZE_FIELD_NAME = "size";
    protected static final String ROWS_FIELD_NAME = "rows";

    private static final String FIELD_VALUE_SEPARATOR = "\\t";

    private String infoServiceURL = "http://rest.genenames.org/info";

    /** Performs HTTP requests to the remote REST service. */
    private final CloseableHttpClient client = HttpClients.createSystem();

    /** The number of documents to be added and committed to Solr at a time. */
    protected abstract int getSolrDocsPerBatch();

    private Map<String, TermData> transform(String ontologyUrl, Map<String, Double> fieldSelection)
    {
        URL url;
        try {
            url = new URL(ontologyUrl);
        } catch (MalformedURLException ex) {
            return null;
        }
        return transform(url, fieldSelection);
    }

    private Map<String, TermData> transform(URL url, Map<String, Double> fieldSelection)
    {
        Map<String, TermData> data = new LinkedHashMap<String, TermData>();

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));

            String line;
            int counter = 0;

            String zeroLine = in.readLine();
            String[] headers = zeroLine.split(FIELD_VALUE_SEPARATOR, -1);
            headers[0] = ID_FIELD_NAME;

            while ((line = in.readLine()) != null) {

                TermData crtTerm = new TermData();

                String[] pieces = line.split(FIELD_VALUE_SEPARATOR, -1);
                // Ignore the whole line if begins with tab symbol
                if (pieces.length > 1 && !"".equals(pieces[0])) {
                    continue;
                }

                for (String term : pieces) {
                    if (!"".equals(term)) {
                        crtTerm.addTo(headers[counter], term);
                    }
                    counter++;
                }

                data.put(crtTerm.getId(), crtTerm);
            }

        } catch (NullPointerException ex) {
            this.logger.error("NullPointer: {}", ex.getMessage());
        } catch (IOException ex) {
            this.logger.error("IOException: {}", ex.getMessage());
        }

        // put version/size here
        TermData metaTerm = new TermData();
        JSONObject info = getInfo();
        metaTerm.addTo(ID_FIELD_NAME, "HEADER_INFO");
        metaTerm.addTo(VERSION_FIELD_NAME, getVersion(info));
        metaTerm.addTo(SIZE_FIELD_NAME, Objects.toString(getSize(info), null));

        data.put("metadata", metaTerm);

        return data;
    }

    @Override
    public OntologyTerm getTerm(String id)
    {
        OntologyTerm result = super.getTerm(id);
        if (result == null) {
            Map<String, String> queryParameters = new HashMap<>();
            queryParameters.put(ALTERNATIVE_ID_FIELD_NAME, id);
            Set<OntologyTerm> results = search(queryParameters);
            if (results != null && !results.isEmpty()) {
                result = search(queryParameters).iterator().next();
            }
        }
        return result;
    }

    @Override
    public Set<OntologyTerm> getTerms(Collection<String> ids)
    {
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
    public int reindex(String ontologyUrl)
    {
        this.clear();
        return this.index(ontologyUrl);
    }

    /**
     * Add an ontology to the index.
     *
     * @param ontologyUrl the address from where to get the ontology file
     * @return {@code 0} if the indexing succeeded, {@code 1} if writing to the Solr server failed, {@code 2} if the
     *         specified URL is invalid
     */
    protected int index(String ontologyUrl)
    {
        String realOntologyUrl = StringUtils.defaultIfBlank(ontologyUrl, getDefaultOntologyLocation());

        Map<String, Double> fieldSelection = new HashMap<String, Double>();
        Map<String, TermData> data = transform(realOntologyUrl, fieldSelection);
        if (data == null) {
            return 2;
        }
        try {
            Collection<SolrInputDocument> termBatch = new HashSet<SolrInputDocument>();
            Iterator<Map.Entry<String, TermData>> dataIterator = data.entrySet().iterator();
            int batchCounter = 0;
            while (dataIterator.hasNext()) {
                /* Resetting when the batch fills */
                if (batchCounter == getSolrDocsPerBatch()) {
                    commitTerms(termBatch);
                    termBatch = new HashSet<>();
                    batchCounter = 0;
                }
                Map.Entry<String, TermData> item = dataIterator.next();
                SolrInputDocument doc = new SolrInputDocument();
                for (Map.Entry<String, Collection<String>> property : item.getValue().entrySet()) {
                    String name = property.getKey();
                    for (String value : property.getValue()) {
                        doc.addField(name, value, ParameterPreparer.DEFAULT_BOOST.floatValue());
                    }
                }
                termBatch.add(doc);
                batchCounter++;
            }
            commitTerms(termBatch);
            return 0;
        } catch (SolrServerException ex) {
            this.logger.warn("Failed to index ontology: {}", ex.getMessage());
        } catch (IOException ex) {
            this.logger.warn("Failed to communicate with the Solr server while indexing ontology: {}", ex.getMessage());
        } catch (OutOfMemoryError ex) {
            this.logger.warn("Failed to add terms to the Solr. Ran out of memory. {}", ex.getMessage());
        }
        return 1;
    }

    protected void commitTerms(Collection<SolrInputDocument> batch)
        throws SolrServerException, IOException, OutOfMemoryError
    {
        this.externalServicesAccess.getServer().add(batch);
        this.externalServicesAccess.getServer().commit();
        this.externalServicesAccess.getCache().removeAll();
    }

    /**
     * Delete all the data in the Solr index.
     *
     * @return {@code 0} if the command was successful, {@code 1} otherwise
     */
    protected int clear()
    {
        try {
            this.externalServicesAccess.getServer().deleteByQuery("*:*");
            return 0;
        } catch (SolrServerException ex) {
            this.logger.error("SolrServerException while clearing the Solr index", ex);
        } catch (IOException ex) {
            this.logger.error("IOException while clearing the Solr index", ex);
        }
        return 1;
    }

    @Override
    public String getVersion()
    {
        QueryResponse response;
        SolrQuery query = new SolrQuery();
        SolrDocumentList termList;
        SolrDocument firstDoc;

        query.setQuery("version:*");
        query.set(ROWS_FIELD_NAME, "1");
        try {
            response = this.externalServicesAccess.getServer().query(query);
            termList = response.getResults();

            if (!termList.isEmpty()) {
                firstDoc = termList.get(0);
                return firstDoc.getFieldValue(VERSION_FIELD_NAME).toString();
            }
        } catch (SolrServerException | SolrException ex) {
            this.logger.warn("Failed to query ontology version: {}", ex.getMessage());
        } catch (IOException ex) {
            this.logger.error("IOException while getting ontology version", ex);
        }
        return null;
    }

    @Override
    public long size()
    {
        QueryResponse response;
        SolrQuery query = new SolrQuery();
        SolrDocumentList termList;
        SolrDocument firstDoc;

        query.setQuery("size:*");
        query.set(ROWS_FIELD_NAME, "1");
        try {
            response = this.externalServicesAccess.getServer().query(query);
            termList = response.getResults();

            if (!termList.isEmpty()) {
                firstDoc = termList.get(0);
                String result = firstDoc.getFieldValue(SIZE_FIELD_NAME).toString();
                return Long.valueOf(result).longValue();
            }
        } catch (SolrServerException | SolrException ex) {
            this.logger.warn("Failed to query ontology size", ex.getMessage());
        } catch (IOException ex) {
            this.logger.error("IOException while getting ontology size", ex);
        }
        return 0;
    }

    private long getSize(JSONObject info)
    {
        return info.isNullObject() ? -1 : info.getLong("numDoc");
    }

    private String getVersion(JSONObject info)
    {
        return info.isNullObject() ? "" : info.getString("lastModified");
    }

    private JSONObject getInfo()
    {
        HttpGet method = new HttpGet(infoServiceURL);
        method.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        try (CloseableHttpResponse httpResponse = client.execute(method)) {
            String response = IOUtils.toString(httpResponse.getEntity().getContent(), Consts.UTF_8);
            JSONObject responseJSON = (JSONObject) JSONSerializer.toJSON(response);
            return responseJSON;
        } catch (IOException | JSONException ex) {
            this.logger.warn("Failed to get HGNC information: {}", ex.getMessage());
        }
        return new JSONObject(true);
    }

}
