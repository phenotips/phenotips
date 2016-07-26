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
package org.phenotips.data;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * Class for representing {@link PatientData patient data} organized as a simple unmodifiable value, although in case
 * the value is a custom object, its immutability is not guaranteed.
 *
 * @param <T> the type of data being managed by this component; since this is a simple value, this refers to its type
 * @version $Id$
 * @since 1.0M13
 */
public class SimpleValuePatientData<T> implements PatientData<T>
{
    /** The name of this custom data. */
    private final String name;

    /** The actual data. */
    private final T value;

    /**
     * Default constructor.
     *
     * @param name the name of this data
     * @param value the value to represent
     */
    public SimpleValuePatientData(String name, T value)
    {
        this.name = name;
        this.value = value;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public int size()
    {
        return 1;
    }

    @Override
    public T getValue()
    {
        return this.value;
    }

    @Override
    public Iterator<T> iterator()
    {
        return Collections.singleton(this.value).iterator();
    }

    @Override
    public boolean isIndexed()
    {
        return false;
    }

    @Override
    public boolean isNamed()
    {
        return false;
    }

    @Override
    public T get(int index)
    {
        return null;
    }

    @Override
    public T get(String name)
    {
        return null;
    }

    @Override
    public boolean containsKey(String key)
    {
        return false;
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
