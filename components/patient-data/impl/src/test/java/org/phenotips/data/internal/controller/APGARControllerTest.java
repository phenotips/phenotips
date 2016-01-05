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

import org.phenotips.data.DictionaryPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;

/**
 * Tests for the {@link APGARController} Component, implementation of the
 * {@link org.phenotips.data.PatientDataController} interface
 */
public class APGARControllerTest
{
    @Rule
    public MockitoComponentMockingRule<PatientDataController<Integer>> mocker =
        new MockitoComponentMockingRule<PatientDataController<Integer>>(APGARController.class);

    private static final String DATA_NAME = "apgar";

    private static final String APGAR_1 = "apgar1";

    private static final String APGAR_5 = "apgar5";

    private DocumentAccessBridge documentAccessBridge;

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject data;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);

        DocumentReference patientDocument = new DocumentReference("wiki", "patient", "00000001");
        doReturn(patientDocument).when(this.patient).getDocument();
        doReturn(this.doc).when(this.documentAccessBridge).getDocument(patientDocument);
        doReturn(this.data).when(this.doc).getXObject(Patient.CLASS_REFERENCE);
    }

    @Test
    public void loadCatchesExceptionWhenPatientDoesNotHavePatientClass() throws ComponentLookupException
    {
        doReturn(null).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        PatientData<Integer> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNull(result);
    }

    @Test
    public void loadDoesNotReturnNullIntegers() throws ComponentLookupException
    {
        doReturn(null).when(this.data).getStringValue(anyString());

        PatientData<Integer> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertEquals(0, result.size());
    }

    @Test
    public void loadDoesNotReturnNonIntegerStrings() throws ComponentLookupException
    {
        doReturn("STRING").when(this.data).getStringValue(anyString());

        PatientData<Integer> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertEquals(0, result.size());
    }

    @Test
    public void loadReturnsExpectedIntegers() throws ComponentLookupException
    {
        doReturn("1").when(this.data).getStringValue(APGAR_1);
        doReturn("2").when(this.data).getStringValue(APGAR_5);

        PatientData<Integer> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertEquals(Integer.valueOf(1), result.get(APGAR_1));
        Assert.assertEquals(Integer.valueOf(2), result.get(APGAR_5));
        Assert.assertEquals(2, result.size());
    }

    @Test
    public void writeJSONReturnsWhenGetDataReturnsNull() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        Assert.assertFalse(json.has(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenGetDataReturnsNull() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new ArrayList<>();
        selectedFields.add(APGAR_1);
        selectedFields.add(APGAR_5);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertFalse(json.has(DATA_NAME));
    }

    @Test
    public void writeJSONReturnsWhenDataIsEmpty() throws ComponentLookupException
    {
        Map<String, Integer> map = new LinkedHashMap<>();
        PatientData<Integer> data = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(data).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        Assert.assertFalse(json.has(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenDataIsEmpty() throws ComponentLookupException
    {
        Map<String, Integer> map = new LinkedHashMap<>();
        PatientData<Integer> data = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(data).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new ArrayList<>();
        selectedFields.add(APGAR_1);
        selectedFields.add(APGAR_5);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertFalse(json.has(DATA_NAME));
    }

    @Test
    public void writeJSONAddsAllDataEntriesToJSON() throws ComponentLookupException
    {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put(APGAR_1, 1);
        map.put(APGAR_5, 2);
        PatientData<Integer> data = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(data).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        Assert.assertNotNull(json.getJSONObject(DATA_NAME));
        Assert.assertEquals(1, json.getJSONObject(DATA_NAME).get(APGAR_1));
        Assert.assertEquals(2, json.getJSONObject(DATA_NAME).get(APGAR_5));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsAllDataEntriesWhenAPGARSelected() throws ComponentLookupException
    {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put(APGAR_1, 1);
        map.put(APGAR_5, 2);
        PatientData<Integer> data = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(data).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new ArrayList<>();
        selectedFields.add("dates");
        selectedFields.add("ethnicity");
        selectedFields.add("apgar");
        selectedFields.add("identifiers");

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.getJSONObject(DATA_NAME));
        Assert.assertEquals(1, json.getJSONObject(DATA_NAME).get(APGAR_1));
        Assert.assertEquals(2, json.getJSONObject(DATA_NAME).get(APGAR_5));
    }

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenAPGARNotSelected() throws ComponentLookupException
    {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put(APGAR_1, 1);
        map.put(APGAR_5, 2);
        PatientData<Integer> data = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(data).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add("dates");
        selectedFields.add("ethnicity");

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertFalse(json.has(DATA_NAME));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void saveIsUnsupported() throws ComponentLookupException
    {
        this.mocker.getComponentUnderTest().save(this.patient);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void readJSONIsUnsupported() throws ComponentLookupException
    {
        this.mocker.getComponentUnderTest().readJSON(new JSONObject());
    }

    @Test
    public void checkGetName() throws ComponentLookupException
    {
        Assert.assertEquals(DATA_NAME, this.mocker.getComponentUnderTest().getName());
    }
}
