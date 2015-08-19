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

import com.xpn.xwiki.objects.BaseProperty;
import org.phenotips.data.DictionaryPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.SimpleValuePatientData;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import net.sf.json.JSONObject;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Test for the {@link AbstractComplexController} defined methods (load, save, writeJSON, readJSON).
 * These methods are tested using a mock implementation of {@link AbstractComplexController} that provides
 * simple definitions of the abstract methods getName, getProperties, and getJsonPropertyName
 */
public class AbstractComplexControllerTest
{

    @Rule
    public MockitoComponentMockingRule<PatientDataController<String>> mocker =
        new MockitoComponentMockingRule<PatientDataController<String>>(
        AbstractComplexControllerTestImplementation.class);

    private DocumentAccessBridge documentAccessBridge;

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject data;

    @Mock
    private BaseProperty<ObjectPropertyReference> baseProperty1;

    @Mock
    private BaseProperty<ObjectPropertyReference> baseProperty2;

    @Mock
    private BaseProperty<ObjectPropertyReference> baseProperty3;

    private final String DATA_NAME = AbstractComplexControllerTestImplementation.DATA_NAME;

    private final String PROPERTY_1 = AbstractComplexControllerTestImplementation.PROPERTY_1;

    private final String PROPERTY_2 = AbstractComplexControllerTestImplementation.PROPERTY_2;

