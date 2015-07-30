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
package org.phenotips.vocabulary.internal;

import org.phenotips.vocabulary.VocabularyTerm;
import org.phenotips.vocabulary.internal.solr.AbstractCSVSolrVocabulary;
import org.phenotips.vocabulary.internal.solr.SolrVocabularyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
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
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.internal.csv.CSVStrategy;

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
public class GeneNomenclature extends AbstractCSVSolrVocabulary
{
    // Approved symbol
    private static final String ORDER_BY = "gd_app_sym_sort";

    private static final String OUTPUT_FORMAT = "text";

    private static final String SELECT_STATUS = "Approved";

    private static final String USE_HGNC_DATABASE_IDENTIFIER = "on";

    private static final String SYMBOL_FIELD_NAME = "symbol";

    private static final String PREV_SYMBOL_FIELD_NAME = "prev_symbol";

    private static final String ALIAS_SYMBOL_FIELD_NAME = "alias_symbol";

    private static final String ACCESSION_SYMBOL_FIELD_NAME = "hgnc_accession";

    private static final String ENSEMBL_GENE_ID_FIELD_NAME = "ensembl_gene_id";

    private static final String ENTREZ_ID_FIELD_NAME = "entrez_id";

    private static final String REFSEQ_ACCESSION_FIELD_NAME = "refseq_accession";

    private static final String REFSEQ_ACCESSION_EXTERNAL_FIELD_NAME = "refseq_accession_external";

    private static final String ENTREZ_ID_EXTERNAL_FIELD_NAME = "entrez_id_external";

    private static final String ENSEMBL_GENE_ID_EXTERNAL_FIELD_NAME = "ensembl_gene_id_external";

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

    private static final List<String> FIELDS = Arrays.asList("id", SYMBOL_FIELD_NAME,
        "name", PREV_SYMBOL_FIELD_NAME, ALIAS_SYMBOL_FIELD_NAME, ACCESSION_SYMBOL_FIELD_NAME, ENTREZ_ID_FIELD_NAME,
        ENSEMBL_GENE_ID_FIELD_NAME, REFSEQ_ACCESSION_FIELD_NAME, "gene_family_id", "gene_family",
        ENTREZ_ID_EXTERNAL_FIELD_NAME, "omim_id", REFSEQ_ACCESSION_EXTERNAL_FIELD_NAME,
        "uniprot_id", ENSEMBL_GENE_ID_EXTERNAL_FIELD_NAME);

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

        this.baseServiceURL =
            this.configuration.getProperty("phenotips.ontologies.hgnc.serviceURL", "http://www.genenames.org/");
        this.infoServiceURL = "http://rest.genenames.org/info";
        this.dataServiceURL = this.baseServiceURL + "cgi-bin/download?";

