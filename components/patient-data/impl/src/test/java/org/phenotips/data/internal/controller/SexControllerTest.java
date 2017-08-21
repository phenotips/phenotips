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

import org.xwiki.context.ExecutionContext;
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
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Test for the {@link SexController} component, implementation of the {@link org.phenotips.data.PatientDataController}
 * interface.
 */
public class SexControllerTest
{
    private static final String DATA_NAME = "sex";

    private static final String INTERNAL_PROPERTY_NAME = "gender";

    private static final String SEX_MALE = "M";

    private static final String SEX_FEMALE = "F";

    private static final String SEX_OTHER = "O";

    private static final String SEX_UNKNOWN = "U";

    @Rule
    public MockitoComponentMockingRule<PatientDataController<String>> mocker =
        new MockitoComponentMockingRule<>(SexController.class);

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private XWiki xwiki;

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject data;

    private PatientDataController<String> component;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.component = this.mocker.getComponentUnderTest();
        DocumentReference patientDocRef = new DocumentReference("wiki", "patient", "00000001");
        doReturn(patientDocRef).when(this.patient).getDocumentReference();
        doReturn(this.doc).when(this.patient).getXDocument();
        doReturn(this.data).when(this.doc).getXObject(Patient.CLASS_REFERENCE);
        doReturn(this.data).when(this.doc).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());

        doReturn(this.xcontext).when(this.executionContext).getProperty("xwikicontext");
        doReturn(this.xwiki).when(this.xcontext).getWiki();
    }

    @Test
    public void loadCatchesInvalidDocument()
    {
        doReturn(null).when(this.patient).getXDocument();

        PatientData<String> result = this.component.load(this.patient);

        verify(this.mocker.getMockedLogger()).error(eq(PatientDataController.ERROR_MESSAGE_LOAD_FAILED), anyString());
        Assert.assertNull(result);
    }

    @Test
    public void loadCatchesExceptionWhenPatientDoesNotHavePatientClass()
    {
        doReturn(null).when(this.doc).getXObject(Patient.CLASS_REFERENCE);

        PatientData<String> result = this.component.load(this.patient);

        Assert.assertNull(result);
    }

    @Test
    public void loadReturnsCorrectSex()
    {
        doReturn(SEX_MALE).when(this.data).getStringValue(INTERNAL_PROPERTY_NAME);
        PatientData<String> result = this.component.load(this.patient);
        Assert.assertEquals(SEX_MALE, result.getValue());

        doReturn(SEX_FEMALE).when(this.data).getStringValue(INTERNAL_PROPERTY_NAME);
        result = this.component.load(this.patient);
        Assert.assertEquals(SEX_FEMALE, result.getValue());

        doReturn(SEX_OTHER).when(this.data).getStringValue(INTERNAL_PROPERTY_NAME);
        result = this.component.load(this.patient);
        Assert.assertEquals(SEX_OTHER, result.getValue());
    }

    @Test
    public void loadReturnsSexUnknown()
    {
        doReturn(SEX_UNKNOWN).when(this.data).getStringValue(INTERNAL_PROPERTY_NAME);
        PatientData<String> result = this.component.load(this.patient);
        Assert.assertEquals(SEX_UNKNOWN, result.getValue());

        doReturn("!!!!!").when(this.data).getStringValue(INTERNAL_PROPERTY_NAME);
        result = this.component.load(this.patient);
        Assert.assertEquals(SEX_UNKNOWN, result.getValue());

        doReturn(null).when(this.data).getStringValue(INTERNAL_PROPERTY_NAME);
        result = this.component.load(this.patient);
        Assert.assertEquals(SEX_UNKNOWN, result.getValue());
    }

    @Test
    public void saveThrowsExceptionIfPatientHasNoPatientClass()
    {
        when(this.doc.getXObject(Patient.CLASS_REFERENCE, true, this.xcontext)).thenReturn(null);
        this.component.save(this.patient);
        verifyZeroInteractions(this.data);
    }

    @Test
    public void saveDoesNothingIfPatientDataIsNullAndPolicyIsUpdate()
    {
        when(this.patient.getData(this.component.getName())).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.UPDATE);

        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());
        verifyNoMoreInteractions(this.doc);
        verifyZeroInteractions(this.data);
    }

    @Test
    public void saveDoesNothingIfPatientDataIsNullAndPolicyIsMerge()
    {
        when(this.patient.getData(this.component.getName())).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.MERGE);

        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());
        verifyNoMoreInteractions(this.doc);
        verifyZeroInteractions(this.data);
    }

    @Test
    public void saveSetsGenderToUnknownIfPatientDataIsNullAndPolicyIsReplace()
    {
        when(this.patient.getData(this.component.getName())).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.REPLACE);

        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());
        verifyNoMoreInteractions(this.doc);

        verify(this.data, times(1)).setStringValue(INTERNAL_PROPERTY_NAME, SEX_UNKNOWN);
        verifyNoMoreInteractions(this.data);
    }

    @Test
    public void saveSetsCorrectSexIfPolicyIsUpdate()
    {
        doReturn(new SimpleValuePatientData<>(DATA_NAME, SEX_MALE)).when(this.patient).getData(DATA_NAME);
        this.component.save(this.patient);
        verify(this.data).setStringValue(INTERNAL_PROPERTY_NAME, SEX_MALE);

        Mockito.reset(this.xwiki);
        doReturn(new SimpleValuePatientData<>(DATA_NAME, SEX_FEMALE)).when(this.patient).getData(DATA_NAME);
        this.component.save(this.patient);
        verify(this.data).setStringValue(INTERNAL_PROPERTY_NAME, SEX_FEMALE);

        Mockito.reset(this.xwiki);
        doReturn(new SimpleValuePatientData<>(DATA_NAME, SEX_OTHER)).when(this.patient).getData(DATA_NAME);
        this.component.save(this.patient);
        verify(this.data).setStringValue(INTERNAL_PROPERTY_NAME, SEX_OTHER);

        Mockito.reset(this.xwiki);
        doReturn(new SimpleValuePatientData<>(DATA_NAME, SEX_UNKNOWN)).when(this.patient).getData(DATA_NAME);
        this.component.save(this.patient);
        verify(this.data).setStringValue(INTERNAL_PROPERTY_NAME, SEX_UNKNOWN);

        Mockito.reset(this.xwiki);
        doReturn(new SimpleValuePatientData<String>(DATA_NAME, null)).when(this.patient).getData(DATA_NAME);
        this.component.save(this.patient);
        verify(this.data).setStringValue(INTERNAL_PROPERTY_NAME, null);
    }

    @Test
    public void saveSetsCorrectSexIfPolicyIsMerge()
    {
        doReturn(new SimpleValuePatientData<>(DATA_NAME, SEX_MALE)).when(this.patient).getData(DATA_NAME);
        this.component.save(this.patient, PatientWritePolicy.MERGE);
        verify(this.data).setStringValue(INTERNAL_PROPERTY_NAME, SEX_MALE);

        Mockito.reset(this.xwiki);
        doReturn(new SimpleValuePatientData<>(DATA_NAME, SEX_FEMALE)).when(this.patient).getData(DATA_NAME);
        this.component.save(this.patient, PatientWritePolicy.MERGE);
        verify(this.data).setStringValue(INTERNAL_PROPERTY_NAME, SEX_FEMALE);

        Mockito.reset(this.xwiki);
        doReturn(new SimpleValuePatientData<>(DATA_NAME, SEX_OTHER)).when(this.patient).getData(DATA_NAME);
        this.component.save(this.patient, PatientWritePolicy.MERGE);
        verify(this.data).setStringValue(INTERNAL_PROPERTY_NAME, SEX_OTHER);

        Mockito.reset(this.xwiki);
        doReturn(new SimpleValuePatientData<>(DATA_NAME, SEX_UNKNOWN)).when(this.patient).getData(DATA_NAME);
        this.component.save(this.patient, PatientWritePolicy.MERGE);
        verify(this.data).setStringValue(INTERNAL_PROPERTY_NAME, SEX_UNKNOWN);

        Mockito.reset(this.xwiki);
        doReturn(new SimpleValuePatientData<String>(DATA_NAME, null)).when(this.patient).getData(DATA_NAME);
        this.component.save(this.patient, PatientWritePolicy.MERGE);
        verify(this.data).setStringValue(INTERNAL_PROPERTY_NAME, null);
    }

    @Test
    public void saveSetsCorrectSexIfPolicyIsReplace()
    {
        doReturn(new SimpleValuePatientData<>(DATA_NAME, SEX_MALE)).when(this.patient).getData(DATA_NAME);
        this.component.save(this.patient, PatientWritePolicy.REPLACE);
        verify(this.data).setStringValue(INTERNAL_PROPERTY_NAME, SEX_MALE);

        Mockito.reset(this.xwiki);
        doReturn(new SimpleValuePatientData<>(DATA_NAME, SEX_FEMALE)).when(this.patient).getData(DATA_NAME);
        this.component.save(this.patient, PatientWritePolicy.REPLACE);
        verify(this.data).setStringValue(INTERNAL_PROPERTY_NAME, SEX_FEMALE);

        Mockito.reset(this.xwiki);
        doReturn(new SimpleValuePatientData<>(DATA_NAME, SEX_OTHER)).when(this.patient).getData(DATA_NAME);
        this.component.save(this.patient, PatientWritePolicy.REPLACE);
        verify(this.data).setStringValue(INTERNAL_PROPERTY_NAME, SEX_OTHER);

        Mockito.reset(this.xwiki);
        doReturn(new SimpleValuePatientData<>(DATA_NAME, SEX_UNKNOWN)).when(this.patient).getData(DATA_NAME);
        this.component.save(this.patient, PatientWritePolicy.REPLACE);
        verify(this.data).setStringValue(INTERNAL_PROPERTY_NAME, SEX_UNKNOWN);

        Mockito.reset(this.xwiki);
        doReturn(new SimpleValuePatientData<String>(DATA_NAME, null)).when(this.patient).getData(DATA_NAME);
        this.component.save(this.patient, PatientWritePolicy.REPLACE);
        verify(this.data).setStringValue(INTERNAL_PROPERTY_NAME, null);
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
    public void writeJSONWithSelectedFieldsReturnsWhenGetDataReturnsNull()
    {
        doReturn(null).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();

        this.component.writeJSON(this.patient, json, selectedFields);

        Assert.assertFalse(json.has(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsReturnsWhenGenderNotSelected()
    {
        doReturn(new SimpleValuePatientData<>(DATA_NAME, SEX_MALE)).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add("dates");

        this.component.writeJSON(this.patient, json, selectedFields);

        Assert.assertFalse(json.has(DATA_NAME));
    }

    @Test
    public void writeJSONAddsSex()
    {
        doReturn(new SimpleValuePatientData<>(DATA_NAME, SEX_FEMALE)).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();

        this.component.writeJSON(this.patient, json);

        Assert.assertEquals(SEX_FEMALE, json.get(DATA_NAME));
    }

    @Test
    public void writeJSONWithSelectedFieldsAddsSex()
    {
        doReturn(new SimpleValuePatientData<>(DATA_NAME, SEX_FEMALE)).when(this.patient).getData(DATA_NAME);
        JSONObject json = new JSONObject();
        Collection<String> selectedFields = new LinkedList<>();
        selectedFields.add(INTERNAL_PROPERTY_NAME);

        this.component.writeJSON(this.patient, json, selectedFields);

        Assert.assertEquals(SEX_FEMALE, json.get(DATA_NAME));
    }

    @Test
    public void readJSONEmptyJsonReturnsNull()
    {
        Assert.assertNull(this.component.readJSON(new JSONObject()));
    }

    @Test
    public void readJSONReturnsCorrectSex()
    {
        JSONObject json = new JSONObject();
        json.put(DATA_NAME, SEX_MALE);
        PatientData<String> result = this.component.readJSON(json);
        Assert.assertEquals(SEX_MALE, result.getValue());

        json = new JSONObject();
        json.put(DATA_NAME, SEX_FEMALE);
        result = this.component.readJSON(json);
        Assert.assertEquals(SEX_FEMALE, result.getValue());

        json = new JSONObject();
        json.put(DATA_NAME, SEX_OTHER);
        result = this.component.readJSON(json);
        Assert.assertEquals(SEX_OTHER, result.getValue());

        json = new JSONObject();
        json.put(DATA_NAME, SEX_UNKNOWN);
        result = this.component.readJSON(json);
        Assert.assertEquals(SEX_UNKNOWN, result.getValue());
    }

    @Test
    public void checkGetName()
    {
        Assert.assertEquals(DATA_NAME, this.component.getName());
    }
}
