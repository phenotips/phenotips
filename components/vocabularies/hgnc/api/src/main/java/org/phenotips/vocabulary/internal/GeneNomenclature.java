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
import org.xwiki.configuration.ConfigurationSource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

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
    private static final String ID_FIELD_NAME = "id";

    private static final String SYMBOL_FIELD_NAME = "symbol";

    private static final String PREV_SYMBOL_FIELD_NAME = "prev_symbol";

    private static final String ALIAS_SYMBOL_FIELD_NAME = "alias_symbol";

    private static final String ACCESSION_SYMBOL_FIELD_NAME = "hgnc_accession";

    private static final String ENSEMBL_GENE_ID_FIELD_NAME = "ensembl_gene_id";

    private static final String ENTREZ_ID_FIELD_NAME = "entrez_id";

    private static final String REFSEQ_ACCESSION_FIELD_NAME = "refseq_accession";

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    @Override
    public String getDefaultSourceLocation()
    {
        return "ftp://ftp.ebi.ac.uk/pub/databases/genenames/new/tsv/hgnc_complete_set.txt";
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
    protected Collection<SolrInputDocument> load(URL url)
    {
        try {
            Collection<SolrInputDocument> solrDocuments = new HashSet<SolrInputDocument>();

            Reader in = new InputStreamReader(url.openConnection().getInputStream(), Charset.forName("UTF-8"));
            for (CSVRecord row : CSVFormat.TDF.withHeader().parse(in)) {
                SolrInputDocument crtTerm = new SolrInputDocument();
                for (Map.Entry<String, String> item : row.toMap().entrySet()) {
                    if ("hgnc_id".equals(item.getKey())) {
                        crtTerm.addField(ID_FIELD_NAME, item.getValue());
                    } else if (StringUtils.isNotBlank(item.getValue())) {
                        crtTerm.addField(item.getKey(), StringUtils.split(item.getValue(), "|"));
                    }
                }
                solrDocuments.add(crtTerm);
            }
            addMetaInfo(solrDocuments);
            return solrDocuments;
        } catch (IOException ex) {
            this.logger.warn("Failed to read/parse the HGNC source: {}", ex.getMessage());
        }
        return null;
    }

    private void addMetaInfo(Collection<SolrInputDocument> data)
    {
        SolrInputDocument metaTerm = new SolrInputDocument();
        metaTerm.addField(ID_FIELD_NAME, "HEADER_INFO");
        metaTerm.addField(VERSION_FIELD_NAME, ISODateTimeFormat.dateTime().withZoneUTC().print(new DateTime()));
        data.add(metaTerm);
    }
}
