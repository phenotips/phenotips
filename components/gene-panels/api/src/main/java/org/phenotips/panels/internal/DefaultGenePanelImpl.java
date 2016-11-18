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
import org.phenotips.panels.PhenotypesForGene;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.stability.Unstable;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * Default implementation of {@link GenePanel}.
 *
 * @version $Id$
 * @since 1.3M5
 */
@Unstable("New API introduced in 1.3")
public class DefaultGenePanelImpl implements GenePanel
{
    /** The property of interest for the provided hpo terms. */
    private static final String ASSOCIATED_GENES = "associated_genes";

    private static final String SIZE = "size";

    private static final String GENES_LABEL = "genes";

    /** The hpo vocabulary. */
    private final Vocabulary hpo;

    /** The hgnc vocabulary. */
    private final Vocabulary hgnc;

    /** The set of observed hpo terms. */
    private final Set<VocabularyTerm> presentFeatures;

    /** An ordered list of objects containing gene count data. */
    private final List<PhenotypesForGene> phenotypesForGeneList;

    private final int panelSize;

    /**
     * Simple constructor, passing in the list of present features and a vocabulary manager.
     * @param presentFeatures a collection of features that are present
     * @param vocabularyManager a vocabulary manager
     */
    public DefaultGenePanelImpl(@Nonnull final Collection<String> presentFeatures,
        @Nonnull final VocabularyManager vocabularyManager)
    {
        this.hpo = vocabularyManager.getVocabulary("hpo");
        this.hgnc = vocabularyManager.getVocabulary("hgnc");

        this.presentFeatures = buildPresentFeatures(presentFeatures);
        this.phenotypesForGeneList = buildPhenotypesForGeneList();
        this.panelSize = this.phenotypesForGeneList.size();
    }

    /**
     * Builds the set of present features, transforming each string ID into a {@link VocabularyTerm}.
     *
     * @return a set of {@link VocabularyTerm} objects corresponding with the provided list of features
     */
    private Set<VocabularyTerm> buildPresentFeatures(@Nonnull final Collection<String> presentFeatures)
    {
        final ImmutableSet.Builder<VocabularyTerm> presentFeatureBuilder = ImmutableSet.builder();
        for (final String feature : presentFeatures) {
            if (StringUtils.isBlank(feature)) {
                continue;
            }
            final VocabularyTerm term = this.hpo.getTerm(feature);
            if (term != null) {
                presentFeatureBuilder.add(term);
            }
        }
        return presentFeatureBuilder.build();
    }

    /**
     * Builds the gene counts data using the obtained vocabularies and the set of present features.
     *
     * @return a list of sorted objects containing the gene count data
     */
    private List<PhenotypesForGene> buildPhenotypesForGeneList()
    {
        // Map storing count data for all the genes.
        final Map<String, PhenotypesForGene> phenotypesForGeneMap = new HashMap<>();

        // Update the data for all HPO identifiers.
        for (final VocabularyTerm term : this.presentFeatures) {
            final List<String> storedGenes = getGeneDataFromTerm(term);
            addPhenotypeForGene(term, storedGenes, phenotypesForGeneMap);
        }

        return buildSortedGeneDataFromMap(phenotypesForGeneMap);
    }

    /**
     * Sorts the values stored in the gene counts map, in descending order, and builds a list.
     *
     * @param phenotypesForGeneMap the map storing gene counts data
     * @return a sorted list of {@link PhenotypesForGene} objects
     */
    private List<PhenotypesForGene> buildSortedGeneDataFromMap(
        @Nonnull final Map<String, PhenotypesForGene> phenotypesForGeneMap)
    {
        final List<Map.Entry<String, PhenotypesForGene>> phenotypesForGeneEntries =
            new LinkedList<>(phenotypesForGeneMap.entrySet());
        Collections.sort(phenotypesForGeneEntries, new Comparator<Map.Entry<String, PhenotypesForGene>>()
        {
            @Override
            public int compare(final Map.Entry<String, PhenotypesForGene> o1,
                final Map.Entry<String, PhenotypesForGene> o2)
            {
                return new Integer(o2.getValue().getCount()).compareTo(o1.getValue().getCount());
            }
        });

        final ImmutableList.Builder<PhenotypesForGene> sortedGeneCountsListBuilder = ImmutableList.builder();
        for (final Map.Entry<String, PhenotypesForGene> entry : phenotypesForGeneEntries) {
            sortedGeneCountsListBuilder.add(entry.getValue());
        }
        return sortedGeneCountsListBuilder.build();
    }

