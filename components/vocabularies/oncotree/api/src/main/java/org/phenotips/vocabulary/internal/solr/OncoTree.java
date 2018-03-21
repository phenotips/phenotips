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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.SpellingParams;

/**
 * Provides access to the OncoTree vocabulary. The vocabulary prefix is {@code ONCO}.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("onco")
@Singleton
@SuppressWarnings("ClassDataAbstractionCoupling")
public class OncoTree extends AbstractCSVSolrVocabulary
{
    /** The base url for the oncotree tumor types file. */
    private static final String BASE_URL = "http://oncotree.mskcc.org/oncotree/api/tumor_types.txt";

    /** The latest stable version. */
    private static final String VERSION = "?version=oncotree_latest_stable";

    /** The default location of the OncoTree data file (the latest stable version). */
    private static final String SOURCE_URL = BASE_URL + VERSION;

    private static final String TISSUE = "tissue";

    private static final String IS_A = "is_a";

    private static final String TERM_CATEGORY = "term_category";

    private static final String ID = "id";

    private static final String NAME = "name";

    private static final String SEPARATOR = ":";

    private static final String HEADER_INFO_LABEL = "HEADER_INFO";

    private static final String OPEN = "(";

    private static final String CLOSE = ")";

    private static final String SYNONYM = "synonym";

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private static final String DISEASE = "disease";

    private static final String CANCER = "cancer";

    /** The list of supported categories for this vocabulary. */
    private static final Collection<String> SUPPORTED_CATEGORIES =
        Collections.unmodifiableCollection(Arrays.asList(DISEASE, CANCER));

    private Map<Integer, String> header;

    private Map<String, SolrInputDocument> dataMap;

    @Override
    protected int getSolrDocsPerBatch()
    {
        return 15000;
    }

    @Override
    protected Collection<SolrInputDocument> load(@Nonnull final URL url)
    {
        this.dataMap = new HashMap<>();
        try (BufferedReader in = new BufferedReader(
            new InputStreamReader(getInputStream(url), StandardCharsets.UTF_8))) {
            final CSVFormat parser = setupCSVParser();
            // Process each csv record row.
            final CSVParser parsed = parser.parse(in);
            this.header = parsed.getHeaderMap().entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
            for (final CSVRecord row : parsed) {
                processDataRow(row);
            }
            this.dataMap.put(VERSION_FIELD_NAME, getVersionDoc(url));
        } catch (final IOException e) {
            this.logger.error("Failed to load vocabulary source: {}", e.getMessage());
        }
        return this.dataMap.values();
    }

    /**
     * Gets an input stream from the provided {@code url}. Visible for testing purposes.
     *
     * @param url the {@link URL} for the cancers data
     * @return an {@link InputStream} with the data
     * @throws IOException if a connection cannot be opened
     */
    @Nonnull
    InputStream getInputStream(@Nonnull final URL url) throws IOException
    {
        return url.openConnection().getInputStream();
    }

    /**
     * Processes a CSV {@code row}, and adds the relevant fields to a {@link #dataMap map}.
     *
     * @param row a {@link CSVRecord} containing data for one path (from root to leaf) of the OncoTree
     */
    private void processDataRow(@Nonnull final CSVRecord row)
    {
        final String tissue = formatTissue(row.get(0));
        // The last entered SolrInputDocument.
        SolrInputDocument doc = null;
        for (int i = 1; i < row.size(); i++) {
            final String value = row.get(i);
            if (StringUtils.isNotBlank(value)) {
                // We're looking at the cancer names.
                if (i < 5) {
                    doc = addNode(doc, value, tissue);
                // Other data, that pertains to the last node in the path.
                } else {
                    addData(doc, this.header.get(i), value);
                }
            }
        }
    }

    /**
     * Adds the {@code fieldName} and {@code fieldValue} to the provided {@code doc}.
     *
     * @param doc the {@link SolrInputDocument} that will store {@code fieldName} and {@code fieldValue} data
     * @param fieldName the name of the field to be stored
     * @param fieldValue the value to be stored
     */
    private void addData(
        @Nullable final SolrInputDocument doc,
        @Nonnull final String fieldName,
        @Nonnull final String fieldValue)
    {
        // The document should not be null.
        if (doc != null) {
            doc.addField(fieldName, fieldValue);
        } else {
            this.logger.error("The field name {} and field value {} being processed are not associated with any "
                + "cancer.", fieldName, fieldValue);
        }
    }

    /**
     * Processes the {@code value cancer name}, and writes the extracted identifier and name information into a
     * {@link SolrInputDocument} associated with {@code value}.
     *
     * @param parent the {@link SolrInputDocument} containing data for a parent cancer to {@code value}
     * @param value the provided raw cancer name
     * @param tissue the tissue affected
     */
    @Nullable
    private SolrInputDocument addNode(
        @Nullable final SolrInputDocument parent,
        @Nonnull final String value,
        @Nonnull final String tissue)
    {
        final String cancerId = lastSubstringBetween(value, OPEN, CLOSE).trim();
        final String cancerName = StringUtils.substringBeforeLast(value, OPEN).trim();
        if (StringUtils.isNotBlank(cancerId)) {
            final SolrInputDocument doc = getSolrInputDocForCancer(cancerId);
            updateCancerName(doc, cancerName);
            updateParents(doc, parent);
            updateTissue(doc, tissue);
            return doc;
        }
        this.logger.error("No identifier could be extracted from the provided cancer name: {}", value);
        return null;
    }

    /**
     * Update the {@code doc} with the {@code tissue} associated with it.
     *
     * @param doc the {@link SolrInputDocument} containing cancer data
     * @param tissue the tissue affected by the cancer
     */
    private void updateTissue(@Nonnull final SolrInputDocument doc, @Nonnull final String tissue)
    {
        final Collection<Object> storedTissues = doc.getFieldValues(TISSUE);
        if (valueIsNotYetAdded(storedTissues, tissue)) {
            doc.addField(TISSUE, tissue);
        }
    }

    /**
     * Retrieves a {@link SolrInputDocument} that contains data for cancer with ID {@code cancerId}. If no such
     * {@link SolrInputDocument} is stored yet, creates it and populates the ID field.
     *
     * @param cancerId the non-prefixed cancer ID
     * @return a {@link SolrInputDocument} associated with {@code cancerId}
     */
    @Nonnull
    private SolrInputDocument getSolrInputDocForCancer(@Nonnull final String cancerId)
    {
        final String prefixedId = getTermPrefix() + SEPARATOR + cancerId;
        if (!this.dataMap.containsKey(prefixedId)) {
            final SolrInputDocument doc = new SolrInputDocument();
            doc.setField(ID, prefixedId);
            this.dataMap.put(prefixedId, doc);
            return doc;
        }
        return this.dataMap.get(prefixedId);
    }

    /**
     * Tries to extract the name of the cancer from the provided raw {@code value name} string, and writes it into
     * {@code doc}.
     *
     * @param doc the {@link SolrInputDocument} into which data is written
     * @param value the provided raw cancer name
     */
    private void updateCancerName(@Nonnull final SolrInputDocument doc, @Nonnull final String value)
    {
        final String storedName = (String) doc.getFieldValue(NAME);
        final Collection<Object> synonyms = doc.getFieldValues(SYNONYM);
        if (StringUtils.isBlank(storedName)) {
            doc.setField(NAME, value);
        } else if (!storedName.equals(value) && valueIsNotYetAdded(synonyms, value)) {
            doc.addField(SYNONYM, value);
        }
    }

    /**
     * Retrieves the last substring between {@code open} and {@code close}.
     *
     * @param value the string being evaluated
     * @param open the opening delimiter
     * @param close the closing delimiter
     * @return the substring between {@code open} and {@code close}, if exists, empty string otherwise
     */
    @Nonnull
    private String lastSubstringBetween(
        @Nonnull final String value,
        @Nonnull final String open,
        @Nonnull final String close)
    {
        final String afterOpen = StringUtils.substringAfterLast(value, open);
        return StringUtils.isNotBlank(afterOpen)
            ? StringUtils.replace(afterOpen, close, StringUtils.EMPTY)
            : StringUtils.EMPTY;
    }

    /**
     * Updates the parents of the cancer that is currently being processed.
     *
     * @param doc the {@link SolrInputDocument} into which data is written
     * @param parent the {@link SolrInputDocument} that contains data for cancer that is a parent to the cancer stored
     *            in {@code doc}
     */
    private void updateParents(@Nonnull final SolrInputDocument doc, @Nullable final SolrInputDocument parent)
    {
        if (parent != null) {
            final String parentId = (String) parent.getFieldValue(ID);
            final Collection<Object> storedParents = doc.getFieldValues(IS_A);
            if (valueIsNotYetAdded(storedParents, parentId)) {
                doc.addField(IS_A, parentId);
            }

            final Set<Object> ancestorSet = new HashSet<>();
            ancestorSet.add(parentId);

            final Collection<Object> parentIds = parent.getFieldValues(TERM_CATEGORY);
            if (CollectionUtils.isNotEmpty(parentIds)) {
                ancestorSet.addAll(parentIds);
            }
            doc.addField(TERM_CATEGORY, ancestorSet);
        }
    }

    /**
     * Returns true if {@code value} has not yet been added to {@code valueCollection}.
     *
     * @param valueCollection a collection which may contain {@code value}; may be null
     * @param value a value of interest; may be null
     * @return false iff {@code valueCollection} contains {@code value}, true otherwise
     */
    private boolean valueIsNotYetAdded(@Nullable final Collection<Object> valueCollection, @Nullable final String value)
    {
        return CollectionUtils.isEmpty(valueCollection) || !valueCollection.contains(value);
    }

    /**
     * Formats the cancer tissue string.
     *
     * @param value the provided tissue property value
     */
    @Nonnull
    private String formatTissue(@Nullable final String value)
    {
        return StringUtils.isNotBlank(value) ? StringUtils.substringBefore(value, OPEN).trim() : StringUtils.EMPTY;
    }

    /**
     * Sets up the CSV parser with tab-delimited format, and first row as header.
     *
     * @return a {@link CSVRecord parser}
     */
    @Nonnull
    private CSVFormat setupCSVParser()
    {
        return CSVFormat.TDF.withFirstRecordAsHeader();
    }

    /**
     * Returns the prefix for the vocabulary terms belonging to the OncoTree vocabulary.
     *
     * @return a prefix for the OncoTree vocabulary terms
     */
    @Nonnull
    private String getTermPrefix()
    {
        return "ONCO";
    }

    @Override
    public String getIdentifier()
    {
        return "onco";
    }

    @Override
    public String getName()
    {
        return "OncoTree";
    }

    @Override
    public Set<String> getAliases()
    {
        final Set<String> aliases = new HashSet<>();
        aliases.add(getName());
        aliases.add(getIdentifier());
        aliases.add(getTermPrefix());
        return Collections.unmodifiableSet(aliases);
    }

    @Override
    public String getDefaultSourceLocation()
    {
        return SOURCE_URL;
    }

    @Override
    public String getWebsite()
    {
        return "http://oncotree.mskcc.org/oncotree/";
    }

    @Override
    public String getCitation()
    {
        return "OncoTree: CMO Tumor Type Tree";
    }

    @Override
    public List<VocabularyTerm> search(
        @Nullable final String input,
        final int maxResults,
        @Nullable final String sort,
        @Nullable final String customFilter)
    {
        return StringUtils.isBlank(input)
            ? Collections.emptyList()
            : searchMatches(input, maxResults, sort, customFilter);
    }

    /**
     * Searches the Solr index for matches to the input string.
     *
     * @param input string to match
     * @param maxResults the maximum number of results
     * @param sort the optional sort parameter
     * @param customFilter custom filter for results
     * @return a list of matching {@link VocabularyTerm} objects; empty if no suitable matches found
     */
    @Nonnull
    private List<VocabularyTerm> searchMatches(
        @Nonnull final String input,
        final int maxResults,
        @Nullable final String sort,
        @Nullable final String customFilter)
    {
        final SolrQuery query = new SolrQuery();
        addGlobalQueryParam(query);
        addFieldQueryParam(query);
        final List<SolrDocument> searchResults = search(addDynamicQueryParam(input, maxResults, sort, customFilter,
            query));
        final List<VocabularyTerm> results = searchResults.stream()
            .map(doc -> new SolrVocabularyTerm(doc, this))
            .collect(Collectors.toCollection(LinkedList::new));
        return Collections.unmodifiableList(results);
    }

    /**
     * Adds dynamic solr query parameters to {@code query}, based on the received {@code rawQuery raw query string},
     * {@code rows the maximum number of results to return}, {@code sort the sorting order}, and {@code customFilter a
     * custom filter}.
     *
     * @param rawQuery unprocessed query string
     * @param rows the maximum number of search items to return
     * @param sort the optional sort parameter
     * @param customFilter custom filter for the results
     * @param query a {@link SolrQuery solr query} object
     * @return the updated {@link SolrQuery solr query} object
     */
    @Nonnull
    private SolrQuery addDynamicQueryParam(
        @Nonnull final String rawQuery,
        @Nonnull final Integer rows,
        @Nullable final String sort,
        @Nullable final String customFilter,
        @Nonnull SolrQuery query)
    {
        final String queryString = rawQuery.trim();
        final String escapedQuery = ClientUtils.escapeQueryChars(queryString);
        if (StringUtils.isNotBlank(customFilter)) {
            query.setFilterQueries(customFilter);
        }
        query.setQuery(escapedQuery);
        query.set(SpellingParams.SPELLCHECK_Q, queryString);
        final String lastWord = StringUtils.defaultIfBlank(StringUtils.substringAfterLast(escapedQuery,
            StringUtils.SPACE),
            escapedQuery) + "*";
        query.set(DisMaxParams.BQ,
            String.format("nameSpell:%1$s^20 text:%1$s^1 textSpell:%1$s^2", lastWord));
        query.setRows(rows);
        if (StringUtils.isNotBlank(sort)) {
            for (final String sortItem : sort.split("\\s*,\\s*")) {
                query.addSort(StringUtils.substringBefore(sortItem, StringUtils.SPACE),
                    sortItem.endsWith(" desc") || sortItem.startsWith("-")
                        ? SolrQuery.ORDER.desc
                        : SolrQuery.ORDER.asc);
            }
        }
        return query;
    }

    /**
     * Given a {@code query} object, adds global query parameters.
     *
     * @param query a {@link SolrQuery solr query} object
     */
    private void addGlobalQueryParam(@Nonnull final SolrQuery query)
    {
        // Add global query parameters.
        query.set("spellcheck", Boolean.toString(true));
        query.set(SpellingParams.SPELLCHECK_COLLATE, Boolean.toString(true));
        query.set(SpellingParams.SPELLCHECK_COUNT, "100");
        query.set(SpellingParams.SPELLCHECK_MAX_COLLATION_TRIES, "3");
        query.set("lowercaseOperators", Boolean.toString(false));
        query.set("defType", "edismax");
    }

    /**
     * Given a {@code query} object, adds field query parameters.
     *
     * @param query a {@link SolrQuery solr query} object
     */
    private void addFieldQueryParam(@Nonnull final SolrQuery query)
    {
        query.set(DisMaxParams.PF, "name^20 nameSpell^36 nameExact^100 namePrefix^30 text^3 textSpell^5");
        query.set(DisMaxParams.QF, "id^100 name^10 nameSpell^18 nameStub^5 text^1 textSpell^2 textStub^0.5");
    }

    /**
     * Creates the version {@link SolrInputDocument}.
     *
     * @param url the {@link URL} where data is stored
     * @return a {@link SolrInputDocument} containing version data
     */
    @Nonnull
    private SolrInputDocument getVersionDoc(@Nonnull final URL url)
    {
        final String urlStr = url.toString();
        final String version = StringUtils.substringAfter(urlStr, VERSION_FIELD_NAME + "=");
        final SolrInputDocument doc = new SolrInputDocument();
        final DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        final Date date = new Date();
        final String datedVersion = StringUtils.isNotBlank(version)
            ? version + "/" + dateFormat.format(date)
            : dateFormat.format(date);
        doc.addField(ID_FIELD_NAME, HEADER_INFO_LABEL);
        doc.addField(VERSION_FIELD_NAME, datedVersion);
        return doc;
    }

    @Override
    public Collection<String> getSupportedCategories()
    {
        return SUPPORTED_CATEGORIES;
    }
}
