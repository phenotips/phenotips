package org.phenotips.data.internal;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.web.Utils;

import static org.mockito.Mockito.when;

public class LazyPatientIteratorTest
{
    @Mock
    private ComponentManager cm;

    @Mock
    private Provider<ComponentManager> mockProvider;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private Patient p1;

    @Mock
    private Patient p2;

    @Before
    public void setup() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        Utils.setComponentManager(this.cm);
        ReflectionUtils.setFieldValue(new ComponentManagerRegistry(), "cmProvider", this.mockProvider);
        when(this.mockProvider.get()).thenReturn(this.cm);
        when(this.cm.getInstance(PatientRepository.class)).thenReturn(this.patientRepository);
        when(this.patientRepository.getPatientById("Patient01")).thenReturn(this.p1);
        when(this.patientRepository.getPatientById("Patient02")).thenReturn(this.p2);
    }

    @Test
    public void emptyIteratorTest() throws ComponentLookupException
    {
        List<String> input = new LinkedList<>();
        LazyPatientIterator iterator = new LazyPatientIterator(input);
        Assert.assertFalse(iterator.hasNext());
    }

    @Test
    public void findNextPatientTest() throws NoSuchElementException
    {
        List<String> input = new LinkedList<>();
        String p1 = "Patient01";
        input.add(p1);
        String p2 = "Patient02";
        input.add(p2);

        LazyPatientIterator iterator = new LazyPatientIterator(input);
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(this.p1, iterator.next());
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(this.p2, iterator.next());
        Assert.assertFalse(iterator.hasNext());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void removeThrowsUnsupportedOperationException() throws UnsupportedOperationException
    {
        List<String> input = new LinkedList<>();
        LazyPatientIterator iterator = new LazyPatientIterator(input);
        iterator.remove();
    }

}
