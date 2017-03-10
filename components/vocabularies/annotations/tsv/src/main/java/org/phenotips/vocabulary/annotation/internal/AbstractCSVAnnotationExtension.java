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
package org.phenotips.vocabulary.annotation.internal;

import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyExtension;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;

/**
 * Implements {@link VocabularyExtension} to annotate {@link org.phenotips.vocabulary.VocabularyInputTerm} from
 * {@link #getTargetVocabularyIds() supported vocabularies} with data from {@link #getAnnotationSource() a tab-separated
 * file}.
 * @version $Id$
 * @since 1.3
 */
public abstract class AbstractCSVAnnotationExtension implements VocabularyExtension
{
    /** Encoding for annotation file. */
    private static final String ENCODING = "UTF-8";

    /** The logger for the annotator. */
    @Inject
    private Logger logger;

    @Override
    public boolean isVocabularySupported(@Nonnull final Vocabulary vocabulary)
    {
        return getTargetVocabularyIds().contains(vocabulary.getIdentifier());
    }

    @Override
    public void indexingStarted(@Nonnull final Vocabulary vocabulary)
    {
        final String vocabularyId = vocabulary.getIdentifier();
        try (final BufferedReader in = new BufferedReader(
            new InputStreamReader(
                new URL(getAnnotationSource()).openConnection().getInputStream(), ENCODING))) {
            for (final CSVRecord row : CSVFormat.TDF.withSkipHeaderRecord().parse(in)) {
                processCSVRecordRow(row, vocabularyId);
            }
        } catch (final IOException ex) {
            this.logger.error("Failed to load annotation source: {}", ex.getMessage());
        }
    }

    @Override
    public void indexingEnded(@Nonnull final Vocabulary vocabulary)
    {
        clearData();
    }

    /**
     * Gets the row item, as string, specified by {@code colName}.
     *
     * @param row the {@link CSVRecord row} currently being processed
     * @param colNumber the number of the column of interest
     * @return the value associated with {@code colName} for {@code row}, if such value exists, null otherwise
     */
    String getRowItem(@Nonnull final CSVRecord row, final int colNumber)
    {
        if (colNumber < row.size()) {
            return row.get(colNumber);
        }
        return null;
    }

    /**
     * Specifies the vocabularies targeted by this annotator.
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
     * Processes and caches the {@link CSVRecord row} data.
     *
     * @param row the {@link CSVRecord data row}
     * @param vocabularyId the vocabulary identifier, as string
     */
    protected abstract void processCSVRecordRow(CSVRecord row, String vocabularyId);

    /**
     * Clears the cached annotation data.
     */
    protected abstract void clearData();
}
