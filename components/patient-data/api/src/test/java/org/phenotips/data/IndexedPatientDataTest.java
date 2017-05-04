package org.phenotips.data;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;

public class IndexedPatientDataTest<T> {

    @Test
    public void test() throws ComponentLookupException {
        String name = "name";
        T val1 = null;
        T val2 = null;
        List<T> data = new ArrayList<T>();
        data.add(val1);
        data.add(val2);
        
        IndexedPatientData<T> indexedData = new IndexedPatientData<T>(name, data);
        
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
    public void nullInternalListTest() throws ComponentLookupException {
        String name = "name";
        List<T> data = new ArrayList<T>();
        IndexedPatientData<T> indexedData = new IndexedPatientData<T>(name, data);
        Assert.assertNull(indexedData.get(0));
    }

    @Test
    public void iteratorTest() throws ComponentLookupException {
        String name = "name";
        T val1 = null;
        List<T> data = new ArrayList<T>();
        data.add(val1);
        
        IndexedPatientData<T> indexedData = new IndexedPatientData<T>(name, data);
        Assert.assertFalse(indexedData.keyIterator().hasNext());
        Assert.assertFalse(indexedData.dictionaryIterator().hasNext());
        Assert.assertTrue(indexedData.iterator().hasNext());
    }
    
    
}
