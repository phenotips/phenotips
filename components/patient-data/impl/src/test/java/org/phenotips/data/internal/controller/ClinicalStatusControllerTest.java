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

import static org.mockito.Mockito.doReturn;

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
        new MockitoComponentMockingRule<PatientDataController<String>>(ClinicalStatusController.class);

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject dataHolder;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        DocumentReference patientDocument = new DocumentReference("wiki", "patient", "00000001");
        doReturn(patientDocument).when(this.patient).getDocumentReference();
        doReturn(this.doc).when(this.patient).getDocument();
        doReturn(this.dataHolder).when(this.doc).getXObject(Patient.CLASS_REFERENCE);
    }

    @Test
    public void loadCatchesExceptionWhenPatientDoesNotHavePatientClass() throws ComponentLookupException
    {
        doReturn(null).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNull(result);
    }

    @Test
    public void loadReturnsNullWhenGivenUnexpectedIntValue() throws ComponentLookupException
    {
        doReturn(7314862).when(this.dataHolder).getIntValue(UNAFFECTED);

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNull(result);
    }

    @Test
    public void loadReturnsAffectedString() throws ComponentLookupException
    {
        doReturn(0).when(this.dataHolder).getIntValue(UNAFFECTED);

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertEquals(AFFECTED, result.getValue());
    }

    @Test
    public void loadReturnsUnaffectedString() throws ComponentLookupException
    {
        doReturn(1).when(this.dataHolder).getIntValue(UNAFFECTED);

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertEquals(UNAFFECTED, result.getValue());
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
    public void writeJSONWithSelectedFieldsReturnsWhenGetDataReturnsNotNull() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(CONTROLLING_FIELDNAME);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertTrue(json.has(DATA_NAME));
    }

    @Test
    public void writeJSONAddsAffectedValueToJSON() throws ComponentLookupException
    {
        PatientData<String> data = new SimpleValuePatientData<>(DATA_NAME, AFFECTED);
        doReturn(data).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        Assert.assertNotNull(json.opt(DATA_NAME));
        Assert.assertEquals(AFFECTED, json.getString(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsAffectedValueToJSON() throws ComponentLookupException
    {
        PatientData<String> data =
            new SimpleValuePatientData<>(DATA_NAME, AFFECTED);
        doReturn(data).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(CONTROLLING_FIELDNAME);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.optString(DATA_NAME, null));
        Assert.assertEquals(AFFECTED, json.getString(DATA_NAME));
    }

    @Test
    public void writeJSONAddsUnaffectedValueToJSON() throws ComponentLookupException
    {
        PatientData<String> data = new SimpleValuePatientData<>(DATA_NAME, UNAFFECTED);
        doReturn(data).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        Assert.assertNotNull(json.optString(DATA_NAME, null));
        Assert.assertEquals(UNAFFECTED, json.getString(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsUnaffectedValueToJSON() throws ComponentLookupException
    {
        PatientData<String> data = new SimpleValuePatientData<>(DATA_NAME, UNAFFECTED);
        doReturn(data).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(CONTROLLING_FIELDNAME);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.optString(DATA_NAME, null));
        Assert.assertEquals(UNAFFECTED, json.getString(DATA_NAME));
    }

    @Test
    public void checkGetName() throws ComponentLookupException
    {
        Assert.assertEquals(DATA_NAME, this.mocker.getComponentUnderTest().getName());
    }
}
