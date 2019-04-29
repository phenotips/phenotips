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
package org.phenotips.panels.internal;

import org.phenotips.data.Feature;
import org.phenotips.data.Gene;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;

/**
 * A patient data adapter stores patient data in a format that can be consumed by the
 * {@link org.phenotips.panels.GenePanel} class, and provides getter methods for retrieval of present terms
 * {@link #getPresentTerms()}, absent terms {@link #getAbsentTerms()}, and rejected genes {@link #getRejectedGenes()}.
 *
 * @version $Id$
 * @since 1.4
 */
final class PatientDataAdapter
{
    /** The set of terms that are present. */
    private final Set<VocabularyTerm> presentTerms;

    /** The set of terms that are absent. */
    private final Set<VocabularyTerm> absentTerms;

    /** The set of gene terms that have been marked as tested negative or rejected candidate. */
    private final Set<VocabularyTerm> rejectedGenes;

    private PatientDataAdapter(@Nonnull final AdapterBuilder builder)
    {
        this.presentTerms = builder.presentTerms;
        this.absentTerms = builder.absentTerms;
        this.rejectedGenes = builder.rejectedGenes != null
            ? builder.rejectedGenes
            : Collections.emptySet();
    }

    /**
     * Retrieves {@link VocabularyTerm terms} present in patient.
     *
     * @return a collection of present {@link VocabularyTerm}
     */
    Collection<VocabularyTerm> getPresentTerms()
    {
        return Collections.unmodifiableSet(this.presentTerms);
    }

    /**
     * Retrieves {@link VocabularyTerm terms} absent in patient.
     *
     * @return a collection of absent {@link VocabularyTerm}
     */
    Collection<VocabularyTerm> getAbsentTerms()
    {
        return Collections.unmodifiableSet(this.absentTerms);
    }

    /**
     * Retrieves {@link VocabularyTerm genes} marked as rejected in patient.
     *
     * @return a collection of {@link VocabularyTerm genes} marked as rejected in patient
     */
    Collection<VocabularyTerm> getRejectedGenes()
    {
        return Collections.unmodifiableSet(this.rejectedGenes);
    }

    /**
     * A builder for the {@link PatientDataAdapter}. The {@link AdapterBuilder} retrieves data from {@link Patient}, in
     * a format that can be consumed by the {@link org.phenotips.panels.GenePanel} class. Present and absent term data
     * is retrieved by default on {@link #build()}. Other data, however, is only retrieved when prompted, by calling
     * appropriate methods (e.g. {@link #withRejectedGenes()}) prior to calling {@link #build()}.
     */
    static class AdapterBuilder
    {
        /** A key denoting present terms. */
        private static final String PRESENT_TERMS = "presentTerms";

        /** A key denoting absent terms. */
        private static final String ABSENT_TERMS = "absentTerms";

        /** The internal {@link Patient} label for global qualifiers. */
        private static final String GLOBAL_QUALIFIERS = "global-qualifiers";

        /** The internal {@link Patient} label for genes. */
        private static final String GENES = "genes";

        /** The internal {@link PatientData} label for tested negative genes. */
        private static final String REJECTED = "rejected";

        /** The internal {@link PatientData} label for rejected candidate genes. */
        private static final String REJECTED_CANDIDATE = "rejected_candidate";

        /** The hgnc vocabulary identifier. */
        private static final String HGNC = "hgnc";

        /** The patient of interest. */
        private final Patient patient;

        /** A vocabulary manager for interacting with various available vocabularies. */
        private final VocabularyManager vocabularyManager;

        /** A set of present {@link VocabularyTerm terms}. */
        private Set<VocabularyTerm> presentTerms;

        /** A set of absent {@link VocabularyTerm terms}. */
        private Set<VocabularyTerm> absentTerms;

        /** A set of rejected {@link VocabularyTerm gene terms}. */
        private Set<VocabularyTerm> rejectedGenes;

        /**
         * The default constructor for the adapter builder.
         *
         * @param patient the {@link Patient} object from which data will be extracted
         * @param vocabularyManager a {@link VocabularyManager}
         */
        AdapterBuilder(@Nonnull final Patient patient, @Nonnull final VocabularyManager vocabularyManager)
        {
            this.patient = patient;
            this.vocabularyManager = vocabularyManager;
        }

        /**
         * Prompts retrieval of rejected {@link VocabularyTerm gene} objects from {@link #patient}.
         *
         * @return {@link AdapterBuilder self}
         */
        AdapterBuilder withRejectedGenes()
        {
            final PatientData<Gene> genes = this.patient.getData(GENES);
            this.rejectedGenes = extractRejectedGenes(genes);
            return this;
        }

