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
