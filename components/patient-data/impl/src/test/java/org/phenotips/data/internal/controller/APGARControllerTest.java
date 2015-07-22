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

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.phenotips.data.DictionaryPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

/**
 * Tests for the {@link APGARController} Component,
 * implementation of the {@link org.phenotips.data.PatientDataController} interface
 */
public class APGARControllerTest
{
    @Rule
    public MockitoComponentMockingRule<PatientDataController> mocker =
            new MockitoComponentMockingRule<PatientDataController>(APGARController.class);

    private static final String DATA_NAME = "apgar";

    private static final String APGAR_1 = "apgar1";

    private static final String APGAR_5 = "apgar5";

    private Logger logger;

    private DocumentAccessBridge documentAccessBridge;

    private APGARController controller;

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

        this.controller = (APGARController)this.mocker.getComponentUnderTest();
        this.logger = this.mocker.getMockedLogger();
        this.documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);

        DocumentReference patientDocument = new DocumentReference("wiki", "patient", "00000001");
        doReturn(patientDocument).when(this.patient).getDocument();
        doReturn(this.doc).when(this.documentAccessBridge).getDocument(patientDocument);
        doReturn(this.data).when(this.doc).getXObject(Patient.CLASS_REFERENCE);
    }

    @Test
    public void loadCatchesExceptionWhenPatientDoesNotHavePatientClass()
    {
        doReturn(null).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        PatientData<Integer> result = this.controller.load(this.patient);

        verify(this.logger).error("Could not find requested document or some unforeseen"
                        + " error has occurred during controller loading ",
                PatientDataController.ERROR_MESSAGE_NO_PATIENT_CLASS);
        Assert.assertNull(result);
    }

    @Test
    public void loadDoesNotReturnNullIntegers()
    {
        doReturn(null).when(this.data).getStringValue(anyString());

        PatientData<Integer> result = this.controller.load(this.patient);

        Assert.assertEquals(0, result.size());
    }

    @Test
    public void loadDoesNotReturnNonIntegerStrings()
    {
        doReturn("STRING").when(this.data).getStringValue(anyString());

        PatientData<Integer> result = this.controller.load(this.patient);

        Assert.assertEquals(0, result.size());
    }

    @Test
    public void loadReturnsExpectedIntegers()
    {
        doReturn("1").when(this.data).getStringValue(APGAR_1);
        doReturn("2").when(this.data).getStringValue(APGAR_5);

        PatientData<Integer> result = this.controller.load(this.patient);

        Assert.assertEquals(Integer.valueOf(1), result.get(APGAR_1));
        Assert.assertEquals(Integer.valueOf(2), result.get(APGAR_5));
        Assert.assertEquals(2, result.size());
    }

    @Test
    public void writeJSONReturnsWhenGetDataReturnsNull() {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.controller.writeJSON(this.patient, json);

        Assert.assertTrue(json.getJSONObject(DATA_NAME) == null || json.getJSONObject(DATA_NAME).isNullObject());
    }

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenGetDataReturnsNull() {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new ArrayList<>();
        selectedFields.add(APGAR_1);
        selectedFields.add(APGAR_5);
        
        this.controller.writeJSON(this.patient, json, selectedFields);

        Assert.assertTrue(json.getJSONObject(DATA_NAME) == null || json.getJSONObject(DATA_NAME).isNullObject());
    }

    @Test
    public void writeJSONReturnsWhenDataIsEmpty() {
        Map<String, Integer> map = new LinkedHashMap<>();
        PatientData<Integer> data = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(data).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.controller.writeJSON(this.patient, json);

        Assert.assertTrue(json.getJSONObject(DATA_NAME) == null || json.getJSONObject(DATA_NAME).isNullObject());
    }

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenDataIsEmpty() {
        Map<String, Integer> map = new LinkedHashMap<>();
        PatientData<Integer> data = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(data).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new ArrayList<>();
        selectedFields.add(APGAR_1);
        selectedFields.add(APGAR_5);

        this.controller.writeJSON(this.patient, json, selectedFields);

        Assert.assertTrue(json.getJSONObject(DATA_NAME) == null || json.getJSONObject(DATA_NAME).isNullObject());
    }

    @Test
    public void writeJSONAddsAllDataEntriesToJSON() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put(APGAR_1, 1);
        map.put(APGAR_5, 2);
        PatientData<Integer> data = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(data).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.controller.writeJSON(this.patient, json);

        Assert.assertNotNull(json.getJSONObject(DATA_NAME));
        Assert.assertEquals(1, json.getJSONObject(DATA_NAME).get(APGAR_1));
        Assert.assertEquals(2, json.getJSONObject(DATA_NAME).get(APGAR_5));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsAllDataEntriesWhenAPGARSelected() {
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

        this.controller.writeJSON(this.patient, json, selectedFields);

        Assert.assertNotNull(json.getJSONObject(DATA_NAME));
        Assert.assertEquals(1, json.getJSONObject(DATA_NAME).get(APGAR_1));
        Assert.assertEquals(2, json.getJSONObject(DATA_NAME).get(APGAR_5));
    }

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenAPGARNotSelected() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put(APGAR_1, 1);
        map.put(APGAR_5, 2);
        PatientData<Integer> data = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(data).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add("dates");
        selectedFields.add("ethnicity");

        this.controller.writeJSON(this.patient, json, selectedFields);

        Assert.assertTrue(json.getJSONObject(DATA_NAME) == null || json.getJSONObject(DATA_NAME).isNullObject());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void saveIsUnsupported(){
        this.controller.save(this.patient);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void readJSONIsUnsupported(){
        this.controller.readJSON(new JSONObject());
    }

    @Test
    public void checkGetName()
    {
        Assert.assertEquals(DATA_NAME, this.controller.getName());
    }
}
