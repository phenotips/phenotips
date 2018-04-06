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

import org.phenotips.panels.GenePanel;
import org.phenotips.panels.MatchCount;
import org.phenotips.panels.TermsForGene;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Default implementation of {@link GenePanel}.
 *
 * @version $Id$
 * @since 1.3
 */
public class DefaultGenePanelImpl implements GenePanel
{
    /** Internal label for associated genes. */
    private static final String ASSOCIATED_GENES = "associated_genes";

    /** The "size" JSON property label. */
    private static final String RETURNED_SIZE = "returnedrows";

    /** The "totalSize" JSON property label. */
    private static final String TOTAL_SIZE = "totalrows";

    /** The "genes" JSON property label. */
    private static final String GENE_ROWS_LABEL = "rows";

    /** The "ensembl_gene_id" label. */
    private static final String ENSEMBL_ID_LABEL = "ensembl_gene_id";

    /** The gene "symbol" label. */
    private static final String SYMBOL_LABEL = "symbol";

    /** HGNC vocabulary label. */
    private static final String HGNC_LABEL = "hgnc";

    private static final String MATCH_COUNT = "matchCount";

    /** The hgnc vocabulary. */
    private final Vocabulary hgnc;

    /** The set of terms observed to be present. */
    private final Set<VocabularyTerm> presentTerms;

    /** The set of terms observed to be absent. */
    private final Set<VocabularyTerm> absentTerms;

    /** An ordered list of objects containing gene count data. */
    private List<TermsForGene> termsForGeneList;

    /** An ordered list of objects containing match count data. */
    private List<MatchCount> matchCounts;

    /**
     * Simple constructor, passing in a collection of {@code presentTerms} and a collection of {@code absentTerms}, as
     * {@link VocabularyTerm} objects, and a {@link VocabularyManager}.
     *
     * @param presentTerms a collection of {@link VocabularyTerm feature identifiers} that are present
     * @param absentTerms a collection of {@link VocabularyTerm feature identifiers} that are absent
     * @param vocabularyManager the {@link VocabularyManager} for accessing the required vocabularies
     */
    DefaultGenePanelImpl(@Nonnull final Collection<VocabularyTerm> presentTerms,
        @Nonnull final Collection<VocabularyTerm> absentTerms, @Nonnull final VocabularyManager vocabularyManager)
    {
        this(presentTerms, absentTerms, Collections.emptySet(), false, vocabularyManager);
    }

    /**
     * Simple constructor, passing in a collection of {@code presentTerms} and a collection of {@code absentTerms}, as
     * {@link VocabularyTerm} objects, and a {@link VocabularyManager}.
     *
     * @param presentTerms a collection of {@link VocabularyTerm feature identifiers} that are present
     * @param absentTerms a collection of {@link VocabularyTerm feature identifiers} that are absent
     * @param vocabularyManager the {@link VocabularyManager} for accessing the required vocabularies
     */
    DefaultGenePanelImpl(@Nonnull final Collection<VocabularyTerm> presentTerms,
        @Nonnull final Collection<VocabularyTerm> absentTerms, final boolean generateMatchCount,
        @Nonnull final VocabularyManager vocabularyManager)
    {
        this(presentTerms, absentTerms, Collections.emptySet(), generateMatchCount, vocabularyManager);
    }

    /**
     * Simple constructor, passing in a collection of {@code presentTerms} and a collection of {@code absentTerms}, as
     * {@link VocabularyTerm} objects, a collection of {@code rejectedGenes rejected genes}, and a
     * {@link VocabularyManager}.
     *
     * @param presentTerms a collection of {@link VocabularyTerm feature identifiers} that are present
     * @param absentTerms a collection of {@link VocabularyTerm feature identifiers} that are absent
     * @param rejectedGenes a collection of genes that were tested to be negative
     * @param vocabularyManager the {@link VocabularyManager} for accessing the required vocabularies
     */
    DefaultGenePanelImpl(
        @Nonnull final Collection<VocabularyTerm> presentTerms,
        @Nonnull final Collection<VocabularyTerm> absentTerms,
        @Nonnull final Collection<VocabularyTerm> rejectedGenes,
        @Nonnull final VocabularyManager vocabularyManager)
    {
        this(presentTerms, absentTerms, rejectedGenes, false, vocabularyManager);
    }

