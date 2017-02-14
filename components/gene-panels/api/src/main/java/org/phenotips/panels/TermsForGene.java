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

import java.util.Set;

import org.json.JSONObject;

/**
 * A DTO containing a set of {@link #getTerms() terms} associated with a specific {@link #getGeneId() gene}.
 *
 * @version $Id$
 * @since 1.3
 */
@Unstable("New API introduced in 1.3")
public interface TermsForGene
{
    /**
     * Gets the total number of {@link #getTerms() terms} associated with {@link #getGeneId()}.
     *
     * @return the total number of {@link #getTerms() terms} associated with {@link #getGeneId()}
     */
    int getCount();

    /**
     * Gets the preferred gene ID as string.
     *
     * @return preferred gene ID as string
     */
    String getGeneId();

    /**
     * Gets the gene symbol as string.
     *
     * @return the gene symbol as string
     */
    String getGeneSymbol();

    /**
     * Get an unmodifiable set of {@link VocabularyTerm} terms associated with {@link #getGeneId()} gene.
     *
     * @return an unmodifiable set of {@link VocabularyTerm} terms
     */
    Set<VocabularyTerm> getTerms();

    /**
     * Creates a {@link JSONObject} of itself.
     *
     * @return a JSON representation of itself
     */
    JSONObject toJSON();
}
