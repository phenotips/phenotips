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

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractNumericValueMap<K, N extends Number> extends LinkedHashMap<K, N> implements
    NumericValueMap<K, N>
{
    private static final long serialVersionUID = 1L;

    public AbstractNumericValueMap()
    {
        super();
    }

    public AbstractNumericValueMap(int initialCapacity)
    {
        super(initialCapacity);
    }

    @Override
    public N addTo(K key, N value)
    {
        N crtValue = this.get(key);
        if (crtValue == null) {
            return this.put(key, value);
        } else {
            return this.put(key, value);
        }
    }

    protected abstract N getZero();

    @Override
    public N reset(K key)
    {
        return this.put(key, getZero());
    }

    @Override
    public N safeGet(K key)
    {
        N value = this.get(key);
        return value == null ? getZero() : value;
    }

    @Override
    public List<K> sort()
    {
        return this.sort(false);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<K> sort(final boolean descending)
    {
        K[] sortedKeys = (K[]) this.keySet().toArray();
        Arrays.sort(sortedKeys, new Comparator<K>()
        {
            @Override
            public int compare(K a, K b)
            {
                if (safeGet(a).equals(safeGet(b))) {
                    return 0;
                }
                try {
                    return (descending ? -1 : 1) * (((Comparable<N>) safeGet(a)).compareTo(safeGet(b)));
                } catch (ClassCastException ex) {
                    return 0;
                }
            }
        });
        List<K> result = new LinkedList<K>();
        for (K key : sortedKeys) {
            result.add(key);
        }
        return result;
    }

    @Override
    public K getMax()
    {
        if (this.size() == 0) {
            return null;
        }
        return this.sort(true).get(0);
    }

    @Override
    public K getMin()
    {
        if (this.size() == 0) {
            return null;
        }
        return this.sort().get(0);
    }

    @Override
    public N getMaxValue()
    {
        return this.safeGet(getMax());
    }

    @Override
    public N getMinValue()
    {
        return this.safeGet(getMin());
    }

    public void writeTo(PrintStream out)
    {
        for (K key : this.keySet()) {
            out.println(key + " : " + this.get(key));
        }
    }
}
