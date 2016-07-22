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

import org.xwiki.stability.Unstable;

import java.util.Iterator;
import java.util.Map.Entry;

/**
 * Non-essential pieces of custom patient data that can be part of the patient record. The data can be structured in
 * three ways:
 * <ul>
 * <li>Simple values, for which {@link #getValue()} must be called. {@link SimpleValuePatientData} provides the basic
 * implementation.</li>
 * <li>Lists of values, for which {@link #get(int)} must be called. {@link IndexedPatientData} provides the basic
 * implementation.</li>
 * <li>A collection of named values (dictionary), for which {@link #get(String)} must be called.
 * {@link DictionaryPatientData} provides the basic implementation.</li>
 * </ul>
 *
 * @param <T> the type of data expected back
 * @version $Id$
 * @see PatientDataController
 * @since 1.0M10
 */
@Unstable
public interface PatientData<T> extends Iterable<T>
{
    /**
     * The name of this custom data.
     *
     * @return a short string
     */
    String getName();

    /**
     * Return the number of elements contained in this data. For a simple value, the size is always {@code 1}. For list
     * and dictionary values, the size is the size of the list or dictionary, respectively.
     *
     * @return a positive integer
     * @since 1.2M5
     */
    int size();

    /**
     * If this type of data is structured as a dictionary, will look up the value attached to the key.
     *
     * @param key the name of the value to return
     * @return the value attached to the key, if any, {@code null} if there's no value stored for this key or if this is
     *         not a dictionary type of data
     * @since 1.0M13
     */
    T get(String key);

    /**
     * If this type of data is structured as a list of values, will lookup the value stored at a specific index.
     *
     * @param index the index of the value to return
     * @return the value at the index, if any, {@code null} if there's no value stored at the specified index or if this
     *         is not an indexed type of data
     * @since 1.0M13
     */
    T get(int index);

    /**
     * If this type of data holds only a single value, return that value.
     *
     * @return the value stored for this type of patient data, if any, {@code null} if there's no value defined or if
     *         this is not a simple value type of patient data
     * @since 1.0M13
     */
    T getValue();

    /**
     * @return {@code true} if the data structure is index based
     * @since 1.0M13
     */
    boolean isIndexed();

    /**
     * @return {@code true} if the data structure is key-value based
     * @since 1.0M13
     */
    boolean isNamed();

    /**
     * For dictionary data only, checks if the given key is present in the dictionary data.
     *
     * @param key the key to be checked for
     * @return {@code true} if the key-value based data structure has the specified key
     * @since 1.3M2
     */
    boolean containsKey(String key);

    /**
     * For dictionary data only, return an iterator over the dictionary keys.
     *
     * @return iterator containing all the keys, or an empty iterator if there are no keys or this is not a dictionary
     *         type of data
     * @since 1.0M13
     */
    Iterator<String> keyIterator();

    /**
     * For dictionary data only, return an iterator over the dictionary entries.
     *
     * @return iterator containing all the data in this dictionary, or an empty iterator if there is no data or this is
     *         not a dictionary type of data
     * @since 1.0M13
     */
    Iterator<Entry<String, T>> dictionaryIterator();
}
