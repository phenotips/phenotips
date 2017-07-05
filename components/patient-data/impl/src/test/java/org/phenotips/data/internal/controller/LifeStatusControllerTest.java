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

import org.xwiki.bridge.DocumentAccessBridge;
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
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
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

    private DocumentAccessBridge documentAccessBridge;

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

        this.documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);

        DocumentReference patientDocument = new DocumentReference("wiki", "patient", "00000001");
        doReturn(patientDocument).when(this.patient).getDocument();
        doReturn(this.doc).when(this.documentAccessBridge).getDocument(patientDocument);
        doReturn(this.data).when(this.doc).getXObject(Patient.CLASS_REFERENCE);
    }

    @Test
    public void loadCatchesExceptionFromDocumentAccess() throws Exception
    {
        doThrow(Exception.class).when(this.documentAccessBridge).getDocument(any(DocumentReference.class));

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        verify(this.mocker.getMockedLogger()).error("Could not find requested document or some unforeseen"
            + " error has occurred during controller loading ", (String) null);
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

    @Test
    public void saveAliveWhenAlive() throws XWikiException, ComponentLookupException
    {
        PatientData<String> lifeStatus = new SimpleValuePatientData<>(DATA_NAME, ALIVE);
        doReturn(lifeStatus).when(this.patient).getData(DATA_NAME);

        this.mocker.getComponentUnderTest().save(this.patient, this.doc);

        verify(this.data).setStringValue(DATA_NAME, ALIVE);
    }

    @Test
    public void saveDeceasedWhenDeceased() throws XWikiException, ComponentLookupException
    {
        PatientData<String> lifeStatus = new SimpleValuePatientData<>(DATA_NAME, DECEASED);
        doReturn(lifeStatus).when(this.patient).getData(DATA_NAME);

        this.mocker.getComponentUnderTest().save(this.patient, this.doc);

        verify(this.data).setStringValue(DATA_NAME, DECEASED);
    }
}
