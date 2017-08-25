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

import org.phenotips.data.permissions.Visibility;
import org.phenotips.data.permissions.internal.visibility.VisibilityHelper;
import org.phenotips.entities.PrimaryEntity;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Filters an iterator over {@link PrimaryEntity}s, returning only those that have their
 * {@link VisibilityHelper#getVisibility(PrimaryEntity)}  visibility} equal or above a threshold. The
 * {@link #remove()} method is not supported.
 *
 * @version $Id$
 * @since 1.3M2, modified 1.4
 */
public class FilteringIterator implements Iterator<PrimaryEntity>
{
    private final Iterator<PrimaryEntity> input;

    private final Visibility thresholdVisibility;

    private final VisibilityHelper visibilityHelper;

    private PrimaryEntity next;

    /**
     * Basic constructor.
     *
     * @param input the iterator to filter, empty iterators are accepted
     * @param thresholdVisibility a threshold visibility
     * @param visibilityHelper required for computing the visibility of each input patient
     */
    public FilteringIterator(Iterator<PrimaryEntity> input, Visibility thresholdVisibility,
        VisibilityHelper visibilityHelper)
    {
        this.input = input;
        this.thresholdVisibility = thresholdVisibility;
        this.visibilityHelper = visibilityHelper;
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

    /**
     * Finds the next entity that meets the visibility criteria.
     */
    private void findNext()
    {
        this.next = null;

        while (this.input.hasNext() && this.next == null) {
            PrimaryEntity potentialNextEntity = this.input.next();
            if (potentialNextEntity != null) {
                Visibility patientVisibility = this.visibilityHelper.getVisibility(potentialNextEntity);
                if (this.thresholdVisibility.compareTo(patientVisibility) <= 0) {
                    this.next = potentialNextEntity;
                }
            }
        }
    }
}
