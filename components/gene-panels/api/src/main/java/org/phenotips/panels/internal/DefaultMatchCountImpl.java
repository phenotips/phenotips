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
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.json.JSONObject;

/**
 * Default implementation of {@link MatchCount}.
 *
 * @version $Id$
 * @since 1.4
 */
public class DefaultMatchCountImpl implements MatchCount
{
    /** The error message to display if the vocabulary term provided to the constructor is null. */
    private static final String NULL_TERM_ERROR_MSG = "The vocabulary term must not be null";

    /** The ID label. */
    private static final String ID_LABEL = "id";

    /** The name label. */
    private static final String NAME_LABEL = "label";

    /** The label for the gene count. */
    private static final String COUNT_LABEL = "count";

    /** The vocabulary term of interest. */
    private final VocabularyTerm term;

    /** The genes that are associated with {@link #term}. */
    private final List<String> genes;

    /**
     * The default constructor, taking in a non-null {@code term} and a collection of {@code associatedGenes}.
     *
     * @param term the {@link VocabularyTerm} of interest
     * @param associatedGenes the genes associated with the provided {@code term}
     */
    DefaultMatchCountImpl(@Nonnull final VocabularyTerm term, @Nullable final Collection<String> associatedGenes)
    {
        Validate.notNull(term, NULL_TERM_ERROR_MSG);
        this.term = term;
        this.genes = associatedGenes == null
            ? new ArrayList<>()
            : associatedGenes.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public int getCount()
    {
        return this.genes.size();
    }

    @Override
    public String getId()
    {
        return this.term.getId();
    }

    @Override
    public String getName()
    {
        return this.term.getTranslatedName();
    }

    @Override
    public Collection<String> getGenes()
    {
        return Collections.unmodifiableList(this.genes);
    }

    @Override
    public JSONObject toJSON()
    {
        return new JSONObject()
            .putOpt(ID_LABEL, getId())
            .putOpt(NAME_LABEL, getName())
            .put(COUNT_LABEL, getCount());
    }

    @Override
    public boolean equals(@Nullable final Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DefaultMatchCountImpl that = (DefaultMatchCountImpl) o;

        return new EqualsBuilder()
            .append(this.term, that.term)
            .append(this.genes, that.genes)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(this.term)
            .append(this.genes)
            .toHashCode();
    }

    @Override
    public int compareTo(@Nonnull final MatchCount o)
    {
        // This comes before o if this has a larger count, or if this has a name that alphabetically precedes o name.
        final int result = Integer.compare(o.getCount(), getCount());
        return result != 0 ? result : getName().compareTo(o.getName());
    }
}
