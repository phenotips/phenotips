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
package org.phenotips.obo2solr.maps;

import java.util.List;
import java.util.Map;
import java.util.Set;

interface NumericValueMap<K, N extends Number>
{
    N addTo(K key, N value);

    N reset(K key);

    N get(K key);

    N safeGet(K key);

    List<K> sort();

    List<K> sort(final boolean descending);

    K getMax();

    K getMin();

    N getMaxValue();

    N getMinValue();

    void clear();

    boolean containsKey(K key);

    boolean isEmpty();

    Set<K> keySet();

    N put(K key, N value);

    void putAll(Map<? extends K, ? extends N> m);

    N remove(K key);

    int size();
}
