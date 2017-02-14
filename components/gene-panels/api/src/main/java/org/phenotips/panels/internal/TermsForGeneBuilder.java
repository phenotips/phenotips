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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * A TermsForGene builder facilitates creating and updating a {@link TermsForGene} object for each provided gene.
 *
 * @version $Id$
 * @since 1.3
 */
class TermsForGeneBuilder
{
    private final Map<String, DefaultTermsForGeneImpl> termsForGeneMap = new HashMap<>();

    /**
     * Creates a new builder.
     */
    TermsForGeneBuilder() { }

    /**
     * Updates the {@link TermsForGene terms for gene} object stored under {@code geneSymbol} key, with the provided
     * vocabulary {@code term}.
     *
     * @param geneSymbol the gene symbol, which will be used as the key
     * @param term the {@link VocabularyTerm vocabulary term} with which to update the stored {@link TermsForGene}
     * @throws NullPointerException if key {@code geneSymbol} has not yet been added to the {@link TermsForGeneBuilder},
     *         or if {@code term} or {@code geneSymbol} are null
     */
    void update(@Nonnull final String geneSymbol, @Nonnull final VocabularyTerm term)
    {
        Validate.notNull(geneSymbol, "The gene symbol must not be null");
        Validate.notNull(term, "The vocabulary term must not be null.");
        termsForGeneMap.get(geneSymbol).addTerm(term);
    }

    /**
     * Adds a new {@link TermsForGene} entry for {@code geneSymbol}. If {@code geneSymbol} entry already exists
     * in the builder, it will be overwritten.
     *
     * @param geneSymbol the gene symbol that will be used as the key, must not be null
     * @param geneId the preferred gene ID, must not be null
     * @param term the first {@link VocabularyTerm} that will be used to add the {@link TermsForGene} entry, not null
     * @throws NullPointerException if either of the {@code geneSymbol}, {@code geneId}, or {@code term} are null
     */
    void add(@Nonnull final String geneSymbol, @Nonnull final String geneId, @Nonnull final VocabularyTerm term)
    {
        Validate.notNull(geneSymbol, "The gene symbol must not be null.");
        Validate.notNull(geneId, "The gene ID must not be null.");
        Validate.notNull(term, "The vocabulary term must not be null");

        final DefaultTermsForGeneImpl termsForGene = new DefaultTermsForGeneImpl(geneSymbol, geneId);
        termsForGene.addTerm(term);
        termsForGeneMap.put(geneSymbol, termsForGene);
    }

    /**
     * Returns true iff the {@link TermsForGeneBuilder} object contains {@code geneSymbol}.
     *
     * @param geneSymbol the gene symbol, that will be used as key
     * @return true iff {@link TermsForGeneBuilder} contains {@code geneSymbol}, false otherwise
     */
    boolean contains(final String geneSymbol)
    {
        return termsForGeneMap.containsKey(geneSymbol);
    }

    /**
     * Returns a newly created, and sorted, unmodifiable list of {@link TermsForGene terms for gene} objects.
     *
     * @return a newly created unmodifiable list of {@link TermsForGene objects}
     */
    List<TermsForGene> build()
    {
        final List<TermsForGene> sortedTermsForGeneList = getSortedTermsForGeneList();
        return Collections.unmodifiableList(sortedTermsForGeneList);
    }

    /**
     * Given a {@code termsForGeneMap map} of gene symbol to {@link TermsForGene} objects, returns a
     * sorted list of {@link TermsForGene terms for gene}, in descending order of relevance.
     *
     * @return a list of {@link TermsForGene} objects, sorted in descending order or relevance
     */
    private List<TermsForGene> getSortedTermsForGeneList()
    {
        final List<TermsForGene> termsForGeneEntries = new ArrayList<TermsForGene>(termsForGeneMap.values());
        Collections.sort(termsForGeneEntries, new Comparator<TermsForGene>()
        {
            @Override
            public int compare(final TermsForGene o1, final TermsForGene o2)
            {
                // First compare by count, in descending order.
                final int countComparison = new Integer(o2.getCount()).compareTo(o1.getCount());
                // Second, if o1 and o2 have the same count, compare by natural order of phenotypes, and if necessary,
                // by gene name.
                return (countComparison != 0)
                    ? countComparison
                    : compareTermsForGene(o1, o2);
            }
        });
        return termsForGeneEntries;
    }

    /**
     * Compares two {@link TermsForGene} objects by first comparing the stored {@link TermsForGene#getTerms() terms},
     * and second, by comparing {@link TermsForGene#getGeneSymbol() the associated gene symbol}.
     *
     * @param o1 the {@link TermsForGene term} that {@code o2} is being compared to
     * @param o2 the {@link TermsForGene term} that is being compared
     * @return {@code 0} if {@code o1} and {@code o2} are equivalent, a value less than {@code 0} if {@code o1} should
     *         be ahead of {@code o2}, a value greater than {@code 0} if {@code o2} should be ahead of {@code o1}
     */
    private int compareTermsForGene(@Nonnull final TermsForGene o1, @Nonnull final TermsForGene o2)
    {
        final int compareByTerms = compareByTermList(o1.getTerms().iterator(), o2.getTerms().iterator());
        return (compareByTerms != 0) ? compareByTerms : o1.getGeneSymbol().compareTo(o2.getGeneSymbol());
    }

    /**
     * Compares two {@link Iterator <VocabularyTerm>} alphabetically.
     *
     * @param first the {@link Iterator<VocabularyTerm>} that {@code second} is being compared to
     * @param second the {@link Iterator<VocabularyTerm>} that is being compared
     * @return {@code 0} if {@code first} and {@code second} are equivalent, a value less than {@code 0} if
     *         {@code first} should be ahead of {@code second}, a value greater than {@code 0} if {@code second}
     *         should be ahead of {@code first}
     */
    private int compareByTermList(@Nonnull final Iterator<VocabularyTerm> first,
        @Nonnull final Iterator<VocabularyTerm> second)
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
    private String getTermName(@Nonnull final VocabularyTerm term)
    {
        final String name = term.getName();
        return StringUtils.isNotBlank(name) ? name : term.getId();
    }
}
