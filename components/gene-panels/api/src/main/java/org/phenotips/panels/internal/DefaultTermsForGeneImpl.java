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

import org.phenotips.panels.TermsForGene;
import org.phenotips.vocabulary.VocabularyTerm;

import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Default implementation of {@link TermsForGene}.
 *
 * @version $Id$
 * @since 1.3
 */
public class DefaultTermsForGeneImpl implements TermsForGene
{
    private static final String COUNT = "count";

    private static final String TERMS = "terms";

    private static final String GENE_SYMBOL = "gene_symbol";

    private static final String GENE_ID = "gene_id";

    private final String geneId;

    private final String geneSymbol;

    private final Set<VocabularyTerm> terms = newSortedSet();

    /**
     * Default constructor, that takes in a {@code geneSymbol gene symbol} and a {@code geneId preferred gene ID}.
     *
     * @param geneSymbol the gene symbol for the gene of interest
     * @param geneId the preferred ID for the gene
     */
    public DefaultTermsForGeneImpl(@Nonnull final String geneSymbol, @Nonnull final String geneId)
    {
        Validate.notNull(geneSymbol);
        Validate.notNull(geneId);
        this.geneSymbol = geneSymbol;
        this.geneId = geneId;
    }

    /**
     * Adds a vocabulary {@code term} to the set of {@link #terms}.
     *
     * @param term a {@link VocabularyTerm} that is associated with {@link #geneSymbol}
     */
    void addTerm(@Nonnull final VocabularyTerm term)
    {
        this.terms.add(term);
    }

    @Override
    public int getCount()
    {
        return this.terms.size();
    }

    @Override
    public String getGeneId()
    {
        return this.geneId;
    }

    @Override
    public String getGeneSymbol()
    {
        return this.geneSymbol;
    }

    @Override
    public Set<VocabularyTerm> getTerms()
    {
        return Collections.unmodifiableSet(this.terms);
    }

    @Override
    public JSONObject toJSON()
    {
        final JSONObject json = new JSONObject();
        final JSONArray phenotypesArray = buildPhenotypesJSON();
        json.put(COUNT, getCount());
        json.put(TERMS, phenotypesArray);
        json.put(GENE_ID, this.geneId);
        json.put(GENE_SYMBOL, this.geneSymbol);
        return json;
    }

    /**
     * Builds a {@link JSONArray} of JSON representations of term {@link VocabularyTerm} objects.
     *
     * @return a {@link JSONArray} of term {@link VocabularyTerm} objects
     */
    private JSONArray buildPhenotypesJSON()
    {
        final JSONArray json = new JSONArray();
        for (final VocabularyTerm term : getTerms()) {
            json.put(term.toJSON());
        }
        return json;
    }

    /**
     * Creates an empty sorted set, where {@link VocabularyTerm} objects are sorted by the natural order of their name.
     *
     * @return an empty sorted set
     */
    private Set<VocabularyTerm> newSortedSet()
    {
        return new TreeSet<>(new Comparator<VocabularyTerm>()
        {
            @Override
            public int compare(final VocabularyTerm o1, final VocabularyTerm o2)
            {
                final String name1 = o1.getName() != null ? o1.getName() : o1.getId();
                final String name2 = o2.getName() != null ? o2.getName() : o2.getId();
                return name1.compareTo(name2);
            }
        });
    }
}
