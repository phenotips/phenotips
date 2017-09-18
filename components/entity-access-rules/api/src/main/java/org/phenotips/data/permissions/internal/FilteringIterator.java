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

import org.phenotips.data.Patient;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.data.permissions.Visibility;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Filters an iterator over {@link Patient}s, returning only those that have their
 * {@link org.phenotips.data.permissions.PatientAccess#getVisibility() visibility} equal or above a threshold. The
 * {@link #remove()} method is not supported.
 *
 * @version $Id$
 * @since 1.3M2
 */
public class FilteringIterator implements Iterator<Patient>
{
    private final Iterator<Patient> input;

    private final Visibility thresholdVisibility;

    private final PermissionsManager permissionsManager;

    private Patient next;

    /**
     * Basic constructor.
     *
     * @param input the iterator to filter, {@code null} and empty iterators are accepted
     * @param thresholdVisibility a threshold visibility; if {@code null}, then the input is returned unchanged
     * @param permissionsManager required for computing the visibility of each input patient
     */
    public FilteringIterator(Iterator<Patient> input, Visibility thresholdVisibility,
        PermissionsManager permissionsManager)
    {
        this.input = input;
        this.thresholdVisibility = thresholdVisibility;
        this.permissionsManager = permissionsManager;
        this.findNext();
    }

    @Override
    public boolean hasNext()
    {
        return this.next != null;
    }

    @Override
    public Patient next()
    {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        Patient toReturn = this.next;
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
            Patient potentialNextPatient = this.input.next();
            if (potentialNextPatient != null) {
                Visibility patientVisibility =
                    this.permissionsManager.getPatientAccess(potentialNextPatient).getVisibility();
                if (this.thresholdVisibility.compareTo(patientVisibility) <= 0) {
                    this.next = potentialNextPatient;
                }
            }
        }
    }
}
