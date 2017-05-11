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
import org.phenotips.data.SimpleValuePatientData;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

/**
 * Test for the {@link MetaDataController} Component, only the overridden methods from {@link AbstractSimpleController}
 * are tested here.
 */
public class MetaDataControllerTest
{
    private static final String UNKNOWN_USER = "Unknown user";

    private static final String DOCUMENT_NAME = "doc.name";

    private static final String DOCUMENT_NAME_STRING = "report_id";

    private static final String CREATION_DATE = "creationDate";

    private static final String AUTHOR = "author";

    private static final String AUTHOR_STRING = "last_modified_by";

    private static final String DATE = "date";

    private static final String DATE_STRING = "last_modification_date";

    private static final String CONTROLLER_NAME = "metadata";

    private static final String DATA_NAME = CONTROLLER_NAME;

    @Rule
    public MockitoComponentMockingRule<PatientDataController<String>> mocker =
        new MockitoComponentMockingRule<PatientDataController<String>>(MetaDataController.class);

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    private DocumentReference documentReference;

    private DocumentReference authorReference;

    private Date creationDate;

    private Date date;

    private DateTimeFormatter formatter;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.documentReference = new DocumentReference("wiki", "patient", "00000001");
        doReturn(this.documentReference).when(this.patient).getDocumentReference();
        doReturn(this.doc).when(this.patient).getXDocument();
        doReturn(this.documentReference).when(this.doc).getDocumentReference();

        this.authorReference = new DocumentReference("wiki", "phenotips", "author");
        doReturn(this.authorReference).when(this.doc).getAuthorReference();
        this.date = new Date(10);
        doReturn(this.date).when(this.doc).getDate();
        this.creationDate = new Date(0);
        doReturn(this.creationDate).when(this.doc).getCreationDate();

        this.formatter = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC);
    }

    @Test
    public void checkGetName() throws ComponentLookupException
    {
        Assert.assertEquals("metadata", this.mocker.getComponentUnderTest().getName());
    }

    @Test
    public void checkGetJsonPropertyName() throws ComponentLookupException
    {
        Assert.assertNull(((AbstractSimpleController) this.mocker.getComponentUnderTest()).getJsonPropertyName());
    }

    @Test
    public void checkGetProperties() throws ComponentLookupException
    {
        List<String> result = ((AbstractSimpleController) this.mocker.getComponentUnderTest()).getProperties();
        Assert.assertTrue(result.isEmpty());
    }

    // --------------------load() is Overridden from AbstractSimpleController--------------------

    @Test
    public void loadCatchesInvalidDocument() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getXDocument();

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        verify(this.mocker.getMockedLogger()).error(eq(PatientDataController.ERROR_MESSAGE_LOAD_FAILED), anyString());
        Assert.assertNull(result);
    }

    @Test
    public void loadAddsAllReferences() throws ComponentLookupException
    {
        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertEquals(4, result.size());
        Assert.assertEquals(this.documentReference.getName(), result.get(DOCUMENT_NAME));
        Assert.assertEquals(this.authorReference.getName(), result.get(AUTHOR));
        Assert.assertEquals(this.formatter.print(new DateTime(this.date)), result.get(DATE));
        Assert.assertEquals(this.formatter.print(new DateTime(this.creationDate)), result.get(CREATION_DATE));
    }

    @Test
    public void loadSetsUnknownUserWhenAuthorIsNull() throws ComponentLookupException
    {
        doReturn(null).when(this.doc).getAuthorReference();

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);

        Assert.assertEquals(UNKNOWN_USER, result.get(AUTHOR));
    }

    // --------------------writeJSON() is Overridden from AbstractSimpleController--------------------

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenGetDataReturnsNull() throws ComponentLookupException
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertEquals(0, json.length());
    }

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenDataIsNotKeyValueBased() throws ComponentLookupException
    {
        PatientData<String> patientData = new SimpleValuePatientData<>(DATA_NAME, "datum");
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertEquals(0, json.length());
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsAllValuesAndConvertedJsonKeys() throws ComponentLookupException
    {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(DOCUMENT_NAME, this.documentReference.getName());
        map.put(CREATION_DATE, this.formatter.print(new DateTime(this.creationDate)));
        map.put(AUTHOR, this.authorReference.getName());
        map.put(DATE, this.formatter.print(new DateTime(this.date)));
        PatientData<String> patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(DOCUMENT_NAME);
        selectedFields.add(CREATION_DATE);
        selectedFields.add(AUTHOR);
        selectedFields.add(DATE);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertEquals(this.documentReference.getName(), json.get(DOCUMENT_NAME_STRING));
        Assert.assertEquals(this.authorReference.getName(), json.get(AUTHOR_STRING));
        Assert.assertEquals(this.formatter.print(new DateTime(this.creationDate)), json.get(DATE));
        Assert.assertEquals(this.formatter.print(new DateTime(this.date)), json.get(DATE_STRING));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsSelectedValues() throws ComponentLookupException
    {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(DOCUMENT_NAME, this.documentReference.getName());
        map.put(CREATION_DATE, this.formatter.print(new DateTime(this.creationDate)));
        map.put(AUTHOR, this.authorReference.getName());
        map.put(DATE, this.formatter.print(new DateTime(this.date)));
        PatientData<String> patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(DOCUMENT_NAME);
        selectedFields.add(AUTHOR);

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, selectedFields);

        Assert.assertEquals(this.documentReference.getName(), json.get(DOCUMENT_NAME_STRING));
        Assert.assertEquals(this.authorReference.getName(), json.get(AUTHOR_STRING));
        Assert.assertFalse(json.has(DATE));
        Assert.assertFalse(json.has(DATE_STRING));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsAllValuesWhenSelectedFieldsNull()
        throws ComponentLookupException
    {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(DOCUMENT_NAME, this.documentReference.getName());
        map.put(CREATION_DATE, this.formatter.print(new DateTime(this.creationDate)));
        map.put(AUTHOR, this.authorReference.getName());
        map.put(DATE, this.formatter.print(new DateTime(this.date)));
        PatientData<String> patientData = new DictionaryPatientData<>(DATA_NAME, map);
        doReturn(patientData).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, null);

        Assert.assertEquals(this.documentReference.getName(), json.get(DOCUMENT_NAME_STRING));
        Assert.assertEquals(this.authorReference.getName(), json.get(AUTHOR_STRING));
        Assert.assertEquals(this.formatter.print(new DateTime(this.creationDate)), json.get(DATE));
        Assert.assertEquals(this.formatter.print(new DateTime(this.date)), json.get(DATE_STRING));
    }
}
