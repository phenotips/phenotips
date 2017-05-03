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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;

public class DictionaryPatientDataTest<T> {

    @Test
    public void test() throws ComponentLookupException {
        T val1 = null;
        T val2 = null;
        Map<String, T> data = new LinkedHashMap<String, T>();
        data.put("key1", val1);
        data.put("key2", val2);
        
        DictionaryPatientData<T> dictData = new DictionaryPatientData<T>("name", data);
        
        Assert.assertEquals("name", dictData.getName());
        Assert.assertSame(2, dictData.size());
        Assert.assertNull(dictData.getValue());
        Assert.assertFalse(dictData.isIndexed());
        Assert.assertTrue(dictData.isNamed());
        Assert.assertNull(dictData.get(0));
        Assert.assertNull(dictData.get("test"));
        Assert.assertFalse(dictData.containsKey("key"));
    }
    
    @Test
    public void nullInternalListTest() throws ComponentLookupException {
        Map<String, T> data = new LinkedHashMap<String, T>();
        DictionaryPatientData<T> dictData = new DictionaryPatientData<T>("name", data);
        Assert.assertNull(dictData.get(0));
    }

    @Test
    public void iteratorTest() throws ComponentLookupException {
        T val1 = null;
        Map<String, T> data = new LinkedHashMap<String, T>();
        data.put("key1", val1);
        DictionaryPatientData<T> dictData = new DictionaryPatientData<T>("name", data);
        
        Assert.assertTrue(dictData.iterator().hasNext());
        Assert.assertTrue(dictData.keyIterator().hasNext());
        Assert.assertTrue(dictData.dictionaryIterator().hasNext());
    }
    

}
