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
package org.phenotips.data.permissions.internal;

import org.phenotips.data.permissions.EntityAccess;
import org.phenotips.data.permissions.EntityPermissionsManager;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.entities.PrimaryEntity;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Filters an iterator over {@link PrimaryEntity}s, returning only those that have their
 * {@link EntityAccess#getVisibility() visibility} equal or above a threshold. The
 * {@link #remove()} method is not supported.
 *
 * @version $Id$
 * @since 1.3M2
 */
public class FilteringIterator implements Iterator<PrimaryEntity>
{
    private final Iterator<? extends PrimaryEntity> input;

    private final Visibility thresholdVisibility;

    private final EntityPermissionsManager entityPermissionsManager;

    private PrimaryEntity next;

    /**
     * Basic constructor.
     *
     * @param input the iterator to filter, {@code null} and empty iterators are accepted
     * @param thresholdVisibility a threshold visibility; if {@code null}, then the input is returned unchanged
     * @param entityPermissionsManager required for computing the visibility of each input entity
     */
    public FilteringIterator(Iterator<? extends PrimaryEntity> input, Visibility thresholdVisibility,
        EntityPermissionsManager entityPermissionsManager)
    {
        this.input = input;
        this.thresholdVisibility = thresholdVisibility;
        this.entityPermissionsManager = entityPermissionsManager;
        this.findNext();
    }

    @Override
    public boolean hasNext()
    {
        return this.next != null;
    }

    @Override
    public PrimaryEntity next()
    {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        PrimaryEntity toReturn = this.next;
        this.findNext();

        return toReturn;
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    private void findNext()
    {
        this.next = null;

        while (this.input.hasNext() && this.next == null) {
            PrimaryEntity potentialNextEntity = this.input.next();
            if (potentialNextEntity != null) {
                Visibility entityVisibility =
                    this.entityPermissionsManager.getEntityAccess(potentialNextEntity).getVisibility();
                if (this.thresholdVisibility.compareTo(entityVisibility) <= 0) {
                    this.next = potentialNextEntity;
                }
            }
        }
    }
}
