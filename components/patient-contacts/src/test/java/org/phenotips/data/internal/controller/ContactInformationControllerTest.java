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
package org.phenotips.data.internal.controller;

import org.phenotips.data.ContactInfo;
import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientContactsManager;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.SimpleValuePatientData;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Arrays;
import java.util.Collections;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

/**
 * Tests for the {@link PatientDataController} implementation responsible for getting contact information,
 * {@link ContactInformationController}.
 *
 * @version $Id$
 */
public class ContactInformationControllerTest
{
    @Rule
    public final MockitoComponentMockingRule<PatientDataController<ContactInfo>> mocker =
        new MockitoComponentMockingRule<PatientDataController<ContactInfo>>(ContactInformationController.class);

    @Mock
    private Patient patient;

    @Mock
    private ContactInfo contact1;

    @Mock
    private ContactInfo contact2;

    @Mock
    private ContactInfo contact3;

    private PatientContactsManager manager;

    @Before
    public void setupComponents() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        this.manager = this.mocker.getInstance(PatientContactsManager.class);
        when(this.contact1.toJSON()).thenReturn(new JSONObject("{\"id\":\"contact1\"}"));
        when(this.contact2.toJSON()).thenReturn(new JSONObject("{\"id\":\"contact2\"}"));
        when(this.contact3.toJSON()).thenReturn(new JSONObject("{\"id\":\"contact3\"}"));
    }

    @Test
    public void loadReturnsContactsFromTheManager() throws ComponentLookupException
    {
        when(this.manager.getAll(this.patient)).thenReturn(Arrays.asList(this.contact1, this.contact2, this.contact3));
        PatientData<ContactInfo> result = this.mocker.getComponentUnderTest().load(this.patient);
        Assert.assertTrue(result.isIndexed());
        Assert.assertEquals(3, result.size());
        Assert.assertSame(this.contact1, result.get(0));
        Assert.assertSame(this.contact2, result.get(1));
        Assert.assertSame(this.contact3, result.get(2));
    }

    @Test
    public void loadReturnsNullWhenNoContactsAvailable() throws ComponentLookupException
    {
        when(this.manager.getAll(this.patient)).thenReturn(null);
        PatientData<ContactInfo> result = this.mocker.getComponentUnderTest().load(this.patient);
        Assert.assertNull(result);

        when(this.manager.getAll(this.patient)).thenReturn(Collections.<ContactInfo>emptyList());
        result = this.mocker.getComponentUnderTest().load(this.patient);
        Assert.assertNull(result);
    }

    @Test
    public void writeDoesNothingWhenNotEnabled() throws ComponentLookupException
    {
        when(this.manager.getAll(this.patient)).thenReturn(Collections.singletonList(this.contact1));
        JSONObject container = new JSONObject();
        this.mocker.getComponentUnderTest().writeJSON(this.patient, container, Arrays.asList("phenotype", "disorders"));
        Assert.assertEquals(0, container.length());
    }

    @Test
    public void writeOutputsAllContacts() throws ComponentLookupException
    {
        when(this.patient.<ContactInfo>getData("contact")).thenReturn(
            new IndexedPatientData<>("contact", Arrays.asList(this.contact1, this.contact3, this.contact2)));
        JSONObject container = new JSONObject();
        this.mocker.getComponentUnderTest().writeJSON(this.patient, container);
        Assert.assertEquals(1, container.length());
        JSONArray contacts = container.getJSONArray("contact");
        Assert.assertTrue(this.contact1.toJSON().similar(contacts.get(0)));
        Assert.assertTrue(this.contact3.toJSON().similar(contacts.get(1)));
        Assert.assertTrue(this.contact2.toJSON().similar(contacts.get(2)));
    }

    @Test
    public void writeOutputsContactsWhenEnabled() throws ComponentLookupException
    {
        when(this.patient.<ContactInfo>getData("contact")).thenReturn(
            new IndexedPatientData<>("contact", Arrays.asList(this.contact1, this.contact3, this.contact2)));
        JSONObject container = new JSONObject();
        this.mocker.getComponentUnderTest().writeJSON(this.patient, container, Arrays.asList("phenotype", "contact"));
        Assert.assertEquals(1, container.length());
        JSONArray contacts = container.getJSONArray("contact");
        Assert.assertTrue(this.contact1.toJSON().similar(contacts.get(0)));
        Assert.assertTrue(this.contact3.toJSON().similar(contacts.get(1)));
        Assert.assertTrue(this.contact2.toJSON().similar(contacts.get(2)));
    }

    @Test
    public void writeDoesNothingWhenNoContactDataAvailable() throws ComponentLookupException
    {
        when(this.patient.<ContactInfo>getData("contact")).thenReturn(null);
        JSONObject container = new JSONObject();
        this.mocker.getComponentUnderTest().writeJSON(this.patient, container);
        Assert.assertEquals(0, container.length());

        when(this.patient.<ContactInfo>getData("contact"))
            .thenReturn(new IndexedPatientData<>("contact", Collections.<ContactInfo>emptyList()));
        this.mocker.getComponentUnderTest().writeJSON(this.patient, container);
        Assert.assertEquals(0, container.length());

        when(this.patient.<ContactInfo>getData("contact"))
            .thenReturn(new SimpleValuePatientData<>("contact", this.contact2));
        this.mocker.getComponentUnderTest().writeJSON(this.patient, container);
        Assert.assertEquals(0, container.length());
    }

    @Test
    public void writeAppendsToExistingContacts() throws ComponentLookupException
    {
        when(this.patient.<ContactInfo>getData("contact")).thenReturn(
            new IndexedPatientData<>("contact", Arrays.asList(this.contact1)));
        JSONObject container = new JSONObject();
        JSONArray existingContacts = new JSONArray();
        existingContacts.put(this.contact3.toJSON());
        container.put("contact", existingContacts);
        this.mocker.getComponentUnderTest().writeJSON(this.patient, container);
        Assert.assertEquals(1, container.length());
        JSONArray contacts = container.getJSONArray("contact");
        Assert.assertEquals(2, contacts.length());
        Assert.assertTrue(this.contact3.toJSON().similar(contacts.get(0)));
        Assert.assertTrue(this.contact1.toJSON().similar(contacts.get(1)));
    }

    @Test
    public void saveDoesntThrowException() throws ComponentLookupException
    {
        this.mocker.getComponentUnderTest().save(this.patient);
    }

    @Test
    public void readDoesntThrowException() throws ComponentLookupException
    {
        this.mocker.getComponentUnderTest().readJSON(null);
    }
}
