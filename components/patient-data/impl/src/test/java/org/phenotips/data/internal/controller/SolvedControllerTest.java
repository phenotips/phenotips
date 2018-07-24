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

import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.PatientWritePolicy;
import org.phenotips.data.SimpleValuePatientData;
import org.phenotips.data.internal.SolvedData;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import org.apache.commons.collections4.CollectionUtils;
import org.json.JSONArray;
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
 * Test for the {@link SolvedController} Component, only the overridden methods from {@link PatientDataController} are
 * tested here.
 */
public class SolvedControllerTest
{
    private static final String SOLVED_STRING = SolvedData.STATUS_PROPERTY_NAME;

    private static final String DATA_NAME = SOLVED_STRING;

    private static final String STATUS_KEY = SOLVED_STRING;

    private static final String STATUS_SOLVED = SOLVED_STRING;

    private static final String STATUS_UNSOLVED = SolvedData.STATUS_UNSOLVED;

    private static final String SOLVED_PUBMED_ID_STRING = SolvedData.PUBMED_ID_PROPERTY_NAME;

    private static final String SOLVED_NOTES_STRING = SolvedData.NOTES_PROPERTY_NAME;

    private static final String STATUS = SolvedData.STATUS_JSON_KEY;

    private static final String PUBMED_ID = SolvedData.PUBMED_ID_JSON_KEY;

    private static final String NOTES = SolvedData.NOTES_JSON_KEY;

    @Rule
    public MockitoComponentMockingRule<PatientDataController<SolvedData>> mocker =
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
    private BaseProperty notesField;

    private PatientDataController<SolvedData> component;

