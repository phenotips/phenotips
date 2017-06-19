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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link AllergiesController} Component, implementation of the
 * {@link org.phenotips.data.PatientDataController} interface.
 */
public class AllergiesControllerTest
{
    private static final String NKDA = "NKDA";

    private static final String ALLERGY_1 = "allergy1";

    private static final String ALLERGY_2 = "allergy2";

    private static final String ALLERGY_3 = "allergy3";

    private static final String ALLERGY_4 = "allergy4";

    @Rule
    public MockitoComponentMockingRule<PatientDataController<String>> mocker =
        new MockitoComponentMockingRule<>(AllergiesController.class);

    @Mock
    private XWikiContext xcontext;

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

        final Provider<XWikiContext> xcp = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcp.get()).thenReturn(this.xcontext);

        final DocumentReference patientDocRef = new DocumentReference("wiki", "patient", "00000001");
        when(this.patient.getDocumentReference()).thenReturn(patientDocRef);
        when(this.patient.getXDocument()).thenReturn(this.doc);
        when(this.doc.getXObject(AllergiesController.CLASS_REFERENCE, true, this.xcontext)).thenReturn(this.dataHolder);
    }

    @Test
    public void saveDoesNothingWhenPatientHasAllergiesClass()
    {
        when(this.doc.getXObject(AllergiesController.CLASS_REFERENCE, true, this.xcontext)).thenReturn(null);
        this.component.save(this.patient);
        verify(this.doc, times(1)).getXObject(AllergiesController.CLASS_REFERENCE, true, this.xcontext);
        verifyNoMoreInteractions(this.doc);
        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveDoesNothingWhenPatientDataHasWrongFormat()
    {
        final PatientData<String> data = new DictionaryPatientData<>(this.component.getName(), Collections.emptyMap());
        doReturn(data).when(this.patient).getData(this.component.getName());
        this.component.save(this.patient);
    }

    @Test
    public void saveDoesNothingWhenPatientDataIsNullAndPolicyIsUpdate()
    {
        when(this.patient.getData(this.component.getName())).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.UPDATE);

        verify(this.doc, times(1)).getXObject(AllergiesController.CLASS_REFERENCE, true, this.xcontext);

        verifyNoMoreInteractions(this.doc);
        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveDoesNothingWhenPatientDataIsNullAndPolicyIsMerge()
    {
        when(this.patient.getData(this.component.getName())).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.MERGE);

        verify(this.doc, times(1)).getXObject(AllergiesController.CLASS_REFERENCE, true, this.xcontext);

        verifyNoMoreInteractions(this.doc);
        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveNullsStoredDataWhenPatientDataIsNullAndPolicyIsReplace()
    {
        when(this.patient.getData(this.component.getName())).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.REPLACE);

        verify(this.doc, times(1)).getXObject(AllergiesController.CLASS_REFERENCE, true, this.xcontext);

        verify(this.dataHolder, times(1)).setIntValue(NKDA, 0);
        verify(this.dataHolder, times(1)).setDBStringListValue(this.component.getName(), null);

        verifyNoMoreInteractions(this.doc, this.dataHolder);
    }

    @Test
    public void saveClearsExistingDataWhenPatientDataIsEmptyAndPolicyIsUpdate()
    {
        final PatientData<String> data = new IndexedPatientData<>(this.component.getName(), Collections.emptyList());
        doReturn(data).when(this.patient).getData(this.component.getName());

        this.component.save(this.patient, PatientWritePolicy.UPDATE);

        verify(this.doc, times(1)).getXObject(AllergiesController.CLASS_REFERENCE, true, this.xcontext);

        verify(this.dataHolder, times(1)).setIntValue(NKDA, 0);
        verify(this.dataHolder, times(1)).setDBStringListValue(this.component.getName(), null);

        verifyNoMoreInteractions(this.doc, this.dataHolder);
    }

    @Test
    public void saveKeepsExistingDataWhenPatientDataIsEmptyAndPolicyIsMerge()
    {
        final PatientData<String> data = new IndexedPatientData<>(this.component.getName(), Collections.emptyList());
        doReturn(data).when(this.patient).getData(this.component.getName());

        final BaseObject storedObject = mock(BaseObject.class);
        when(this.doc.getXObject(AllergiesController.CLASS_REFERENCE)).thenReturn(storedObject);
        when(storedObject.getListValue(this.component.getName())).thenReturn(Arrays.asList(NKDA, ALLERGY_1, ALLERGY_2));

        this.component.save(this.patient, PatientWritePolicy.MERGE);

        final List<String> resultList = Arrays.asList(ALLERGY_1, ALLERGY_2);

        verify(this.doc, times(1)).getXObject(AllergiesController.CLASS_REFERENCE, true, this.xcontext);
        verify(this.doc, times(1)).getXObject(AllergiesController.CLASS_REFERENCE);

        verify(this.dataHolder, times(1)).setIntValue(NKDA, 1);
        verify(this.dataHolder, times(1)).setDBStringListValue(this.component.getName(), resultList);

        verifyNoMoreInteractions(this.doc, this.dataHolder);
    }

    @Test
    public void saveClearsExistingDataWhenPatientDataIsEmptyAndPolicyIsReplace()
    {
        final PatientData<String> data = spy(new IndexedPatientData<>(this.component.getName(),
            Collections.emptyList()));
        doReturn(data).when(this.patient).getData(this.component.getName());

        this.component.save(this.patient, PatientWritePolicy.REPLACE);

        verify(this.doc, times(1)).getXObject(AllergiesController.CLASS_REFERENCE, true, this.xcontext);

        verify(this.dataHolder, times(1)).setIntValue(NKDA, 0);
        verify(this.dataHolder, times(1)).setDBStringListValue(this.component.getName(), null);

        verifyNoMoreInteractions(this.doc, this.dataHolder);
    }

    @Test
    public void saveReplacesOldDataWhenNewDataIsProvidedAndPolicyIsUpdate()
    {
        final List<String> allergyArray = Arrays.asList(ALLERGY_3, ALLERGY_4, NKDA);
        final PatientData<String> data = new IndexedPatientData<>(this.component.getName(), allergyArray);
        doReturn(data).when(this.patient).getData(this.component.getName());

        this.component.save(this.patient, PatientWritePolicy.UPDATE);

        final List<String> resultList = Arrays.asList(ALLERGY_3, ALLERGY_4);

        verify(this.doc, times(1)).getXObject(AllergiesController.CLASS_REFERENCE, true, this.xcontext);

        verify(this.dataHolder, times(1)).setIntValue(NKDA, 1);
        verify(this.dataHolder, times(1)).setDBStringListValue(this.component.getName(), resultList);

        verifyNoMoreInteractions(this.doc, this.dataHolder);

    }

    @Test
    public void saveMergesWithOldDataWhenNewDataIsProvidedAndPolicyIsMerge()
    {
        final List<String> allergyArray = Arrays.asList(ALLERGY_3, ALLERGY_4);
        final PatientData<String> data = new IndexedPatientData<>(this.component.getName(), allergyArray);
        doReturn(data).when(this.patient).getData(this.component.getName());

        final BaseObject storedObject = mock(BaseObject.class);
        when(this.doc.getXObject(AllergiesController.CLASS_REFERENCE)).thenReturn(storedObject);
        when(storedObject.getListValue(this.component.getName())).thenReturn(Arrays.asList(ALLERGY_1, ALLERGY_2));

        this.component.save(this.patient, PatientWritePolicy.MERGE);

        final List<String> resultList = Arrays.asList(ALLERGY_1, ALLERGY_2, ALLERGY_3, ALLERGY_4);

        verify(this.doc, times(1)).getXObject(AllergiesController.CLASS_REFERENCE, true, this.xcontext);
        verify(this.doc, times(1)).getXObject(AllergiesController.CLASS_REFERENCE);

        verify(this.dataHolder, times(1)).setIntValue(NKDA, 0);
        verify(this.dataHolder, times(1)).setDBStringListValue(this.component.getName(), resultList);

        verifyNoMoreInteractions(this.doc, this.dataHolder);
    }

    @Test
    public void saveReplacesOldDataWhenNewDataIsProvidedAndPolicyIsReplace()
    {
        final List<String> allergyArray = Arrays.asList(ALLERGY_3, ALLERGY_4, NKDA);
        final PatientData<String> data = new IndexedPatientData<>(this.component.getName(), allergyArray);
        doReturn(data).when(this.patient).getData(this.component.getName());

        this.component.save(this.patient, PatientWritePolicy.REPLACE);

        final List<String> resultList = Arrays.asList(ALLERGY_3, ALLERGY_4);

        verify(this.doc, times(1)).getXObject(AllergiesController.CLASS_REFERENCE, true, this.xcontext);

        verify(this.dataHolder, times(1)).setIntValue(NKDA, 1);
        verify(this.dataHolder, times(1)).setDBStringListValue(this.component.getName(), resultList);

        verifyNoMoreInteractions(this.doc, this.dataHolder);
    }
}
