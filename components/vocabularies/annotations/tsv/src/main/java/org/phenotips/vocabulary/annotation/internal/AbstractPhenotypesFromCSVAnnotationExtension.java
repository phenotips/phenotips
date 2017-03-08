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
import org.phenotips.vocabulary.VocabularyInputTerm;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import java.util.Collection;
import java.util.HashSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

/**
 * Extends {@link AbstractCSVAnnotationExtension} to annotate a {@link VocabularyInputTerm} with its associated
 * phenotypes. Two annotations are added: one contains phenotypes directly from the annotation source
 * (labeled {@link #getDirectPhenotypesLabel()}), the other one contains the phenotypes directly from the annotation
 * source, as well as their ancestor phenotypes (labeled {@link #getAllAncestorPhenotypesLabel()}).
 * @version $Id$
 * @since 1.4
 */
public abstract class AbstractPhenotypesFromCSVAnnotationExtension extends AbstractCSVAnnotationExtension
{
    /** The vocabulary manager for easy access to various vocabularies. */
    @Inject
    private VocabularyManager vocabularyManager;

    /** The name of the field where ancestors are stored. */
    private static final String ANCESTORS_LABEL = "term_category";

    /** A map of disorder to symptoms, as outlined in the annotation source. */
    private final MultiValuedMap<String, String> directPhenotypes = new HashSetValuedHashMap<>();

    /** A map of disorder to symptoms from the annotation source, and the inferred ancestor phenotypes. */
    private final MultiValuedMap<String, String> allAncestorPhenotypes = new HashSetValuedHashMap<>();

    @Override
    protected void processCSVRecordRow(@Nonnull final CSVRecord row, @Nonnull final String vocabularyId)
    {
        // The annotation source file contains data for several disorder databases. Only want to look at data that is
        // relevant for the current vocabulary.
        final String dbName = getRowItem(row, getDBNameColNumber());
        if (StringUtils.isNotBlank(dbName) && dbName.equalsIgnoreCase(vocabularyId)) {
            final String diseaseId = getRowItem(row, getDiseaseColNumber());
            final String symptomId = getRowItem(row, getPhenotypeColNumber());
            if (StringUtils.isNotBlank(diseaseId) && StringUtils.isNotBlank(symptomId)) {
                this.directPhenotypes.put(diseaseId, symptomId);
                final Collection<String> terms = getSelfAndAncestorTermIds(symptomId);
                this.allAncestorPhenotypes.putAll(diseaseId, terms);
            }
        }
    }

    @Override
    protected void clearData()
    {
        this.directPhenotypes.clear();
        this.allAncestorPhenotypes.clear();
    }

    @Override
    public void extendTerm(@Nonnull final VocabularyInputTerm diseaseTerm, @Nonnull final Vocabulary vocabulary)
    {
        final String diseaseTermId = getTermId(diseaseTerm);
        final Collection<String> directIds = this.directPhenotypes.get(diseaseTermId);
        extendTerm(diseaseTerm, getDirectPhenotypesLabel(), directIds);

        final Collection<String> allIds = this.allAncestorPhenotypes.get(diseaseTermId);
        extendTerm(diseaseTerm, getAllAncestorPhenotypesLabel(), allIds);
    }

    /**
     * Extends the {@code term} with a {@code label} and a {@code value}.
     *
     * @param term the {@link VocabularyInputTerm term} currently being added to the index
     * @param label the field label to add to {@code term}
     * @param value the field value to add to {@code term}
     */
    private void extendTerm(@Nonnull final VocabularyInputTerm term, @Nonnull final String label,
        @Nullable final Collection<String> value)
    {
        if (CollectionUtils.isNotEmpty(value)) {
            term.set(label, value);
        }
    }

    /**
     * Gets the ID of the {@code term} sans prefix.
     *
     * @param term the {@link VocabularyInputTerm} of interest
     * @return {@link VocabularyInputTerm#getId()}, sans vocabulary-specific prefix
     */
    private String getTermId(@Nonnull final VocabularyInputTerm term)
    {
        final String rawId = term.getId();
        final String noPrefixId = StringUtils.substringAfter(rawId, ":");
        return StringUtils.isNotBlank(noPrefixId) ? noPrefixId : rawId;
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
            final Collection<String> retrievedAncestorIds = (Collection<String>) vocabularyTerm.get(ANCESTORS_LABEL);
            if (CollectionUtils.isNotEmpty(retrievedAncestorIds)) {
                ancestors.addAll(retrievedAncestorIds);
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

    /**
     * Gets the column number where the disease database name is stored.
     *
     * @return the column number where the disease database name is stored
     */
    protected abstract int getDBNameColNumber();

    /**
     * Gets the column number where the disease ID is stored.
     *
     * @return the column number where the disease ID is stored
     */
    protected abstract int getDiseaseColNumber();

    /**
     * Gets the column number where the phenotype ID is stored.
     *
     * @return the column number where the phenotype ID is stored
     */
    protected abstract int getPhenotypeColNumber();
}
