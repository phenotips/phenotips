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

import org.phenotips.panels.MatchCount;
import org.phenotips.vocabulary.VocabularyTerm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The MatchCount builder facilitates creating a list of {@link MatchCount} objects for a list of terms.
 *
 * @version $Id$
 * @since 1.4
 */
class MatchCountBuilder
{
    /** The internal list of {@link MatchCount} objects. Unsorted until {@link #build()} is called. */
    private List<MatchCount> matchCounts;

    /**
     * Default constructor.
     */
    MatchCountBuilder()
    {
        // Initialize the match counts array.
        this.matchCounts = new ArrayList<>();
    }

    /**
     * Adds the {@code term} and the associated {@code genes} to the collection.
     *
     * @param term the {@link VocabularyTerm} of interest
     * @param genes the collection of genes associated with {@code term}
     */
    void add(@Nonnull final VocabularyTerm term, @Nullable final Collection<String> genes)
    {
        this.matchCounts.add(new DefaultMatchCountImpl(term, genes));
    }

    /**
     * Builds an unmodifiable sorted list of {@link MatchCount} objects.
     *
     * @return a sorted, unmodifiable list of {@link MatchCount} objects
     */
    List<MatchCount> build()
    {
        this.matchCounts.sort(Comparable::compareTo);
        return Collections.unmodifiableList(this.matchCounts);
    }
}
