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
package org.phenotips.util.maps;

public abstract class AbstractMapOperator<K, N extends Number> implements MapOperator<K, N>
{
    @Override
    @SuppressWarnings("unchecked")
    public NumericValueMap<K, N> apply(NumericValueMap<K, N> a, NumericValueMap<K, N> b)
    {
        if (!a.keySet().equals(b.keySet())) {
            return null;
        }
        DoubleMap<K> result = new DoubleMap<K>();
        for (K key : a.keySet()) {
            result.put(key, this.applyToValues(a.get(key), b.get(key)));
        }
        return (NumericValueMap<K, N>) result;
    }

    protected abstract Double applyToValues(N a, N b);
}
