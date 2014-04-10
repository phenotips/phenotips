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
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Class for representing {@link PatientData patient data} organized as a list of simple key-value pairs, where the key
 * is a string (name) and the value is an object of a specific type. This list is immutable, and the each pair in the
 * list is also immutable; but the immutability of the values is not guaranteed, although recommended.
 * 
 * @param <T> the type of data being managed by this component; since this is a list of key-value pairs, this refers to
 *            the type of the values stored on the right side of the pairs
 * @version $Id$
 * @since 1.0M10
 */
public class SimpleNamedData<T> extends LinkedList<ImmutablePair<String, T>>
    implements PatientData<ImmutablePair<String, T>>
{
    private static final long serialVersionUID = 1L;

    private final String name;

    /**
     * Default constructor copying the values from a list.
     * 
     * @param name the name of this data
     * @param data the list of values to represent; an immutable version of the list is used internally and returned
     */
    public SimpleNamedData(String name, List<ImmutablePair<String, T>> data)
    {
        super(Collections.unmodifiableList(data));
        this.name = name;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    /**
     * Shortcut method for getting a value from this list, the first one where the key is equal to the requested name.
     * 
     * @param name the name of the entry to retrieve
     * @return the value found in the pair with the key equal to the requested name; if more than one such pairs exists,
     *         the value from the first one is returned; if no such pair exists, {@code null} is returned
     */
    public T get(String name)
    {
        for (Pair<String, T> entry : this) {
            if (StringUtils.equals(entry.getKey(), name)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