    /**
     * Counts the number of occurrences for each gene.
     *
     * @param term the HPO term of the phenotype associated with the provided list of genes
     * @param phenotypesForGeneMap a map containing counts data for each gene
     * @param genes list of genes
     */
    private void addPhenotypeForGene(@Nonnull final VocabularyTerm term, @Nonnull final List<String> genes,
        @Nonnull final Map<String, PhenotypesForGene> phenotypesForGeneMap)
    {
        for (final String gene : genes) {
            final PhenotypesForGene phenotypesForGene = (null != phenotypesForGeneMap.get(gene))
                ? phenotypesForGeneMap.get(gene)
                : createPhenotypesForGeneObj(phenotypesForGeneMap, gene);
            phenotypesForGene.addTerm(term);
        }
    }

    /**
     * Creates a new {@link PhenotypesForGene} object, and puts it into the geneCountsMap map.
     *
     * @param geneCountsMap a map containing counts data for each gene
     * @param geneSymbol the gene, the occurrences of which are being counted
     * @return the newly created {@link PhenotypesForGene} object for the gene
     */
    private PhenotypesForGene createPhenotypesForGeneObj(@Nonnull final Map<String, PhenotypesForGene> geneCountsMap,
        @Nonnull final String geneSymbol)
    {
        final String geneId = getGeneId(geneSymbol);
        final PhenotypesForGene phenotypesForGene = new DefaultPhenotypesForGeneImpl(geneSymbol, geneId);
        geneCountsMap.put(geneSymbol, phenotypesForGene);
        return phenotypesForGene;
    }

    /**
     * Gets the gene ID from the provided gene symbol.
     * @param geneSymbol the gene symbol
     * @return the gene ID, or geneSymbol if such ID cannot be found
     */
    private String getGeneId(@Nonnull final String geneSymbol)
    {
        final VocabularyTerm geneTerm = this.hgnc.getTerm(geneSymbol);

        if (geneTerm != null) {
            @SuppressWarnings("unchecked")
            final List<String> geneIdList = (List<String>) geneTerm.get("ensembl_gene_id");
            return CollectionUtils.isEmpty(geneIdList) ? geneSymbol : geneIdList.get(0);
        }
        return geneSymbol;
    }

    /**
     * Returns a list of genes, given an HPO {@link VocabularyTerm}.
     *
     * @param term an HPO vocabulary term
     * @return a list of genes associated with the HPO term, or an empty list
     */
    @SuppressWarnings("unchecked")
    private List<String> getGeneDataFromTerm(@Nonnull final VocabularyTerm term)
    {
        return Optional.fromNullable((List<String>) term.get(ASSOCIATED_GENES)).or(Collections.<String>emptyList());
    }

    @Override
    public Set<VocabularyTerm> getPresentFeatures()
    {
        return this.presentFeatures;
    }

    @Override
    public List<PhenotypesForGene> getPhenotypesForGeneList()
    {
        return this.phenotypesForGeneList;
    }

    @Override
    public JSONObject toJSON()
    {
        final JSONObject jsonObject = new JSONObject();
        final JSONArray phenotypesForGeneListJSON = buildPhenotypesForGeneListJSON();
        jsonObject.put(SIZE, this.size());
        jsonObject.put(GENES_LABEL, phenotypesForGeneListJSON);
        return jsonObject;
    }

    /**
     * Creates a {@link JSONArray} of JSON representations of {@link PhenotypesForGene} objects.
     *
     * @return a JSON representation of {@link PhenotypesForGene} objects
     */
    private JSONArray buildPhenotypesForGeneListJSON()
    {
        final JSONArray jsonArray = new JSONArray();
        for (final PhenotypesForGene geneObj : this.phenotypesForGeneList) {
            jsonArray.put(geneObj.toJSON());
        }
        return jsonArray;
    }

    @Override
    public int size()
    {
        return this.panelSize;
    }
}