    /**
     * Simple constructor, passing in a collection of {@code presentTerms} and a collection of {@code absentTerms}, as
     * {@link VocabularyTerm} objects, a collection of {@code rejectedGenes rejected genes}, and a
     * {@link VocabularyManager}.
     *
     * @param presentTerms a collection of {@link VocabularyTerm feature identifiers} that are present
     * @param absentTerms a collection of {@link VocabularyTerm feature identifiers} that are absent
     * @param rejectedGenes a collection of genes that were tested to be negative
     * @param generateMatchCount iff true, generate a term to number of associated genes mapping
     * @param vocabularyManager the {@link VocabularyManager} for accessing the required vocabularies
     */
    DefaultGenePanelImpl(
        @Nonnull final Collection<VocabularyTerm> presentTerms,
        @Nonnull final Collection<VocabularyTerm> absentTerms,
        @Nonnull final Collection<VocabularyTerm> rejectedGenes,
        final boolean generateMatchCount,
        @Nonnull final VocabularyManager vocabularyManager)
    {
        this.hgnc = vocabularyManager.getVocabulary(HGNC_LABEL);

        this.presentTerms = Collections.unmodifiableSet(new HashSet<>(presentTerms));
        this.absentTerms = Collections.unmodifiableSet(new HashSet<>(absentTerms));

        buildTermsForGeneList(rejectedGenes, generateMatchCount);
    }

    /**
     * Builds a list of {@link TermsForGene} objects for a given set of {@link #getPresentTerms()}. The
     * {@link #getAbsentTerms()} are ignored in this version of {@link GenePanel}. Genes specified as
     * {@code absentGenes absent genes} are excluded from the returned list.
     *
     * @param absentGenes genes that were tested negative
     * @param generateMatchCount iff true, generate a term to number of associated genes mapping
     */
    private void buildTermsForGeneList(@Nonnull final Collection<VocabularyTerm> absentGenes,
        boolean generateMatchCount)
    {
        final Set<String> geneExclusions = getAllExcludedGenes(absentGenes);
        // A builder to add and update the count data for all the genes.
        final TermsForGeneBuilder termsForGeneBuilder = new TermsForGeneBuilder(geneExclusions);
        // Update the data for all HPO identifiers.
        if (generateMatchCount) {
            final MatchCountBuilder matchCountBuilder = new MatchCountBuilder();
            getPresentTerms().forEach(
                term -> addMatchCountsAndTermsForGene(term, geneExclusions, termsForGeneBuilder, matchCountBuilder));
            this.matchCounts = matchCountBuilder.build();
        } else {
            getPresentTerms().forEach(term -> addTermForGenes(term, getGeneDataFromTerm(term), termsForGeneBuilder));
        }
        this.termsForGeneList = termsForGeneBuilder.build();
    }

    /**
     * Adds the match counts and terms for gene.
     *
     * @param term the {@link VocabularyTerm} of interest
     * @param geneExclusions the identifiers for genes to be excluded
     * @param termsForGeneBuilder the {@link TermsForGeneBuilder} object
     * @param matchCountBuilder the {@link MatchCountBuilder} object
     */
    private void addMatchCountsAndTermsForGene(
        @Nonnull final VocabularyTerm term,
        @Nonnull final Set<String> geneExclusions,
        @Nonnull final TermsForGeneBuilder termsForGeneBuilder,
        @Nonnull final MatchCountBuilder matchCountBuilder)
    {
        final List<String> storedGenes = getGeneDataFromTerm(term);
        matchCountBuilder.add(term, CollectionUtils.subtract(storedGenes, geneExclusions));
        addTermForGenes(term, storedGenes, termsForGeneBuilder);
    }

    /**
     * Given a collection of {@code absentGenes} retrieves and returns all the names that they are known by, as strings.
     *
     * @param absentGenes a collection of gene {@link VocabularyTerm} objects
     * @return a set of gene names and aliases, as strings
     */
    private Set<String> getAllExcludedGenes(@Nonnull final Collection<VocabularyTerm> absentGenes)
    {
        return absentGenes.stream()
            // Remove any nulls.
            .filter(Objects::nonNull)
            // Flatten the streams of names for each gene.
            .flatMap(this::getGeneNameStream)
            // Remove any nulls.
            .filter(Objects::nonNull)
            // Get the set of all names and alternative names for the absent genes.
            .collect(Collectors.toSet());
    }

    /**
     * Returns a stream of gene names, as strings.
     *
     * @param gene a gene {@link VocabularyTerm}
     * @return a stream of all known names for {@code gene}
     */
    private Stream<String> getGeneNameStream(@Nonnull final VocabularyTerm gene)
    {
        final String symbol = (String) gene.get(SYMBOL_LABEL);

        // Contains alias_symbol, prev_symbol, entrez_id, ensembl_gene_id, refseq_accession, and ena.
        final Collection<String> aliases = (Collection<String>) gene.get("alt_id");
        // Get the stream of all names.
        return Stream.concat(Stream.of(symbol), aliases != null ? aliases.stream() : Stream.empty());
    }

