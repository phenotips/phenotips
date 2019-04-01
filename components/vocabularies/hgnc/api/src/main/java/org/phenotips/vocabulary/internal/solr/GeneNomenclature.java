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
package org.phenotips.vocabulary.internal.solr;

import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;

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

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.SpellingParams;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Provides access to the HUGO Gene Nomenclature Committee's GeneNames vocabulary. The vocabulary prefix is
 * {@code HGNC}.
 *
 * @version $Id$
 * @since 1.2RC1
 */
@Component
@Named("hgnc")
@Singleton
public class GeneNomenclature extends AbstractCSVSolrVocabulary
{
    private static final String SEPARATOR = ":";

    /** The list of supported categories for this vocabulary. */
    private static final Collection<String> SUPPORTED_CATEGORIES = Collections.singletonList("gene");

    private static final String ID_FIELD_NAME = "id";

    private static final String SYMBOL_FIELD_NAME = "symbol";

    private static final String ALTERNATIVE_ID_FIELD_NAME = "alt_id";

    private static final Map<String, String> COMMON_SEARCH_OPTIONS;

    private static final Map<String, String> DISMAX_SEARCH_OPTIONS;

    private static final Map<String, String> IDENTIFIER_SEARCH_OPTIONS;

    private static final Map<String, String> TEXT_SEARCH_OPTIONS;

    private static final Map<String, String> SPELLCHECKED_TEXT_SEARCH_OPTIONS;

    static {
        Map<String, String> options = new HashMap<>();
        options.put("lowercaseOperators", Boolean.toString(false));
        options.put("defType", "edismax");
        COMMON_SEARCH_OPTIONS = Collections.unmodifiableMap(options);

        String spellcheck = "spellcheck";

        options = new HashMap<>();
        options.put(DisMaxParams.QF, "symbol^100 symbolStub^75 "
            + "alt_id^60 alt_idStub^40 "
            + "name^10 nameSpell^18 nameStub^5 "
            + "synonym^6 synonymSpell^10 synonymStub^3 "
            + "text^1 textSpell^2 textStub^0.5");
        options.put(DisMaxParams.PF, "name^20 nameSpell^36 nameExact^100 namePrefix^30 "
            + "synonym^15 synonymSpell^25 synonymExact^70 synonymPrefix^20 "
            + "text^3 textSpell^5");
        DISMAX_SEARCH_OPTIONS = Collections.unmodifiableMap(options);

        options = new HashMap<>();
        options.putAll(COMMON_SEARCH_OPTIONS);
        options.put(spellcheck, Boolean.toString(false));
        options.put(DisMaxParams.QF,
            "symbol^50 symbolStub^25 alt_id^20 alt_idStub^10 ensembl_gene_id^40 ensembl_gene_idStub^20");
        IDENTIFIER_SEARCH_OPTIONS = Collections.unmodifiableMap(options);

        options = new HashMap<>();
        options.putAll(COMMON_SEARCH_OPTIONS);
        options.put(spellcheck, Boolean.toString(false));
        options.putAll(DISMAX_SEARCH_OPTIONS);
        TEXT_SEARCH_OPTIONS = Collections.unmodifiableMap(options);

        options = new HashMap<>();
        options.putAll(COMMON_SEARCH_OPTIONS);
        options.put(spellcheck, Boolean.toString(true));
        options.put(SpellingParams.SPELLCHECK_COLLATE, Boolean.toString(true));
        options.put(SpellingParams.SPELLCHECK_COUNT, "100");
        options.put(SpellingParams.SPELLCHECK_MAX_COLLATION_TRIES, "3");
        options.putAll(DISMAX_SEARCH_OPTIONS);
        SPELLCHECKED_TEXT_SEARCH_OPTIONS = Collections.unmodifiableMap(options);
    }

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
    public String getIdentifier()
    {
        return "hgnc";
    }

    @Override
    public String getName()
    {
        return "HUGO Gene Nomenclature Committee's GeneNames (HGNC)";
    }

    @Override
    public Collection<String> getSupportedCategories()
    {
        return SUPPORTED_CATEGORIES;
    }

    @Override
    public Set<String> getAliases()
    {
        Set<String> result = new HashSet<>();
        result.add(getIdentifier());
        result.add("HGNC");
        return result;
    }

