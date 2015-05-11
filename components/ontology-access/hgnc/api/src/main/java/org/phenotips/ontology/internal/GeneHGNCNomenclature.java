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

import org.phenotips.obo2solr.TermData;
import org.phenotips.ontology.OntologyTerm;
import org.phenotips.ontology.internal.solr.AbstractCSVSolrOntologyService;
import org.phenotips.ontology.internal.solr.SolrOntologyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
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
public class GeneHGNCNomenclature extends AbstractCSVSolrOntologyService
{
    /** For determining if a query is a an id. */
    private static final Pattern ID_PATTERN = Pattern.compile("^HGNC:[0-9]+$", Pattern.CASE_INSENSITIVE);

    private static final String FIELD_VALUE_SEPARATOR = "\\t";

    private static final String HGNC_ID = "gd_hgnc_id";

    private static final String APPROVED_SYMBOL = "gd_app_sym";

    private static final String APPROVED_NAME = "gd_app_name";

    private static final String PREVIOUS_SYMBOLS = "gd_prev_sym";

    private static final String SYNONYMS = "gd_aliases";

    private static final String ACCESSION_NUMBERS = "gd_pub_acc_ids";

    private static final String ENTREZ_GENE_ID = "gd_pub_eg_id";

    private static final String ENSEMBL_GENE_ID = "gd_pub_ensembl_id";

    private static final String REFSEQ_IDS = "gd_pub_refseq_ids";

    private static final String GENE_FAMILY_ID = "family.id";

    private static final String GENE_FAMILY_NAME = "family.name";

    private static final String EXTERNAL_ENTREZ_GENE_ID = "md_eg_id";

    private static final String EXTERNAL_OMIM_ID = "md_mim_id";

    private static final String EXTERNAL_REFSEQ = "md_refseq_id";

    private static final String EXTERNAL_UNIPROT_ID = "md_prot_id";

    private static final String EXTERNAL_ENSEMBL_ID = "md_ensembl_id";

    // Approved symbol
    private static final String ORDER_BY = "gd_app_sym_sort";

    private static final String OUTPUT_FORMAT = "text";

    private static final String SELECT_STATUS = "Approved";

    private static final String USE_HGNC_DATABASE_IDENTIFIER = "on";

    private static Map<String, Boolean> config;

    /** Performs HTTP requests to the remote REST service. */
    private final CloseableHttpClient client = HttpClients.createSystem();

    private String baseServiceURL;

    private String infoServiceURL;

    private String dataServiceURL;

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    @Override
    public void initialize() throws InitializationException
    {
        this.baseServiceURL =
            this.configuration.getProperty("phenotips.ontologies.hgnc.serviceURL", "http://rest.genenames.org/");
        this.infoServiceURL = this.baseServiceURL + "info";
        this.dataServiceURL = this.baseServiceURL + "cgi-bin/download?";
        // assemble the columns
        for (Map.Entry<String, Boolean> item : config.entrySet()) {
            if (item.getValue()) {
                this.dataServiceURL += "col=" + item.getKey() + "&";
            }
        }

        this.dataServiceURL +=
            "status=" + SELECT_STATUS
                + "&order_by=" + ORDER_BY
                + "&format=" + OUTPUT_FORMAT
                + "&hgnc_dbtag=" + USE_HGNC_DATABASE_IDENTIFIER
                // those come by default in every query
                + "&status_opt=2&where=&limit=&submit=submit";

        config.put(HGNC_ID, true);
        config.put(APPROVED_SYMBOL, true);
        config.put(APPROVED_NAME, true);
        config.put(PREVIOUS_SYMBOLS, true);
        config.put(SYNONYMS, true);
        config.put(ACCESSION_NUMBERS, true);
        config.put(ENTREZ_GENE_ID, true);
        config.put(ENSEMBL_GENE_ID, true);
        config.put(REFSEQ_IDS, true);
        config.put(GENE_FAMILY_ID, true);
        config.put(GENE_FAMILY_NAME, true);
        config.put(EXTERNAL_ENTREZ_GENE_ID, true);
        config.put(EXTERNAL_OMIM_ID, true);
        config.put(EXTERNAL_REFSEQ, true);
        config.put(EXTERNAL_UNIPROT_ID, true);
        config.put(EXTERNAL_ENSEMBL_ID, true);
    }

    @Override
    public String getDefaultOntologyLocation()
    {
        return this.dataServiceURL;
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

    @Override
    protected Map<String, TermData> transform(Map<String, Double> fieldSelection)
    {
        URL url;
        URL infoURL;
        try {
            url = new URL(getDefaultOntologyLocation());
            infoURL = new URL(this.infoServiceURL);
        } catch (MalformedURLException ex) {
            return null;
        }
        return transform(url, infoURL, fieldSelection);
    }

    private Map<String, TermData> transform(URL searchURL, URL infoURL, Map<String, Double> fieldSelection)
    {
        Map<String, TermData> data = new LinkedHashMap<String, TermData>();

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(searchURL.openConnection().getInputStream()));

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
        HttpGet method = new HttpGet(this.infoServiceURL);
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
