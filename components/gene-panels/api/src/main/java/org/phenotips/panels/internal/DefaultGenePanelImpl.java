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
import org.phenotips.data.Patient;
import org.phenotips.panels.GenePanel;
import org.phenotips.panels.TermsForGene;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Default implementation of {@link GenePanel}.
 *
 * @version $Id$
 * @since 1.3M6
 */
public class DefaultGenePanelImpl implements GenePanel
{
    /** Internal label for associated genes. */
    private static final String ASSOCIATED_GENES = "associated_genes";

    /** The "size" JSON property label. */
    private static final String SIZE = "size";

    /** The "totalSize" JSON property label. */
    private static final String TOTAL_SIZE = "totalSize";

    /** The "genes" JSON property label. */
    private static final String GENES_LABEL = "genes";

    /** The "ensembl_gene_id" label. */
    private static final String ENSEMBL_ID_LABEL = "ensembl_gene_id";

    /** HPO vocabulary label. */
    private static final String HPO_LABEL = "hpo";

    /** HGNC vocabulary label. */
    private static final String HGNC_LABEL = "hgnc";

    private static final String PRESENT_LABEL = "present";

    private static final String ABSENT_LABEL = "absent";

    /** The hgnc vocabulary. */
    private final Vocabulary hgnc;

    /** The set of terms observed to be present. */
    private final Set<VocabularyTerm> presentTerms;

    /** The set of terms observed to be absent. */
    private final Set<VocabularyTerm> absentTerms;

    /** An ordered list of objects containing gene count data. */
    private final List<TermsForGene> termsForGeneList;

    /** The number of gene objects stored in this gene panel. */
    private final int panelSize;

    /**
     * Simple constructor, passing in a collection of {@code presentTerms} and a collection of {@code absentTerms}, as
     * {@link VocabularyTerm} objects, and a {@link VocabularyManager}.
     *
     * @param presentTerms a collection of feature identifiers, as string, that are present
     * @param absentTerms a collection of feature identifiers, as string, that are absent
     * @param vocabularyManager the {@link VocabularyManager} for accessing the required ontologies
     */
    public DefaultGenePanelImpl(@Nonnull final Collection<VocabularyTerm> presentTerms,
        @Nonnull final Collection<VocabularyTerm> absentTerms, @Nonnull final VocabularyManager vocabularyManager)
    {
        this.hgnc = vocabularyManager.getVocabulary(HGNC_LABEL);

        this.presentTerms = Collections.unmodifiableSet(new HashSet<>(presentTerms));
        this.absentTerms = Collections.unmodifiableSet(new HashSet<>(absentTerms));
        this.termsForGeneList = buildTermsForGeneList();
        this.panelSize = this.termsForGeneList.size();
    }

    /**
     * Constructor passing a collection of {@link Feature} objects and a {@link VocabularyManager}.
     *
     * @param features a collection of {@link Feature} objects
     * @param vocabularyManager the {@link VocabularyManager} for accessing the required ontologies
     */
    public DefaultGenePanelImpl(@Nonnull final Collection<? extends Feature> features,
        @Nonnull final VocabularyManager vocabularyManager)
    {
        final Vocabulary featureVocabulary = vocabularyManager.getVocabulary(HPO_LABEL);
        final Map<String, Set<VocabularyTerm>> termData = buildTermsFromFeatures(features, featureVocabulary);

        this.hgnc = vocabularyManager.getVocabulary(HGNC_LABEL);

        this.presentTerms = termData.get(PRESENT_LABEL);
        this.absentTerms = termData.get(ABSENT_LABEL);
        this.termsForGeneList = buildTermsForGeneList();
        this.panelSize = this.termsForGeneList.size();
    }

    /**
     * Constructor passing in a {@link Patient} object from which feature data will be extracted, and a
     * {@link VocabularyManager}.
     *
     * @param patient a patient of interest
     * @param vocabularyManager the {@link VocabularyManager} for accessing the required ontologies
     */
    public DefaultGenePanelImpl(@Nonnull final Patient patient, @Nonnull final VocabularyManager vocabularyManager)
    {
        this(patient.getFeatures(), vocabularyManager);
    }

    /**
     * Builds a map containing a set of {@link #PRESENT_LABEL} {@link VocabularyTerm} objects and a set of
     * {@link #ABSENT_LABEL} {@link VocabularyTerm} objects.
     *
     * @param features a collection of {@link Feature} objects
     * @param featureVocabulary a {@link Vocabulary} object that feature data belongs to
     * @return a map containing sets of {@link #PRESENT_LABEL} and {@link #PRESENT_LABEL} {@link VocabularyTerm} objects
     */
    private Map<String, Set<VocabularyTerm>> buildTermsFromFeatures(
        @Nonnull final Collection<? extends Feature> features, @Nonnull final Vocabulary featureVocabulary)
    {
        final Set<VocabularyTerm> presentTermsFromFeatures = new HashSet<>();
        final Set<VocabularyTerm> absentTermsFromFeatures = new HashSet<>();
        for (final Feature feature : features) {
            final VocabularyTerm term = featureVocabulary.getTerm(feature.getValue());
            if (feature.isPresent() && term != null) {
                presentTermsFromFeatures.add(term);
            } else {
                absentTermsFromFeatures.add(term);
            }
        }
        final Map<String, Set<VocabularyTerm>> terms = new HashMap<>();
        terms.put(PRESENT_LABEL, Collections.unmodifiableSet(presentTermsFromFeatures));
        terms.put(ABSENT_LABEL, Collections.unmodifiableSet(absentTermsFromFeatures));
        return Collections.unmodifiableMap(terms);
    }

