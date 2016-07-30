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
package org.phenotips.entities.internal;

import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.PrimaryEntityManager;

import java.util.Iterator;
import java.util.List;

/**
 * A lazy iterator on an immutable collection of primary entities, which only loads an entity when it is actually
 * requested out of the iterator.
 *
 * @param <E> the type of entities handled by this iterator
 * @version $Id$
 * @since 1.3M2
 */
public class LazyPrimaryEntityIterator<E extends PrimaryEntity> implements Iterator<E>
{
    private final PrimaryEntityManager<E> entityManager;

    private Iterator<String> iterator;

    /**
     * Default constructor.
     *
     * @param identifiers the identifiers of the entities to be contained in the lazy collection
     * @param entityManager the entity manager responsible for actually loading the entities
     */
    public LazyPrimaryEntityIterator(List<String> identifiers, PrimaryEntityManager<E> entityManager)
    {
        this.iterator = identifiers.iterator();
        this.entityManager = entityManager;
    }

    @Override
    public boolean hasNext()
    {
        return this.iterator.hasNext();
    }

    @Override
    public E next()
    {
        String id = this.iterator.next();
        return this.entityManager.get(id);
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
