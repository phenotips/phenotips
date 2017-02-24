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
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
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
    /** The standard name of this vocabulary, used as a term prefix. */
    public static final String STANDARD_NAME = "MIM";

    /** The location for the official OMIM source. */
    public static final String OMIM_SOURCE_URL = "http://data.omim.org/downloads/???/mimTitles.txt";

    private static final String ANNOTATIONS_BASE_URL =
        "http://compbio.charite.de/hudson/job/hpo.annotations/lastStableBuild/artifact/misc/";

    private static final String GENE_ANNOTATIONS_URL = "http://omim.org/static/omim/data/mim2gene.txt";

    private static final String POSITIVE_ANNOTATIONS_URL = ANNOTATIONS_BASE_URL + "phenotype_annotation.tab";

    private static final String NEGATIVE_ANNOTATIONS_URL = ANNOTATIONS_BASE_URL + "negative_phenotype_annotation.tab";

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

    private static final String SYNONYM_SEPARATOR = ";;";

    /** The map of symbols preceding a MIM number to their symbolic representations. */
    private static final Map<String, String> SYMBOLS;

    /** The map of symbols preceding a MIM number to their corresponding types. */
    private static final Map<String, String[]> TYPES;

    private SolrInputDocument crtTerm;

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

        symbols.put("Plus", "#");
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
    protected String getCoreName()
    {
        return getIdentifier();
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
        index();
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
        query.set(DisMaxParams.PF, "name^40 nameSpell^70 synonym^15 synonymSpell^25 text^3 textSpell^5");
        query.set(DisMaxParams.QF, "name^10 short_name^5 included_name^5 nameSpell^18 nameStub^5 "
            + "synonym^6 synonymSpell^10 synonymStub^3 text^1 textSpell^2 textStub^0.5");
        return query;
    }

    private SolrQuery addDynamicQueryParameters(String originalQuery, Integer rows, String sort, String customFq,
        SolrQuery query)
    {
        String queryString = originalQuery.trim();
        String escapedQuery = ClientUtils.escapeQueryChars(queryString);
        query.setFilterQueries(StringUtils.defaultIfBlank(customFq, "-(nameSort:\\** nameSort:\\+* nameSort:\\^*)"));
        query.setQuery(escapedQuery);
        query.set(SpellingParams.SPELLCHECK_Q, queryString);
        String lastWord = StringUtils.substringAfterLast(escapedQuery, " ");
        if (StringUtils.isBlank(lastWord)) {
            lastWord = escapedQuery;
        }
        lastWord += "*";
        query.set(DisMaxParams.BQ,
            String.format("nameSpell:%1$s^20 keywords:%1$s^2 text:%1$s^1 textSpell:%1$s^2", lastWord));
        query.setRows(rows);
        if (StringUtils.isNotBlank(sort)) {
            for (String sortItem : sort.split("\\s*,\\s*")) {
                query.addSort(StringUtils.substringBefore(sortItem, " "),
                    sortItem.endsWith(" desc") || sortItem.startsWith("-") ? ORDER.desc : ORDER.asc);
            }
        }
        return query;
    }

    @Override
    public String getVersion()
    {
        SolrQuery query = new SolrQuery();
        query.setQuery("version:*");
        query.set(CommonParams.ROWS, "1");
        try {
            QueryResponse response = this.externalServicesAccess.getSolrConnection(getCoreName()).query(query);
            SolrDocumentList termList = response.getResults();
            if (!termList.isEmpty()) {
                return termList.get(0).getFieldValue("version").toString();
            }
        } catch (SolrServerException | SolrException ex) {
            this.logger.warn("Failed to query vocabulary version: {}", ex.getMessage());
        } catch (IOException ex) {
            this.logger.error("IOException while getting vocabulary version", ex);
        }
        return null;
    }

    @Override
    public synchronized int reindex(String sourceURL)
    {
        try {
            index();
            Collection<SolrInputDocument> omimData = this.data.values();
            if (omimData.isEmpty()) {
                return 2;
            }
            if (clear() == 1) {
                return 1;
            }
            this.externalServicesAccess.getSolrConnection(getCoreName()).add(omimData);
            this.externalServicesAccess.getSolrConnection(getCoreName()).commit();
            this.externalServicesAccess.getTermCache(getCoreName()).removeAll();
        } catch (SolrServerException | IOException ex) {
            this.logger.error("Failed to reindex OMIM: {}", ex.getMessage(), ex);
            return 1;
        }
        return 0;
    }

    /**
     * Delete all the data in the Solr index.
     *
     * @return {@code 0} if the command was successful, {@code 1} otherwise
     */
    @Override
    public int clear()
    {
        try {
            this.externalServicesAccess.getSolrConnection(getCoreName()).deleteByQuery("*:*");
            return 0;
        } catch (SolrServerException ex) {
            this.logger.error("SolrServerException while clearing the Solr index", ex);
        } catch (IOException ex) {
            this.logger.error("IOException while clearing the Solr index", ex);
        }
        return 1;
    }

    private void index()
    {
        try {
            parseOmimData();
            loadGenes();
            loadSymptoms(true);
            loadSymptoms(false);
            loadGeneReviews();
            loadVersion();
        } catch (NullPointerException ex) {
            this.logger.error("Failed to prepare the OMIM index: {}", ex.getMessage(), ex);
        }
    }

    private void parseOmimData()
    {
        try {
            Reader in =
                new InputStreamReader(new URL(OMIM_SOURCE_URL).openConnection().getInputStream(),
                    Charset.forName(ENCODING));
            for (CSVRecord row : CSVFormat.TDF.parse(in)) {
                // ignore the comments, moved or removed entries
                if (row.get(0).startsWith("#") || ("Caret").equals(row.get(0))
                    || ("REMOVED FROM DATABASE").equals(row.get(2))) {
                    continue;
                }

                this.crtTerm = new SolrInputDocument();
                // set id
                setCrtTerm(ID_FIELD, row.get(1));

                // set symbol
                setCrtTerm(SYMBOL_FIELD, SYMBOLS.get(row.get(0)));
                // set type (multivalued)
                for (String type : TYPES.get(row.get(0))) {
                    setCrtTerm(TYPE_FIELD, type);
                }
                // set name
                String name = StringUtils.substringBefore(row.get(2), TITLE_SEPARATOR).trim();
                setCrtTerm(NAME_FIELD, name);
                // set short name
                String shortNameString = StringUtils.substringAfter(row.get(2), TITLE_SEPARATOR).trim();
                String[] shortNames = StringUtils.split(shortNameString, TITLE_SEPARATOR);
                for (String shortName : shortNames) {
                    setCrtTerm(SHORT_NAME_FIELD, shortName.trim());
                }

                // set synonyms
                if (StringUtils.isNotBlank(row.get(3))) {
                    String[] synonyms = StringUtils.split(row.get(3), SYNONYM_SEPARATOR);
                    for (String synonym : synonyms) {
                        setCrtTerm(SYNONYM_FIELD, synonym.trim());
                    }
                }
                // set included name
                if (StringUtils.isNotBlank(row.get(4))) {
                    String[] synonyms = StringUtils.split(row.get(4), SYNONYM_SEPARATOR);
                    for (String synonym : synonyms) {
                        setCrtTerm(INCLUDED_NAME_FIELD, synonym.replace(", INCLUDED", "").trim());
                    }
                }

                this.data.put(String.valueOf(this.crtTerm.get(ID_FIELD).getFirstValue()), this.crtTerm);
            }
        } catch (IOException ex) {
            this.logger.warn("Failed to read/parse the HGNC source: {}", ex.getMessage());
        }
    }

    private void setCrtTerm(String key, String value)
    {
        if (StringUtils.isNotBlank(value)) {
            this.crtTerm.setField(key, value);
        }
    }

    private void loadGenes()
    {
        try (BufferedReader in = new BufferedReader(
            new InputStreamReader(new URL(GENE_ANNOTATIONS_URL).openConnection().getInputStream(), ENCODING))) {
            for (CSVRecord row : CSVFormat.TDF.parse(in)) {
                // ignore comments rows and rows of not a gene MIM Entry Type
                if (row.get(0).startsWith("#") || !row.get(1).contains("gene")
                    || StringUtils.isNotBlank(row.get(2).trim())) {
                    continue;
                }
                SolrInputDocument term = this.data.get(row.get(2).trim());
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
        try (BufferedReader in = new BufferedReader(
            new InputStreamReader(new URL(GENEREVIEWS_MAPPING_URL).openConnection().getInputStream(), ENCODING))) {
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

    private void loadSymptoms(boolean positive)
    {
        String omimId = "";
        String previousOmimId = null;
        Set<String> ancestors = new HashSet<>();
        try (BufferedReader in = new BufferedReader(
            new InputStreamReader(new URL(positive ? POSITIVE_ANNOTATIONS_URL : NEGATIVE_ANNOTATIONS_URL)
                .openConnection().getInputStream(), ENCODING))) {
            for (CSVRecord row : CSVFormat.TDF.parse(in)) {
                if ("OMIM".equals(row.get(0))) {
                    omimId = row.get(1);
                    addAncestors(previousOmimId, omimId, ancestors, positive);
                    previousOmimId = omimId;
                    SolrInputDocument term = this.data.get(omimId);
                    if (term != null) {
                        term.addField(positive ? "actual_symptom" : "actual_not_symptom", row.get(4));
                    }
                    VocabularyTerm vterm = this.hpo.getTerm(row.get(4));
                    if (vterm != null) {
                        for (VocabularyTerm ancestor : vterm.getAncestorsAndSelf()) {
                            ancestors.add(ancestor.getId());
                        }
                    }
                }
            }
            addAncestors(omimId, null, ancestors, positive);
        } catch (IOException ex) {
            this.logger.error("Failed to load OMIM-HPO links: {}", ex.getMessage(), ex);
        }
    }

    private void addAncestors(String previousOmimId, String newOmimId, Set<String> ancestors, boolean positive)
    {
        if (previousOmimId == null || previousOmimId.equals(newOmimId)) {
            return;
        }
        final String symptomField = "symptom";
        SolrInputDocument term = this.data.get(previousOmimId);
        if (term == null) {
            return;
        }
        if (!positive) {
            ancestors.removeAll(term.getFieldValues(symptomField));
            term.addField("not_symptom", new HashSet<String>(ancestors));
        } else {
            term.addField(symptomField, new HashSet<String>(ancestors));
        }
        ancestors.clear();
    }
}
