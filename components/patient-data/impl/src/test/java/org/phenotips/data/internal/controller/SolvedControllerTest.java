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
import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.PatientWritePolicy;
import org.phenotips.data.SimpleValuePatientData;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Test for the {@link SolvedController} Component, only the overridden methods from {@link AbstractSimpleController}
 * are tested here.
 */
public class SolvedControllerTest
{
    private static final String SOLVED_STRING = "solved";

    private static final String DATA_NAME = SOLVED_STRING;

    private static final String INTERNAL_PROPERTY_NAME = SOLVED_STRING;

    private static final String STATUS_KEY = SOLVED_STRING;

    private static final String STATUS_SOLVED = SOLVED_STRING;

    private static final String STATUS_UNSOLVED = "unsolved";

    private static final String SOLVED_PUBMED_ID_STRING = "solved__pubmed_id";

    private static final String SOLVED_NOTES_STRING = "solved__notes";

    private static final String STATUS = "status";

    private static final String PUBMED_ID = "pubmed_id";

    private static final String NOTES = "notes";

    @Rule
    public MockitoComponentMockingRule<PatientDataController<String>> mocker =
        new MockitoComponentMockingRule<>(SolvedController.class);

    @Mock
    private BaseObject dataHolder;

    @Mock
    private XWikiDocument doc;

    @Mock
    private Patient patient;

    @Mock
    private BaseProperty solvedField;

    @Mock
    private BaseProperty pubmedIdField;

    @Mock
    private BaseProperty notesField;

    private PatientDataController<String> component;

