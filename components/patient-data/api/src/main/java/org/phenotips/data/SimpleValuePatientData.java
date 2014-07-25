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
