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

import java.util.Iterator;

/**
 * Non-essential pieces of custom patient data that can be part of the patient record.
 *
 * @param <T> the type of data expected back
 * @version $Id$
 * @see PatientDataController
 * @since 1.0M10
 */
public interface PatientData<T> extends Iterable<T>
{
    /**
     * The name of this custom data.
     *
     * @return a short string
     */
    String getName();

    /**
     * If the underlying concrete class is structured as or akin to a map, will look up value attached to the key.
     *
     * @param key the name of the value to return
     * @return the value attached to the key
     */
    T get(String key);

    /**
     * If the underlying concrete class is structured as or akin to a list, will perform a lookup value attached to the
     * index.
     *
     * @param index for which to search
     * @return the value at the index
     */
    T get(int index);

    /**
     * Used if the underlying concrete class holds only a single value.
     *
     * @return the value stored in the class
     */
    T getValue();

    /**
     * @return true if the data structure is index based
     */
    Boolean isIndexed();

    /**
     * @return true if the data structure is key-value based
     */
    Boolean isNamed();

    /**
     * For named data only.
     * @param <K> type of keys
     * @return iterator containing all the keys
     */
    <K> Iterator<K> keyIterator();
}
