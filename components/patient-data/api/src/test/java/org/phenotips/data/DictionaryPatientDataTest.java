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
