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
package org.phenotips.vocabulary.internal.solr;

import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyTerm;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * A lazy-loading set that transforms a set of term identifiers into real terms only when actually accessing the terms.
 *
 * @version $Id$
 * @since 1.2M4 (under a different package since 1.0M8)
 */
public class LazySolrTermSet implements Set<VocabularyTerm>
{
    /** The original set of term identifiers. */
    private Collection<String> identifiers;

    /** The loaded terms, {@code null} until it is actually needed. */
    private Collection<VocabularyTerm> terms;

    /** The vocabulary owning all the terms in this set. Used for loading the terms. */
    private Vocabulary vocabulary;

    /**
     * Constructor that provides the list of {@link #identifiers terms identifier} and the {@link #vocabulary owner
     * vocabulary}.
     *
     * @param identifiers the {@link #identifiers identifiers to load}
     * @param vocabulary the {@link #vocabulary owner vocabulary}
     */
    public LazySolrTermSet(Collection<Object> identifiers, Vocabulary vocabulary)
    {
        if (identifiers == null || identifiers.isEmpty()) {
            this.identifiers = Collections.emptySet();
            this.terms = Collections.emptySet();
        } else {
            this.identifiers = new LinkedHashSet<>(identifiers.size());
            for (Object id : identifiers) {
                this.identifiers.add(StringUtils.substringBefore(String.valueOf(id), " "));
            }
        }
        this.vocabulary = vocabulary;
    }

    @Override
    public int size()
    {
        return this.identifiers.size();
    }

    @Override
    public boolean isEmpty()
    {
        return this.identifiers.isEmpty();
    }

    @Override
    public boolean contains(Object o)
    {
        if (String.class.isInstance(o)) {
            return this.identifiers.contains(o);
        } else if (VocabularyTerm.class.isInstance(o)) {
            return this.identifiers.contains(((VocabularyTerm) o).getId());
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c)
    {
        for (Object o : c) {
            if (!this.contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Iterator<VocabularyTerm> iterator()
    {
        loadTerms();
        return this.terms.iterator();
    }

    @Override
    public Object[] toArray()
    {
        loadTerms();
        return this.terms.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a)
    {
        loadTerms();
        return this.terms.toArray(a);
    }

    @Override
    public boolean add(VocabularyTerm e)
    {
        // This is readonly, nothing can be added
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o)
    {
        // This is readonly, nothing can be removed
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends VocabularyTerm> c)
    {
        // This is readonly, nothing can be added
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
        // This is readonly, nothing can be removed
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
        // This is readonly, nothing can be removed
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear()
    {
        // This is readonly, nothing can be removed
        throw new UnsupportedOperationException();
    }

    /** Loads the terms from the vocabulary when needed. */
    private synchronized void loadTerms()
    {
        if (this.terms == null) {
            this.terms = this.vocabulary.getTerms(this.identifiers);
        }
    }
}
