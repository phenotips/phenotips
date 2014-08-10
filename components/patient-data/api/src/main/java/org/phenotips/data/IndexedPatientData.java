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
package org.phenotips.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

/**
 * Class for representing {@link PatientData patient data} organized as a list of values of a specific type. This list
 * is immutable, and each value in the list should also be immutable, although the immutability of the values is not
 * guaranteed.
 *
 * @param <T> the type of data being managed by this component
 * @version $Id$
 * @since 1.0M13
 */
public class IndexedPatientData<T> implements PatientData<T>
{
    /** The name of this custom data. */
    private final String name;

    /** The actual data. */
    private final List<T> internalList;

    /**
     * Default constructor copying the values into an internal unmodifiable list.
     *
     * @param name the name of this data
     * @param data the list of values to represent
     */
    public IndexedPatientData(String name, List<T> data)
    {
        this.name = name;
        this.internalList = Collections.unmodifiableList(new ArrayList<T>(data));
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public T get(int index)
    {
        if (this.internalList == null || index < 0 || index >= this.internalList.size()) {
            return null;
        }
        return this.internalList.get(index);
    }

    @Override
    public Iterator<T> iterator()
    {
        if (this.internalList != null) {
            return this.internalList.iterator();
        }
        return Collections.emptyIterator();
    }

    @Override
    public boolean isIndexed()
    {
        return true;
    }

    @Override
    public boolean isNamed()
    {
        return false;
    }

    @Override
    public T getValue()
    {
        return null;
    }

    @Override
    public T get(String name)
    {
        return null;
    }

    @Override
    public Iterator<String> keyIterator()
    {
        return Collections.emptyIterator();
    }

    @Override
    public Iterator<Entry<String, T>> dictionaryIterator()
    {
        return Collections.emptyIterator();
    }
}
