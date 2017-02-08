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
import org.phenotips.data.PatientContactsManager;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

/**
 * Tests for the default {@link PatientContactsManager} implementation, {@link DefaultPatientContactsManager}.
 *
 * @version $Id$
 */
public class DefaultPatientContactsManagerTest
{
    @Rule
    public final MockitoComponentMockingRule<PatientContactsManager> mocker =
        new MockitoComponentMockingRule<PatientContactsManager>(DefaultPatientContactsManager.class);

    @Mock
    private Patient patient;

    @Mock
    private PatientContactProvider provider1;

    @Mock
    private PatientContactProvider provider2;

    @Mock
    private Provider<List<PatientContactProvider>> providersProvider;

    @Mock
    private ContactInfo contact1;

    @Mock
    private ContactInfo contact2;

    @Mock
    private ContactInfo contact3;

    @Before
    public void setupComponents() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        ParameterizedType listType =
            new DefaultParameterizedType(null, List.class, PatientContactProvider.class);
        ParameterizedType providerType =
            new DefaultParameterizedType(null, Provider.class, listType);
        this.mocker.registerComponent(providerType, this.providersProvider);
    }

    @Test
    public void contactsAreCollectedInOrderOfProviders() throws ComponentLookupException
    {
        List<ContactInfo> contacts1 = new ArrayList<>();
        contacts1.add(this.contact1);
        contacts1.add(this.contact2);
        List<ContactInfo> contacts2 = new ArrayList<>();
        contacts2.add(this.contact3);
        when(this.provider1.getContacts(this.patient)).thenReturn(contacts1);
        when(this.provider2.getContacts(this.patient)).thenReturn(contacts2);
        List<PatientContactProvider> providers = new ArrayList<>();
        providers.add(this.provider1);
        providers.add(this.provider2);
        when(this.providersProvider.get()).thenReturn(providers);

        List<ContactInfo> result = this.mocker.getComponentUnderTest().getAll(this.patient);
        Assert.assertEquals(3, result.size());
        Assert.assertSame(this.contact1, result.get(0));
        Assert.assertSame(this.contact2, result.get(1));
        Assert.assertSame(this.contact3, result.get(2));

        // Let's swap the order of the providers, check that the contacts are swapped too
        providers = new ArrayList<>();
        providers.add(this.provider2);
        providers.add(this.provider1);
        when(this.providersProvider.get()).thenReturn(providers);
        result = this.mocker.getComponentUnderTest().getAll(this.patient);
        Assert.assertEquals(3, result.size());
        Assert.assertSame(this.contact3, result.get(0));
        Assert.assertSame(this.contact1, result.get(1));
        Assert.assertSame(this.contact2, result.get(2));
    }

    @Test
    public void nullContactsAreIgnored() throws ComponentLookupException
    {
        List<ContactInfo> contacts = new ArrayList<>();
        contacts.add(this.contact1);
        contacts.add(this.contact2);
        when(this.provider1.getContacts(this.patient)).thenReturn(null);
        when(this.provider2.getContacts(this.patient)).thenReturn(contacts);
        List<PatientContactProvider> providers = new ArrayList<>();
        providers.add(this.provider1);
        providers.add(this.provider2);
        when(this.providersProvider.get()).thenReturn(providers);

        List<ContactInfo> result = this.mocker.getComponentUnderTest().getAll(this.patient);
        Assert.assertEquals(2, result.size());
        Assert.assertSame(this.contact1, result.get(0));
        Assert.assertSame(this.contact2, result.get(1));
    }

    @Test
    public void emptyListReturnedFromNoContacts() throws ComponentLookupException
    {
        when(this.provider1.getContacts(this.patient)).thenReturn(null);
        List<PatientContactProvider> providers = new ArrayList<>();
        providers.add(this.provider1);
        when(this.providersProvider.get()).thenReturn(providers);
        List<ContactInfo> result = this.mocker.getComponentUnderTest().getAll(this.patient);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void emptyListReturnedFromNoProviders() throws ComponentLookupException
    {
        when(this.providersProvider.get()).thenReturn(Collections.<PatientContactProvider>emptyList());
        List<ContactInfo> result = this.mocker.getComponentUnderTest().getAll(this.patient);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void firstContactIsReturned() throws ComponentLookupException
    {
        List<ContactInfo> contacts1 = new ArrayList<>();
        contacts1.add(this.contact1);
        contacts1.add(this.contact2);
        List<ContactInfo> contacts2 = new ArrayList<>();
        contacts2.add(this.contact3);
        when(this.provider1.getContacts(this.patient)).thenReturn(contacts1);
        when(this.provider2.getContacts(this.patient)).thenReturn(contacts2);
        List<PatientContactProvider> providers = new ArrayList<>();
        providers.add(this.provider1);
        providers.add(this.provider2);
        when(this.providersProvider.get()).thenReturn(providers);

        Assert.assertSame(this.contact1, this.mocker.getComponentUnderTest().getFirst(this.patient));

        // Let's swap the order of the providers, check that the contacts are swapped too
        providers = new ArrayList<>();
        providers.add(this.provider2);
        providers.add(this.provider1);
        when(this.providersProvider.get()).thenReturn(providers);
        Assert.assertSame(this.contact3, this.mocker.getComponentUnderTest().getFirst(this.patient));

    }

    @Test
    public void nullFirstReturnedFromNoContacts() throws ComponentLookupException
    {
        when(this.provider1.getContacts(this.patient)).thenReturn(null);
        List<PatientContactProvider> providers = new ArrayList<>();
        providers.add(this.provider1);
        when(this.providersProvider.get()).thenReturn(providers);
        Assert.assertNull(this.mocker.getComponentUnderTest().getFirst(this.patient));
    }

    @Test
    public void nullFirstReturnedFromNoProviders() throws ComponentLookupException
    {
        when(this.providersProvider.get()).thenReturn(Collections.<PatientContactProvider>emptyList());
        Assert.assertNull(this.mocker.getComponentUnderTest().getFirst(this.patient));
    }
}
