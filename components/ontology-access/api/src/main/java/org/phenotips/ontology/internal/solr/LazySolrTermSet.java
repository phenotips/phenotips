/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.ontology.internal.solr;

import org.phenotips.ontology.OntologyService;
import org.phenotips.ontology.OntologyTerm;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * A lazy-loading set that transforms a set of term identifiers into real terms only when actually accessing the terms.
 *
 * @version $Id$
 * @since 1.0M8
 */
public class LazySolrTermSet implements Set<OntologyTerm>
{
    /** The original set of term identifiers. */
    private Collection<String> identifiers;

    /** The loaded terms, {@code null} until it is actually needed. */
    private Collection<OntologyTerm> terms;

    /** The ontology owning all the terms in this set. Used for loading the terms. */
    private OntologyService ontology;

    /**
     * Constructor that provides the list of {@link #identifiers terms identifier} and the {@link #ontology owner
     * ontology}.
     *
     * @param identifiers the {@link #identifiers identifiers to load}
     * @param ontology the {@link #ontology owner ontology}
     */
    public LazySolrTermSet(Collection<Object> identifiers, OntologyService ontology)
    {
        if (identifiers == null || identifiers.isEmpty()) {
            this.identifiers = Collections.emptySet();
            this.terms = Collections.emptySet();
        } else {
            this.identifiers = new HashSet<String>(identifiers.size());
            for (Object id : identifiers) {
                this.identifiers.add(StringUtils.substringBefore(String.valueOf(id), " "));
            }
        }
        this.ontology = ontology;
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
        } else if (OntologyTerm.class.isInstance(o)) {
            return this.identifiers.contains(((OntologyTerm) o).getId());
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
    public Iterator<OntologyTerm> iterator()
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
    public boolean add(OntologyTerm e)
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
    public boolean addAll(Collection<? extends OntologyTerm> c)
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

    /** Loads the terms from the ontology when needed. */
    private synchronized void loadTerms()
    {
        if (this.terms == null) {
            this.terms = this.ontology.getTerms(this.identifiers);
        }
    }
}
