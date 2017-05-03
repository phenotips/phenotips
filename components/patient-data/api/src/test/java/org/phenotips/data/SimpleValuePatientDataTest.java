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

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;

public class SimpleValuePatientDataTest<T> {

    @Test
    public void getTest() throws ComponentLookupException{
        String name = "name";
        T value = null;
        SimpleValuePatientData<T> data = new SimpleValuePatientData<T>(name, value);
        Assert.assertEquals("name", data.getName());
        Assert.assertSame(1, data.size());
        Assert.assertNull(data.getValue());
        Assert.assertFalse(data.isIndexed());
        Assert.assertFalse(data.isNamed());
        Assert.assertNull(data.get(0));
        Assert.assertNull(data.get("test"));
        Assert.assertFalse(data.containsKey("key"));
    }

    @Test
    public void iteratorTest() throws ComponentLookupException {
        String name = "name";
        T value = null;
        SimpleValuePatientData<T> data = new SimpleValuePatientData<T>(name, value);
        Assert.assertTrue(data.iterator().hasNext());
        Assert.assertFalse(data.dictionaryIterator().hasNext());
    }
    
    @Test
    public void emptyKeyIteratorTest() throws ComponentLookupException {
        String name = "name";
        T value = null;
        SimpleValuePatientData<T> data = new SimpleValuePatientData<T>(name, value);
        Assert.assertFalse(data.keyIterator().hasNext());
    }
}
