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
