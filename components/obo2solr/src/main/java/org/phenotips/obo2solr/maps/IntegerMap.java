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

public class IntegerMap<K> extends AbstractNumericValueMap<K, Integer>
{
    private static final long serialVersionUID = 1L;

    public IntegerMap()
    {
        super();
    }

    public IntegerMap(int initialCapacity)
    {
        super(initialCapacity);
    }

    @Override
    protected final Integer getZero()
    {
        return 0;
    }

    @Override
    public Integer addTo(K key, Integer value)
    {
        Integer crtValue = this.get(key);
        if (value == null) {
            return this.put(key, value);
        } else {
            return this.put(key, crtValue + value);
        }
    }
}
