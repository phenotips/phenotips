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

import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyExtension;
import org.phenotips.vocabulary.VocabularyInputTerm;

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
 * Implements {@link VocabularyExtension} to annotate {@link org.phenotips.vocabulary.VocabularyInputTerm} from
 * {@link #getTargetVocabularyIds() supported vocabularies} with data from {@link #getAnnotationSource() a tab-separated
 * file}.
 *
 * @version $Id$
 * @since 1.3
 */
public abstract class AbstractCSVAnnotationExtension implements VocabularyExtension
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
     * Processes and caches the row data. By default, it simply copies every mapped value from the row as a single
     * value. Override if further processing of the data is needed.
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
     * Specifies the annotation source URL.
     *
     * @return a valid annotation source URL
     */
    protected abstract String getAnnotationSource();

    /**
     * Sets up a CSV parser so that it accepts the format of the input file, and has names for each column. Giving names
     * to columns is mandatory. A culumn named {@code id} holding the identifier of the target term is required with the
     * default implementation of {@link AbstractCSVAnnotationExtension#processCSVRecordRow(CSVRecord, String)}, and only
     * named column will be automatically extracted as data to add to each
     * {@link #extendTerm(VocabularyInputTerm, Vocabulary) extended term}.
     *
     * @param vocabulary the identifier of the vocabulary being indexed
     * @return a CSV parser that can read the annotation file
     */
    protected abstract CSVFormat setupCSVParser(Vocabulary vocabulary);
}