    @Before
    public void setUp() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);

        this.component = this.mocker.getComponentUnderTest();
        final DocumentReference patientDocRef = new DocumentReference("wiki", "patient", "00000001");
        when(this.patient.getDocumentReference()).thenReturn(patientDocRef);
        when(this.patient.getXDocument()).thenReturn(this.doc);
        when(this.doc.getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any())).thenReturn(this.dataHolder);

        when(this.dataHolder.getField(SOLVED_STRING)).thenReturn(this.solvedField);
        when(this.dataHolder.getField(SOLVED_PUBMED_ID_STRING)).thenReturn(this.pubmedIdField);
        when(this.dataHolder.getField(SOLVED_NOTES_STRING)).thenReturn(this.notesField);
    }

    @Test
    public void checkGetName()
    {
        Assert.assertEquals(DATA_NAME, this.component.getName());
    }

    @Test
    public void checkGetJsonPropertyName()
    {
        Assert.assertEquals(INTERNAL_PROPERTY_NAME,
            ((AbstractSimpleController) this.component).getJsonPropertyName());
    }

    @Test
    public void checkGetProperties()
    {
        List<String> result = ((AbstractSimpleController) this.component).getProperties();

        Assert.assertEquals(3, result.size());
        Assert.assertThat(result, Matchers.hasItem(STATUS_KEY));
        Assert.assertThat(result, Matchers.hasItem("solved__pubmed_id"));
        Assert.assertThat(result, Matchers.hasItem("solved__notes"));
    }

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenGetDataReturnsNull()
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();

        this.component.writeJSON(this.patient, json, selectedFields);

        Assert.assertEquals(0, json.length());
    }

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenDataIsNotKeyValueBased()
    {
        PatientData<String> patientData = new SimpleValuePatientData<>(DATA_NAME, "datum");
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();

        this.component.writeJSON(this.patient, json, selectedFields);

        Assert.assertEquals(0, json.length());
    }

    @Test
    public void writeJSONWithSelectedFieldsConvertsSolvedStatus()
    {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(STATUS_KEY, "1");
        PatientData<String> patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(STATUS_KEY);

        this.component.writeJSON(this.patient, json, selectedFields);
        Assert.assertEquals(STATUS_SOLVED, json.getJSONObject(DATA_NAME).get("status"));

        map.clear();
        json = new JSONObject();
        map.put(STATUS_KEY, "0");
        patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);

        this.component.writeJSON(this.patient, json, selectedFields);
        Assert.assertEquals(STATUS_UNSOLVED, json.getJSONObject(DATA_NAME).get("status"));

        map.clear();
        json = new JSONObject();
        map.put(STATUS_KEY, "solved");
        patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);

        this.component.writeJSON(this.patient, json, selectedFields);
        Assert.assertFalse(json.has(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsAllValuesAndConvertedJsonKeys()
    {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(STATUS_KEY, "1");
        String pubmedID = "pubmed:0001";
        map.put("solved__pubmed_id", pubmedID);
        String notes = "some notes about the solved case";
        map.put("solved__notes", notes);
        PatientData<String> patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(STATUS_KEY);
        selectedFields.add("solved__pubmed_id");
        selectedFields.add("solved__notes");

        this.component.writeJSON(this.patient, json, selectedFields);

        JSONObject container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals(STATUS_SOLVED, container.get("status"));
        Assert.assertEquals(pubmedID, container.get("pubmed_id"));
        Assert.assertEquals(notes, container.get("notes"));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsSelectedValues()
    {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(STATUS_KEY, "1");
        String pubmedID = "pubmed:0001";
        map.put("solved__pubmed_id", pubmedID);
        String notes = "some notes about the solved case";
        map.put("solved__notes", notes);
        PatientData<String> patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(STATUS_KEY);
        selectedFields.add("solved__notes");

        this.component.writeJSON(this.patient, json, selectedFields);

        JSONObject container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals(2, container.length());
        Assert.assertEquals(STATUS_SOLVED, container.get("status"));
        Assert.assertEquals(notes, container.get("notes"));
        Assert.assertFalse(container.has("pubmed_id"));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsAllValuesWhenSelectedFieldsNull()
    {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(STATUS_KEY, "1");
        String pubmedID = "pubmed:0001";
        map.put("solved__pubmed_id", pubmedID);
        String notes = "some notes about the solved case";
        map.put("solved__notes", notes);
        PatientData<String> patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.component.writeJSON(this.patient, json, null);

        JSONObject container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals(STATUS_SOLVED, container.get("status"));
        Assert.assertEquals(pubmedID, container.get("pubmed_id"));
        Assert.assertEquals(notes, container.get("notes"));
    }

    @Test
    public void writeJSONAllowsForUnconvertedFields()
    {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("solved_new_field", "field_value");
        PatientData<String> patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add("solved_new_field");

        this.component.writeJSON(this.patient, json, selectedFields);
        Assert.assertEquals("field_value", json.getJSONObject(DATA_NAME).get("solved_new_field"));
    }

    @Test
    public void saveDoesNothingWhenPatientHasNoPatientClass()
    {
        when(this.doc.getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any())).thenReturn(null);

        this.component.save(this.patient);
        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveDoesNothingWhenPatientHasDataOfWrongFormat()
    {
        when(this.doc.getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any())).thenReturn(null);

        final PatientData<String> data = new IndexedPatientData<>(this.component.getName(), Collections.emptyList());
        doReturn(data).when(this.patient).getData(this.component.getName());

        this.component.save(this.patient);
        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveDoesNothingWhenDataIsNullAndPolicyIsUpdate()
    {
        when(this.patient.getData(this.component.getName())).thenReturn(null);

        this.component.save(this.patient, PatientWritePolicy.UPDATE);

        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());

        verifyNoMoreInteractions(this.doc);
        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveDoesNothingWhenDataIsNullAndPolicyIsMerge()
    {
        when(this.patient.getData(this.component.getName())).thenReturn(null);

        this.component.save(this.patient, PatientWritePolicy.MERGE);

        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());

        verifyNoMoreInteractions(this.doc);
        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveNullsAllSavedDataWhenDataIsNullAndPolicyIsReplace()
    {
        when(this.patient.getData(this.component.getName())).thenReturn(null);

        this.component.save(this.patient, PatientWritePolicy.REPLACE);

        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());

        verify(this.dataHolder, times(1)).getField(SOLVED_STRING);
        verify(this.dataHolder, times(1)).getField(SOLVED_PUBMED_ID_STRING);
        verify(this.dataHolder, times(1)).getField(SOLVED_NOTES_STRING);

        verify(this.solvedField, times(1)).setValue(null);
        verify(this.pubmedIdField, times(1)).setValue(null);
        verify(this.notesField, times(1)).setValue(null);

        verifyNoMoreInteractions(this.doc, this.dataHolder, this.solvedField, this.pubmedIdField,
            this.notesField);
    }

    @Test
    public void saveDoesNothingWhenDataIsEmptyAndPolicyIsUpdate()
    {
        final PatientData<String> data = spy(new DictionaryPatientData<>(this.component.getName(),
            Collections.emptyMap()));
        doReturn(data).when(this.patient).getData(this.component.getName());

        this.component.save(this.patient, PatientWritePolicy.UPDATE);

        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());

        verify(data, times(1)).containsKey(STATUS);
        verify(data, times(1)).containsKey(PUBMED_ID);
        verify(data, times(1)).containsKey(NOTES);
        verify(data, times(1)).isNamed();

        verifyNoMoreInteractions(this.doc, data);
        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveDoesNothingWhenDataIsEmptyAndPolicyIsMerge()
    {
        final PatientData<String> data = spy(new DictionaryPatientData<>(this.component.getName(),
            Collections.emptyMap()));
        doReturn(data).when(this.patient).getData(this.component.getName());

        this.component.save(this.patient, PatientWritePolicy.MERGE);

        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());

        verify(data, times(1)).containsKey(STATUS);
        verify(data, times(1)).containsKey(PUBMED_ID);
        verify(data, times(1)).containsKey(NOTES);
        verify(data, times(1)).isNamed();

        verifyNoMoreInteractions(this.doc, data);
        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveNullsAllSavedDataWhenDataIsEmptyAndPolicyIsReplace()
    {
        final PatientData<String> data = spy(new DictionaryPatientData<>(this.component.getName(),
            Collections.emptyMap()));
        doReturn(data).when(this.patient).getData(this.component.getName());

        this.component.save(this.patient, PatientWritePolicy.REPLACE);

        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());

        verify(data, times(1)).get(STATUS);
        verify(data, times(1)).get(PUBMED_ID);
        verify(data, times(1)).get(NOTES);
        verify(data, times(1)).isNamed();

        verify(this.dataHolder, times(1)).getField(SOLVED_STRING);
        verify(this.dataHolder, times(1)).getField(SOLVED_PUBMED_ID_STRING);
        verify(this.dataHolder, times(1)).getField(SOLVED_NOTES_STRING);

        verify(this.solvedField, times(1)).setValue(null);
        verify(this.pubmedIdField, times(1)).setValue(null);
        verify(this.notesField, times(1)).setValue(null);

        verifyNoMoreInteractions(this.doc, data, this.dataHolder, this.solvedField, this.pubmedIdField,
            this.notesField);
    }

    @Test
    public void saveWritesOnlySpecifiedPropertiesWhenPolicyIsUpdate()
    {
        final Map<String, String> propertyMap = new LinkedHashMap<>();
        propertyMap.put(STATUS, "0");
        propertyMap.put(PUBMED_ID, "PMID23193287");
        final PatientData<String> data = spy(new DictionaryPatientData<>(this.component.getName(), propertyMap));
        doReturn(data).when(this.patient).getData(this.component.getName());

        this.component.save(this.patient, PatientWritePolicy.UPDATE);

        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());

        verify(data, times(1)).containsKey(STATUS);
        verify(data, times(1)).containsKey(PUBMED_ID);
        verify(data, times(1)).containsKey(NOTES);
        verify(data, times(1)).get(STATUS);
        verify(data, times(1)).get(PUBMED_ID);
        verify(data, times(1)).isNamed();

        verify(this.dataHolder, times(1)).getField(SOLVED_STRING);
        verify(this.dataHolder, times(1)).getField(SOLVED_PUBMED_ID_STRING);

        verify(this.solvedField, times(1)).setValue(0);
        verify(this.pubmedIdField, times(1)).setValue("PMID23193287");

        verifyZeroInteractions(this.notesField);
        verifyNoMoreInteractions(this.doc, data, this.dataHolder, this.solvedField, this.pubmedIdField);
    }

    @Test
    public void saveWritesOnlySpecifiedPropertiesWhenPolicyIsMerge()
    {
        final Map<String, String> propertyMap = new LinkedHashMap<>();
        propertyMap.put(STATUS, "0");
        propertyMap.put(PUBMED_ID, "PMID23193287");
        final PatientData<String> data = spy(new DictionaryPatientData<>(this.component.getName(), propertyMap));
        doReturn(data).when(this.patient).getData(this.component.getName());

        this.component.save(this.patient, PatientWritePolicy.MERGE);

        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());

        verify(data, times(1)).containsKey(STATUS);
        verify(data, times(1)).containsKey(PUBMED_ID);
        verify(data, times(1)).containsKey(NOTES);
        verify(data, times(1)).get(STATUS);
        verify(data, times(1)).get(PUBMED_ID);
        verify(data, times(1)).isNamed();

        verify(this.dataHolder, times(1)).getField(SOLVED_STRING);
        verify(this.dataHolder, times(1)).getField(SOLVED_PUBMED_ID_STRING);

        verify(this.solvedField, times(1)).setValue(0);
        verify(this.pubmedIdField, times(1)).setValue("PMID23193287");

        verifyZeroInteractions(this.notesField);
        verifyNoMoreInteractions(this.doc, data, this.dataHolder, this.solvedField, this.pubmedIdField);
    }

    @Test
    public void saveWritesOnlySpecifiedPropertiesAndNullsTheRestWhenPolicyIsReplace()
    {
        final Map<String, String> propertyMap = new LinkedHashMap<>();
        propertyMap.put(STATUS, "0");
        propertyMap.put(PUBMED_ID, "PMID23193287");
        final PatientData<String> data = spy(new DictionaryPatientData<>(this.component.getName(), propertyMap));
        doReturn(data).when(this.patient).getData(this.component.getName());

        this.component.save(this.patient, PatientWritePolicy.REPLACE);

        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());

        verify(data, times(1)).get(STATUS);
        verify(data, times(1)).get(PUBMED_ID);
        verify(data, times(1)).get(NOTES);
        verify(data, times(1)).isNamed();

        verify(this.dataHolder, times(1)).getField(SOLVED_STRING);
        verify(this.dataHolder, times(1)).getField(SOLVED_PUBMED_ID_STRING);
        verify(this.dataHolder, times(1)).getField(SOLVED_NOTES_STRING);

        verify(this.solvedField, times(1)).setValue(0);
        verify(this.pubmedIdField, times(1)).setValue("PMID23193287");
        verify(this.notesField, times(1)).setValue(null);

        verifyNoMoreInteractions(this.doc, data, this.dataHolder, this.solvedField, this.pubmedIdField,
            this.notesField);
    }
}