    /**
     * For each gene in a list of {@code genes}, adds the gene as key and {@code term} as value to the provided
     * {@code termsForGeneBuilder}.
     * @param term the {@link VocabularyTerm HPO vocabulary term} associated with the provided list of {@code genes}
     * @param genes a list of gene symbols associated with {@code term}
     * @param termsForGeneBuilder a builder for creating and updating {@link TermsForGene} objects for each gene
     */
    private void addTermForGenes(@Nonnull final VocabularyTerm term, @Nonnull final List<String> genes,
        @Nonnull final TermsForGeneBuilder termsForGeneBuilder)
    {
        for (final String enteredGene : genes) {
            // Get the gene term. May be null if gene is not a valid symbol.
            final VocabularyTerm geneTerm = this.hgnc.getTerm(enteredGene);
            // Since entered gene may be an alias, get the current gene symbol and gene ID.
            final String geneSymbol = getGeneSymbol(enteredGene, geneTerm);
            final String geneId = getGeneId(geneSymbol, geneTerm);
            if (termsForGeneBuilder.contains(geneId)) {
                termsForGeneBuilder.update(geneId, term);
            } else {
                termsForGeneBuilder.add(geneSymbol, geneId, term);
            }
        }
    }

    /**
     * Tries to obtain the preferred gene symbol, given entered {@code symbol}, and {@code geneTerm} vocabulary term.
     *
     * @param symbol the provided gene symbol
     * @param geneTerm the {@link VocabularyTerm} gene vocabulary term
     * @return the preferred gene symbol, or entered {@code symbol} if no symbol is recorded
     */
    private String getGeneSymbol(@Nonnull final String symbol, @Nullable final VocabularyTerm geneTerm)
    {
        return geneTerm != null ? StringUtils.defaultIfBlank((String) geneTerm.get(SYMBOL_LABEL), symbol) : symbol;
    }

    /**
     * Tries to obtain the preferred gene ID, given {@code symbol}.
     *
     * @param symbol the GeneCards gene symbol
     * @param geneTerm the {@link VocabularyTerm} gene vocabulary term
     * @return the preferred gene ID, or {@code symbol} if no preferred ID is recorded
     */
    private String getGeneId(@Nonnull final String symbol, @Nullable final VocabularyTerm geneTerm)
    {
        if (geneTerm != null) {
            @SuppressWarnings("unchecked")
            final List<String> geneIdList = (List<String>) geneTerm.get(ENSEMBL_ID_LABEL);
            return CollectionUtils.isEmpty(geneIdList) ? symbol : geneIdList.get(0);
        }
        return symbol;
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
        return CollectionUtils.isNotEmpty(geneList) ? geneList : Collections.emptyList();
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
    public List<MatchCount> getMatchCounts()
    {
        return this.matchCounts;
    }

    @Override
    public JSONObject toJSON()
    {
        final JSONObject jsonObject = buildPhenotypesForGeneJSON(0, this.size());
        tryAddMatchCountsJson(jsonObject);
        return jsonObject.put(RETURNED_SIZE, this.size()).put(TOTAL_SIZE, this.size());
    }

    @Override
    public JSONObject toJSON(final int fromIndex, final int toIndex)
    {
        final JSONObject jsonObject = buildPhenotypesForGeneJSON(fromIndex, toIndex);
        tryAddMatchCountsJson(jsonObject);
        return jsonObject.put(RETURNED_SIZE, toIndex - fromIndex).put(TOTAL_SIZE, this.size());
    }

    /**
     * If the builder was initialized, builds the match counts JSON.
     *
     * @param panelJson the {@link JSONObject} containing gene panel data
     */
    private void tryAddMatchCountsJson(@Nonnull final JSONObject panelJson)
    {
        if (this.matchCounts != null) {
            final JSONArray matchCountsJson = new JSONArray();
            this.matchCounts.forEach(matchCount -> matchCountsJson.put(matchCount.toJSON()));
            panelJson.put(MATCH_COUNT, matchCountsJson);
        }
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

        jsonObject.put(GENE_ROWS_LABEL, jsonArray);
        return jsonObject;
    }

    @Override
    public int size()
    {
        return this.termsForGeneList.size();
    }
}
