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
import org.phenotips.data.PatientWritePolicy;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Test for the {@link FamilyHistoryController} Component, only the overridden methods from
 * {@link AbstractComplexController} are tested here.
 */
public class FamilyHistoryControllerTest
{
    private static final String CONSANGUINITY = "consanguinity";

    private static final String MISCARRIAGES = "miscarriages";

    private static final String AFFECTED_RELATIVES = "affectedRelatives";

    private static final String FAMILY_HISTORY = "familyHistory";

    @Rule
    public MockitoComponentMockingRule<PatientDataController<Integer>> mocker =
        new MockitoComponentMockingRule<>(FamilyHistoryController.class);

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject dataHolder;

    @Mock
    private XWikiContext xcontext;

    private PatientDataController<Integer> component;

    @Before
    public void setUp() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);

        this.component = this.mocker.getComponentUnderTest();

        final Provider<XWikiContext> xcp = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcp.get()).thenReturn(this.xcontext);

        final DocumentReference patientDocRef = new DocumentReference("wiki", "patient", "00000001");
        when(this.patient.getDocumentReference()).thenReturn(patientDocRef);
        when(this.patient.getXDocument()).thenReturn(this.doc);
        when(this.doc.getXObject(Patient.CLASS_REFERENCE)).thenReturn(this.dataHolder);
        when(this.doc.getXObject(Patient.CLASS_REFERENCE, true, this.xcontext)).thenReturn(this.dataHolder);
    }

    @Test
    public void checkGetName() throws ComponentLookupException
    {
        Assert.assertEquals("familyHistory", this.mocker.getComponentUnderTest().getName());
    }

    @Test
    public void checkGetJsonPropertyName() throws ComponentLookupException
    {
        Assert.assertEquals("family_history",
            ((AbstractComplexController<Integer>) this.mocker.getComponentUnderTest()).getJsonPropertyName());
    }

    @Test
    public void checkGetProperties() throws ComponentLookupException
    {
        List<String> result =
            ((AbstractComplexController<Integer>) this.mocker.getComponentUnderTest()).getProperties();

        Assert.assertEquals(3, result.size());
        Assert.assertThat(result, Matchers.hasItem(CONSANGUINITY));
        Assert.assertThat(result, Matchers.hasItem(MISCARRIAGES));
        Assert.assertThat(result, Matchers.hasItem(AFFECTED_RELATIVES));
    }

    @Test
    public void checkGetBooleanFields() throws ComponentLookupException
    {
        List<String> result =
            ((AbstractComplexController<Integer>) this.mocker.getComponentUnderTest()).getBooleanFields();

        Assert.assertEquals(3, result.size());
        Assert.assertThat(result, Matchers.hasItem(CONSANGUINITY));
        Assert.assertThat(result, Matchers.hasItem(MISCARRIAGES));
        Assert.assertThat(result, Matchers.hasItem(AFFECTED_RELATIVES));
    }

    @Test
    public void checkGetCodeFields() throws ComponentLookupException
    {
        Assert.assertTrue(
            ((AbstractComplexController<Integer>) this.mocker.getComponentUnderTest()).getCodeFields().isEmpty());
    }


    @Test
    public void saveDoesNothingIfPatientHasNoPatientClass()
    {
        when(this.doc.getXObject(Patient.CLASS_REFERENCE)).thenReturn(null);
        this.component.save(this.patient);
        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveReplacesExistingDataWhenDataIsNullAndPolicyIsReplace()
    {
        when(this.patient.getData(FAMILY_HISTORY)).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.REPLACE);
        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.xcontext);
        verify(this.dataHolder, times(1)).set(CONSANGUINITY, null, this.xcontext);
        verify(this.dataHolder, times(1)).set(AFFECTED_RELATIVES, null, this.xcontext);
        verify(this.dataHolder, times(1)).set(MISCARRIAGES, null, this.xcontext);
        verifyNoMoreInteractions(this.doc);
        verifyNoMoreInteractions(this.dataHolder);
    }

    @Test
    public void saveDoesNothingWhenDataIsNullAndPolicyIsMerge()
    {
        when(this.patient.getData(FAMILY_HISTORY)).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.MERGE);
        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.xcontext);
        verifyNoMoreInteractions(this.doc);
        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveDoesNothingWhenDataIsNullAndPolicyIsUpdate()
    {
        when(this.patient.getData(FAMILY_HISTORY)).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.UPDATE);
        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.xcontext);
        verifyNoMoreInteractions(this.doc);
        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveUpdatesOnlySpecifiedFieldsWhenPolicyIsUpdate()
    {
        final Map<String, Integer> historyMap = new LinkedHashMap<>();
        historyMap.put(CONSANGUINITY, 0);

        final PatientData<Integer> ethnicities = new DictionaryPatientData<>(FAMILY_HISTORY, historyMap);

        doReturn(ethnicities).when(this.patient).getData(FAMILY_HISTORY);
        this.component.save(this.patient, PatientWritePolicy.UPDATE);
        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.xcontext);
        verify(this.dataHolder, times(1)).set(CONSANGUINITY, 0, this.xcontext);
        verify(this.dataHolder, never()).set(matches(AFFECTED_RELATIVES), any(Integer.class), any(XWikiContext.class));
        verify(this.dataHolder, never()).set(matches(MISCARRIAGES), any(Integer.class), any(XWikiContext.class));
        verifyNoMoreInteractions(this.doc);
        verifyNoMoreInteractions(this.dataHolder);
    }

    @Test
    public void saveUpdatesOnlySpecifiedFieldsWhenPolicyIsMerge()
    {
        final Map<String, Integer> updatedMap = new LinkedHashMap<>();
        updatedMap.put(CONSANGUINITY, 0);
        updatedMap.put(MISCARRIAGES, 0);

        final BaseProperty miscarriagesField = mock(BaseProperty.class);
        when(this.dataHolder.getField(MISCARRIAGES)).thenReturn(miscarriagesField);
        when(miscarriagesField.getValue()).thenReturn(1);

        final PatientData<Integer> familyHistory = new DictionaryPatientData<>(FAMILY_HISTORY, updatedMap);

        doReturn(familyHistory).when(this.patient).getData(FAMILY_HISTORY);
        this.component.save(this.patient, PatientWritePolicy.MERGE);
        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE);
        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.xcontext);
        verify(this.dataHolder, times(1)).set(CONSANGUINITY, 0, this.xcontext);
        verify(this.dataHolder, times(1)).set(MISCARRIAGES, 0, this.xcontext);
        verify(this.dataHolder, never()).set(matches(AFFECTED_RELATIVES), any(), any(XWikiContext.class));
        verify(this.dataHolder, times(1)).getField(CONSANGUINITY);
        verify(this.dataHolder, times(1)).getField(MISCARRIAGES);
        verify(this.dataHolder, times(1)).getField(AFFECTED_RELATIVES);
        verifyNoMoreInteractions(this.doc);
        verifyNoMoreInteractions(this.dataHolder);
    }

    @Test
    public void saveUpdatesOnlySpecifiedFieldsAndClearsTheRestWhenPolicyIsReplace()
    {
        final Map<String, Integer> updatedMap = new LinkedHashMap<>();
        updatedMap.put(CONSANGUINITY, 0);
        updatedMap.put(MISCARRIAGES, 1);

        final PatientData<Integer> familyHistory = new DictionaryPatientData<>(FAMILY_HISTORY, updatedMap);

        doReturn(familyHistory).when(this.patient).getData(FAMILY_HISTORY);
        this.component.save(this.patient, PatientWritePolicy.REPLACE);
        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.xcontext);
        verify(this.dataHolder, times(1)).set(CONSANGUINITY, 0, this.xcontext);
        verify(this.dataHolder, times(1)).set(MISCARRIAGES, 1, this.xcontext);
        verify(this.dataHolder, times(1)).set(AFFECTED_RELATIVES, null, this.xcontext);
        verifyNoMoreInteractions(this.doc);
        verifyNoMoreInteractions(this.dataHolder);
    }
}
