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

import org.phenotips.data.ContactInfo;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientContactProvider;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.List;

import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

/**
 * Tests for the {@link PatientContactProvider} provider, {@link PatientContactProviderListProvider}.
 *
 * @version $Id$
 */
public class PatientContactProviderListProviderTest
{
    @Rule
    public final MockitoComponentMockingRule<Provider<List<PatientContactProvider>>> mocker =
        new MockitoComponentMockingRule<Provider<List<PatientContactProvider>>>(
            PatientContactProviderListProvider.class);

    @Mock
    private PatientContactProvider provider1;

    @Mock
    private PatientContactProvider provider2;

    @Mock
    private PatientContactProvider provider3;

    @Before
    public void setupComponents() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        this.mocker.registerComponent(ComponentManager.class, "wiki", this.mocker);
    }

    @Test
    public void providersAreSortedByPriorityFirst() throws Exception
    {
        when(this.provider1.getPriority()).thenReturn(100);
        when(this.provider2.getPriority()).thenReturn(200);
        when(this.provider3.getPriority()).thenReturn(300);

        this.mocker.registerComponent(PatientContactProvider.class, "y", this.provider2);
        this.mocker.registerComponent(PatientContactProvider.class, "z", this.provider1);
        this.mocker.registerComponent(PatientContactProvider.class, "x", this.provider3);

        List<PatientContactProvider> result = this.mocker.getComponentUnderTest().get();
        Assert.assertEquals(3, result.size());
        Assert.assertSame(this.provider1, result.get(0));
        Assert.assertSame(this.provider2, result.get(1));
        Assert.assertSame(this.provider3, result.get(2));
    }

    @Test
    public void providersAreSortedByClassnameLast() throws Exception
    {
        this.provider1 = new ProviderA();
        this.provider2 = new ProviderB();
        this.provider3 = new ProviderC();
        this.mocker.registerComponent(PatientContactProvider.class, "y", this.provider1);
        this.mocker.registerComponent(PatientContactProvider.class, "z", this.provider2);
        this.mocker.registerComponent(PatientContactProvider.class, "x", this.provider3);

        List<PatientContactProvider> result = this.mocker.getComponentUnderTest().get();
        Assert.assertEquals(3, result.size());
        Assert.assertSame(this.provider1, result.get(0));
        Assert.assertSame(this.provider2, result.get(1));
        Assert.assertSame(this.provider3, result.get(2));
    }

    @Test
    public void emptyListReturnedWhenNoProvidersAreAvailable() throws ComponentLookupException
    {
        List<PatientContactProvider> result = this.mocker.getComponentUnderTest().get();
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void emptyListReturnedWhenProvidersLookupThrowsException() throws Exception
    {
        ComponentManager cm = Mockito.mock(ComponentManager.class);
        when(cm.getInstanceList(PatientContactProvider.class)).thenThrow(new ComponentLookupException("failed"));
        this.mocker.registerComponent(ComponentManager.class, "wiki", cm);
        List<PatientContactProvider> result = this.mocker.getComponentUnderTest().get();
        Assert.assertTrue(result.isEmpty());
    }

    static class ProviderA implements PatientContactProvider
    {
        @Override
        public int getPriority()
        {
            return 100;
        }

        @Override
        public List<ContactInfo> getContacts(Patient patient)
        {
            return null;
        }
    }

    static class ProviderB implements PatientContactProvider
    {
        @Override
        public int getPriority()
        {
            return 100;
        }

        @Override
        public List<ContactInfo> getContacts(Patient patient)
        {
            return null;
        }
    }

    static class ProviderC implements PatientContactProvider
    {
        @Override
        public int getPriority()
        {
            return 100;
        }

        @Override
        public List<ContactInfo> getContacts(Patient patient)
        {
            return null;
        }
    }
}
