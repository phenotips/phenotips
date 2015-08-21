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

import org.phenotips.configuration.RecordConfiguration;
import org.phenotips.configuration.RecordConfigurationManager;
import org.phenotips.data.DictionaryPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

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
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for the {@link DatesController} Component, implementation of the
 * {@link org.phenotips.data.PatientDataController} interface
 */
public class DatesControllerTest
{
    @Rule
    public MockitoComponentMockingRule<PatientDataController<Date>> mocker =
        new MockitoComponentMockingRule<PatientDataController<Date>>(DatesController.class);

    private DocumentAccessBridge documentAccessBridge;

    private RecordConfigurationManager configurationManager;

    private Execution execution;

    @Mock
    private RecordConfiguration configuration;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private XWikiContext xWikiContext;

    @Mock
    private XWiki xWiki;

    @Mock
    private BaseObject data;

    @Mock
    private PatientData<Date> dateData;

    @Mock
    private XWikiDocument doc;

    @Mock
    private Patient patient;

    private static final String DATA_NAME = "dates";

    private DateFormat dateFormat;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);
        this.configurationManager = this.mocker.getInstance(RecordConfigurationManager.class);
        this.execution = this.mocker.getInstance(Execution.class);

        DocumentReference patientDocument = new DocumentReference("wiki", "patient", "00000001");
        doReturn(patientDocument).when(this.patient).getDocument();
        doReturn(this.doc).when(this.documentAccessBridge).getDocument(patientDocument);
        doReturn(this.data).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        doReturn(this.configuration).when(this.configurationManager).getActiveConfiguration();
        doReturn("yyyy-MM-dd'T'HH:mm:ss.SSSZ").when(this.configuration).getISODateFormat();

        doReturn(this.executionContext).when(this.execution).getContext();
        doReturn(this.xWikiContext).when(this.executionContext).getProperty("xwikicontext");
        doReturn(this.xWiki).when(this.xWikiContext).getWiki();
    }

    @Test
    public void loadCatchesExceptionWhenPatientDoesNotHavePatientClass() throws ComponentLookupException
    {
        doReturn(null).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        PatientData<Date> result = this.mocker.getComponentUnderTest().load(this.patient);

        verify(this.mocker.getMockedLogger()).error("Could not find requested document or some unforeseen"
            + " error has occurred during controller loading ",
            PatientDataController.ERROR_MESSAGE_NO_PATIENT_CLASS);
        Assert.assertNull(result);
    }

    @Test
    public void loadDoesNotReturnNullDates() throws ComponentLookupException
    {
        doReturn(null).when(this.data).getDateValue(anyString());

        PatientData<Date> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertEquals(0, result.size());
    }

    @Test
    public void loadReturnsExpectedDates() throws ComponentLookupException
    {
        Date birthDate = new Date(0);
        Date deathDate = new Date(999999999);
        Date examDate = new Date(100000);
        doReturn(birthDate).when(this.data).getDateValue(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME);
        doReturn(deathDate).when(this.data).getDateValue(DatesController.PATIENT_DATEOFDEATH_FIELDNAME);
        doReturn(examDate).when(this.data).getDateValue(DatesController.PATIENT_EXAMDATE_FIELDNAME);

        PatientData<Date> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertSame(birthDate, result.get(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME));
        Assert.assertSame(deathDate, result.get(DatesController.PATIENT_DATEOFDEATH_FIELDNAME));
        Assert.assertSame(examDate, result.get(DatesController.PATIENT_EXAMDATE_FIELDNAME));
    }

    @Test
    public void saveCatchesExceptionWhenPatientDoesNotHavePatientClass() throws ComponentLookupException
    {
        doReturn(null).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        this.mocker.getComponentUnderTest().save(this.patient);

        verify(this.mocker.getMockedLogger()).error("Failed to save dates: [{}]",
            PatientDataController.ERROR_MESSAGE_NO_PATIENT_CLASS);
    }

    @Test
    public void saveCatchesExceptionWhenGetDateDataReturnsNull() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);

        this.mocker.getComponentUnderTest().save(this.patient);

        verify(this.mocker.getMockedLogger()).error("Failed to save dates: [{}]", (String) null);
    }

    @Test
    public void saveDoesNotContinueIfDateDataIsNotNamed() throws ComponentLookupException
    {
        doReturn(this.dateData).when(this.patient).getData(DATA_NAME);
        doReturn(false).when(this.dateData).isNamed();

        this.mocker.getComponentUnderTest().save(this.patient);

        verify(this.data, never()).setDateValue(anyString(), any(Date.class));
    }

    @Test
    public void saveDoesNotAddNullDates() throws ComponentLookupException
    {
        doReturn(this.dateData).when(this.patient).getData(DATA_NAME);
        doReturn(true).when(this.dateData).isNamed();
        doReturn(null).when(this.dateData).get(anyString());

        this.mocker.getComponentUnderTest().save(this.patient);

        verify(this.data, never()).setDateValue(anyString(), any(Date.class));
    }

    @Test
    public void saveCatchesXWikiException() throws XWikiException, ComponentLookupException
    {
        doReturn(this.dateData).when(this.patient).getData(DATA_NAME);
        doReturn(true).when(this.dateData).isNamed();
        doReturn(null).when(this.dateData).get(anyString());
        XWikiException exception = new XWikiException();
        doThrow(exception).when(this.xWiki).saveDocument(any(XWikiDocument.class),
            anyString(), any(Boolean.class), any(XWikiContext.class));

        this.mocker.getComponentUnderTest().save(this.patient);

        verify(this.mocker.getMockedLogger()).error("Failed to save dates: [{}]", exception.getMessage());
    }

    @Test
    public void saveAddsAllDates() throws XWikiException, ComponentLookupException
    {
        Map<String, Date> datesMap = new LinkedHashMap<String, Date>();
        Date birthDate = new Date(0);
        Date deathDate = new Date(999999999);
        Date examDate = new Date(100000);
        datesMap.put(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME, birthDate);
        datesMap.put(DatesController.PATIENT_DATEOFDEATH_FIELDNAME, deathDate);
        datesMap.put(DatesController.PATIENT_EXAMDATE_FIELDNAME, examDate);
        PatientData<Date> datesData = new DictionaryPatientData<>(DATA_NAME, datesMap);
        doReturn(datesData).when(this.patient).getData(DATA_NAME);

        this.mocker.getComponentUnderTest().save(this.patient);

        verify(this.data).setDateValue(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME, birthDate);
        verify(this.data).setDateValue(DatesController.PATIENT_DATEOFDEATH_FIELDNAME, deathDate);
        verify(this.data).setDateValue(DatesController.PATIENT_EXAMDATE_FIELDNAME, examDate);

        verify(this.xWiki).saveDocument(same(this.doc), anyString(), eq(true), same(this.xWikiContext));
    }

    @Test
    public void writeJSONReturnsWhenGetDataReturnsNull() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);
    }

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenGetDataReturnsNull() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new ArrayList<>();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);
    }

    @Test
    public void writeJSONAddsAllDates() throws ComponentLookupException
    {
        Map<String, Date> datesMap = new LinkedHashMap<String, Date>();
        Date birthDate = new Date(0);
        Date deathDate = new Date(999999999);
        Date examDate = new Date(100000);
        datesMap.put(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME, birthDate);
        datesMap.put(DatesController.PATIENT_DATEOFDEATH_FIELDNAME, deathDate);
        datesMap.put(DatesController.PATIENT_EXAMDATE_FIELDNAME, examDate);
        PatientData<Date> datesData = new DictionaryPatientData<>(DATA_NAME, datesMap);
        doReturn(datesData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        Assert.assertEquals(this.dateFormat.format(birthDate),
            json.get(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME));
        Assert.assertEquals(this.dateFormat.format(deathDate),
            json.get(DatesController.PATIENT_DATEOFDEATH_FIELDNAME));
        Assert.assertEquals(this.dateFormat.format(examDate),
            json.get(DatesController.PATIENT_EXAMDATE_FIELDNAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsAllSelectedDates() throws ComponentLookupException
    {
        Map<String, Date> datesMap = new LinkedHashMap<String, Date>();
        Date birthDate = new Date(0);
        Date deathDate = new Date(999999999);
        Date examDate = new Date(100000);
        datesMap.put(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME, birthDate);
        datesMap.put(DatesController.PATIENT_DATEOFDEATH_FIELDNAME, deathDate);
        datesMap.put(DatesController.PATIENT_EXAMDATE_FIELDNAME, examDate);
        PatientData<Date> datesData = new DictionaryPatientData<>(DATA_NAME, datesMap);
        doReturn(datesData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new ArrayList<>();
        selectedFields.add(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME);
        selectedFields.add(DatesController.PATIENT_EXAMDATE_FIELDNAME);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertEquals(this.dateFormat.format(birthDate),
            json.get(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME));
        Assert.assertNull(json.get(DatesController.PATIENT_DATEOFDEATH_FIELDNAME));
        Assert.assertEquals(this.dateFormat.format(examDate),
            json.get(DatesController.PATIENT_EXAMDATE_FIELDNAME));
    }

    @Test
    public void readJSONReturnsNullWhenJSONIsEmpty() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();
        Assert.assertNull(this.mocker.getComponentUnderTest().readJSON(json));
    }

    @Test
    public void readJSONCatchesParseException() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();
        Date deathDate = new Date(999999999);
        Date examDate = new Date(100000);
        json.put(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME, "!!!!!!!!!!!!!!!!!!!!");
        json.put(DatesController.PATIENT_DATEOFDEATH_FIELDNAME, this.dateFormat.format(deathDate));
        json.put(DatesController.PATIENT_EXAMDATE_FIELDNAME, this.dateFormat.format(examDate));

        PatientData<Date> result = this.mocker.getComponentUnderTest().readJSON(json);

        Assert.assertNull(result.get(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME));
        Assert.assertEquals(deathDate, result.get(DatesController.PATIENT_DATEOFDEATH_FIELDNAME));
        Assert.assertEquals(examDate, result.get(DatesController.PATIENT_EXAMDATE_FIELDNAME));
    }

    @Test
    public void readJSONReturnsAllDates() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();
        Date birthDate = new Date(0);
        Date deathDate = new Date(999999999);
        Date examDate = new Date(100000);
        json.put(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME, this.dateFormat.format(birthDate));
        json.put(DatesController.PATIENT_DATEOFDEATH_FIELDNAME, this.dateFormat.format(deathDate));
        json.put(DatesController.PATIENT_EXAMDATE_FIELDNAME, this.dateFormat.format(examDate));

        PatientData<Date> result = this.mocker.getComponentUnderTest().readJSON(json);

        Assert.assertEquals(birthDate, result.get(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME));
        Assert.assertEquals(deathDate, result.get(DatesController.PATIENT_DATEOFDEATH_FIELDNAME));
        Assert.assertEquals(examDate, result.get(DatesController.PATIENT_EXAMDATE_FIELDNAME));
    }

    @Test
    public void checkGetName() throws ComponentLookupException
    {
        Assert.assertEquals(DATA_NAME, this.mocker.getComponentUnderTest().getName());
    }
}