    @Override
    public String getWebsite()
    {
        return "http://www.genenames.org/";
    }

    @Override
    public String getCitation()
    {
        return "HGNC Database, HUGO Gene Nomenclature Committee (HGNC), EMBL Outstation - Hinxton, European"
            + " Bioinformatics Institute, Wellcome Trust Genome Campus, Hinxton, Cambridgeshire, CB10 1SD, UK";
    }

    @Override
    public VocabularyTerm getTerm(String symbol)
    {
        if (StringUtils.isBlank(symbol)) {
            return null;
        }
        final String id = (StringUtils.contains(symbol, SEPARATOR)
                          && !StringUtils.isBlank(StringUtils.substringAfter(symbol, SEPARATOR)))
            ? StringUtils.substringAfter(symbol, SEPARATOR)
            : symbol;
        return requestTerm(ClientUtils.escapeQueryChars(id));
    }

    /**
     * Access an individual term from the vocabulary, identified by its gene symbol, alternative ID, or HGNC ID.
     *
     * @param id the term identifier that is one of the property names: {@code symbol}, {@code alt_id}, {@code id}
     * @return the requested term, or {@code null} if the term doesn't exist in this vocabulary
     */
    private VocabularyTerm requestTerm(final String id)
    {
        final String queryStr = String.format("%2$s:%1$s^50 %3$s:%1$s %4$s:HGNC\\:%1$s", id, SYMBOL_FIELD_NAME,
            ALTERNATIVE_ID_FIELD_NAME, ID_FIELD_NAME);
        return requestTerm(queryStr, null);
    }

    private SolrQuery produceDynamicSolrParams(Map<String, String> staticOptions, String originalQuery, Integer rows,
        String sort, String customFilter)
    {
        String escapedQuery = ClientUtils.escapeQueryChars(originalQuery.trim());

        SolrQuery params = new SolrQuery(escapedQuery);
        for (Map.Entry<String, String> option : staticOptions.entrySet()) {
            params.set(option.getKey(), option.getValue());
        }
        params.setRows(rows);
        if (StringUtils.isNotBlank(sort)) {
            params.add(CommonParams.SORT, sort);
        }
        params.add(CommonParams.FQ, StringUtils.defaultIfBlank(customFilter, "status:Approved"));
        return params;
    }

    @Override
    public List<VocabularyTerm> search(String input, int maxResults, String sort, String customFilter)
    {
        if (StringUtils.isBlank(input)) {
            return Collections.emptyList();
        }
        List<VocabularyTerm> result = searchIdentifiers(input, maxResults, sort, customFilter);
        if (result == null || result.isEmpty()) {
            result = searchText(input, maxResults, sort, customFilter);
        }
        if (result == null || result.isEmpty()) {
            result = searchTextSpellchecked(input, maxResults, sort, customFilter);
        }
        return result;
    }

    private List<VocabularyTerm> searchIdentifiers(String input, int maxResults, String sort, String customFilter)
    {
        SolrQuery params = produceDynamicSolrParams(IDENTIFIER_SEARCH_OPTIONS, input, maxResults, sort, customFilter);
        List<VocabularyTerm> result = new LinkedList<>();
        for (SolrDocument doc : this.search(params)) {
            result.add(new SolrVocabularyTerm(doc, this));
        }
        return result;
    }

    private List<VocabularyTerm> searchText(String input, int maxResults, String sort, String customFilter)
    {
        SolrQuery params = produceDynamicSolrParams(TEXT_SEARCH_OPTIONS, input, maxResults, sort, customFilter);
        List<VocabularyTerm> result = new LinkedList<>();
        for (SolrDocument doc : this.search(params)) {
            result.add(new SolrVocabularyTerm(doc, this));
        }
        return result;
    }

    private List<VocabularyTerm> searchTextSpellchecked(String input, int maxResults, String sort, String customFilter)
    {
        SolrQuery params =
            produceDynamicSolrParams(SPELLCHECKED_TEXT_SEARCH_OPTIONS, input, maxResults, sort, customFilter);
        List<VocabularyTerm> result = new LinkedList<>();
        for (SolrDocument doc : this.search(params)) {
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
            Collection<SolrInputDocument> solrDocuments = new HashSet<>();

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
