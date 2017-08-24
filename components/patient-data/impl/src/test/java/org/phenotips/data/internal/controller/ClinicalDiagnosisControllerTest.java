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
import org.phenotips.data.Disorder;
import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.PatientWritePolicy;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Provider;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.ListProperty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link ClinicalDiagnosisController} Component, implementation of the
 * {@link org.phenotips.data.PatientDataController} interface.
 */
public class ClinicalDiagnosisControllerTest
{
    private static final String DATA_NAME = "clinical-diagnosis";

    private static final String XPROPERTY_NAME = "clinical_diagnosis";

    private static final String DISORDER1 = "ORDO:1";

    private static final String DISORDER2 = "ORDO:2";

    @Rule
    public MockitoComponentMockingRule<PatientDataController<Disorder>> mocker =
        new MockitoComponentMockingRule<>(ClinicalDiagnosisController.class);

    @Mock
    private XWikiContext xcontext;

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject dataHolder;

    @Mock
    private ListProperty xproperty;

    @Mock
    private Disorder disorder1;

    @Mock
    private Disorder disorder2;

    private PatientDataController<Disorder> component;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.component = this.mocker.getComponentUnderTest();
        Provider<XWikiContext> xcp = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcp.get()).thenReturn(this.xcontext);

        DocumentReference patientDocRef = new DocumentReference("wiki", "patient", "00000001");
        doReturn(patientDocRef).when(this.patient).getDocumentReference();
        doReturn(this.doc).when(this.patient).getXDocument();

        when(this.doc.getXObject(Patient.CLASS_REFERENCE, true, this.xcontext)).thenReturn(this.dataHolder);
        when(this.doc.getXObject(Patient.CLASS_REFERENCE)).thenReturn(this.dataHolder);
        when(this.dataHolder.get(XPROPERTY_NAME)).thenReturn(this.xproperty);
        when(this.disorder1.getValue()).thenReturn(DISORDER1);
        when(this.disorder1.toJSON()).thenReturn(new JSONObject("{\"id\":\"ORDO:1\"}"));
        when(this.disorder2.getValue()).thenReturn(DISORDER2);
        when(this.disorder2.toJSON()).thenReturn(new JSONObject("{\"id\":\"ORDO:2\"}"));
    }

    @Test
    public void loadForNonPatientReturnsNull()
    {
        when(this.doc.getXObject(Patient.CLASS_REFERENCE)).thenReturn(null);

        PatientData<Disorder> result = this.component.load(this.patient);

        assertNull(result);
    }

    @Test
    public void loadWithNoDataReturnsNull() throws XWikiException
    {
        when(this.dataHolder.get(XPROPERTY_NAME)).thenReturn(null);

        PatientData<Disorder> result = this.component.load(this.patient);

        assertNull(result);
    }

    @Test
    public void loadReturnsExpectedDisorders()
    {
        when(this.xproperty.getList()).thenReturn(Arrays.asList("ORDO:1", "ORDO:2"));

        PatientData<Disorder> result = this.component.load(this.patient);

        assertEquals(2, result.size());
        assertEquals("ORDO:1", result.get(0).getValue());
        assertEquals("ORDO:2", result.get(1).getValue());
    }

    @Test
    public void loadSkipsEmptyValues()
    {
        when(this.xproperty.getList()).thenReturn(Arrays.asList("ORDO:1", "", null, "ORDO:2"));

        PatientData<Disorder> result = this.component.load(this.patient);

        assertEquals(2, result.size());
        assertEquals("ORDO:1", result.get(0).getValue());
        assertEquals("ORDO:2", result.get(1).getValue());
    }

    @Test
    public void loadCatchesXWikiExceptionsThenReturnsNull() throws XWikiException
    {
        when(this.dataHolder.get(XPROPERTY_NAME)).thenThrow(new XWikiException());

        PatientData<Disorder> result = this.component.load(this.patient);

        assertNull(result);
    }

    @Test
    public void saveThrowsExceptionWhenPatientHasNoPatientClass()
    {
        when(this.doc.getXObject(Patient.CLASS_REFERENCE, true, this.xcontext)).thenReturn(null);
        this.component.save(this.patient);
    }

    @Test
    public void saveDoesNothingWhenNoDataAvailableAndPolicyIsUpdate()
    {
        when(this.patient.getData(DATA_NAME)).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.UPDATE);
        Mockito.verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.xcontext);
        verifyZeroInteractions(this.dataHolder);
        verifyNoMoreInteractions(this.doc);
    }

    @Test
    public void saveDoesNothingWhenNoDataAvailableAndPolicyIsMerge()
    {
        when(this.patient.getData(DATA_NAME)).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.MERGE);
        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.xcontext);
        verifyZeroInteractions(this.dataHolder);
        verifyNoMoreInteractions(this.doc);
    }

    @Test
    public void saveDeletesAllStoredDataWhenNoDataAvailableAndPolicyIsReplace()
    {
        when(this.patient.getData(DATA_NAME)).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.REPLACE);
        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.xcontext);
        verify(this.dataHolder, times(1)).set(XPROPERTY_NAME, null, this.xcontext);
        verifyNoMoreInteractions(this.doc);
        verifyNoMoreInteractions(this.dataHolder);
    }

    @Test
    public void saveResetsDiagnosisToNullForEmptyData()
    {
        when(this.patient.getData(DATA_NAME)).thenReturn(new IndexedPatientData<>(DATA_NAME, Collections.emptyList()));
        this.component.save(this.patient);
        verify(this.dataHolder).set(XPROPERTY_NAME, null, this.xcontext);
    }

    @Test
    public void saveDoesNothingForNonIndexedData()
    {
        when(this.patient.<Disorder>getData(DATA_NAME))
            .thenReturn(new DictionaryPatientData<>(DATA_NAME, Collections.emptyMap()));
        this.component.save(this.patient);
        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.xcontext);
        verifyZeroInteractions(this.dataHolder);
        verifyNoMoreInteractions(this.doc);
    }

    @Test
    public void saveSetsCorrectDiagnosisWhenPolicyIsUpdate()
    {
        final List<String> dataList = Arrays.asList(DISORDER1, DISORDER2);
        PatientData<Disorder> data = new IndexedPatientData<>(DATA_NAME, Arrays.asList(this.disorder1, this.disorder2));
        when(this.patient.<Disorder>getData(DATA_NAME)).thenReturn(data);
        this.component.save(this.patient);
        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.xcontext);
        verify(this.dataHolder).set(XPROPERTY_NAME, dataList, this.xcontext);
        verifyNoMoreInteractions(this.dataHolder);
        verifyNoMoreInteractions(this.doc);
    }

    @Test
    public void saveSetsCorrectDiagnosisWhenPolicyIsMerge() throws XWikiException
    {
        final List<String> dataList = Arrays.asList(DISORDER2, DISORDER1);
        PatientData<Disorder> data = new IndexedPatientData<>(DATA_NAME, Collections.singletonList(this.disorder1));
        when(this.patient.<Disorder>getData(DATA_NAME)).thenReturn(data);

        final ListProperty stored = mock(ListProperty.class);
        when(this.dataHolder.get(XPROPERTY_NAME)).thenReturn(stored);
        when(stored.getList()).thenReturn(Collections.singletonList(DISORDER2));

        this.component.save(this.patient, PatientWritePolicy.MERGE);
        // Once for save() and once for load().
        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.xcontext);
        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE);
        verify(this.dataHolder, times(1)).get(XPROPERTY_NAME);
        verify(this.dataHolder, times(1)).set(XPROPERTY_NAME, dataList, this.xcontext);
        verifyNoMoreInteractions(this.dataHolder);
        verifyNoMoreInteractions(this.doc);
    }

    @Test
    public void saveSetsCorrectDiagnosisWhenPolicyIsReplace()
    {
        final List<String> dataList = Collections.singletonList(DISORDER1);
        PatientData<Disorder> data = new IndexedPatientData<>(DATA_NAME, Collections.singletonList(this.disorder1));
        when(this.patient.<Disorder>getData(DATA_NAME)).thenReturn(data);

        this.component.save(this.patient, PatientWritePolicy.REPLACE);
        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.xcontext);
        verify(this.dataHolder, times(1)).set(XPROPERTY_NAME, dataList, this.xcontext);
        verifyNoMoreInteractions(this.dataHolder);
        verifyNoMoreInteractions(this.doc);
    }

    @Test
    public void writeJSONWritesEmptyArrayWhenNoDataAvailable()
    {
        when(this.patient.getData(DATA_NAME)).thenReturn(null);
        JSONObject json = new JSONObject();

        this.component.writeJSON(this.patient, json);

        assertTrue(json.has(DATA_NAME));
        assertEquals(0, json.getJSONArray(DATA_NAME).length());
    }

    @Test
    public void writeJSONWithSelectedFieldsWritesEmptyArrayWhenNoDataAvailable()
    {
        when(this.patient.getData(DATA_NAME)).thenReturn(null);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new ArrayList<>();
        selectedFields.add(XPROPERTY_NAME);

        this.component.writeJSON(this.patient, json, selectedFields);

        assertTrue(json.has(DATA_NAME));
        assertEquals(0, json.getJSONArray(DATA_NAME).length());
    }

    @Test
    public void writeJSONOutputsData()
    {
        PatientData<Disorder> data = new IndexedPatientData<>(DATA_NAME, Arrays.asList(this.disorder1, this.disorder2));
        when(this.patient.<Disorder>getData(DATA_NAME)).thenReturn(data);
        JSONObject json = new JSONObject();

        this.component.writeJSON(this.patient, json);

        assertNotNull(json.getJSONArray(DATA_NAME));
        assertEquals(2, json.getJSONArray(DATA_NAME).length());
        assertTrue(this.disorder1.toJSON().similar(json.getJSONArray(DATA_NAME).get(0)));
        assertTrue(this.disorder2.toJSON().similar(json.getJSONArray(DATA_NAME).get(1)));
    }

    @Test
    public void writeJSONWithoutFieldSelectedDoesNothing()
    {
        PatientData<Disorder> data = new IndexedPatientData<>(DATA_NAME, Arrays.asList(this.disorder1, this.disorder2));
        when(this.patient.<Disorder>getData(DATA_NAME)).thenReturn(data);
        JSONObject json = new JSONObject();

        this.component.writeJSON(this.patient, json, Collections.singletonList("phenotype"));

        assertFalse(json.has(DATA_NAME));
    }

    @Test
    public void readJSONReturnsNullForNullInput()
    {
        assertNull(this.component.readJSON(null));
    }

    @Test
    public void readJSONReturnsNullWhenDataMissing()
    {
        assertNull(this.component.readJSON(new JSONObject()));
    }

    @Test
    public void readJSONReturnsNullWhenDataIsNotArray()
    {
        assertNull(this.component.readJSON(new JSONObject("{\"clinical-diagnosis\":true}")));
        assertNull(this.component.readJSON(new JSONObject("{\"clinical-diagnosis\":5}")));
        assertNull(this.component.readJSON(new JSONObject("{\"clinical-diagnosis\":{}}")));
    }

    @Test
    public void readJSONReadsDisorders()
    {
        PatientData<Disorder> result = this.component.readJSON(
            new JSONObject("{\"clinical-diagnosis\":[{\"id\":\"ORDO:1\"},{\"id\":\"ORDO:2\"}]}"));
        assertEquals(2, result.size());
        assertEquals(DISORDER1, result.get(0).getId());
        assertEquals(DISORDER2, result.get(1).getId());
    }

    @Test
    public void readJSONIgnoresNonDisorders()
    {
        PatientData<Disorder> result = this.component.readJSON(
            new JSONObject("{\"clinical-diagnosis\":[{\"id\":\"ORDO:1\"},null,false,5,{\"id\":\"ORDO:2\"}]}"));
        assertEquals(2, result.size());
        assertEquals(DISORDER1, result.get(0).getId());
        assertEquals(DISORDER2, result.get(1).getId());
    }

    @Test
    public void checkGetName()
    {
        Assert.assertEquals(DATA_NAME, this.component.getName());
    }
}
