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
package org.phenotips.obo2solr.maps;

public abstract class AbstractMapOperator<K, N extends Number> implements MapOperator<K, N>
{
    @Override
    @SuppressWarnings("unchecked")
    public NumericValueMap<K, N> apply(NumericValueMap<K, N> a, NumericValueMap<K, N> b)
    {
        if (!a.keySet().equals(b.keySet())) {
            return null;
        }
        DoubleMap<K> result = new DoubleMap<>();
        for (K key : a.keySet()) {
            result.put(key, this.applyToValues(a.get(key), b.get(key)));
        }
        return (NumericValueMap<K, N>) result;
    }

    protected abstract Double applyToValues(N a, N b);
}
