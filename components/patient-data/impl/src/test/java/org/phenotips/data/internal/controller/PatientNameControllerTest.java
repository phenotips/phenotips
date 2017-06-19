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
import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.PatientWritePolicy;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Test for the {@link PatientNameController} Component, only the overridden methods from
 * {@link AbstractSimpleController} are tested here.
 */
public class PatientNameControllerTest
{
    private static final String FIRST_NAME = "first_name";

    private static final String LAST_NAME = "last_name";

    @Rule
    public MockitoComponentMockingRule<PatientDataController<String>> mocker =
        new MockitoComponentMockingRule<>(PatientNameController.class);

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject dataHolder;

    private PatientDataController<String> component;

    @Before
    public void setUp() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);

        this.component = this.mocker.getComponentUnderTest();

        final DocumentReference patientDocRef = new DocumentReference("wiki", "patient", "00000001");
        when(this.patient.getDocumentReference()).thenReturn(patientDocRef);
        when(this.patient.getXDocument()).thenReturn(this.doc);
        when(this.doc.getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any())).thenReturn(this.dataHolder);
    }

    @Test
    public void checkGetName()
    {
        Assert.assertEquals("patientName", this.component.getName());
    }

    @Test
    public void checkGetJsonPropertyName()
    {
        Assert.assertEquals("patient_name",
            ((AbstractSimpleController) this.component).getJsonPropertyName());
    }

    @Test
    public void checkGetProperties()
    {
        List<String> result = ((AbstractSimpleController) this.component).getProperties();

        Assert.assertEquals(2, result.size());
        Assert.assertThat(result, Matchers.hasItem(FIRST_NAME));
        Assert.assertThat(result, Matchers.hasItem(LAST_NAME));
    }

    @Test
    public void saveThrowsExceptionWhenPatientHasNoPatientClass()
    {
        when(this.doc.getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any())).thenReturn(null);
        this.component.save(this.patient);
        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveDoesNothingWhenPatientDataIsNullAndPolicyIsUpdate()
    {
        when(this.patient.getData(this.component.getName())).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.UPDATE);

        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());
        verifyNoMoreInteractions(this.doc);

        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveDoesNothingWhenPatientDataIsNullAndPolicyIsMerge()
    {
        when(this.patient.getData(this.component.getName())).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.MERGE);

        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());
        verifyNoMoreInteractions(this.doc);

        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveRemovesExistingDataWhenPatientDataIsNullAndPolicyIsReplace()
    {
        when(this.patient.getData(this.component.getName())).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.REPLACE);

        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());
        verifyNoMoreInteractions(this.doc);

        verify(this.dataHolder, times(1)).setStringValue(FIRST_NAME, null);
        verify(this.dataHolder, times(1)).setStringValue(LAST_NAME, null);
        verifyNoMoreInteractions(this.dataHolder);
    }

    @Test
    public void saveDoesNothingWhenDataHasWrongFormat()
    {
        final PatientData<List<String>> data = spy(new IndexedPatientData<>(this.component.getName(),
            Collections.emptyList()));

        doReturn(data).when(this.patient).getData(this.component.getName());

        this.component.save(this.patient, PatientWritePolicy.UPDATE);
        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveUpdatesOnlySpecifiedPropertiesWhenPolicyIsUpdate()
    {
        final Map<String, String> result = new LinkedHashMap<>();
        result.put(FIRST_NAME, FIRST_NAME);
        final PatientData<String> data = spy(new DictionaryPatientData<>(this.component.getName(), result));

        doReturn(data).when(this.patient).getData(this.component.getName());
        when(data.isNamed()).thenReturn(true);

        this.component.save(this.patient, PatientWritePolicy.UPDATE);

        verify(data, times(1)).isNamed();
        verify(data, times(1)).containsKey(FIRST_NAME);
        verify(data, times(1)).containsKey(LAST_NAME);
        verify(data, times(1)).get(FIRST_NAME);
        verify(data, never()).get(LAST_NAME);
        verifyNoMoreInteractions(data);

        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());
        verifyNoMoreInteractions(this.doc);

        verify(this.dataHolder, times(1)).setStringValue(FIRST_NAME, FIRST_NAME);
        verifyNoMoreInteractions(this.dataHolder);
    }

    @Test
    public void saveUpdatesOnlySpecifiedPropertiesWhenPolicyIsMerge()
    {
        final Map<String, String> result = new LinkedHashMap<>();
        result.put(FIRST_NAME, FIRST_NAME);
        final PatientData<String> data = spy(new DictionaryPatientData<>(this.component.getName(), result));

        doReturn(data).when(this.patient).getData(this.component.getName());
        when(data.isNamed()).thenReturn(true);

        this.component.save(this.patient, PatientWritePolicy.MERGE);

        verify(data, times(1)).isNamed();
        verify(data, times(1)).containsKey(FIRST_NAME);
        verify(data, times(1)).containsKey(LAST_NAME);
        verify(data, times(1)).get(FIRST_NAME);
        verify(data, never()).get(LAST_NAME);
        verifyNoMoreInteractions(data);

        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());
        verifyNoMoreInteractions(this.doc);

        verify(this.dataHolder, times(1)).setStringValue(FIRST_NAME, FIRST_NAME);
        verifyNoMoreInteractions(this.dataHolder);
    }

    @Test
    public void saveUpdatesOnlySpecifiedPropertiesAndNullsTheRestWhenPolicyIsReplace()
    {
        final Map<String, String> result = new LinkedHashMap<>();
        result.put(FIRST_NAME, FIRST_NAME);
        final PatientData<String> data = spy(new DictionaryPatientData<>(this.component.getName(), result));

        doReturn(data).when(this.patient).getData(this.component.getName());
        when(data.isNamed()).thenReturn(true);

        this.component.save(this.patient, PatientWritePolicy.REPLACE);

        verify(data, times(1)).isNamed();
        verify(data, times(1)).get(FIRST_NAME);
        verify(data, times(1)).get(LAST_NAME);
        verifyNoMoreInteractions(data);

        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());
        verifyNoMoreInteractions(this.doc);

        verify(this.dataHolder, times(1)).setStringValue(FIRST_NAME, FIRST_NAME);
        verify(this.dataHolder, times(1)).setStringValue(LAST_NAME, null);
        verifyNoMoreInteractions(this.dataHolder);
    }
}
