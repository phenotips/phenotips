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
import org.phenotips.data.PatientWritePolicy;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectPropertyReference;
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
import com.xpn.xwiki.objects.BaseProperty;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link APGARController} Component, implementation of the
 * {@link org.phenotips.data.PatientDataController} interface.
 */
public class APGARControllerTest
{
    private static final String DATA_NAME = "apgar";

    private static final String APGAR_1 = "apgar1";

    private static final String APGAR_5 = "apgar5";

    private static final String UNKNOWN = "unknown";

    @Rule
    public MockitoComponentMockingRule<PatientDataController<Integer>> mocker =
        new MockitoComponentMockingRule<>(APGARController.class);

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject dataHolder;

    @Mock
    private BaseProperty<ObjectPropertyReference> field1;

    @Mock
    private BaseProperty<ObjectPropertyReference> field5;

    private PatientDataController<Integer> component;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        this.component = this.mocker.getComponentUnderTest();

        DocumentReference patientDocRef = new DocumentReference("wiki", "patient", "00000001");
        doReturn(patientDocRef).when(this.patient).getDocumentReference();
        doReturn(this.doc).when(this.patient).getXDocument();
        doReturn(this.dataHolder).when(this.doc).getXObject(Patient.CLASS_REFERENCE);
        doReturn(this.dataHolder).when(this.doc).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());

        when(this.dataHolder.getField(APGAR_1)).thenReturn(this.field1);
        when(this.dataHolder.getField(APGAR_5)).thenReturn(this.field5);
    }

    @Test
    public void loadCatchesExceptionWhenPatientDoesNotHavePatientClass()
    {
        doReturn(null).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        PatientData<Integer> result = this.component.load(this.patient);

        Assert.assertNull(result);
    }

    @Test
    public void loadDoesNotReturnNullIntegers()
    {
        doReturn(null).when(this.dataHolder).getStringValue(anyString());

        PatientData<Integer> result = this.component.load(this.patient);

        Assert.assertEquals(0, result.size());
    }

    @Test
    public void loadDoesNotReturnNonIntegerStrings()
    {
        doReturn("STRING").when(this.dataHolder).getStringValue(anyString());

        PatientData<Integer> result = this.component.load(this.patient);

        Assert.assertEquals(0, result.size());
    }

    @Test
    public void loadReturnsExpectedIntegers()
    {
        doReturn("1").when(this.dataHolder).getStringValue(APGAR_1);
        doReturn("2").when(this.dataHolder).getStringValue(APGAR_5);

        PatientData<Integer> result = this.component.load(this.patient);

        Assert.assertEquals(Integer.valueOf(1), result.get(APGAR_1));
        Assert.assertEquals(Integer.valueOf(2), result.get(APGAR_5));
        Assert.assertEquals(2, result.size());
    }

    @Test
    public void saveThrowsExceptionIfPatientHasNoPatientClass()
    {
        when(this.doc.getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any())).thenReturn(null);
        this.component.save(this.patient);
    }

    @Test
    public void saveWithUpdatePolicyDoesNothingWhenPatientHasNoData()
    {
        when(this.patient.getData(DATA_NAME)).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.UPDATE);
        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());
        verifyNoMoreInteractions(this.doc);
        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveWithMergePolicyDoesNothingWhenPatientHasNoData()
    {
        when(this.patient.getData(DATA_NAME)).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.MERGE);
        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());
        verifyNoMoreInteractions(this.doc);
        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveWithReplacePolicyDeletesAllStoredDataWhenPatientHasNoData()
    {
        when(this.patient.getData(DATA_NAME)).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.REPLACE);
        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());
        verify(this.dataHolder, times(1)).getField(APGAR_1);
        verify(this.dataHolder, times(1)).getField(APGAR_5);
        verify(this.field1, times(1)).setValue(UNKNOWN);
        verify(this.field5, times(1)).setValue(UNKNOWN);
        verifyNoMoreInteractions(this.doc);
        verifyNoMoreInteractions(this.dataHolder);
    }

    @Test
    public void saveSetsCorrectAPGARScoresWhenPolicyIsUpdate()
    {
        final Map<String, Integer> dataMap = new LinkedHashMap<>();
        dataMap.put(APGAR_1, 5);
        final PatientData<Integer> data = new DictionaryPatientData<>(DATA_NAME, dataMap);
        when(this.patient.<Integer>getData(DATA_NAME)).thenReturn(data);
        this.component.save(this.patient, PatientWritePolicy.UPDATE);
        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());
        verify(this.dataHolder, times(1)).getField(APGAR_1);
        verify(this.field1, times(1)).setValue("5");
        verify(this.dataHolder, never()).getField(APGAR_5);
        verify(this.field5, never()).setValue(anyString());
        verifyNoMoreInteractions(this.dataHolder);
        verifyNoMoreInteractions(this.doc);
    }

    @Test
    public void saveSetsCorrectAPGARScoresWhenPolicyIsMerge()
    {
        final Map<String, Integer> dataMap = new LinkedHashMap<>();
        dataMap.put(APGAR_1, 5);
        final PatientData<Integer> data = new DictionaryPatientData<>(DATA_NAME, dataMap);
        when(this.patient.<Integer>getData(DATA_NAME)).thenReturn(data);
        this.component.save(this.patient, PatientWritePolicy.MERGE);
        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());
        verify(this.dataHolder, times(1)).getField(APGAR_1);
        verify(this.field1, times(1)).setValue("5");
        verify(this.dataHolder, never()).getField(APGAR_5);
        verify(this.field5, never()).setValue(anyString());
        verifyNoMoreInteractions(this.dataHolder);
        verifyNoMoreInteractions(this.doc);
    }

    @Test
    public void saveSetsCorrectAPGARScoresWhenPolicyIsReplace()
    {
        final Map<String, Integer> dataMap = new LinkedHashMap<>();
        dataMap.put(APGAR_1, 5);
        final PatientData<Integer> data = new DictionaryPatientData<>(DATA_NAME, dataMap);
        when(this.patient.<Integer>getData(DATA_NAME)).thenReturn(data);
        this.component.save(this.patient, PatientWritePolicy.REPLACE);
        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());
        verify(this.dataHolder, times(1)).getField(APGAR_1);
        verify(this.field1, times(1)).setValue("5");
        verify(this.dataHolder, times(1)).getField(APGAR_5);
        verify(this.field5, times(1)).setValue(UNKNOWN);
        verifyNoMoreInteractions(this.dataHolder);
        verifyNoMoreInteractions(this.doc);
    }

    @Test
    public void writeJSONReturnsWhenGetDataReturnsNull()
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.component.writeJSON(this.patient, json);

        Assert.assertFalse(json.has(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenGetDataReturnsNotNull()
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new ArrayList<>();
        selectedFields.add(APGAR_1);
        selectedFields.add(APGAR_5);

        this.component.writeJSON(this.patient, json, selectedFields);

        Assert.assertTrue(json.has(DATA_NAME));
    }

    @Test
    public void writeJSONReturnsWhenSelectedFieldsEmpty()
    {
        Map<String, Integer> map = new LinkedHashMap<>();
        PatientData<Integer> data = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(data).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.component.writeJSON(this.patient, json);

        Assert.assertNotNull(json.getJSONObject(DATA_NAME));
        Assert.assertEquals(0, json.getJSONObject(DATA_NAME).length());
    }

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenDataIsEmpty()
    {
        Map<String, Integer> map = new LinkedHashMap<>();
        PatientData<Integer> data = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(data).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new ArrayList<>();
        selectedFields.add(APGAR_1);
        selectedFields.add(APGAR_5);

        this.component.writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.getJSONObject(DATA_NAME));
        Assert.assertEquals(0, json.getJSONObject(DATA_NAME).length());
    }

    @Test
    public void writeJSONAddsAllDataEntriesToJSON()
    {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put(APGAR_1, 1);
        map.put(APGAR_5, 2);
        PatientData<Integer> data = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(data).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.component.writeJSON(this.patient, json);

        Assert.assertNotNull(json.getJSONObject(DATA_NAME));
        Assert.assertEquals(1, json.getJSONObject(DATA_NAME).get(APGAR_1));
        Assert.assertEquals(2, json.getJSONObject(DATA_NAME).get(APGAR_5));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsAllDataEntriesWhenAPGARSelected()
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

        this.component.writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.getJSONObject(DATA_NAME));
        Assert.assertEquals(1, json.getJSONObject(DATA_NAME).get(APGAR_1));
        Assert.assertEquals(2, json.getJSONObject(DATA_NAME).get(APGAR_5));
    }

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenAPGARNotSelected()
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

        this.component.writeJSON(this.patient, json, selectedFields);

        Assert.assertFalse(json.has(DATA_NAME));
    }

    @Test
    public void checkGetName()
    {
        Assert.assertEquals(DATA_NAME, this.component.getName());
    }
}
