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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Class for representing {@link PatientData patient data} organized as a dictionary (map) of simple key-value pairs,
 * where the key is a string (name) and the value is an object of a specific type. This dictionary is immutable, and
 * each pair in the dictionary should also be immutable, although the immutability of the values is not guaranteed.
 *
 * @param <T> the type of data being managed by this component; since this is a dictionary of key-value pairs, this
 *            refers to the type of the values stored on the right side of the pairs
 * @version $Id$
 * @since 1.0RC1
 */
public class DictionaryPatientData<T> implements PatientData<T>
{
    /** The name of this custom data. */
    private final String name;

    /** The actual data. */
    private final Map<String, T> internalMap;

    /**
     * Default constructor copying the values into an internal unmodifiable map.
     *
     * @param name the name of this data
     * @param data the map of values to represent
     */
    public DictionaryPatientData(String name, Map<String, T> data)
    {
        this.name = name;
        this.internalMap = Collections.unmodifiableMap(new LinkedHashMap<String, T>(data));
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public T get(String name)
    {
        if (this.internalMap != null) {
            return this.internalMap.get(name);
        }
        return null;
    }

    @Override
    public Iterator<T> iterator()
    {
        if (this.internalMap != null) {
            return this.internalMap.values().iterator();
        }
        return Collections.emptyIterator();
    }

    @Override
    public Iterator<String> keyIterator()
    {
        if (this.internalMap != null) {
            return this.internalMap.keySet().iterator();
        }
        return Collections.emptyIterator();
    }

    @Override
    public Iterator<Entry<String, T>> dictionaryIterator()
    {
        if (this.internalMap != null) {
            return this.internalMap.entrySet().iterator();
        }
        return Collections.emptyIterator();
    }

    @Override
    public boolean isIndexed()
    {
        return false;
    }

    @Override
    public boolean isNamed()
    {
        return true;
    }

    @Override
    public T getValue()
    {
        return null;
    }

    @Override
    public T get(int index)
    {
        return null;
    }
}
