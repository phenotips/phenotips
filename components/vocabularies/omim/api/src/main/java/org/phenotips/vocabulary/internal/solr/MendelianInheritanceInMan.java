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

import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.SpellingParams;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Provides access to the Online Mendelian Inheritance in Man (OMIM) vocabulary. The vocabulary prefix is {@code MIM}.
 *
 * @version $Id$
 * @since 1.0M8
 */
@Component
@Named("omim")
@Singleton
public class MendelianInheritanceInMan extends AbstractCSVSolrVocabulary
{
    /** The location for the official OMIM source. */
    public static final String OMIM_SOURCE_URL = "http://data.omim.org/downloads/???/mimTitles.txt";

    private static final String DISEASE = "disease";

    private static final String GENE = "gene";

    /** The list of supported categories for this vocabulary. */
    private static final Collection<String> SUPPORTED_CATEGORIES =
        Collections.unmodifiableCollection(Arrays.asList(DISEASE, GENE));

    /** The standard name of this vocabulary, used as a term prefix. */
    private static final String STANDARD_NAME = "MIM";

    /** The default filter for disease OMIM vocabulary searches. */
    private static final String DEFAULT_DISEASE_FILTER = "+type:disorder";

    /** The default filter for GENE OMIM vocabulary searches. */
    private static final String DEFAULT_GENE_FILTER = "+type:gene";

    private static final String GENE_ANNOTATIONS_URL = "http://omim.org/static/omim/data/mim2gene.txt";

    private static final String GENEREVIEWS_MAPPING_URL =
        "ftp://ftp.ncbi.nih.gov/pub/GeneReviews/NBKid_shortname_OMIM.txt";

    private static final String ENCODING = "UTF-8";

    private static final String ID_FIELD = "id";

    private static final String SYMBOL_FIELD = "symbol";

    private static final String TYPE_FIELD = "type";

    private static final String NAME_FIELD = "name";

    private static final String SHORT_NAME_FIELD = "short_name";

    private static final String SYNONYM_FIELD = "synonym";

    private static final String INCLUDED_NAME_FIELD = "included_name";

    private static final String GENE_FIELD = "GENE";

    private static final String TITLE_SEPARATOR = ";";

    private static final String LIST_SEPARATOR = ";;";

    /** The map of symbols preceding a MIM number to their symbolic representations. */
    private static final Map<String, String> SYMBOLS;

    /** The map of symbols preceding a MIM number to their corresponding types. */
    private static final Map<String, String[]> TYPES;

    private Map<String, SolrInputDocument> data = new HashMap<>();

    static {
        Map<String, String> symbols = new HashMap<>();
        Map<String, String[]> types = new HashMap<>();
        symbols.put("NULL", "");
        types.put("NULL", new String[] { "disorder" });

        symbols.put("Asterisk", "*");
        types.put("Asterisk", new String[] { "gene" });

        symbols.put("Number Sign", "#");
        types.put("Number Sign", new String[] { "disorder" });

        symbols.put("Plus", "+");
        types.put("Plus", new String[] { "gene", "disorder" });

        symbols.put("Percent", "%");
        types.put("Percent", new String[] { "disorder" });

        SYMBOLS = Collections.unmodifiableMap(symbols);
        TYPES = Collections.unmodifiableMap(types);
    }

    @Inject
    @Named("hpo")
    private Vocabulary hpo;

    @Override
    public String getDefaultSourceLocation()
    {
        return OMIM_SOURCE_URL;
    }

    @Override
    protected int getSolrDocsPerBatch()
    {
        return 500000;
    }

    @Override
    public List<VocabularyTerm> search(String input, String category, int maxResults, String sort, String customFilter)
    {
        if (!getSupportedCategories().contains(category)) {
            this.logger.warn("The provided category [{}] is not supported by the OMIM vocabulary.", category);
            return Collections.emptyList();
        }
        final String filter = StringUtils.defaultIfBlank(customFilter, generateDefaultFilter(category));
        return search(input, maxResults, sort, filter);
    }

    /**
     * Generates the default filter for the OMIM vocabulary given the {@code category vocabulary category}.
     *
     * @param category the valid vocabulary category
     * @return the default filter for the query
     */
    private String generateDefaultFilter(final String category)
    {
        return DISEASE.equals(category) ? DEFAULT_DISEASE_FILTER : DEFAULT_GENE_FILTER;
    }