    /**
     * Builds a list of {@link TermsForGene} objects for a given set of {@link #getPresentTerms()}. The
     * {@link #getAbsentTerms()} are ignored in this version of {@link GenePanel}.
     *
     * @return a list of {@link TermsForGene} objects, sorted in descending order or relevance
     */
    private List<TermsForGene> buildTermsForGeneList()
    {
        // Map storing count data for all the genes.
        final Map<String, TermsForGene> termsForGeneMap = new HashMap<>();

        // Update the data for all HPO identifiers.
        for (final VocabularyTerm term : getPresentTerms()) {
            final List<String> storedGenes = getGeneDataFromTerm(term);
            addTermForGene(term, storedGenes, termsForGeneMap);
        }

        return buildSortedGeneDataFromMap(termsForGeneMap);
    }

    /**
     * Given a {@code termsForGeneMap map} of gene symbol to {@link TermsForGene} objects, returns a sorted
     * {@link #getTermsForGeneList() list of genes}, in descending order of relevance.
     *
     * @param termsForGeneMap a map containing {@link TermsForGene} objects for each gene
     * @return a list of {@link TermsForGene} objects, sorted in descending order or relevance
     */
    private List<TermsForGene> buildSortedGeneDataFromMap(@Nonnull final Map<String, TermsForGene> termsForGeneMap)
    {
        final List<Map.Entry<String, TermsForGene>> phenotypesForGeneEntries =
            new LinkedList<>(termsForGeneMap.entrySet());
        Collections.sort(phenotypesForGeneEntries, new Comparator<Map.Entry<String, TermsForGene>>()
        {
            @Override
            public int compare(final Map.Entry<String, TermsForGene> o1,
                final Map.Entry<String, TermsForGene> o2)
            {
                final TermsForGene firstTerm = o1.getValue();
                final TermsForGene secondTerm = o2.getValue();
                // First compare by count, in descending order.
                int result = new Integer(secondTerm.getCount()).compareTo(firstTerm.getCount());
                // Second, if firstTerm and secondTerm have the same count, compare by natural order.
                if (result == 0) {
                    result = compareByTermList(firstTerm.getTerms().iterator(), secondTerm.getTerms().iterator());
                }
                if (result == 0) {
                    result = o1.getKey().compareTo(o2.getKey());
                }
                return result;
            }
        });

        final List<TermsForGene> sortedGeneCountsList = new ArrayList<>();
        for (final Map.Entry<String, TermsForGene> entry : phenotypesForGeneEntries) {
            sortedGeneCountsList.add(entry.getValue());
        }
        return Collections.unmodifiableList(sortedGeneCountsList);
    }

    /**
     * Compares two {@link Iterator<VocabularyTerm>} alphabetically.
     *
     * @param first the {@link Iterator<VocabularyTerm>} that {@code second} is being compared to
     * @param second the {@link Iterator<VocabularyTerm>} that is being compared
     * @return {@code 0} if {@code first} and {@code second} are equivalent, a value less than {@code 0} if
     *         {@code first} should be ahead of {@code second}, a value greater than {@code 0} if {@code second} should
     *         be ahead of {@code first}
     */
    private int compareByTermList(final Iterator<VocabularyTerm> first, final Iterator<VocabularyTerm> second)
    {
        // The two lists are equivalent.
        if (!first.hasNext()) {
            return 0;
        }
        // Both our iterators are of the same size.
        final String firstName = getTermName(first.next());
        final String secondName = getTermName(second.next());
        final int compValue = firstName.compareTo(secondName);
        // If compValue is 0 then we need to compare by next item.
        if (compValue == 0) {
            return compareByTermList(first, second);
        }
        // If compValue is not 0, then we figured out which item should be sorted ahead.
        return compValue;
    }

    /**
     * Returns the {@link VocabularyTerm#getName() term name} if it is specified, otherwise returns
     * {@link VocabularyTerm#getId()}.
     *
     * @param term the {@link VocabularyTerm} of interest
     * @return the name of the term if specified, the ID otherwise
     */
    private String getTermName(final VocabularyTerm term)
    {
        final String name = term.getName();
        return StringUtils.isNotBlank(name) ? name : term.getId();
    }

