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

import org.phenotips.panels.PhenotypesForGene;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.stability.Unstable;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Default implementation of {@link PhenotypesForGene}.
 *
 * @version $Id$
 * @since 1.3M5
 */
@Unstable("New API introduced in 1.3")
public class DefaultPhenotypesForGeneImpl implements PhenotypesForGene
{
    private static final String COUNT = "count";

    private static final String PHENOTYPES = "phenotypes";

    private static final String GENE_SYMBOL = "gene_symbol";

    private static final String GENE_ID = "gene_id";

    private final String geneId;

    private final String geneSymbol;

    private final Set<VocabularyTerm> phenotypes = new HashSet<>();

    private int count;

    private boolean countUpToDate;

    /**
     * Default constructor for the class.
     *
     * @param geneSymbol the name of the gene for which we are storing phenotype data
     * @param geneId the ID for the gene
     */
    public DefaultPhenotypesForGeneImpl(@Nonnull final String geneSymbol, @Nonnull final String geneId)
    {
        Validate.notNull(geneSymbol);
        Validate.notNull(geneId);
        this.geneSymbol = geneSymbol;
        this.geneId = geneId;
    }

    @Override
    public void addTerm(@Nonnull final VocabularyTerm term)
    {
        this.phenotypes.add(term);
        this.countUpToDate = false;
    }

    @Override
    public int getCount()
    {
        if (!countUpToDate) {
            this.count = this.phenotypes.size();
            this.countUpToDate = true;
        }
        return this.count;
    }

    @Override
    public JSONObject toJSON()
    {
        final JSONObject json = new JSONObject();
        final JSONArray phenotypesArray = buildPhenotypesJSON();
        json.put(COUNT, getCount());
        json.put(PHENOTYPES, phenotypesArray);
        json.put(GENE_ID, this.geneId);
        json.put(GENE_SYMBOL, this.geneSymbol);
        return json;
    }

    /**
     * Builds a {@link JSONArray} of JSON representations of phenotype {@link VocabularyTerm} objects.
     *
     * @return a {@link JSONArray} of phenotypes
     */
    private JSONArray buildPhenotypesJSON()
    {
        final JSONArray json = new JSONArray();
        for (final VocabularyTerm phenotype : this.phenotypes) {
            json.put(phenotype.toJSON());
        }
        return json;
    }
}