    @Override
    public String getIdentifier()
    {
        return "omim";
    }

    @Override
    public String getName()
    {
        return "Online Mendelian Inheritance in Man (OMIM)";
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
        result.add(STANDARD_NAME);
        result.add("OMIM");
        return result;
    }

    @Override
    public String getWebsite()
    {
        return "http://www.omim.org/";
    }

    @Override
    public String getCitation()
    {
        return "Online Mendelian Inheritance in Man, OMIM\u00ae. McKusick-Nathans Institute of Genetic Medicine,"
            + " Johns Hopkins University (Baltimore, MD)";
    }

    @Override
    protected Collection<SolrInputDocument> load(URL url)
    {
        parseOmimData(url);
        loadGenes();
        loadGeneReviews();
        loadVersion();
        return this.data.values();
    }

    @Override
    public VocabularyTerm getTerm(String id)
    {
        VocabularyTerm result = super.getTerm(id);
        if (result == null) {
            String optionalPrefix = STANDARD_NAME + ":";
            if (StringUtils.startsWith(id, optionalPrefix)) {
                result = getTerm(StringUtils.substringAfter(id, optionalPrefix));
            }
        }
        return result;
    }

    @Override
    public List<VocabularyTerm> search(String input, int maxResults, String sort, String customFilter)
    {
        if (StringUtils.isBlank(input)) {
            return Collections.emptyList();
        }
        SolrQuery query = new SolrQuery();
        addFieldQueryParameters(addGlobalQueryParameters(query));
        List<VocabularyTerm> result = new LinkedList<>();
        for (SolrDocument doc : search(addDynamicQueryParameters(input, maxResults, sort, customFilter, query))) {
            result.add(new SolrVocabularyTerm(doc, this));
        }
        return result;
    }

    private SolrQuery addGlobalQueryParameters(SolrQuery query)
    {
        query.set("spellcheck", Boolean.toString(true));
        query.set(SpellingParams.SPELLCHECK_COLLATE, Boolean.toString(true));
        query.set(SpellingParams.SPELLCHECK_COUNT, "100");
        query.set(SpellingParams.SPELLCHECK_MAX_COLLATION_TRIES, "3");
        query.set("lowercaseOperators", Boolean.toString(false));
        query.set("defType", "edismax");
        return query;
    }

    private SolrQuery addFieldQueryParameters(SolrQuery query)
    {
        query.set(DisMaxParams.PF, "name^40 nameSpell^70 synonym^15 synonymSpell^25 "
            + "included_name^15 included_nameSpell^25 text^3 textSpell^5");
        query.set(DisMaxParams.QF, "id^40 name^10 nameSpell^18 nameStub^5 "
            + "short_name^18 short_nameStub^5 "
            + "synonym^6 synonymSpell^10 synonymStub^3 "
            + "included_name^6 included_nameSpell^10 included_nameStub^3 "
            + "text^1 textSpell^2 textStub^0.5");
        return query;
    }

    private SolrQuery addDynamicQueryParameters(String originalQuery, Integer rows, String sort, String customFq,
        SolrQuery query)
    {
        String queryString = originalQuery.trim();
        String escapedQuery = ClientUtils.escapeQueryChars(queryString);

        query.setFilterQueries(StringUtils.defaultIfBlank(customFq, DEFAULT_DISEASE_FILTER));

        query.setQuery(escapedQuery);
        query.set(SpellingParams.SPELLCHECK_Q, queryString);
        String lastWord = StringUtils.substringAfterLast(escapedQuery, " ");
        if (StringUtils.isBlank(lastWord)) {
            lastWord = escapedQuery;
        }
        lastWord += "*";
        query.set(DisMaxParams.BQ,
            String.format("nameSpell:%1$s^20 short_name:%1$s^20 synonymSpell:%1$s^12 text:%1$s^1 textSpell:%1$s^2",
                lastWord));
        query.setRows(rows);
        if (StringUtils.isNotBlank(sort)) {
            for (String sortItem : sort.split("\\s*,\\s*")) {
                query.addSort(StringUtils.substringBefore(sortItem, " "),
                    sortItem.endsWith(" desc") || sortItem.startsWith("-") ? ORDER.desc : ORDER.asc);
            }
        }
        return query;
    }

