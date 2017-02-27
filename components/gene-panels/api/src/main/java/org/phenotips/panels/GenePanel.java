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
package org.phenotips.panels;

import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.stability.Unstable;

import java.util.List;
import java.util.Set;

import org.json.JSONObject;

/**
 * A Gene Panel links {@link #getPresentTerms() a collection of observed symptoms} to {@link #getTermsForGeneList()
 * a list of genes} frequently associated with these symptoms, in descending order of relevance.
 *
 * @version $Id$
 * @since 1.3
 */
@Unstable("New API introduced in 1.3")
public interface GenePanel
{
    /**
     * Returns a set of {@link VocabularyTerm} objects observed to be present.
     *
     * @return the set of present {@link VocabularyTerm} objects
     */
    Set<VocabularyTerm> getPresentTerms();

    /**
     * Returns the set of {@link VocabularyTerm} objects observed to be absent.
     *
     * @return the set of absent {@link VocabularyTerm} objects
     */
    Set<VocabularyTerm> getAbsentTerms();

    /**
     * Returns a list of {@link TermsForGene} objects generated for {@link #getPresentTerms()} and
     * {@link #getAbsentTerms()}, in descending order of relevance.
     *
     * @return an ordered list of {@link TermsForGene} objects, in descending order of relevance.
     */
    List<TermsForGene> getTermsForGeneList();

    /**
     * Creates a {@link JSONObject} representation of {@link GenePanel#getTermsForGeneList() a list of genes}.
     *
     * @return a JSON representation of itself
     */
    JSONObject toJSON();

    /**
     * Creates a {@link JSONObject} representation of {@link GenePanel#getTermsForGeneList() a list of genes} from
     * {@code fromIndex}, inclusive, to {@code toIndex}, exclusive.
     *
     * @param fromIndex the starting index (inclusive), 0 based
     * @param toIndex the last index (exclusive), 0 based
     * @return a {@link JSONObject} containing data from {@code fromIndex}, inclusive, to {@code toIndex}, exclusive
     * @throws IndexOutOfBoundsException if (<tt>fromIndex &lt; 0 || toIndex &gt; {@link #size()} ||
     *         fromIndex &gt; toIndex</tt>)
     */
    JSONObject toJSON(final int fromIndex, final int toIndex);

    /**
     * Returns the number of genes represented by the gene panel.
     *
     * @return the number of genes represented by the gene panel
     */
    int size();
}
