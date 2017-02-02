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

import org.phenotips.obo2solr.TermData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for parsing the hpo source, while gathering annotations from HPO-gene mapping from the Human Phenotype
 * Ontology.
 *
 * @version $Id$
 * @since 1.3M4
 */
public class HpoAnnotationSourceParser
{
    private static final String PHENOTYPE_TO_GENES_ANNOTATIONS_URL =
            "http://compbio.charite.de/jenkins/job/hpo.annotations.monthly/lastStableBuild/artifact/annotation/"
                    + "ALL_SOURCES_ALL_FREQUENCIES_phenotype_to_genes.txt";

    /** Encoding for annotation file. */
    private static final String ENCODING = "UTF-8";

    /** The key under which the genes associated with the HPO term will be stored. */
    private static final String GENES = "associated_genes";

    /** The provided HPO data. */
    private final Map<String, TermData> data;

    /** The logging object for the class. */
    private final Logger logger = LoggerFactory.getLogger(HpoAnnotationSourceParser.class);

    /**
     * Default constructor that takes a {@link Map} containing {@link TermData} for the HPO vocabulary, and adds
     * gene annotations to relevant HPO terms.
     *
     * @param hpoData a {@link Map} containing HPO vocabulary data
     */
    HpoAnnotationSourceParser(@Nullable final Map<String, TermData> hpoData)
    {
        this.data = hpoData;
        // Load gene data for the HPO, only if we have HPO vocabulary data available.
        if (hpoData != null && !hpoData.isEmpty()) {
            loadGenes();
        }
    }

    /**
     * Reads a CSV file containing phenotype to gene annotations, and adds this data to {@link #data}.
     */
    private void loadGenes()
    {
        try (final BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        new URL(PHENOTYPE_TO_GENES_ANNOTATIONS_URL).openConnection().getInputStream(), ENCODING))) {
            for (final CSVRecord row : CSVFormat.TDF.withHeader().parse(in)) {
                final String termName = getRowItem(row, 0);
                final TermData termData = this.data.get(termName);
                final String geneSymbol = getRowItem(row, 3);
                linkGeneToPhenotype(termData, geneSymbol);
            }
        } catch (final IOException e) {
            this.logger.error("Failed to load HPO-Gene links: {}", e.getMessage(), e);
        }
    }

    /**
     * Links a {@code geneSymbol gene} to its corresponding {@code termData} object containing HPO term data.
     *
     * @param termData {@link TermData} object containing HPO term data
     * @param geneSymbol the name of the gene to be added to the {@link #data} data set
     */
    private void linkGeneToPhenotype(@Nullable final TermData termData, @Nullable final String geneSymbol)
    {
        if (termData != null && StringUtils.isNotBlank(geneSymbol)) {
            termData.addTo(GENES, geneSymbol);
        }
    }

    /**
     * Returns the {@link #data HPO vocabulary data}.
     * @return the HPO vocabulary {@link #data}
     */
    public Map<String, TermData> getData()
    {
        return this.data;
    }

    /**
     * Gets the row item specified by {@code colName}.
     *
     * @param row the {@link CSVRecord} currently being processed
     * @param colNumber the number of the column of interest
     * @return the value associated with {@code colName} for {@code row}, if such value exists, null otherwise
     */
    private String getRowItem(@Nonnull final CSVRecord row, final int colNumber)
    {
        if (colNumber < row.size()) {
            return row.get(colNumber);
        }
        return null;
    }
}