    /**
     * For each gene in a list of {@code genes}, adds the gene as key and {@code term} as value to the provided
     * {@code termsForGeneMap}.
     *
     * @param term the {@link VocabularyTerm HPO vocabulary term} associated with the provided list of {@code genes}
     * @param genes a list of gene symbols associated with {@code term}
     * @param termsForGeneMap a map containing {@link TermsForGene} objects for each gene
     */
    private void addTermForGene(@Nonnull final VocabularyTerm term, @Nonnull final List<String> genes,
        @Nonnull final Map<String, TermsForGene> termsForGeneMap)
    {
        for (final String gene : genes) {
            final TermsForGene termsForGene = termsForGeneMap.containsKey(gene)
                ? termsForGeneMap.get(gene)
                : createTermsForGeneObj(termsForGeneMap, gene);
            ((DefaultTermsForGeneImpl) termsForGene).addTerm(term);
        }
    }

    /**
     * Adds a new {@link TermsForGene} object for {@code geneSymbol} to the {@code geneCountsMap map}.
     *
     * @param geneCountsMap a {@link Map} containing counts data for each {@code geneSymbol}
     * @param geneSymbol the GeneCards gene symbol for the gene being counted
     * @return the newly created {@link TermsForGene} object for {@code geneSymbol}
     */
    private TermsForGene createTermsForGeneObj(@Nonnull final Map<String, TermsForGene> geneCountsMap,
        @Nonnull final String geneSymbol)
    {
        final String geneId = getGeneId(geneSymbol);
        final TermsForGene termsForGene = new DefaultTermsForGeneImpl(geneSymbol, geneId);
        geneCountsMap.put(geneSymbol, termsForGene);
        return termsForGene;
    }

    /**
     * Tries to obtain the preferred gene ID, given {@code geneSymbol}.
     *
     * @param geneSymbol the GeneCards gene symbol
     * @return the preferred gene ID, or geneSymbol if no preferred ID is recorded
     */
    private String getGeneId(@Nonnull final String geneSymbol)
    {
        final VocabularyTerm geneTerm = this.hgnc.getTerm(geneSymbol);

        if (geneTerm != null) {
            @SuppressWarnings("unchecked")
            final List<String> geneIdList = (List<String>) geneTerm.get(ENSEMBL_ID_LABEL);
            return CollectionUtils.isEmpty(geneIdList) ? geneSymbol : geneIdList.get(0);
        }
        return geneSymbol;
    }

    /**
     * Returns a list of {@link VocabularyTerm genes}, given an {@link VocabularyTerm HPO term}.
     *
     * @param term an HPO {@link VocabularyTerm}
     * @return a list of {@link VocabularyTerm genes} associated with the provided {@code term}, or an empty list
     */
    private List<String> getGeneDataFromTerm(@Nonnull final VocabularyTerm term)
    {
        @SuppressWarnings("unchecked")
        final List<String> geneList = (List<String>) term.get(ASSOCIATED_GENES);
        return CollectionUtils.isNotEmpty(geneList) ? geneList : Collections.<String>emptyList();
    }

    @Override
    public Set<VocabularyTerm> getPresentTerms()
    {
        return this.presentTerms;
    }

    @Override
    public Set<VocabularyTerm> getAbsentTerms()
    {
        return this.absentTerms;
    }

    @Override
    public List<TermsForGene> getTermsForGeneList()
    {
        return this.termsForGeneList;
    }

    @Override
    public JSONObject toJSON()
    {
        final JSONObject jsonObject = buildPhenotypesForGeneJSON(0, this.size());
        return jsonObject.put(SIZE, this.size()).put(TOTAL_SIZE, this.size());
    }

    @Override
    public JSONObject toJSON(final int fromIndex, final int toIndex)
    {
        final JSONObject jsonObject = buildPhenotypesForGeneJSON(fromIndex, toIndex);
        return jsonObject.put(SIZE, toIndex - fromIndex).put(TOTAL_SIZE, this.size());
    }

    /**
     * Builds a {@link JSONObject} that contains {@link #termsForGeneList a list of genes} starting from
     * {@code fromIndex}, inclusive, and up to {@code toIndex}, exclusive, as a {@link JSONArray}. Will throw an
     * {@link IndexOutOfBoundsException} if one or both indices are out of bounds.
     *
     * @param fromIndex the starting position
     * @param toIndex the end position (exclusive)
     * @return a {@link JSONObject} containing the requested subset of {@link GenePanel} data
     * @throws IndexOutOfBoundsException if (<tt>fromIndex &lt; 0 || toIndex &gt; {@link #size()} ||
     *         fromIndex &gt; toIndex</tt>)
     */
    private JSONObject buildPhenotypesForGeneJSON(final int fromIndex, final int toIndex)
    {
        final JSONObject jsonObject = new JSONObject();
        final JSONArray jsonArray = new JSONArray();
        for (int i = fromIndex; i < toIndex; i++) {
            jsonArray.put(this.termsForGeneList.get(i).toJSON());
        }

        jsonObject.put(GENES_LABEL, jsonArray);
        return jsonObject;
    }

    @Override
    public int size()
    {
        return this.panelSize;
    }
}
