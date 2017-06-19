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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DictionaryPatientDataTest
{
    @Mock
    private Object val1;

    @Mock
    private Object val2;

    private PatientData<Object> dataset;

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("key1", this.val1);
        data.put("key2", this.val2);
        this.dataset = new DictionaryPatientData<>("name", data);
    }

    @Test
    public void isANamedDataset() throws ComponentLookupException
    {
        Assert.assertEquals("name", this.dataset.getName());
        Assert.assertEquals(2, this.dataset.size());

        // Is not an indexed dataset
        Assert.assertFalse(this.dataset.isIndexed());
        Assert.assertNull(this.dataset.get(0));

        // Is not a simple dataset
        Assert.assertNull(this.dataset.getValue());

        // Is a named dataset
        Assert.assertTrue(this.dataset.isNamed());
        Assert.assertTrue(this.dataset.containsKey("key1"));
        Assert.assertSame(this.val1, this.dataset.get("key1"));
        Assert.assertTrue(this.dataset.containsKey("key2"));
        Assert.assertSame(this.val2, this.dataset.get("key2"));
        Assert.assertFalse(this.dataset.containsKey("key"));
        Assert.assertNull(this.dataset.get("key"));
    }

    @Test
    public void dictionaryIteratorWorks()
    {
        Iterator<Entry<String, Object>> dit = this.dataset.dictionaryIterator();
        Assert.assertTrue(dit.hasNext());
        Entry<String, Object> item = dit.next();
        Assert.assertEquals("key1", item.getKey());
        Assert.assertSame(this.val1, item.getValue());
        item = dit.next();
        Assert.assertEquals("key2", item.getKey());
        Assert.assertSame(this.val2, item.getValue());
        Assert.assertFalse(dit.hasNext());
    }

    @Test
    public void valueIteratorWorks()
    {
        Iterator<Object> it = this.dataset.iterator();
        Assert.assertTrue(it.hasNext());
        Assert.assertSame(this.val1, it.next());
        Assert.assertTrue(it.hasNext());
        Assert.assertSame(this.val2, it.next());
        Assert.assertFalse(it.hasNext());
    }

    @Test
    public void keyIteratorWorks()
    {
        Iterator<String> kit = this.dataset.keyIterator();
        Assert.assertTrue(kit.hasNext());
        Assert.assertSame("key1", kit.next());
        Assert.assertTrue(kit.hasNext());
        Assert.assertSame("key2", kit.next());
        Assert.assertFalse(kit.hasNext());
    }

    @Test
    public void emptyInternalMapGetsEmptyDataset() throws ComponentLookupException
    {
        this.dataset = new DictionaryPatientData<>("name", Collections.emptyMap());

        Assert.assertEquals("name", this.dataset.getName());
        Assert.assertEquals(0, this.dataset.size());

        // Is not an indexed dataset
        Assert.assertFalse(this.dataset.isIndexed());
        Assert.assertNull(this.dataset.get(0));

        // Is not a simple dataset
        Assert.assertNull(this.dataset.getValue());

        // Is a named dataset
        Assert.assertTrue(this.dataset.isNamed());
        Assert.assertFalse(this.dataset.containsKey("key1"));
        Assert.assertNull(this.dataset.get("key1"));
    }

    @Test
    public void nullInternalMapGetsEmptyDataset() throws ComponentLookupException
    {
        this.dataset = new DictionaryPatientData<>("name", null);

        Assert.assertEquals("name", this.dataset.getName());
        Assert.assertEquals(0, this.dataset.size());

        // Is not an indexed dataset
        Assert.assertFalse(this.dataset.isIndexed());
        Assert.assertNull(this.dataset.get(0));

        // Is not a simple dataset
        Assert.assertNull(this.dataset.getValue());

        // Is a named dataset
        Assert.assertTrue(this.dataset.isNamed());
        Assert.assertFalse(this.dataset.containsKey("key1"));
        Assert.assertNull(this.dataset.get("key1"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void removingDataThroughIteratorIsNotAllowed()
    {
        Iterator<Object> it = this.dataset.iterator();
        it.next();
        it.remove();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void removingDataThroughKeyIteratorIsNotAllowed()
    {
        Iterator<String> kit = this.dataset.keyIterator();
        kit.next();
        kit.remove();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void removingDataThroughDictionaryIteratorIsNotAllowed()
    {
        Iterator<Entry<String, Object>> dit = this.dataset.dictionaryIterator();
        dit.next();
        dit.remove();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void changingDataThroughDictionaryIteratorIsNotAllowed()
    {
        Iterator<Entry<String, Object>> dit = this.dataset.dictionaryIterator();
        dit.next().setValue(this.val2);
    }
}