    private final String PROPERTY_3 = AbstractComplexControllerTestImplementation.PROPERTY_3;


    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);

        DocumentReference patientDocument = new DocumentReference("wiki", "patient", "00000001");
        doReturn(patientDocument).when(this.patient).getDocument();
        doReturn(this.doc).when(this.documentAccessBridge).getDocument(patientDocument);
        doReturn(this.data).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        doReturn(baseProperty1).when(this.data).getField(PROPERTY_1);
        doReturn(baseProperty2).when(this.data).getField(PROPERTY_2);
        doReturn(baseProperty3).when(this.data).getField(PROPERTY_3);
    }

    @Test
    public void checkGetName() throws ComponentLookupException
    {
        Assert.assertEquals(DATA_NAME, this.mocker.getComponentUnderTest().getName());
    }

    //-----------------------------------load() tests-----------------------------------

    @Test
    public void loadCatchesExceptionFromDocumentAccess() throws Exception
    {
        Exception exception = new Exception();
        doThrow(exception).when(this.documentAccessBridge).getDocument(any(DocumentReference.class));

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        verify(this.mocker.getMockedLogger()).error("Could not find requested document or some unforeseen error has " +
            "occurred during controller loading");
        Assert.assertNull(result);
    }

    @Test
    public void loadCatchesExceptionWhenPatientDoesNotHavePatientClass() throws ComponentLookupException
    {
        doReturn(null).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        verify(this.mocker.getMockedLogger()).error("Could not find requested document or some unforeseen"
            + " error has occurred during controller loading");
        Assert.assertNull(result);
    }

    @Test
    public void loadReturnsAllData() throws ComponentLookupException
    {
        String datum1 = "datum2";
        String datum2 = "datum2";
        String datum3 = "datum3";
        doReturn(datum1).when(this.baseProperty1).getValue();
        doReturn(datum2).when(this.baseProperty2).getValue();
        doReturn(datum3).when(this.baseProperty3).getValue();

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertEquals(datum1, result.get(PROPERTY_1));
        Assert.assertEquals(datum2, result.get(PROPERTY_2));
        Assert.assertEquals(datum3, result.get(PROPERTY_3));
        Assert.assertEquals(3, result.size());
    }

    //-----------------------------------save() tests-----------------------------------

    @Test(expected = UnsupportedOperationException.class)
    public void saveIsUnsupported() throws ComponentLookupException
    {
        this.mocker.getComponentUnderTest().save(this.patient);
    }

    //-----------------------------------writeJSON() tests-----------------------------------

    @Test
    public void writeJSONReturnsWhenGetDataReturnsNull() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        Assert.assertNull(json.get(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenGetDataReturnsNull() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNull(json.get(DATA_NAME));
    }

    @Test
    public void writeJSONReturnsWhenDataIsNotKeyValueBased() throws ComponentLookupException
    {
        PatientData<String> patientData = new SimpleValuePatientData<>(DATA_NAME, "datum");
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        Assert.assertNull(json.get(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenDataIsNotKeyValueBased() throws ComponentLookupException
    {
        PatientData<String> patientData = new SimpleValuePatientData<>(DATA_NAME, "datum");
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNull(json.get(DATA_NAME));
    }

    @Test
    public void writeJSONAddsContainerWithAllValues() throws ComponentLookupException
    {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(PROPERTY_1, "datum1");
        map.put(PROPERTY_2, "datum2");
        map.put(PROPERTY_3, "datum3");
        PatientData<String> patientData = new DictionaryPatientData<String>(this.DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        Assert.assertNotNull(json.get(DATA_NAME));
        Assert.assertTrue(json.get(DATA_NAME) instanceof JSONObject);
        JSONObject container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals("datum1", container.get(PROPERTY_1));
        Assert.assertEquals("datum2", container.get(PROPERTY_2));
        Assert.assertEquals("datum3", container.get(PROPERTY_3));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsContainerWithAllValues() throws ComponentLookupException
    {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(PROPERTY_1, "datum1");
        map.put(PROPERTY_2, "datum2");
        map.put(PROPERTY_3, "datum3");
        PatientData<String> patientData = new DictionaryPatientData<String>(this.DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(PROPERTY_1);
        selectedFields.add(PROPERTY_2);
        selectedFields.add(PROPERTY_3);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.get(DATA_NAME));
        Assert.assertTrue(json.get(DATA_NAME) instanceof JSONObject);
        JSONObject container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals("datum1", container.get(PROPERTY_1));
        Assert.assertEquals("datum2", container.get(PROPERTY_2));
        Assert.assertEquals("datum3", container.get(PROPERTY_3));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsSelectedValues() throws ComponentLookupException
    {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(PROPERTY_1, "datum1");
        map.put(PROPERTY_2, "datum2");
        map.put(PROPERTY_3, "datum3");
        PatientData<String> patientData = new DictionaryPatientData<String>(this.DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(PROPERTY_1);
        selectedFields.add(PROPERTY_3);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.get(DATA_NAME));
        Assert.assertTrue(json.get(DATA_NAME) instanceof JSONObject);
        JSONObject container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals("datum1", container.get(PROPERTY_1));
        Assert.assertEquals("datum3", container.get(PROPERTY_3));
        Assert.assertNull(container.get(PROPERTY_2));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsContainerWithAllValuesWhenSelectedFieldsNull()
            throws ComponentLookupException
    {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(PROPERTY_1, "datum1");
        map.put(PROPERTY_2, "datum2");
        map.put(PROPERTY_3, "datum3");
        PatientData<String> patientData = new DictionaryPatientData<String>(this.DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, null);

        Assert.assertNotNull(json.get(DATA_NAME));
        Assert.assertTrue(json.get(DATA_NAME) instanceof JSONObject);
        JSONObject container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals("datum1", container.get(PROPERTY_1));
        Assert.assertEquals("datum2", container.get(PROPERTY_2));
        Assert.assertEquals("datum3", container.get(PROPERTY_3));
    }

    @Test
    public void writeJSONDoesNotOverwriteContainer() throws ComponentLookupException
    {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(PROPERTY_1, "datum1");
        map.put(PROPERTY_2, "datum2");
        map.put(PROPERTY_3, "datum3");
        PatientData<String> patientData = new DictionaryPatientData<String>(this.DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(PROPERTY_1);
        selectedFields.add(PROPERTY_3);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.get(DATA_NAME));
        Assert.assertTrue(json.get(DATA_NAME) instanceof JSONObject);
        JSONObject container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals("datum1", container.get(PROPERTY_1));
        Assert.assertEquals("datum3", container.get(PROPERTY_3));
        Assert.assertNull(container.get(PROPERTY_2));

        selectedFields.clear();
        selectedFields.add(PROPERTY_2);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.get(DATA_NAME));
        Assert.assertTrue(json.get(DATA_NAME) instanceof JSONObject);
        container = json.getJSONObject(DATA_NAME);
        Assert.assertEquals("datum1", container.get(PROPERTY_1));
        Assert.assertEquals("datum2", container.get(PROPERTY_2));
        Assert.assertEquals("datum3", container.get(PROPERTY_3));
    }


    //-----------------------------------readJSON() tests-----------------------------------

    @Test(expected = UnsupportedOperationException.class)
    public void readJSONIsUnsupported() throws ComponentLookupException
    {
        this.mocker.getComponentUnderTest().readJSON(new JSONObject());
    }
}