        // assemble the columns
        for (String column : SELECTED_COLUMNS) {
            this.dataServiceURL += "col=" + column + "&";
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
    public String getDefaultSourceLocation()
    {
        return this.dataServiceURL;
    }

    @Override
    protected int getSolrDocsPerBatch()
    {
        return 500000;
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

    @Override
    public VocabularyTerm getTerm(String symbol)
    {
        String escapedSymbol = ClientUtils.escapeQueryChars(symbol);

        String queryString = String.format("%s:%s OR %s:%s OR %s:%s",
            SYMBOL_FIELD_NAME, escapedSymbol,
            PREV_SYMBOL_FIELD_NAME, escapedSymbol,
            ALIAS_SYMBOL_FIELD_NAME, escapedSymbol);
        return requestTerm(queryString, SYMBOL_EXACT);
    }

    /**
     * Access an individual term from the vocabulary, identified by its alternative ids: either Ensembl Gene ID or
     * Entrez Gene ID.
     *
     * @param id the term identifier that is one of property names: {@code ensembl_gene_id} or {@code entrez_id}
     * @return the requested term, or {@code null} if the term doesn't exist in this vocabulary
     */
    public VocabularyTerm getTermByAlternativeId(String id)
    {
        String escapedSymbol = ClientUtils.escapeQueryChars(id);

        String queryString = String.format("%s:%s OR %s:%s OR %s:%s OR %s:%s",
            ACCESSION_SYMBOL_FIELD_NAME, escapedSymbol,
            ENSEMBL_GENE_ID_FIELD_NAME, escapedSymbol,
            ENTREZ_ID_FIELD_NAME, escapedSymbol,
            REFSEQ_ACCESSION_FIELD_NAME, escapedSymbol);
        return requestTerm(queryString, null);
    }

    private Map<String, String> getStaticSolrParams()
    {
        Map<String, String> params = new HashMap<>();
        params.put("lowercaseOperators", "false");
        params.put("defType", "edismax");
        return params;
    }

    private Map<String, String> getStaticFieldSolrParams()
    {
        Map<String, String> params = new HashMap<>();
        params.put(DisMaxParams.QF, "symbol^10 symbolPrefix^7 symbolSort^5 "
            + "synonymExact^12 synonymPrefix^3 text^1 textSpell^2 textStub^0.5");
        params.put(DisMaxParams.PF, "symbol^50 symbolExact^100 symbolPrefix^30 symbolSort^35 "
            + "synonymExact^70 synonymPrefix^21 text^3 textSpell^5");
        return params;
    }

    private SolrParams produceDynamicSolrParams(String originalQuery, Integer rows, String sort, String customFq)
    {
        String escapedQuery = ClientUtils.escapeQueryChars(originalQuery.trim());

        ModifiableSolrParams params = new ModifiableSolrParams();
        params.add(CommonParams.Q, escapedQuery);
        params.add(CommonParams.ROWS, rows.toString());
        if (StringUtils.isNotBlank(sort)) {
            params.add(CommonParams.SORT, sort);
        }
        return params;
    }

    @Override
    public List<VocabularyTerm> search(String input, int maxResults, String sort, String customFilter)
    {
        if (StringUtils.isBlank(input)) {
            return Collections.emptyList();
        }
        String query = ClientUtils.escapeQueryChars(input.trim());
        Map<String, String> options = this.getStaticSolrParams();
        options.putAll(this.getStaticFieldSolrParams());

        List<VocabularyTerm> result = new LinkedList<>();
        SolrParams params = produceDynamicSolrParams(query, maxResults, sort, customFilter);
        for (SolrDocument doc : this.search(params, options)) {
            result.add(new SolrVocabularyTerm(doc, this));
        }
        return result;
    }

    @Override
    public Set<VocabularyTerm> getTerms(Collection<String> symbols)
    {
        Set<VocabularyTerm> result = new LinkedHashSet<>();
        for (String symbol : symbols) {
            VocabularyTerm term = getTerm(symbol);
            if (term != null) {
                result.add(term);
            }
        }
        if (result.isEmpty()) {
            for (String symbol : symbols) {
                VocabularyTerm term = getTermByAlternativeId(symbol);
                if (term != null) {
                    result.add(term);
                }
            }
        }
        return result;
    }

    @Override
    public long getDistance(String fromTermId, String toTermId)
    {
        // Flat nomenclature
        return -1;
    }

    @Override
    public long getDistance(VocabularyTerm fromTerm, VocabularyTerm toTerm)
    {
        // Flat nomenclature
        return -1;
    }

    @Override
    protected Collection<SolrInputDocument> transform(Map<String, Double> fieldSelection)
    {
        Map<String, String> headerToFieldMap = getHeaderToFieldMapping();
        CSVFileService data =
            new CSVFileService(getDefaultSourceLocation(), headerToFieldMap, CSVStrategy.TDF_STRATEGY);
        addMetaInfo(data.solrDocuments);
        processDuplicates(data.solrDocuments);
        return data.solrDocuments;
    }

    private Map<String, String> getHeaderToFieldMapping()
    {
        Map<String, String> map = new HashMap<String, String>();
        int count = 0;
        for (String field : FIELDS) {
            map.put(HEADERS.get(count), field);
            count++;
        }
        return map;
    }

    private void processDuplicates(Collection<SolrInputDocument> solrdocs)
    {
        List<String> curated =
            Arrays.asList(ENTREZ_ID_FIELD_NAME, ENSEMBL_GENE_ID_FIELD_NAME, REFSEQ_ACCESSION_FIELD_NAME);
        List<String> external =
            Arrays.asList(ENTREZ_ID_EXTERNAL_FIELD_NAME, ENSEMBL_GENE_ID_EXTERNAL_FIELD_NAME,
                REFSEQ_ACCESSION_EXTERNAL_FIELD_NAME);
        // Remove external fields, copy their values to corresponding curated field values if they are empty
        for (SolrInputDocument solrdoc : solrdocs) {
            int count = 0;
            for (String field : curated) {
                if (solrdoc.get(field) == null) {
                    if (!REFSEQ_ACCESSION_FIELD_NAME.equals(field)
                        && solrdoc.getFieldValue(external.get(count)) != null) {
                        solrdoc.setField(field, solrdoc.getFieldValue(external.get(count)));
                    } else {
                        if (solrdoc.getFieldValues(external.get(count)) != null) {
                            solrdoc.setField(field, solrdoc.getFieldValues(external.get(count)));
                        }
                    }
                }
                solrdoc.removeField(external.get(count));
                count++;
            }
        }
    }

    private void addMetaInfo(Collection<SolrInputDocument> data)
    {
        // put version/size here
        SolrInputDocument metaTerm = new SolrInputDocument();
        JSONObject info = getInfo();
        metaTerm.addField(ID_FIELD_NAME, "HEADER_INFO");
        metaTerm.addField(VERSION_FIELD_NAME, getVersion(info));

        data.add(metaTerm);
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
