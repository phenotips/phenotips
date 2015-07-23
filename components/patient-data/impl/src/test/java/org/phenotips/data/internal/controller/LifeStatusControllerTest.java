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

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
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
import org.phenotips.data.SimpleValuePatientData;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import javax.inject.Provider;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Test for the {@link LifeStatusController} component,
 * implementation of the {@link org.phenotips.data.PatientDataController} interface
 */
public class LifeStatusControllerTest
{
    @Rule
    public MockitoComponentMockingRule<PatientDataController> mocker =
            new MockitoComponentMockingRule<PatientDataController>(LifeStatusController.class);

    private Logger logger;

    private DocumentAccessBridge documentAccessBridge;

    private LifeStatusController controller;

    private XWikiContext xcontext;

    @Mock
    private XWiki xwiki;

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject data;

    private final String DATA_NAME = "life_status";

    private final String PATIENT_UNKNOWN_DATEOFDEATH_FIELDNAME = "date_of_death_unknown";

    private final String PATIENT_DATEOFDEATH_FIELDNAME = "date_of_death";

    private final String ALIVE = "alive";

    private final String DECEASED = "deceased";

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.controller = (LifeStatusController)this.mocker.getComponentUnderTest();
        this.logger = this.mocker.getMockedLogger();
        this.documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);

        DocumentReference patientDocument = new DocumentReference("wiki", "patient", "00000001");
        doReturn(patientDocument).when(this.patient).getDocument();
        doReturn(this.doc).when(this.documentAccessBridge).getDocument(patientDocument);
        doReturn(this.data).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        Provider<XWikiContext> provider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        this.xcontext = provider.get();
        doReturn(this.xwiki).when(this.xcontext).getWiki();
    }

    @Test
    public void loadCatchesExceptionFromDocumentAccess() throws Exception
    {
        doThrow(Exception.class).when(this.documentAccessBridge).getDocument(any(DocumentReference.class));

        PatientData<String> result = this.controller.load(this.patient);

        verify(this.logger).error("Could not find requested document or some unforeseen"
            + " error has occurred during controller loading ", (String)null);
        Assert.assertNull(result);
    }

    @Test
    public void loadCatchesExceptionWhenPatientDoesNotHavePatientClass()
    {
        doReturn(null).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        PatientData<String> result = this.controller.load(this.patient);

        verify(this.logger).error("Could not find requested document or some unforeseen"
            + " error has occurred during controller loading ",
            PatientDataController.ERROR_MESSAGE_NO_PATIENT_CLASS);
        Assert.assertNull(result);
    }

    @Test
    public void loadChecksUnknownDateOfDeathFieldWhenDateOfDeathIsNull()
    {
        doReturn(null).when(this.data).getDateValue(PATIENT_DATEOFDEATH_FIELDNAME);
        doReturn(1).when(this.data).getIntValue(PATIENT_UNKNOWN_DATEOFDEATH_FIELDNAME);

        PatientData<String> result = this.controller.load(this.patient);

        verify(this.data).getIntValue(PATIENT_UNKNOWN_DATEOFDEATH_FIELDNAME);
        Assert.assertEquals(DECEASED, result.getValue());
    }

    @Test
    public void loadReturnsDeceasedWhenDateOfDeathIsDefined()
    {
        doReturn(new Date(0)).when(this.data).getDateValue(PATIENT_DATEOFDEATH_FIELDNAME);
        doReturn(0).when(this.data).getIntValue(PATIENT_UNKNOWN_DATEOFDEATH_FIELDNAME);

        PatientData<String> result = this.controller.load(this.patient);

        Assert.assertEquals(DECEASED, result.getValue());
    }

    @Test
    public void loadReturnsAliveWhenDateOfDeathIsNullAndUnknownDateOfDeathIsNotSet()
    {
        doReturn(null).when(this.data).getDateValue(PATIENT_DATEOFDEATH_FIELDNAME);
        doReturn(0).when(this.data).getIntValue(PATIENT_UNKNOWN_DATEOFDEATH_FIELDNAME);

        PatientData<String> result = this.controller.load(this.patient);

        verify(this.data).getDateValue(PATIENT_DATEOFDEATH_FIELDNAME);
        verify(this.data).getIntValue(PATIENT_UNKNOWN_DATEOFDEATH_FIELDNAME);
        Assert.assertEquals(ALIVE, result.getValue());
    }

    @Test
    public void saveCatchesExceptionFromDocumentAccess() throws Exception
    {
        doThrow(Exception.class).when(this.documentAccessBridge).getDocument(any(DocumentReference.class));

        this.controller.save(this.patient);

        verify(this.logger).error("Failed to save life status: [{}]", (String)null);
    }

    @Test
    public void saveCatchesExceptionWhenPatientDoesNotHavePatientClass()
    {
        doReturn(null).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        this.controller.save(this.patient);

        verify(this.logger).error("Failed to save life status: [{}]",
            PatientDataController.ERROR_MESSAGE_NO_PATIENT_CLASS);
    }

    @Test
    public void saveCatchesExceptionFromSaveDocument() throws XWikiException
    {
        XWikiException exception = new XWikiException();
        doThrow(exception).when(this.xwiki).saveDocument(any(XWikiDocument.class),
            anyString(), anyBoolean(), any(XWikiContext.class));
        doReturn(null).when(this.patient).getData(DATA_NAME);
        doReturn(null).when(this.patient).getData("dates");

        this.controller.save(this.patient);

        verify(this.xwiki).saveDocument(any(XWikiDocument.class),
            anyString(), anyBoolean(), any(XWikiContext.class));
        verify(this.logger).error("Failed to save life status: [{}]",
                exception.getMessage());
    }

    @Test
    public void saveSetsDateOfDeathUnknownWhenDeceasedAndDatesNull() throws XWikiException
    {
        PatientData<String> lifeStatus = new SimpleValuePatientData<String>(DATA_NAME, DECEASED);
        doReturn(lifeStatus).when(this.patient).getData(DATA_NAME);
        doReturn(null).when(this.patient).getData("dates");

        this.controller.save(this.patient);

        verify(this.data).setIntValue(PATIENT_UNKNOWN_DATEOFDEATH_FIELDNAME, 1);
        verify(this.xwiki).saveDocument(this.doc, "Updated life status from JSON", true, this.xcontext);
    }

    @Test
    public void saveSetsDateOfDeathUnknownWhenDeceasedAndDateOfDeathNull() throws XWikiException
    {
        PatientData<String> lifeStatus = new SimpleValuePatientData<String>(DATA_NAME, DECEASED);
        doReturn(lifeStatus).when(this.patient).getData(DATA_NAME);
        Map<String, Date> datesMap = new LinkedHashMap<>();
        datesMap.put(PATIENT_DATEOFDEATH_FIELDNAME, null);
        PatientData<Date> dates = new DictionaryPatientData<Date>("dates", datesMap);
        doReturn(dates).when(this.patient).getData("dates");

        this.controller.save(this.patient);

        verify(this.data).setIntValue(PATIENT_UNKNOWN_DATEOFDEATH_FIELDNAME, 1);
        verify(this.xwiki).saveDocument(this.doc, "Updated life status from JSON", true, this.xcontext);
    }

    @Test
    public void saveClearsDateOfDeathUnknownByDefault() throws XWikiException
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        doReturn(null).when(this.patient).getData("dates");

        this.controller.save(this.patient);

        verify(this.data).setIntValue(PATIENT_UNKNOWN_DATEOFDEATH_FIELDNAME, 0);
        verify(this.xwiki).saveDocument(this.doc, "Updated life status from JSON", true, this.xcontext);
    }

    @Test
    public void saveClearsDateOfDeathUnknownWhenAlive() throws XWikiException
    {
        PatientData<String> lifeStatus = new SimpleValuePatientData<String>(DATA_NAME, ALIVE);
        doReturn(lifeStatus).when(this.patient).getData(DATA_NAME);
        doReturn(null).when(this.patient).getData("dates");

        this.controller.save(this.patient);

        verify(this.data).setIntValue(PATIENT_UNKNOWN_DATEOFDEATH_FIELDNAME, 0);
        verify(this.xwiki).saveDocument(this.doc, "Updated life status from JSON", true, this.xcontext);
    }

    @Test
    public void saveClearsDateOfDeathUnknownWhenDeceasedAndDateOfDeathDefined() throws XWikiException
    {
        PatientData<String> lifeStatus = new SimpleValuePatientData<String>(DATA_NAME, DECEASED);
        doReturn(lifeStatus).when(this.patient).getData(DATA_NAME);
        Map<String, Date> datesMap = new LinkedHashMap<>();
        datesMap.put(PATIENT_DATEOFDEATH_FIELDNAME, new Date(0));
        PatientData<Date> dates = new DictionaryPatientData<Date>("dates", datesMap);
        doReturn(dates).when(this.patient).getData("dates");

        this.controller.save(this.patient);

        verify(this.data).setIntValue(PATIENT_UNKNOWN_DATEOFDEATH_FIELDNAME, 0);
        verify(this.xwiki).saveDocument(this.doc, "Updated life status from JSON", true, this.xcontext);
    }

    @Test
    public void saveIgnoresDatesWhenDatesIsNotKeyValueBased() throws XWikiException
    {
        PatientData<String> lifeStatus = new SimpleValuePatientData<String>(DATA_NAME, DECEASED);
        doReturn(lifeStatus).when(this.patient).getData(DATA_NAME);
        PatientData<Date> dates = new SimpleValuePatientData<>(PATIENT_DATEOFDEATH_FIELDNAME, new Date());
        doReturn(dates).when(this.patient).getData("dates");

        this.controller.save(this.patient);

        verify(this.data).setIntValue(PATIENT_UNKNOWN_DATEOFDEATH_FIELDNAME, 1);
        verify(this.xwiki).saveDocument(this.doc, "Updated life status from JSON", true, this.xcontext);
    }

    @Test
    public void writeJSONReturnsWhenGetDataReturnsNull()
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.controller.writeJSON(this.patient, json);

        Assert.assertNull(json.get(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenGetDataReturnsNull()
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();

        this.controller.writeJSON(this.patient, json, selectedFields);

        Assert.assertNull(json.get(DATA_NAME));
    }

    @Test
    public void writeJSONAddsLifeStatus()
    {
        doReturn(new SimpleValuePatientData<String>(DATA_NAME, ALIVE)).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.controller.writeJSON(this.patient, json);

        Assert.assertEquals(ALIVE, json.get(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsLifeStatus()
    {
        doReturn(new SimpleValuePatientData<String>(DATA_NAME, DECEASED)).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();

        this.controller.writeJSON(this.patient, json, selectedFields);

        Assert.assertEquals(DECEASED, json.get(DATA_NAME));
    }

    @Test
    public void readJSONEmptyJsonReturnsNull()
    {
        Assert.assertNull(this.controller.readJSON(new JSONObject()));
    }

    @Test
    public void readJSONReturnsCorrectLifeStatus()
    {
        JSONObject json = new JSONObject();
        json.put(DATA_NAME, ALIVE);
        PatientData<String> result = this.controller.readJSON(json);
        Assert.assertEquals(ALIVE, result.getValue());

        json = new JSONObject();
        json.put(DATA_NAME, DECEASED);
        result = this.controller.readJSON(json);
        Assert.assertEquals(DECEASED, result.getValue());
    }

    @Test
    public void readJSONDoesNotReturnUnexpectedValue()
    {
        JSONObject json = new JSONObject();
        json.put(DATA_NAME, "!!!!!");
        Assert.assertNull(this.controller.readJSON(json));
    }

    @Test
    public void checkGetName()
    {
        Assert.assertEquals(DATA_NAME, this.controller.getName());
    }


}
