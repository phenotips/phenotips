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

import org.xwiki.component.manager.ComponentLookupException;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class IndexedPatientDataTest
{
    @Test
    public void test() throws ComponentLookupException
    {
        String name = "name";
        Object val1 = new Object();
        Object val2 = new Object();
        List<Object> data = new ArrayList<>();
        data.add(val1);
        data.add(val2);

        IndexedPatientData<Object> indexedData = new IndexedPatientData<>(name, data);

        Assert.assertEquals("name", indexedData.getName());
        Assert.assertSame(2, indexedData.size());
        Assert.assertNull(indexedData.getValue());
        Assert.assertTrue(indexedData.isIndexed());
        Assert.assertFalse(indexedData.isNamed());
        Assert.assertNull(indexedData.get(0));
        Assert.assertNull(indexedData.get("test"));
        Assert.assertFalse(indexedData.containsKey("key"));
    }

    @Test
    public void nullInternalListTest() throws ComponentLookupException
    {
        String name = "name";
        List<Object> data = new ArrayList<>();
        IndexedPatientData<Object> indexedData = new IndexedPatientData<>(name, data);
        Assert.assertNull(indexedData.get(0));
    }

    @Test
    public void iteratorTest() throws ComponentLookupException
    {
        String name = "name";
        Object val1 = new Object();
        List<Object> data = new ArrayList<>();
        data.add(val1);

        IndexedPatientData<Object> indexedData = new IndexedPatientData<>(name, data);
        Assert.assertFalse(indexedData.keyIterator().hasNext());
        Assert.assertFalse(indexedData.dictionaryIterator().hasNext());
        Assert.assertTrue(indexedData.iterator().hasNext());
    }

}
