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
import org.phenotips.data.SimpleValuePatientData;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Collection;
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

import static org.mockito.Mockito.doReturn;

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

    @Rule
    public MockitoComponentMockingRule<PatientDataController<String>> mocker =
        new MockitoComponentMockingRule<PatientDataController<String>>(SolvedController.class);

    @Mock
    private Patient patient;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void checkGetName() throws ComponentLookupException
    {
        Assert.assertEquals(DATA_NAME, this.mocker.getComponentUnderTest().getName());
    }

    @Test
    public void checkGetJsonPropertyName() throws ComponentLookupException
    {
        Assert.assertEquals(INTERNAL_PROPERTY_NAME,
            ((AbstractSimpleController) this.mocker.getComponentUnderTest()).getJsonPropertyName());
    }

    @Test
    public void checkGetProperties() throws ComponentLookupException
    {
        List<String> result = ((AbstractSimpleController) this.mocker.getComponentUnderTest()).getProperties();

        Assert.assertEquals(3, result.size());
        Assert.assertThat(result, Matchers.hasItem(STATUS_KEY));
        Assert.assertThat(result, Matchers.hasItem("solved__pubmed_id"));
        Assert.assertThat(result, Matchers.hasItem("solved__notes"));
    }

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenGetDataReturnsNull() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertEquals(0, json.length());
    }

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenDataIsNotKeyValueBased() throws ComponentLookupException
    {
        PatientData<String> patientData = new SimpleValuePatientData<>(DATA_NAME, "datum");
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertEquals(0, json.length());
    }

    @Test
    public void writeJSONWithSelectedFieldsConvertsSolvedStatus() throws ComponentLookupException
    {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(STATUS_KEY, "1");
        PatientData<String> patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(STATUS_KEY);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);
        Assert.assertEquals(STATUS_SOLVED, json.getJSONObject(DATA_NAME).get("status"));

        map.clear();
        json = new JSONObject();
        map.put(STATUS_KEY, "0");
        patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);
        Assert.assertEquals(STATUS_UNSOLVED, json.getJSONObject(DATA_NAME).get("status"));

        map.clear();
        json = new JSONObject();
        map.put(STATUS_KEY, "solved");
        patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);
        Assert.assertFalse(json.getJSONObject(DATA_NAME).has("status"));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsAllValuesAndConvertedJsonKeys() throws ComponentLookupException
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

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        JSONObject container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals(STATUS_SOLVED, container.get("status"));
        Assert.assertEquals(pubmedID, container.get("pubmed_id"));
        Assert.assertEquals(notes, container.get("notes"));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsSelectedValues() throws ComponentLookupException
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

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        JSONObject container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals(2, container.length());
        Assert.assertEquals(STATUS_SOLVED, container.get("status"));
        Assert.assertEquals(notes, container.get("notes"));
        Assert.assertFalse(container.has("pubmed_id"));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsAllValuesWhenSelectedFieldsNull() throws ComponentLookupException
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

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, null);

        JSONObject container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals(STATUS_SOLVED, container.get("status"));
        Assert.assertEquals(pubmedID, container.get("pubmed_id"));
        Assert.assertEquals(notes, container.get("notes"));
    }

    @Test
    public void writeJSONAllowsForUnconvertedFields() throws ComponentLookupException
    {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("solved_new_field", "field_value");
        PatientData<String> patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add("solved_new_field");

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);
        Assert.assertEquals("field_value", json.getJSONObject(DATA_NAME).get("solved_new_field"));
    }
}
