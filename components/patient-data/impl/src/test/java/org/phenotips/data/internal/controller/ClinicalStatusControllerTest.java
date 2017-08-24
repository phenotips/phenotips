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

import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.PatientWritePolicy;
import org.phenotips.data.SimpleValuePatientData;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Collection;
import java.util.LinkedList;

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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Test for the {@link ClinicalStatusController} component, implementation of the
 * {@link org.phenotips.data.PatientDataController} interface.
 */
public class ClinicalStatusControllerTest
{
    private static final String AFFECTED = "affected";

    private static final String UNAFFECTED = "unaffected";

    private static final String DATA_NAME = "clinicalStatus";

    private static final String CONTROLLING_FIELDNAME = UNAFFECTED;

    @Rule
    public MockitoComponentMockingRule<PatientDataController<String>> mocker =
        new MockitoComponentMockingRule<>(ClinicalStatusController.class);

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject dataHolder;

    @Mock
    private BaseProperty field;

    private PatientDataController<String> component;

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
        when(this.dataHolder.getField(UNAFFECTED)).thenReturn(this.field);
    }

    @Test
    public void loadCatchesExceptionWhenPatientDoesNotHavePatientClass() throws ComponentLookupException
    {
        doReturn(null).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        PatientData<String> result = this.component.load(this.patient);

        Assert.assertNull(result);
    }

    @Test
    public void loadReturnsNullWhenGivenUnexpectedIntValue() throws ComponentLookupException
    {
        doReturn(7314862).when(this.dataHolder).getIntValue(UNAFFECTED);

        PatientData<String> result = this.component.load(this.patient);

        Assert.assertNull(result);
    }

    @Test
    public void loadReturnsAffectedString() throws ComponentLookupException
    {
        doReturn(0).when(this.dataHolder).getIntValue(UNAFFECTED);

        PatientData<String> result = this.component.load(this.patient);

        Assert.assertEquals(AFFECTED, result.getValue());
    }

    @Test
    public void loadReturnsUnaffectedString() throws ComponentLookupException
    {
        doReturn(1).when(this.dataHolder).getIntValue(UNAFFECTED);

        PatientData<String> result = this.component.load(this.patient);

        Assert.assertEquals(UNAFFECTED, result.getValue());
    }

    @Test
    public void saveDoesNothingIfPatientHasNoPatientClass()
    {
        when(this.doc.getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any())).thenReturn(null);
        this.component.save(this.patient);
        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveDoesNothingIfClinicalStatusFieldIsAbsent()
    {
        when(this.dataHolder.getField(UNAFFECTED)).thenReturn(null);
        this.component.save(this.patient);
        verify(this.dataHolder, times(1)).getField(UNAFFECTED);
        verifyNoMoreInteractions(this.dataHolder);
    }

    @Test
    public void saveSetsStatusAsAffectedIfPolicyIsReplaceAndNoDataIsProvided()
    {
        when(this.patient.getData(this.component.getName())).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.REPLACE);
        verify(this.dataHolder, times(1)).getField(UNAFFECTED);
        verify(this.field, times(1)).setValue(0);
        verifyNoMoreInteractions(this.dataHolder);
        verifyNoMoreInteractions(this.field);
    }

    @Test
    public void saveDoesNothingIfPolicyIsUpdateAndNoDataIsProvided()
    {
        when(this.patient.getData(this.component.getName())).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.UPDATE);
        verify(this.dataHolder, times(1)).getField(UNAFFECTED);
        verifyZeroInteractions(this.field);
    }

    @Test
    public void saveDoesNothingIfPolicyIsMergeAndNoDataIsProvided()
    {
        when(this.patient.getData(this.component.getName())).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.MERGE);
        verify(this.dataHolder, times(1)).getField(UNAFFECTED);
        verifyZeroInteractions(this.field);
    }

    @Test
    public void saveSavesDataIfPolicyIsMerge()
    {
        final PatientData<String> data = new SimpleValuePatientData<>(this.component.getName(), AFFECTED);
        doReturn(data).when(this.patient).getData(this.component.getName());
        this.component.save(this.patient, PatientWritePolicy.MERGE);
        verify(this.dataHolder, times(1)).getField(UNAFFECTED);
        verify(this.field, times(1)).setValue(0);
        verifyNoMoreInteractions(this.dataHolder);
        verifyNoMoreInteractions(this.field);
    }

    @Test
    public void saveSavesDataIfPolicyIsUpdate()
    {
        final PatientData<String> data = new SimpleValuePatientData<>(this.component.getName(), AFFECTED);
        doReturn(data).when(this.patient).getData(this.component.getName());
        this.component.save(this.patient, PatientWritePolicy.UPDATE);
        verify(this.dataHolder, times(1)).getField(UNAFFECTED);
        verify(this.field, times(1)).setValue(0);
        verifyNoMoreInteractions(this.dataHolder);
        verifyNoMoreInteractions(this.field);
    }

    @Test
    public void saveSavesDataIfPolicyIsReplace()
    {
        final PatientData<String> data = new SimpleValuePatientData<>(this.component.getName(), UNAFFECTED);
        doReturn(data).when(this.patient).getData(this.component.getName());
        this.component.save(this.patient, PatientWritePolicy.REPLACE);
        verify(this.dataHolder, times(1)).getField(UNAFFECTED);
        verify(this.field, times(1)).setValue(1);
        verifyNoMoreInteractions(this.dataHolder);
        verifyNoMoreInteractions(this.field);
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
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(CONTROLLING_FIELDNAME);

        this.component.writeJSON(this.patient, json, selectedFields);

        Assert.assertTrue(json.has(DATA_NAME));
    }

    @Test
    public void writeJSONAddsAffectedValueToJSON()
    {
        PatientData<String> data = new SimpleValuePatientData<>(DATA_NAME, AFFECTED);
        doReturn(data).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.component.writeJSON(this.patient, json);

        Assert.assertNotNull(json.opt(DATA_NAME));
        Assert.assertEquals(AFFECTED, json.getString(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsAffectedValueToJSON()
    {
        PatientData<String> data =
            new SimpleValuePatientData<>(DATA_NAME, AFFECTED);
        doReturn(data).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(CONTROLLING_FIELDNAME);

        this.component.writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.optString(DATA_NAME, null));
        Assert.assertEquals(AFFECTED, json.getString(DATA_NAME));
    }

    @Test
    public void writeJSONAddsUnaffectedValueToJSON()
    {
        PatientData<String> data = new SimpleValuePatientData<>(DATA_NAME, UNAFFECTED);
        doReturn(data).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.component.writeJSON(this.patient, json);

        Assert.assertNotNull(json.optString(DATA_NAME, null));
        Assert.assertEquals(UNAFFECTED, json.getString(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsUnaffectedValueToJSON()
    {
        PatientData<String> data = new SimpleValuePatientData<>(DATA_NAME, UNAFFECTED);
        doReturn(data).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(CONTROLLING_FIELDNAME);

        this.component.writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.optString(DATA_NAME, null));
        Assert.assertEquals(UNAFFECTED, json.getString(DATA_NAME));
    }

    @Test
    public void checkGetName() throws ComponentLookupException
    {
        Assert.assertEquals(DATA_NAME, this.component.getName());
    }
}
