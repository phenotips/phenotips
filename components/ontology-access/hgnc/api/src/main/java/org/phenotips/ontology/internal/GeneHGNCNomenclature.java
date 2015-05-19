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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
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
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
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

    private static final List<String> SELECTED_COLUMNS = Arrays.asList("gd_hgnc_id", "gd_app_sym",
        "gd_app_name", "gd_prev_sym", "gd_aliases", "gd_pub_acc_ids", "gd_pub_eg_id",
        "gd_pub_ensembl_id", "gd_pub_refseq_ids", "family.id", "family.name",
        "md_eg_id", "md_mim_id", "md_refseq_id",
        "md_prot_id", "md_ensembl_id");

    private static final List<String> HEADERS = Arrays.asList("HGNC ID", "Approved Symbol",
        "Approved Name", "Previous Symbols", "Synonyms", "Accession Numbers", "Entrez Gene ID",
        "Ensembl Gene ID", "RefSeq IDs", "Gene Family ID", "Gene Family Name",
        "Entrez Gene ID(supplied by NCBI)", "OMIM ID(supplied by OMIM)", "RefSeq(supplied by NCBI)",
        "UniProt ID(supplied by UniProt)", "Ensembl ID(supplied by Ensembl)");

    private static final List<String> FIELDS = Arrays.asList("id", "symbol",
        "name", "prev_symbol", "alias_symbol", "ena", "entrez_id",
        "ensembl_gene_id", "refseq_accession", "gene_family_id", "gene_family",
        "entrez_id_external", "omim_id", "refseq_accession_external",
        "uniprot_ids", "ensembl_gene_id_external");

    // Approved symbol
    private static final String ORDER_BY = "gd_app_sym_sort";

    private static final String OUTPUT_FORMAT = "text";

    private static final String SELECT_STATUS = "Approved";

    private static final String USE_HGNC_DATABASE_IDENTIFIER = "on";

    private static final String COMMON_PARAMS_PF = "pf";

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
        super.initialize();
        Map<String, Boolean> config = new HashMap<String, Boolean>();
        for (String column : SELECTED_COLUMNS) {
            config.put(column, true);
        }

        this.baseServiceURL =
            this.configuration.getProperty("phenotips.ontologies.hgnc.serviceURL", "http://www.genenames.org/");
        this.infoServiceURL = "http://rest.genenames.org/info";
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
        params.put(COMMON_PARAMS_PF, "name^20 nameSpell^36 nameExact^100 namePrefix^30 "
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
    public OntologyTerm getTerm(String symbol)
    {
        QueryResponse response;
        SolrQuery query = new SolrQuery();
        SolrDocumentList termList;
        OntologyTerm term;

        String escapedSymbol = ClientUtils.escapeQueryChars(symbol);

        String queryString = String.format("%s:%s OR %s:%s OR %s:%s",
            SYMBOL_FIELD_NAME, escapedSymbol,
            PREV_SYMBOL_FIELD_NAME, escapedSymbol,
            ALIAS_SYMBOL_FIELD_NAME, escapedSymbol);
        query.setQuery(queryString);
        query.setRows(1);
        query.set(COMMON_PARAMS_PF, "nameExact^100");
        try {
            response = this.externalServicesAccess.getServer().query(query);
            termList = response.getResults();

            if (!termList.isEmpty()) {
                term = new SolrOntologyTerm(termList.get(0), this);
                return term;
            }
        } catch (SolrServerException | SolrException ex) {
            this.logger.warn("Failed to query ontology term: {}", ex.getMessage());
        } catch (IOException ex) {
            this.logger.error("IOException while getting ontology term", ex);
        }
        return null;
    }

    @Override
    public Set<OntologyTerm> getTerms(Collection<String> symbols)
    {
        Set<OntologyTerm> result = new LinkedHashSet<>();
        for (String symbol : symbols) {
            OntologyTerm term = getTerm(symbol);
            if (term != null) {
                result.add(term);
            }
        }
        return result;
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
            SolrOntologyTerm ontTerm = new SolrOntologyTerm(doc, this);
            result.add(ontTerm);
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
    protected Collection<SolrInputDocument> transform(Map<String, Double> fieldSelection)
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

    private void getFieldNames(String[] headers)
    {
        Map<String, String> fieldNames = new HashMap<String, String>();
        int count = 0;
        for (String field : FIELDS) {
            fieldNames.put(HEADERS.get(count), field);
            count++;
        }

        for (int i = 0; i < headers.length; i++) {
            headers[i] = fieldNames.get(headers[i]);
        }
    }

    private void processDuplicates(SolrInputDocument solrdoc)
    {
        List<String> curated = Arrays.asList("entrez_id", "ensembl_gene_id", "refseq_accession");
        List<String> external =
            Arrays.asList("entrez_id_external", "ensembl_gene_id_external", "refseq_accession_external");
        // Remove extrenal fields, copy their values to corresponding curated field values if they are empty
        int count = 0;
        for (String field : curated) {
            if ("".equals(solrdoc.get(field))) {
                if (!"refseq_accession".equals(field)) {
                    solrdoc.setField(field, solrdoc.getFieldValue(external.get(count)));
                } else {
                    solrdoc.setField(field, solrdoc.getFieldValues(external.get(count)));
                }
            }
            solrdoc.removeField(external.get(count));
            count++;
        }
    }

    private Collection<SolrInputDocument> transform(URL searchURL, URL infoURL, Map<String, Double> fieldSelection)
    {
        Collection<SolrInputDocument> data = new LinkedList<>();

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(searchURL.openConnection().getInputStream()));

            String line;

            String zeroLine = in.readLine();
            String[] headers = zeroLine.split(FIELD_VALUE_SEPARATOR, -1);

            // get correct field names for velocity
            getFieldNames(headers);

            while ((line = in.readLine()) != null) {
                String[] pieces = line.split(FIELD_VALUE_SEPARATOR, -1);
                // Ignore the whole line if begins with tab symbol
                if (pieces.length != headers.length || "".equals(pieces[0])) {
                    continue;
                }

                SolrInputDocument crtTerm = new SolrInputDocument();
                int counter = 0;
                for (String term : pieces) {
                    if (!"".equals(term)) {
                        crtTerm.addField(headers[counter], term);
                    }
                    counter++;
                }

                processDuplicates(crtTerm);

                data.add(crtTerm);
            }

        } catch (NullPointerException ex) {
            this.logger.error("NullPointer: {}", ex.getMessage());
        } catch (IOException ex) {
            this.logger.error("IOException: {}", ex.getMessage());
        }

        // put version/size here
        SolrInputDocument metaTerm = new SolrInputDocument();
        JSONObject info = getInfo();
        metaTerm.addField(ID_FIELD_NAME, "HEADER_INFO");
        metaTerm.addField(VERSION_FIELD_NAME, getVersion(info));
        metaTerm.addField(SIZE_FIELD_NAME, Objects.toString(getSize(info)));

        data.add(metaTerm);

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
        try (CloseableHttpResponse httpResponse = this.client.execute(method)) {
            String response = IOUtils.toString(httpResponse.getEntity().getContent(), Consts.UTF_8);
            JSONObject responseJSON = (JSONObject) JSONSerializer.toJSON(response);
            return responseJSON;
        } catch (IOException | JSONException ex) {
            this.logger.warn("Failed to get HGNC information: {}", ex.getMessage());
        }
        return new JSONObject(true);
    }

}