    private void parseOmimData(URL sourceUrl)
    {
        try {
            Reader in =
                new InputStreamReader(sourceUrl.openConnection().getInputStream(),
                    Charset.forName(ENCODING));
            for (CSVRecord row : CSVFormat.TDF.withCommentMarker('#').parse(in)) {
                // Ignore moved or removed entries
                if ("Caret".equals(row.get(0))) {
                    continue;
                }

                SolrInputDocument crtTerm = new SolrInputDocument();
                // set id
                addFieldValue(ID_FIELD, row.get(1), crtTerm);

                // set symbol
                addFieldValue(SYMBOL_FIELD, SYMBOLS.get(row.get(0)), crtTerm);
                // set type (multivalued)
                for (String type : TYPES.get(row.get(0))) {
                    addFieldValue(TYPE_FIELD, type, crtTerm);
                }
                // set name
                String name = StringUtils.substringBefore(row.get(2), TITLE_SEPARATOR).trim();
                addFieldValue(NAME_FIELD, name, crtTerm);
                // set short name
                String shortNameString = StringUtils.substringAfter(row.get(2), TITLE_SEPARATOR).trim();
                String[] shortNames = StringUtils.split(shortNameString, TITLE_SEPARATOR);
                for (String shortName : shortNames) {
                    addFieldValue(SHORT_NAME_FIELD, shortName.trim(), crtTerm);
                }

                // set synonyms
                setListFieldValue(SYNONYM_FIELD, row.get(3), crtTerm);
                // set included name
                setListFieldValue(INCLUDED_NAME_FIELD, row.get(4), crtTerm);

                this.data.put(String.valueOf(crtTerm.get(ID_FIELD).getFirstValue()), crtTerm);
            }
        } catch (IOException ex) {
            this.logger.warn("Failed to read/parse the OMIM source: {}", ex.getMessage());
        }
    }

    private void setListFieldValue(String targetField, String value, SolrInputDocument doc)
    {
        if (StringUtils.isNotBlank(value)) {
            String[] items = StringUtils.split(value, LIST_SEPARATOR);
            for (String item : items) {
                addFieldValue(targetField, item.replaceAll(", INCLUDED$", "").trim(), doc);
            }
        }
    }

    private void addFieldValue(String targetField, String value, SolrInputDocument doc)
    {
        if (StringUtils.isNotBlank(value)) {
            doc.addField(targetField, value);
        }
    }

    private void loadGenes()
    {
        try (BufferedReader in =
            new BufferedReader(
                new InputStreamReader(new URL(this.relocationService.getRelocation(GENE_ANNOTATIONS_URL))
                    .openConnection().getInputStream(), ENCODING))) {
            for (CSVRecord row : CSVFormat.TDF.withCommentMarker('#').parse(in)) {
                SolrInputDocument term = this.data.get(row.get(0).trim());
                if (term != null) {
                    String gs = row.get(3).trim();
                    if (StringUtils.isNotBlank(gs)) {
                        term.addField(GENE_FIELD, gs);
                    }
                    String eidLine = row.get(4).trim();
                    if (StringUtils.isNotBlank(eidLine)) {
                        String[] eids = StringUtils.split(eidLine, ",");
                        for (String eid : eids) {
                            term.addField(GENE_FIELD, eid.trim());
                        }
                    }
                }
            }
        } catch (IOException ex) {
            this.logger.error("Failed to load OMIM-Gene links: {}", ex.getMessage(), ex);
        }
    }

    private void loadGeneReviews()
    {
        try (BufferedReader in =
            new BufferedReader(
                new InputStreamReader(new URL(this.relocationService.getRelocation(GENEREVIEWS_MAPPING_URL))
                    .openConnection().getInputStream(), ENCODING))) {
            for (CSVRecord row : CSVFormat.TDF.withHeader().parse(in)) {
                SolrInputDocument term = this.data.get(row.get(2));
                if (term != null) {
                    term.setField("gene_reviews_link", "https://www.ncbi.nlm.nih.gov/books/" + row.get(0));
                }
            }
        } catch (IOException ex) {
            this.logger.error("Failed to load OMIM-GeneReviews links: {}", ex.getMessage(), ex);
        }
    }

    private void loadVersion()
    {
        SolrInputDocument metaTerm = new SolrInputDocument();
        metaTerm.addField(ID_FIELD, "HEADER_INFO");
        metaTerm.addField("version", ISODateTimeFormat.dateTime().withZoneUTC().print(new DateTime()));
        this.data.put("VERSION", metaTerm);
    }
}
