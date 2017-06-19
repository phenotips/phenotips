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

import java.util.Arrays;
import java.util.Collections;
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
import static org.mockito.Matchers.anySetOf;
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
 * Test for the {@link EthnicityController} Component, only the overridden methods from
 * {@link AbstractComplexController} are tested here.
 */
public class EthnicityControllerTest
{
    private static final String ETHNICITY = "ethnicity";

    private static final String MATERNAL = "maternal_ethnicity";

    private static final String PATERNAL = "paternal_ethnicity";

    private static final String ETH_A = "A";

    private static final String ETH_B = "B";

    private static final String ETH_C = "C";

    private static final String ETH_D = "D";

    @Rule
    public MockitoComponentMockingRule<PatientDataController<List<String>>> mocker =
        new MockitoComponentMockingRule<>(EthnicityController.class);

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject dataHolder;

    @Mock
    private XWikiContext xcontext;

    private PatientDataController<List<String>> component;

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
        Assert.assertEquals(ETHNICITY, this.component.getName());
    }

    @Test
    public void checkGetJsonPropertyName() throws ComponentLookupException
    {
        Assert.assertEquals(ETHNICITY,
            ((AbstractComplexController<List<String>>) this.component).getJsonPropertyName());
    }

    @Test
    public void checkGetProperties() throws ComponentLookupException
    {
        List<String> result =
            ((AbstractComplexController<List<String>>) this.component).getProperties();

        Assert.assertEquals(2, result.size());
        Assert.assertThat(result, Matchers.hasItem(MATERNAL));
        Assert.assertThat(result, Matchers.hasItem(PATERNAL));
    }

    @Test
    public void checkGetBooleanFields() throws ComponentLookupException
    {
        Assert.assertTrue(
            ((AbstractComplexController<List<String>>) this.component).getBooleanFields()
                .isEmpty());
    }

    @Test
    public void checkGetCodeFields() throws ComponentLookupException
    {
        Assert.assertTrue(
            ((AbstractComplexController<List<String>>) this.component).getCodeFields().isEmpty());
    }

    @Test
    public void saveDoesNothingIfPatientHasNoPatientClass()
    {
        when(this.doc.getXObject(Patient.CLASS_REFERENCE, true, this.xcontext)).thenReturn(null);
        this.component.save(this.patient);
        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveReplacesExistingDataWhenDataIsNullAndPolicyIsReplace()
    {
        when(this.patient.getData(ETHNICITY)).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.REPLACE);
        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.xcontext);
        verify(this.dataHolder, times(1)).set(MATERNAL, null, this.xcontext);
        verify(this.dataHolder, times(1)).set(PATERNAL, null, this.xcontext);
        verifyNoMoreInteractions(this.doc);
        verifyNoMoreInteractions(this.dataHolder);
    }

    @Test
    public void saveDoesNothingWhenDataIsNullAndPolicyIsMerge()
    {
        when(this.patient.getData(ETHNICITY)).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.MERGE);
        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.xcontext);
        verifyNoMoreInteractions(this.doc);
        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveDoesNothingWhenDataIsNullAndPolicyIsUpdate()
    {
        when(this.patient.getData(ETHNICITY)).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.UPDATE);
        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.xcontext);
        verifyNoMoreInteractions(this.doc);
        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveUpdatesOnlySpecifiedFieldsWhenPolicyIsUpdate()
    {
        final List<String> ethnicitiesList = Arrays.asList(ETH_A, ETH_B);

        final Map<String, List<String>> ethnicitiesMap = new LinkedHashMap<>();
        ethnicitiesMap.put(MATERNAL, ethnicitiesList);

        final PatientData<List<String>> ethnicities = new DictionaryPatientData<>(ETHNICITY, ethnicitiesMap);

        doReturn(ethnicities).when(this.patient).getData(ETHNICITY);
        this.component.save(this.patient, PatientWritePolicy.UPDATE);
        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.xcontext);
        verify(this.dataHolder, times(1)).set(MATERNAL, ethnicitiesList, this.xcontext);
        verify(this.dataHolder, never()).set(matches(PATERNAL), anySetOf(String.class), any(this.xcontext.getClass()));
        verifyNoMoreInteractions(this.doc);
        verifyNoMoreInteractions(this.dataHolder);
    }

    @Test
    public void saveUpdatesOnlySpecifiedFieldsWhenPolicyIsMerge()
    {
        final List<String> maternalList = Collections.singletonList(ETH_C);
        final List<String> paternalList = Collections.singletonList(ETH_D);
        final Map<String, List<String>> updatedMap = new LinkedHashMap<>();
        updatedMap.put(MATERNAL, maternalList);
        updatedMap.put(PATERNAL, paternalList);

        final List<String> storedMaternalList = Arrays.asList(ETH_A, ETH_B);
        final List<String> storedPaternalList = Collections.singletonList(ETH_D);
        final BaseProperty maternalField = mock(BaseProperty.class);
        when(this.dataHolder.getField(MATERNAL)).thenReturn(maternalField);
        when(maternalField.getValue()).thenReturn(storedMaternalList);
        final BaseProperty paternalField = mock(BaseProperty.class);
        when(this.dataHolder.getField(PATERNAL)).thenReturn(paternalField);
        when(paternalField.getValue()).thenReturn(storedPaternalList);

        final PatientData<List<String>> ethnicities = new DictionaryPatientData<>(ETHNICITY, updatedMap);

        final List<String> mergedMaternal = Arrays.asList(ETH_A, ETH_B, ETH_C);

        final List<String> mergedPaternal = Collections.singletonList(ETH_D);

        doReturn(ethnicities).when(this.patient).getData(ETHNICITY);
        this.component.save(this.patient, PatientWritePolicy.MERGE);
        // Once on save(), another time on load().
        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE);
        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.xcontext);
        verify(this.dataHolder, times(1)).set(MATERNAL, mergedMaternal, this.xcontext);
        verify(this.dataHolder, times(1)).set(PATERNAL, mergedPaternal, this.xcontext);
        verify(this.dataHolder, times(1)).getField(MATERNAL);
        verify(this.dataHolder, times(1)).getField(PATERNAL);
        verifyNoMoreInteractions(this.doc);
        verifyNoMoreInteractions(this.dataHolder);
    }

    @Test
    public void saveUpdatesOnlySpecifiedFieldsAndClearsTheRestWhenPolicyIsReplace()
    {
        final List<String> ethnicitiesList = Arrays.asList(ETH_A, ETH_B);

        final Map<String, List<String>> ethnicitiesMap = new LinkedHashMap<>();
        ethnicitiesMap.put(MATERNAL, ethnicitiesList);

        final PatientData<List<String>> ethnicities = new DictionaryPatientData<>(ETHNICITY, ethnicitiesMap);

        doReturn(ethnicities).when(this.patient).getData(ETHNICITY);
        this.component.save(this.patient, PatientWritePolicy.REPLACE);
        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.xcontext);
        verify(this.dataHolder, times(1)).set(MATERNAL, ethnicitiesList, this.xcontext);
        verify(this.dataHolder, times(1)).set(PATERNAL, null, this.xcontext);
        verifyNoMoreInteractions(this.doc);
        verifyNoMoreInteractions(this.dataHolder);
    }
}