        /**
         * Builds a {@link PatientDataAdapter} object.
         *
         * @return a {@link PatientDataAdapter} object with the set data
         */
        PatientDataAdapter build()
        {
            setTerms();
            return new PatientDataAdapter(this);
        }

        /**
         * Sets present and absent {@link VocabularyTerm} vocabulary terms from {@link #patient}.
         */
        private void setTerms()
        {
            final Set<? extends Feature> features = this.patient.getFeatures();
            final PatientData<List<VocabularyTerm>> qualifiers = this.patient.getData(GLOBAL_QUALIFIERS);
            final Map<String, Set<VocabularyTerm>> terms = extractTerms(features, qualifiers);
            this.presentTerms = terms.get(PRESENT_TERMS);
            this.absentTerms = terms.get(ABSENT_TERMS);
        }

        /**
         * Extracts rejected genes from provided {@code genes} data.
         *
         * @param genes a {@link PatientData} object containing gene data for the {@link #patient}
         * @return a set of rejected {@link VocabularyTerm genes}
         */
        private Set<VocabularyTerm> extractRejectedGenes(@Nullable final PatientData<Gene> genes)
        {
            if (genes == null) {
                return Collections.emptySet();
            }
            final Set<VocabularyTerm> rejected = new HashSet<>();
            final Vocabulary hgnc = this.vocabularyManager.getVocabulary(HGNC);
            for (final Gene gene : genes) {
                if (REJECTED.equals(gene.getStatus()) || REJECTED_CANDIDATE.equals(gene.getStatus())) {
                    final String geneID = gene.getId();
                    final VocabularyTerm geneObj = hgnc.getTerm(geneID);
                    CollectionUtils.addIgnoreNull(rejected, geneObj);
                }
            }
            return rejected;
        }

        /**
         * Extracts present and absent terms from provided {@code features} and {@code qualifiers}.
         *
         * @param features a set of patient features
         * @param qualifiers global patient qualifiers
         * @return a map of present and absent {@link VocabularyTerm terms}
         */
        private Map<String, Set<VocabularyTerm>> extractTerms(
            @Nullable final Set<? extends Feature> features,
            @Nullable final PatientData<List<VocabularyTerm>> qualifiers)
        {
            final Map<String, Set<VocabularyTerm>> terms = new HashMap<>();
            final Set<VocabularyTerm> present = new HashSet<>();
            final Set<VocabularyTerm> absent = new HashSet<>();
            if (qualifiers != null) {
                addTermsFromQualifiers(qualifiers, present);
            }
            if (features != null) {
                addTermsFromFeatures(features, present, absent);
            }
            terms.put(PRESENT_TERMS, present);
            terms.put(ABSENT_TERMS, absent);
            return terms;
        }

        /**
         * Adds {@code qualifiers global qualifiers} to a set of present {@code retrievedPresentTerms terms}.
         *
         * @param qualifiers a {@link PatientData} object that contains lists of global {@link VocabularyTerm}
         *            qualifiers
         * @param retrievedPresentTerms a set of present {@link VocabularyTerm} objects
         */
        private void addTermsFromQualifiers(@Nonnull final PatientData<List<VocabularyTerm>> qualifiers,
            @Nonnull final Set<VocabularyTerm> retrievedPresentTerms)
        {
            for (final List<VocabularyTerm> qualifierTerms : qualifiers) {
                retrievedPresentTerms.addAll(qualifierTerms);
            }
        }

        /**
         * Converts and adds {@code features} to sets of present {@code retrievedPresentTerms terms} and absent
         * {@code retrievedAbsentTerms terms}.
         *
         * @param features a collection of {@link Feature} objects
         * @param retrievedPresentTerms a set of present {@link VocabularyTerm} objects
         * @param retrievedAbsentTerms a set of absent {@link VocabularyTerm} objects
         */
        private void addTermsFromFeatures(
            @Nonnull final Collection<? extends Feature> features,
            @Nonnull final Set<VocabularyTerm> retrievedPresentTerms,
            @Nonnull final Set<VocabularyTerm> retrievedAbsentTerms)
        {
            for (final Feature feature : features) {
                final VocabularyTerm term = this.vocabularyManager.resolveTerm(feature.getValue());
                if (term != null) {
                    if (feature.isPresent()) {
                        retrievedPresentTerms.add(term);
                    } else {
                        retrievedAbsentTerms.add(term);
                    }
                }
            }
        }
    }
}