    @Before
    public void setUp() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);

        this.component = this.mocker.getComponentUnderTest();
        final DocumentReference patientDocRef = new DocumentReference("wiki", "patient", "00000001");
        when(this.patient.getDocumentReference()).thenReturn(patientDocRef);
        when(this.patient.getXDocument()).thenReturn(this.doc);
        when(this.doc.getXObject(Patient.CLASS_REFERENCE)).thenReturn(this.dataHolder);
        when(this.doc.getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any())).thenReturn(this.dataHolder);

        when(this.dataHolder.getField(SOLVED_STRING)).thenReturn(this.solvedField);
        when(this.dataHolder.getField(SOLVED_NOTES_STRING)).thenReturn(this.notesField);
    }

    @Test
    public void checkGetName()
    {
        Assert.assertEquals(DATA_NAME, this.component.getName());
    }

    @Test
    public void loadWorks() throws Exception
    {
        when(this.dataHolder.getStringValue(SolvedData.STATUS_PROPERTY_NAME)).thenReturn("1");
        when(this.dataHolder.getStringValue(SolvedData.NOTES_PROPERTY_NAME)).thenReturn("n1");
        when(this.dataHolder.getListValue(SolvedData.PUBMED_ID_PROPERTY_NAME)).thenReturn(Arrays.asList("123", "abc"));

        PatientData<SolvedData> result = this.component.load(this.patient);

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("1", result.getValue().getStatus());
        Assert.assertEquals("n1", result.getValue().getNotes());
        Assert.assertTrue(
            CollectionUtils.isEqualCollection(Arrays.asList("123", "abc"), result.getValue().getPubmedIds()));
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
    public void writeJSONWithSelectedFieldsConvertsSolvedStatus()
    {
        SolvedData data = new SolvedData("1", null, null);
        PatientData<SolvedData> patientData = new SimpleValuePatientData<>(DATA_NAME, data);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(STATUS_KEY);

        this.component.writeJSON(this.patient, json, selectedFields);
        Assert.assertEquals(STATUS_SOLVED, json.getJSONObject(DATA_NAME).get("status"));

        data.setStatus("0");
        json = new JSONObject();
        patientData = new SimpleValuePatientData<>(DATA_NAME, data);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);

        this.component.writeJSON(this.patient, json, selectedFields);
        Assert.assertEquals(STATUS_UNSOLVED, json.getJSONObject(DATA_NAME).get("status"));

        json = new JSONObject();
        data = new SolvedData(null, null, null);
        data.setStatus("solved");
        patientData = new SimpleValuePatientData<>(DATA_NAME, data);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);

        this.component.writeJSON(this.patient, json, selectedFields);
        Assert.assertFalse(json.has(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsAllValuesAndConvertedJsonKeys()
    {
        String notes = "some notes about the solved case";
        String pubmedID = "pubmed:0001";
        SolvedData data = new SolvedData("1", notes, Arrays.asList(pubmedID));
        PatientData<SolvedData> patientData = new SimpleValuePatientData<>(DATA_NAME, data);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(STATUS_KEY);
        selectedFields.add("solved__pubmed_id");
        selectedFields.add("solved__notes");

        this.component.writeJSON(this.patient, json, selectedFields);

        JSONObject container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals(STATUS_SOLVED, container.get("status"));
        Assert.assertEquals(pubmedID, ((JSONArray) container.get("pubmed_id")).get(0));
        Assert.assertEquals(notes, container.get("notes"));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsSelectedValues()
    {
        String notes = "some notes about the solved case";
        String pubmedID = "pubmed:0001";
        SolvedData data = new SolvedData("1", notes, Arrays.asList(pubmedID));
        PatientData<SolvedData> patientData = new SimpleValuePatientData<>(DATA_NAME, data);
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
        String notes = "some notes about the solved case";
        String pubmedID = "pubmed:0001";
        SolvedData data = new SolvedData("1", notes, Arrays.asList(pubmedID));
        PatientData<SolvedData> patientData = new SimpleValuePatientData<>(DATA_NAME, data);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.component.writeJSON(this.patient, json, null);

        JSONObject container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals(STATUS_SOLVED, container.get("status"));
        Assert.assertEquals(pubmedID, ((JSONArray) container.get("pubmed_id")).get(0));
        Assert.assertEquals(notes, container.get("notes"));
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

        final PatientData<SolvedData> data =
            new IndexedPatientData<>(this.component.getName(), Collections.emptyList());
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
        verify(this.dataHolder, times(1)).getField(SOLVED_NOTES_STRING);

        verify(this.solvedField, times(1)).setValue(null);
        verify(this.notesField, times(1)).setValue(null);
        verify(this.dataHolder, times(1)).setDBStringListValue(SOLVED_PUBMED_ID_STRING, null);

        verifyNoMoreInteractions(this.doc, this.dataHolder, this.solvedField, this.notesField);
    }

    @Test
    public void saveDoesNothingWhenDataIsEmptyAndPolicyIsUpdate()
    {
        SolvedData data = new SolvedData(null, null, null);
        final PatientData<SolvedData> patientData = spy(new SimpleValuePatientData<>(DATA_NAME, data));
        doReturn(patientData).when(this.patient).getData(this.component.getName());

        this.component.save(this.patient, PatientWritePolicy.UPDATE);

        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());

        verifyNoMoreInteractions(this.doc);
        verifyZeroInteractions(this.solvedField, this.notesField);
    }

    @Test
    public void saveDoesNothingWhenDataIsEmptyAndPolicyIsMerge()
    {
        SolvedData data = new SolvedData(null, null, null);
        final PatientData<SolvedData> patientData = spy(new SimpleValuePatientData<>(DATA_NAME, data));
        doReturn(patientData).when(this.patient).getData(this.component.getName());

        this.component.save(this.patient, PatientWritePolicy.MERGE);

        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());

        verifyNoMoreInteractions(this.doc);
        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveNullsAllSavedDataWhenDataIsEmptyAndPolicyIsReplace()
    {
        SolvedData data = new SolvedData(null, null, null);
        final PatientData<SolvedData> patientData = spy(new SimpleValuePatientData<>(DATA_NAME, data));
        doReturn(patientData).when(this.patient).getData(this.component.getName());

        this.component.save(this.patient, PatientWritePolicy.REPLACE);

        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());

        verify(this.dataHolder, times(1)).getField(SOLVED_STRING);
        verify(this.dataHolder, times(1)).getField(SOLVED_NOTES_STRING);

        verify(this.solvedField, times(1)).setValue(null);
        verify(this.dataHolder, times(1)).setDBStringListValue(SOLVED_PUBMED_ID_STRING, null);
        verify(this.notesField, times(1)).setValue(null);

        verifyNoMoreInteractions(this.doc, this.dataHolder, this.solvedField, this.notesField);
    }

    @Test
    public void saveWritesOnlySpecifiedPropertiesWhenPolicyIsUpdate()
    {
        SolvedData data = new SolvedData("0", null, Arrays.asList("PMID23193287"));
        final PatientData<SolvedData> patientData = spy(new SimpleValuePatientData<>(DATA_NAME, data));
        doReturn(patientData).when(this.patient).getData(this.component.getName());

        this.component.save(this.patient, PatientWritePolicy.UPDATE);

        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());

        verify(this.solvedField, times(1)).setValue(0);
        verify(this.dataHolder, times(1)).setDBStringListValue(SOLVED_PUBMED_ID_STRING, Arrays.asList("PMID23193287"));

        verifyZeroInteractions(this.notesField);
        verifyNoMoreInteractions(this.doc, this.solvedField);
    }

    @Test
    public void saveWritesOnlySpecifiedPropertiesWhenPolicyIsMerge()
    {
        SolvedData data = new SolvedData("0", null, Arrays.asList("PMID23193287"));
        final PatientData<SolvedData> patientData = spy(new SimpleValuePatientData<>(DATA_NAME, data));
        doReturn(patientData).when(this.patient).getData(this.component.getName());

        this.component.save(this.patient, PatientWritePolicy.MERGE);

        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());

        verify(this.dataHolder, times(1)).getField(SOLVED_STRING);

        verify(this.solvedField, times(1)).setValue(0);
        verify(this.dataHolder, times(1)).setDBStringListValue(SOLVED_PUBMED_ID_STRING, Arrays.asList("PMID23193287"));

        verifyZeroInteractions(this.notesField);
        verifyNoMoreInteractions(this.solvedField);
    }

    @Test
    public void saveWritesOnlySpecifiedPropertiesAndNullsTheRestWhenPolicyIsReplace()
    {
        SolvedData data = new SolvedData("0", null, Arrays.asList("PMID23193287"));
        final PatientData<SolvedData> patientData = spy(new SimpleValuePatientData<>(DATA_NAME, data));
        doReturn(patientData).when(this.patient).getData(this.component.getName());

        this.component.save(this.patient, PatientWritePolicy.REPLACE);

        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());

        verify(this.dataHolder, times(1)).getField(SOLVED_STRING);
        verify(this.dataHolder, times(1)).getField(SOLVED_NOTES_STRING);

        verify(this.solvedField, times(1)).setValue(0);
        verify(this.dataHolder, times(1)).setDBStringListValue(SOLVED_PUBMED_ID_STRING, Arrays.asList("PMID23193287"));
        verify(this.notesField, times(1)).setValue(null);

        verifyNoMoreInteractions(this.doc, this.dataHolder, this.solvedField, this.notesField);
    }
}
