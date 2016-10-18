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
import org.phenotips.data.PhenoTipsDate;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONObject;
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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Tests for the {@link DatesController} Component, implementation of the
 * {@link org.phenotips.data.PatientDataController} interface.
 */
public class DatesControllerTest
{
    private static final String DATA_NAME = "dates";

    @Rule
    public MockitoComponentMockingRule<PatientDataController<PhenoTipsDate>> mocker =
        new MockitoComponentMockingRule<PatientDataController<PhenoTipsDate>>(DatesController.class);

    private DocumentAccessBridge documentAccessBridge;

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

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);

        DocumentReference patientDocument = new DocumentReference("wiki", "patient", "00000001");
        doReturn(patientDocument).when(this.patient).getDocumentReference();
        doReturn(this.doc).when(this.documentAccessBridge).getDocument(patientDocument);
        doReturn(this.data).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        doReturn(this.xWikiContext).when(this.executionContext).getProperty("xwikicontext");
        doReturn(this.xWiki).when(this.xWikiContext).getWiki();
    }

    @Test
    public void loadCatchesExceptionWhenPatientDoesNotHavePatientClass() throws ComponentLookupException
    {
        doReturn(null).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        PatientData<PhenoTipsDate> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertNull(result);
    }

    @Test
    public void loadDoesNotReturnNullDates() throws ComponentLookupException
    {
        doReturn(null).when(this.data).getDateValue(anyString());
        doReturn(null).when(this.data).getStringValue(anyString());

        PatientData<PhenoTipsDate> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertEquals(0, result.size());
    }

    @Test
    public void loadReturnsExpectedDates() throws ComponentLookupException
    {
        Date birthDate = new Date(0);
        Date deathDate = new Date(999999999);
        Date examDate = new Date(10000);
        doReturn(birthDate).when(this.data).getDateValue(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME);
        doReturn(deathDate).when(this.data).getDateValue(DatesController.PATIENT_DATEOFDEATH_FIELDNAME);
        doReturn(examDate).when(this.data).getDateValue(DatesController.PATIENT_EXAMDATE_FIELDNAME);

        PatientData<PhenoTipsDate> result = this.mocker.getComponentUnderTest().load(this.patient);

        // Note: deathDate and examDate are dates with non-0 hours/minutes/seconds. However time is always
        // assumed to be 00:00:00 since we do not store time.
        // So need to construct the dates which are the 00:00:00 of the same day and compare with that
        Assert.assertEquals(getDateWithZeroTime(birthDate),
            result.get(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME).toEarliestPossibleISODate());
        Assert.assertEquals(getDateWithZeroTime(deathDate),
            result.get(DatesController.PATIENT_DATEOFDEATH_FIELDNAME).toEarliestPossibleISODate());
        Assert.assertEquals(getDateWithZeroTime(examDate),
            result.get(DatesController.PATIENT_EXAMDATE_FIELDNAME).toEarliestPossibleISODate());
    }

    @Test
    public void loadPrioritizesDateAsEntered() throws ComponentLookupException
    {
        Date wrongDate = new Date(1);
        doReturn(wrongDate).when(this.data).getDateValue(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME);
        doReturn(wrongDate).when(this.data).getDateValue(DatesController.PATIENT_DATEOFDEATH_FIELDNAME);

        PhenoTipsDate deathDate = new PhenoTipsDate(new JSONObject("{'year': 1990, 'month': 11, 'day': 2}"));
        PhenoTipsDate birthDate = new PhenoTipsDate(new JSONObject("{'year': 2000, 'range': {'years': 10}}"));

        String deathAsEnteredField =
            DatesController.CORRESPONDING_ASENTERED_FIELDNAMES.get(DatesController.PATIENT_DATEOFDEATH_FIELDNAME);
        doReturn(deathDate.toString()).when(this.data).getStringValue(deathAsEnteredField);
        String birthAsEnteredField =
            DatesController.CORRESPONDING_ASENTERED_FIELDNAMES.get(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME);
        doReturn(birthDate.toString()).when(this.data).getStringValue(birthAsEnteredField);

        PatientData<PhenoTipsDate> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertEquals(birthDate.toString(),
            result.get(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME).toString());
        Assert.assertEquals(deathDate.toString(),
            result.get(DatesController.PATIENT_DATEOFDEATH_FIELDNAME).toString());
    }

    @Test
    public void loadWorksWithDeprecatedDateFormats() throws ComponentLookupException
    {
        // test that if "date as entered" is in the deprecated format it is properly read and parsed
        // into an equivalent date in the current format

        PhenoTipsDate deathDateDeprecated = new PhenoTipsDate(new JSONObject("{'decade': '1990s'}"));
        PhenoTipsDate deathDate = new PhenoTipsDate(new JSONObject("{'year': 1990, 'range': {'years': 10}}"));

        PhenoTipsDate birthDateDeprecated = new PhenoTipsDate(
            new JSONObject("{'decade': '1990s', 'year': '1990', 'month': '10'}"));
        PhenoTipsDate birthDate = new PhenoTipsDate(new JSONObject("{'year': 1990, 'month': 10}"));

        String deathAsEnteredField =
            DatesController.CORRESPONDING_ASENTERED_FIELDNAMES.get(DatesController.PATIENT_DATEOFDEATH_FIELDNAME);
        doReturn(deathDateDeprecated.toString()).when(this.data).getStringValue(deathAsEnteredField);
        String birthAsEnteredField =
            DatesController.CORRESPONDING_ASENTERED_FIELDNAMES.get(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME);
        doReturn(birthDateDeprecated.toString()).when(this.data).getStringValue(birthAsEnteredField);

        PatientData<PhenoTipsDate> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertEquals(birthDate.toString(),
            result.get(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME).toString());
        Assert.assertEquals(deathDate.toString(),
            result.get(DatesController.PATIENT_DATEOFDEATH_FIELDNAME).toString());
    }

    // Returns a Date which has the same year-month-day as the input date, but with time equal to 00:00:00
    private Date getDateWithZeroTime(Date date)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    @Test(expected = IllegalArgumentException.class)
    public void saveDoesNotContinueIfDateDataIsNotNamed() throws ComponentLookupException
    {
        doReturn(this.dateData).when(this.patient).getData(DATA_NAME);
        doReturn(false).when(this.dateData).isNamed();

        this.mocker.getComponentUnderTest().save(this.patient, this.doc);

        verify(this.data, never()).setDateValue(anyString(), any(Date.class));
    }

    @Test
    public void saveCorrectlyHandlesNullData() throws ComponentLookupException
    {
        doReturn(this.dateData).when(this.patient).getData(DATA_NAME);
        doReturn(true).when(this.dateData).isNamed();
        doReturn(true).when(this.dateData).containsKey(anyString());
        doReturn(null).when(this.dateData).get(anyString());

        this.mocker.getComponentUnderTest().save(this.patient, this.doc);

        String deathAsEnteredField =
            DatesController.CORRESPONDING_ASENTERED_FIELDNAMES.get(DatesController.PATIENT_DATEOFDEATH_FIELDNAME);
        verify(this.data).setStringValue(deathAsEnteredField, "");
        verify(this.data).setDateValue(DatesController.PATIENT_DATEOFDEATH_FIELDNAME, null);

        verify(this.data).setDateValue(DatesController.PATIENT_EXAMDATE_FIELDNAME, null);
    }

    @Test
    public void saveCorrectlyHandlesNoData() throws ComponentLookupException
    {
        doReturn(this.dateData).when(this.patient).getData(DATA_NAME);
        doReturn(true).when(this.dateData).isNamed();
        doReturn(false).when(this.dateData).containsKey(anyString());

        this.mocker.getComponentUnderTest().save(this.patient, this.doc);

        verifyZeroInteractions(this.data);
    }

    @Test
    public void saveAddsAllDates() throws XWikiException, ComponentLookupException
    {
        Map<String, PhenoTipsDate> datesMap = new LinkedHashMap<>();
        PhenoTipsDate birthDate = new PhenoTipsDate("1990s-11-12");
        PhenoTipsDate deathDate = new PhenoTipsDate(new Date(999999999));
        PhenoTipsDate examDate = new PhenoTipsDate(new JSONObject("{'year': 1990, 'range': {'years': 10}}"));

        datesMap.put(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME, birthDate);
        datesMap.put(DatesController.PATIENT_DATEOFDEATH_FIELDNAME, deathDate);
        datesMap.put(DatesController.PATIENT_EXAMDATE_FIELDNAME, examDate);

        PatientData<PhenoTipsDate> datesData = new DictionaryPatientData<>(DATA_NAME, datesMap);
        doReturn(datesData).when(this.patient).getData(DATA_NAME);

        this.mocker.getComponentUnderTest().save(this.patient, this.doc);

        // birth date and death date have "date_as_entered" companion fields, both field+companion should be set
        String deathAsEnteredField =
            DatesController.CORRESPONDING_ASENTERED_FIELDNAMES.get(DatesController.PATIENT_DATEOFDEATH_FIELDNAME);
        verify(this.data).setStringValue(deathAsEnteredField, deathDate.toString());
        verify(this.data).setDateValue(DatesController.PATIENT_DATEOFDEATH_FIELDNAME,
            deathDate.toEarliestPossibleISODate());

        String birthAsEnteredField =
            DatesController.CORRESPONDING_ASENTERED_FIELDNAMES.get(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME);
        verify(this.data).setStringValue(birthAsEnteredField, birthDate.toString());
        verify(this.data).setDateValue(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME,
            birthDate.toEarliestPossibleISODate());

        // exam date does not have a "date_as_entered" companion
        verify(this.data).setDateValue(DatesController.PATIENT_EXAMDATE_FIELDNAME,
            examDate.toEarliestPossibleISODate());
    }

    @Test
    public void writeJSONDoesNotPlaceDataWhenGetDataReturnsNull() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        Assert.assertEquals(json.has(DatesController.PATIENT_DATEOFDEATH_FIELDNAME), false);
        Assert.assertEquals(json.has(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME), false);
        Assert.assertEquals(json.has(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME), false);
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsEmptyDataWhenGetDataReturnsNull() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new ArrayList<>();
        selectedFields.add(DatesController.CONTROLLING_FIELDNAMES.get(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME));
        selectedFields.add(DatesController.CONTROLLING_FIELDNAMES.get(DatesController.PATIENT_EXAMDATE_FIELDNAME));

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertEquals(json.has(DatesController.PATIENT_DATEOFDEATH_FIELDNAME), false);
        Assert.assertEquals(json.getString(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME), "");
        Assert.assertEquals(json.getString(DatesController.PATIENT_EXAMDATE_FIELDNAME), "");
    }

    @Test
    public void writeJSONAddsAllDates() throws ComponentLookupException
    {
        Map<String, PhenoTipsDate> datesMap = new LinkedHashMap<>();
        PhenoTipsDate birthDate = new PhenoTipsDate("1990s-11-12");
        PhenoTipsDate deathDate = new PhenoTipsDate(new Date(999999999));
        PhenoTipsDate examDate = new PhenoTipsDate(
            new JSONObject("{'year': 1990, 'month': 11, 'day': 2, 'range': {'years': 10}}"));
        datesMap.put(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME, birthDate);
        datesMap.put(DatesController.PATIENT_DATEOFDEATH_FIELDNAME, deathDate);
        datesMap.put(DatesController.PATIENT_EXAMDATE_FIELDNAME, examDate);
        PatientData<PhenoTipsDate> datesData = new DictionaryPatientData<>(DATA_NAME, datesMap);
        doReturn(datesData).when(this.patient).getData(DATA_NAME);

        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        Assert.assertEquals(birthDate.toJSON().toString(),
            json.getJSONObject(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME).toString());
        Assert.assertEquals(deathDate.toJSON().toString(),
            json.getJSONObject(DatesController.PATIENT_DATEOFDEATH_FIELDNAME).toString());
        Assert.assertEquals(examDate.toJSON().toString(),
            json.getJSONObject(DatesController.PATIENT_EXAMDATE_FIELDNAME).toString());
    }

    @Test
    public void writeJSONSkipsEmptyFields() throws ComponentLookupException
    {
        Map<String, PhenoTipsDate> datesMap = new LinkedHashMap<>();
        PhenoTipsDate birthDate = new PhenoTipsDate("1990s-11-12");
        PhenoTipsDate examDate = new PhenoTipsDate("1980-11");
        datesMap.put(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME, birthDate);
        datesMap.put(DatesController.PATIENT_EXAMDATE_FIELDNAME, examDate);
        PatientData<PhenoTipsDate> datesData = new DictionaryPatientData<>(DATA_NAME, datesMap);
        doReturn(datesData).when(this.patient).getData(DATA_NAME);

        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);

        // date of death should be skipped
        Assert.assertEquals(false, json.has(DatesController.PATIENT_DATEOFDEATH_FIELDNAME));
        // other fields should be there
        Assert.assertEquals(birthDate.toJSON().toString(),
            json.getJSONObject(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME).toString());
        Assert.assertEquals(examDate.toJSON().toString(),
            json.getJSONObject(DatesController.PATIENT_EXAMDATE_FIELDNAME).toString());
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsAllSelectedDates() throws ComponentLookupException
    {
        Map<String, PhenoTipsDate> datesMap = new LinkedHashMap<>();
        PhenoTipsDate birthDate = new PhenoTipsDate("1990s-11-12");
        PhenoTipsDate deathDate = new PhenoTipsDate(new Date(999999999));
        PhenoTipsDate examDate = new PhenoTipsDate(new JSONObject("{'year': 1990, 'month': 11 }"));
        datesMap.put(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME, birthDate);
        datesMap.put(DatesController.PATIENT_DATEOFDEATH_FIELDNAME, deathDate);
        datesMap.put(DatesController.PATIENT_EXAMDATE_FIELDNAME, examDate);
        PatientData<PhenoTipsDate> datesData = new DictionaryPatientData<>(DATA_NAME, datesMap);
        doReturn(datesData).when(this.patient).getData(DATA_NAME);

        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new ArrayList<>();
        selectedFields.add(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME);
        selectedFields.add(DatesController.PATIENT_EXAMDATE_FIELDNAME);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertEquals(birthDate.toJSON().toString(),
            json.getJSONObject(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME).toString());
        Assert.assertFalse(json.has(DatesController.PATIENT_DATEOFDEATH_FIELDNAME));
        Assert.assertEquals(examDate.toJSON().toString(),
            json.getJSONObject(DatesController.PATIENT_EXAMDATE_FIELDNAME).toString());
    }

    @Test
    public void readJSONReturnsNullWhenJSONIsEmpty() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();
        Assert.assertNull(this.mocker.getComponentUnderTest().readJSON(json));
    }

    @Test
    public void readJSONSkipsMissingDataAndHandlesMisformattedInput() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();
        PhenoTipsDate examDate = new PhenoTipsDate(new JSONObject("{'year': 800, 'range': {'years': 100}}"));

        json.put(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME, "!!!!!!!!!!!!!!!!!!!!");
        json.put(DatesController.PATIENT_EXAMDATE_FIELDNAME, examDate.toJSON());

        PatientData<PhenoTipsDate> result = this.mocker.getComponentUnderTest().readJSON(json);

        Assert.assertEquals("{}", result.get(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME).toString());
        Assert.assertNull(result.get(DatesController.PATIENT_DATEOFDEATH_FIELDNAME));
        Assert.assertEquals(examDate.toString(), result.get(DatesController.PATIENT_EXAMDATE_FIELDNAME).toString());
    }

    @Test
    public void readJSONReturnsAllDates() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();
        PhenoTipsDate birthDate = new PhenoTipsDate("1990-11-12");
        PhenoTipsDate deathDate = new PhenoTipsDate(new Date(999999999));
        PhenoTipsDate examDate = new PhenoTipsDate(new JSONObject("{'year': 1990, 'month': 11, day: 6}"));

        json.put(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME, birthDate.toJSON());
        json.put(DatesController.PATIENT_DATEOFDEATH_FIELDNAME, deathDate.toJSON());
        json.put(DatesController.PATIENT_EXAMDATE_FIELDNAME, examDate.toJSON());

        PatientData<PhenoTipsDate> result = this.mocker.getComponentUnderTest().readJSON(json);

        Assert.assertEquals(birthDate.toString(), result.get(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME).toString());
        Assert.assertEquals(deathDate.toString(), result.get(DatesController.PATIENT_DATEOFDEATH_FIELDNAME).toString());
        Assert.assertEquals(examDate.toString(), result.get(DatesController.PATIENT_EXAMDATE_FIELDNAME).toString());

        PhenoTipsDate birthDateOldStyle = new PhenoTipsDate(new JSONObject("{'decade': '1990s'}"));
        PhenoTipsDate deathDateOldStyle = new PhenoTipsDate(new JSONObject("{'decade': '1990s', 'year': '1995'}"));
        JSONObject json2 = new JSONObject();
        json2.put(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME, birthDateOldStyle.toJSON());
        json2.put(DatesController.PATIENT_DATEOFDEATH_FIELDNAME, deathDateOldStyle.toJSON());
        PatientData<PhenoTipsDate> result2 = this.mocker.getComponentUnderTest().readJSON(json2);
        Assert.assertEquals(birthDateOldStyle.toString(),
            result2.get(DatesController.PATIENT_DATEOFBIRTH_FIELDNAME).toString());
        Assert.assertEquals(deathDateOldStyle.toString(),
            result2.get(DatesController.PATIENT_DATEOFDEATH_FIELDNAME).toString());
    }

    @Test
    public void checkGetName() throws ComponentLookupException
    {
        Assert.assertEquals(DATA_NAME, this.mocker.getComponentUnderTest().getName());
    }
}
