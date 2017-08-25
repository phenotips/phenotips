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
package org.phenotips.vocabulary;

import org.xwiki.stability.Unstable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.slf4j.Logger;

/**
 * Implements {@link VocabularyExtension} to annotate {@link VocabularyInputTerm} from {@link #getTargetVocabularyIds
 * supported vocabularies} with data from {@link #getAnnotationSource a tab- or comma-separated file}. The default
 * behavior implemented in this base class is to gather data from the named columns in the file, and add this data to
 * the respective terms when reindexing a supported vocabulary. Setting up the names of the columns is done by the
 * concrete class, either by {@link #setupCSVParser telling} the CSV parser to treat the first row as the header
 * definition, or by explicitly assigning names to columns.
 * <p>
 * To let the first row be parsed as the column names:
 * </p>
 *
 * <pre>
 * {@code
 *   protected CSVFormat setupCSVParser(Vocabulary vocabulary)
 *   {
 *       return CSVFormat.TDF.withHeader();
 *   }
 * }
 * </pre>
 * <p>
 * To explicitly name columns:
 * </p>
 *
 * <pre>
 * {@code
 *   protected CSVFormat setupCSVParser(Vocabulary vocabulary)
 *   {
 *       return CSVFormat.TDF.withHeader("id", null, "symptom");
 *   }
 * }
 * </pre>
 * <p>
 * With the default implementation of {@link #processCSVRecordRow the row processing function}, having a column named
 * {@code id} is mandatory.
 * </p>
 * <p>
 * Columns that are not named are ignored.
 * </p>
 * <p>
 * Missing, empty, or whitespace-only cells will be ignored.
 * </p>
 * <p>
 * If multiple rows for the same term identifier exists, then the values are accumulated in lists of values.
 * </p>
 * <p>
 * If one or more of the fields parsed happen to already have values already in the term being extended, then the
 * existing values will be discarded and replaced with the data read from the input file.
 * </p>
 * <p>
 * If multiple rows for the same term identifier exists, then the values are accumulated in lists of values. If in the
 * schema definition a field is set as non-multi-valued, then it's the responsibility of the user to make sure that only
 * one value will be specified for such fields. If a value is specified multiple times in the input file, then it will
 * be added multiple times in the field.
 * </p>
 * <p>
 * Example: for the following parser set-up:
 * </p>
 *
 * <pre>
 * {@code
 * CSVFormat.CSV.withHeader("id", null, "symptom", null, "frequency")
 * }
 * </pre>
 *
 * and the following input file:
 *
 * <pre>
 * {@code
 * MIM:162200,"NEUROFIBROMATOSIS, TYPE I",HP:0009737,"Lisch nodules",HP:0040284,HPO:curators
 * MIM:162200,"NEUROFIBROMATOSIS, TYPE I",HP:0001256,"Intellectual disability, mild",HP:0040283,HPO:curators
 * MIM:162200,"NEUROFIBROMATOSIS, TYPE I",HP:0000316,"Hypertelorism",,HPO:curators
 * MIM:162200,"NEUROFIBROMATOSIS, TYPE I",HP:0000501,"Glaucoma",HP:0040284,HPO:curators
 * }
 * </pre>
 *
 * the following fields will be added:
 * <dl>
 * <dt>{@code "symptom"}</dt>
 * <dd>{@code "HP:0009737"}, {@code HP:0001256}</dd>
 * <dt>{@code "frequency"}</dt>
 * <dd>{@code "HP:0040284"}, {@code HP:0040283}, {@code "HP:0040284"}</dd>
 * </dl>
 *
 * @version $Id$
 * @since 1.3
 */
@Unstable("New API introduced in 1.3")
public abstract class AbstractCSVAnnotationsExtension implements VocabularyExtension
{
    protected static final String ID_KEY = "id";

    /**
     * Data read from the source file. The key of the outer map is the identifier of the term being extended, and the
     * value of the outer map is the data to add to the term. The key of the inner map is the name of the field, while
     * the value of the inner map is the values to add to that field.
     */
    protected Map<String, MultiValuedMap<String, String>> data = new HashMap<>();

    /** Logging helper object. */
    @Inject
    protected Logger logger;

    private AtomicInteger operationsInProgress = new AtomicInteger(0);

    @Override
    public boolean isVocabularySupported(@Nonnull final Vocabulary vocabulary)
    {
        return getTargetVocabularyIds().contains(vocabulary.getIdentifier());
    }

