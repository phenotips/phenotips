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
package org.phenotips.vocabulary.internal.hpoannotations;

import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyInputTerm;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;
import org.phenotips.vocabulary.internal.AbstractCSVAnnotationExtension;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

/**
 * Extends {@link AbstractCSVAnnotationExtension} to annotate a {@link VocabularyInputTerm} with its associated
 * phenotypes. Two annotations are added: one contains phenotypes directly from the annotation source (labeled
 * {@link #getDirectPhenotypesLabel()}), the other one contains the phenotypes directly from the annotation source, as
 * well as their ancestor phenotypes (labeled {@link #getAllAncestorPhenotypesLabel()}).
 *
 * @version $Id$
 * @since 1.3
 */
public abstract class AbstractHPOAnnotationsExtension extends AbstractCSVAnnotationExtension
{
    private static final int VOCABULARY_ID_COLUMN = 0;

    private static final int TERM_ID_COLUMN = 1;

    private static final int PHENOTYPE_COLUMN = 4;

    private static final Collection<String> TARGET_VOCABULARIES =
        Collections.unmodifiableList(Arrays.asList("omim", "orphanet", "decipher"));

    /** The vocabulary manager for easy access to various vocabularies. */
    @Inject
    private VocabularyManager vocabularyManager;

    @Override
    protected Collection<String> getTargetVocabularyIds()
    {
        return TARGET_VOCABULARIES;
    }

    @Override
    protected void processCSVRecordRow(@Nonnull final CSVRecord row, @Nonnull final Vocabulary vocabulary)
    {
        // The annotation source file contains data for several disorder databases. Only want to look at data that is
        // relevant for the current vocabulary.
        final String dbName = getRowItem(row, VOCABULARY_ID_COLUMN);
        if (StringUtils.isNotBlank(dbName)) {
            String diseaseId = getRowItem(row, TERM_ID_COLUMN);
            final String symptomId = getRowItem(row, PHENOTYPE_COLUMN);
            if (StringUtils.isNotBlank(diseaseId) && StringUtils.isNotBlank(symptomId)) {
                if (!"OMIM".equals(dbName)) {
                    diseaseId = dbName + ':' + diseaseId;
                }
                MultiValuedMap<String, String> termData = this.data.get(diseaseId);
                if (termData == null) {
                    termData = new HashSetValuedHashMap<>();
                    this.data.put(diseaseId, termData);
                }

                termData.put(getDirectPhenotypesLabel(), symptomId);
                termData.putAll(getAllAncestorPhenotypesLabel(), getSelfAndAncestorTermIds(symptomId));
            }
        }
    }

    @Override
    protected CSVFormat setupCSVParser(Vocabulary vocabulary)
    {
        return CSVFormat.TDF;
    }

    /**
     * Returns a set with {@code termId} and the IDs of its ancestor terms.
     *
     * @param termId the term ID
     * @return a set with {@code termId} and the IDs of all its ancestor terms
     */
    private Collection<String> getSelfAndAncestorTermIds(@Nonnull final String termId)
    {
        // The collection that will contain termId and the IDs of its ancestors.
        final Collection<String> ancestors = new HashSet<>();
        ancestors.add(termId);

        // Find the term in the vocabulary, and if it exists, retrieve its ancestors, if any.
        final VocabularyTerm vocabularyTerm = this.vocabularyManager.resolveTerm(termId);
        if (vocabularyTerm != null) {
            for (VocabularyTerm ancestor : vocabularyTerm.getAncestors()) {
                ancestors.add(ancestor.getId());
            }
        } else {
            this.logger.warn("Could not find term with ID: {} in indexed vocabularies.", termId);
        }
        return ancestors;
    }

    /**
     * Gets the label for the field that contains phenotypes obtained directly from {@link #getAnnotationSource()}.
     *
     * @return the label for the field as string
     */
    protected abstract String getDirectPhenotypesLabel();

    /**
     * Gets the label for the field that contains phenotypes obtained directly from {@link #getAnnotationSource()} as
     * well as their ancestors.
     *
     * @return the label for the field as string
     */
    protected abstract String getAllAncestorPhenotypesLabel();
}
