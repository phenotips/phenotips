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

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

/**
 * Test for the {@link LifeStatusController} component, implementation of the
 * {@link org.phenotips.data.PatientDataController} interface.
 */
public class LifeStatusControllerTest
{
    private static final String DATA_NAME = "life_status";

    private static final String ALIVE = "alive";

    private static final String DECEASED = "deceased";

    @Rule
    public MockitoComponentMockingRule<PatientDataController<String>> mocker =
        new MockitoComponentMockingRule<PatientDataController<String>>(LifeStatusController.class);

    @Mock
    private XWiki xwiki;

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

        DocumentReference patientDocRef = new DocumentReference("wiki", "patient", "00000001");
        doReturn(patientDocRef).when(this.patient).getDocumentReference();
        doReturn(this.doc).when(this.patient).getXDocument();
        doReturn(this.data).when(this.doc).getXObject(Patient.CLASS_REFERENCE);
    }

    @Test
    public void loadCatchesInvalidDocument() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getXDocument();

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        verify(this.mocker.getMockedLogger()).error(eq(PatientDataController.ERROR_MESSAGE_LOAD_FAILED), anyString());
        Assert.assertNull(result);
    }

    @Test
    public void loadCatchesExceptionWhenPatientDoesNotHavePatientClass() throws ComponentLookupException
    {
        doReturn(null).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNull(result);
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
        selectedFields.add(DATA_NAME);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertTrue(json.has(DATA_NAME));
    }

    @Test
    public void writeJSONAddsLifeStatus() throws ComponentLookupException
    {
        doReturn(new SimpleValuePatientData<>(DATA_NAME, ALIVE)).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        Assert.assertEquals(ALIVE, json.get(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsLifeStatus() throws ComponentLookupException
    {
        doReturn(new SimpleValuePatientData<>(DATA_NAME, DECEASED)).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(DATA_NAME);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertEquals(DECEASED, json.get(DATA_NAME));
    }

    @Test
    public void readJSONEmptyJsonReturnsNull() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().readJSON(new JSONObject()));
    }

    @Test
    public void readJSONReturnsCorrectLifeStatus() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();
        json.put(DATA_NAME, ALIVE);
        PatientData<String> result = this.mocker.getComponentUnderTest().readJSON(json);
        Assert.assertEquals(ALIVE, result.getValue());

        json = new JSONObject();
        json.put(DATA_NAME, DECEASED);
        result = this.mocker.getComponentUnderTest().readJSON(json);
        Assert.assertEquals(DECEASED, result.getValue());
    }

    @Test
    public void readJSONDoesNotReturnUnexpectedValue() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();
        json.put(DATA_NAME, "!!!!!");
        Assert.assertNull(this.mocker.getComponentUnderTest().readJSON(json));
    }

    @Test
    public void checkGetName() throws ComponentLookupException
    {
        Assert.assertEquals(DATA_NAME, this.mocker.getComponentUnderTest().getName());
    }
}