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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class IndexedPatientDataTest
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
        List<Object> data = new ArrayList<>(2);
        data.add(this.val1);
        data.add(this.val2);
        this.dataset = new IndexedPatientData<>("name", data);
    }

    @Test
    public void isAnIndexedDataset() throws ComponentLookupException
    {
        Assert.assertEquals("name", this.dataset.getName());
        Assert.assertEquals(2, this.dataset.size());

        // Is not a named dataset
        Assert.assertFalse(this.dataset.isNamed());
        Assert.assertFalse(this.dataset.containsKey("0"));
        Assert.assertNull(this.dataset.get("0"));

        // Is not a simple dataset
        Assert.assertNull(this.dataset.getValue());

        // Is an indexed dataset
        Assert.assertTrue(this.dataset.isIndexed());
        Assert.assertSame(this.val1, this.dataset.get(0));
        Assert.assertSame(this.val2, this.dataset.get(1));
        Assert.assertNull(this.dataset.get(2));
        Assert.assertNull(this.dataset.get(-1));
    }

    @Test
    public void dictionaryIteratorIsEmpty()
    {
        Iterator<Entry<String, Object>> dit = this.dataset.dictionaryIterator();
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
    public void keyIteratorIsEmpty()
    {
        Iterator<String> kit = this.dataset.keyIterator();
        Assert.assertFalse(kit.hasNext());
    }

    @Test
    public void emptyInternalListGetsEmptyDataset() throws ComponentLookupException
    {
        this.dataset = new IndexedPatientData<>("name", Collections.emptyList());

        Assert.assertEquals("name", this.dataset.getName());
        Assert.assertEquals(0, this.dataset.size());

        // Is not a named dataset
        Assert.assertFalse(this.dataset.isNamed());
        Assert.assertNull(this.dataset.get("0"));

        // Is not a simple dataset
        Assert.assertNull(this.dataset.getValue());

        // Is an indexed dataset
        Assert.assertTrue(this.dataset.isIndexed());
        Assert.assertNull(this.dataset.get(0));
    }

    @Test
    public void nullInternalListGetsEmptyDataset() throws ComponentLookupException
    {
        this.dataset = new IndexedPatientData<>("name", null);

        Assert.assertEquals("name", this.dataset.getName());
        Assert.assertEquals(0, this.dataset.size());

        // Is not a named dataset
        Assert.assertFalse(this.dataset.isNamed());
        Assert.assertNull(this.dataset.get(0));

        // Is not a simple dataset
        Assert.assertNull(this.dataset.getValue());

        // Is an indexed dataset
        Assert.assertTrue(this.dataset.isIndexed());
        Assert.assertNull(this.dataset.get(0));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void removingDataThroughIteratorIsNotAllowed()
    {
        Iterator<Object> it = this.dataset.iterator();
        it.next();
        it.remove();
    }
}
