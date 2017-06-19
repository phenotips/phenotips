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

import java.util.Iterator;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SimpleValuePatientDataTest
{
    @Mock
    private Object val;

    private PatientData<Object> dataset;

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        this.dataset = new SimpleValuePatientData<>("name", this.val);
    }

    @Test
    public void isASimpleDataset() throws ComponentLookupException
    {
        Assert.assertEquals("name", this.dataset.getName());
        Assert.assertEquals(1, this.dataset.size());

        // Is not a named dataset
        Assert.assertFalse(this.dataset.isNamed());
        Assert.assertFalse(this.dataset.containsKey("0"));
        Assert.assertNull(this.dataset.get("0"));

        // Is not an indexed dataset
        Assert.assertFalse(this.dataset.isIndexed());
        Assert.assertNull(this.dataset.get(0));

        // Is a simple value dataset
        Assert.assertSame(this.val, this.dataset.getValue());
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
        Assert.assertSame(this.val, it.next());
        Assert.assertFalse(it.hasNext());
    }

    @Test
    public void keyIteratorIsEmpty()
    {
        Iterator<String> kit = this.dataset.keyIterator();
        Assert.assertFalse(kit.hasNext());
    }

    @Test
    public void nullInternalValueGetsNullValue() throws ComponentLookupException
    {
        this.dataset = new SimpleValuePatientData<>("name", null);

        Assert.assertEquals("name", this.dataset.getName());
        Assert.assertEquals(1, this.dataset.size());

        // Is not a named dataset
        Assert.assertFalse(this.dataset.isNamed());
        Assert.assertFalse(this.dataset.containsKey("0"));
        Assert.assertNull(this.dataset.get("0"));

        // Is not an indexed dataset
        Assert.assertFalse(this.dataset.isIndexed());
        Assert.assertNull(this.dataset.get(0));

        // Is a simple value dataset
        Assert.assertNull(this.dataset.getValue());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void removingDataThroughIteratorIsNotAllowed()
    {
        Iterator<Object> it = this.dataset.iterator();
        it.next();
        it.remove();
    }
}