    @Override
    public void indexingStarted(@Nonnull final Vocabulary vocabulary)
    {
        if (this.operationsInProgress.incrementAndGet() == 1) {
            this.data = new HashMap<>();
            try (BufferedReader in = new BufferedReader(
                new InputStreamReader(
                    new URL(getAnnotationSource()).openConnection().getInputStream(), StandardCharsets.UTF_8))) {
                CSVFormat parser = setupCSVParser(vocabulary);
                for (final CSVRecord row : parser.parse(in)) {
                    processCSVRecordRow(row, vocabulary);
                }
            } catch (final IOException ex) {
                this.logger.error("Failed to load annotation source: {}", ex.getMessage());
            }
        }
    }

    @Override
    public void extendTerm(VocabularyInputTerm term, Vocabulary vocabulary)
    {
        MultiValuedMap<String, String> termData = this.data.get(term.getId());
        if (termData == null || termData.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Collection<String>> datum : termData.asMap().entrySet()) {
            if (!datum.getValue().isEmpty()) {
                term.set(datum.getKey(), datum.getValue());
            }
        }
    }

    @Override
    public void indexingEnded(Vocabulary vocabulary)
    {
        if (this.operationsInProgress.decrementAndGet() == 0) {
            this.data = null;
        }
    }

    @Override
    public void extendQuery(SolrQuery query, Vocabulary vocabulary)
    {
        // The base extension doesn't change queries in any way, assuming that the extra fields are only to be stored or
        // explicitly queried, not queried automatically. Override if new fields must automatically be included in
        // queries.
    }

    /**
     * Processes and caches the row data. By default, it simply copies every mapped value from the row. Override if
     * further processing of the data is needed.
     *
     * @param row the {@link CSVRecord data row} to process
     * @param vocabulary the vocabulary being indexed
     */
    protected void processCSVRecordRow(final CSVRecord row, final Vocabulary vocabulary)
    {
        Map<String, String> csvData = row.toMap();
        MultiValuedMap<String, String> termData = this.data.get(row.get(ID_KEY));
        if (termData == null) {
            termData = new ArrayListValuedHashMap<>();
            this.data.put(row.get(ID_KEY), termData);
        }
        for (Map.Entry<String, String> item : csvData.entrySet()) {
            if (!ID_KEY.equals(item.getKey()) && StringUtils.isNoneBlank(item.getKey(), item.getValue())) {
                termData.put(item.getKey(), item.getValue());
            }
        }
    }

    /**
     * Helper method that gets the cell on the specified column, as string, if it exists, without throwing exceptions.
     *
     * @param row the {@link CSVRecord row} currently being processed
     * @param colNumber the number of the column of interest
     * @return the value on the target column, if such value exists, {@code null} otherwise
     */
    protected String getRowItem(@Nonnull final CSVRecord row, final int colNumber)
    {
        if (colNumber < row.size()) {
            return row.get(colNumber);
        }
        return null;
    }

    /**
     * Specifies the vocabularies targeted by this extension.
     *
     * @return a collection of valid {@link Vocabulary#getIdentifier() vocabulary identifiers}
     */
    protected abstract Collection<String> getTargetVocabularyIds();

    /**
     * <p>
     * Sets up a CSV parser so that it accepts the format of the input file, and has names for each column of interest.
     * Giving names to columns is mandatory if the default implementation of {@link #processCSVRecordRow} is used. A
     * column named {@code id} holding the identifier of the target term is required, and only named columns will be
     * automatically extracted as data to add to each {@link #extendTerm extended term}. For example:
     * {@code return CSVFormat.TDF.withHeader("id", null, "symptom")}.
     * </p>
     * <p>
     * If the file has the first row as a header, the it can be automatically parsed as column names with
     * {@code return CSVFormat.TDF.withHeader()}.
     * <p>
     * Columns that aren't mapped, or are mapped to {@code null} or the empty string, will be ignored.
     * <p>
     * If a custom implementation of {@link #processCSVRecordRow} that doesn't rely on named columns is used, then
     * simply specifying the format of the file is enough, for example {@code return CSVFormat.CSV} or
     * {@code return CSVFormat.TDF.withSkipHeaderRecord().withCommentMarker('#')}.
     * </p>
     *
     * @param vocabulary the identifier of the vocabulary being indexed
     * @return a CSV parser that can read the annotation file
     */
    protected abstract CSVFormat setupCSVParser(Vocabulary vocabulary);
}
