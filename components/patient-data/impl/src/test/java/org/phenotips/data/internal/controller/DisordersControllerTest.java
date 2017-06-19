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
import org.phenotips.data.Disorder;
import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.PatientWritePolicy;
import org.phenotips.data.internal.PhenoTipsDisorder;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Provider;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.ListProperty;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link DisordersController} Component, implementation of the
 * {@link org.phenotips.data.PatientDataController} interface.
 */
public class DisordersControllerTest
{
    private static final String DISORDER_PROPERTIES_OMIMID = "omim_id";

    private static final String OMIM_1 = "omim1";

    private static final String OMIM_2 = "omim2";

    private static final String OMIM_3 = "omim3";

    private static final String ID = "id";

    @Rule
    public MockitoComponentMockingRule<PatientDataController<Disorder>> mocker =
        new MockitoComponentMockingRule<>(DisordersController.class);

    @Mock
    private XWikiContext xcontext;

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject dataHolder;

    private PatientDataController<Disorder> component;

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
    public void saveDoesNothingWhenPatientHasNoPatientClass()
    {
        when(this.doc.getXObject(Patient.CLASS_REFERENCE, true, this.xcontext)).thenReturn(null);
        this.component.save(this.patient);
        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveDoesNothingWhenPatientDataIsOfWrongFormat()
    {
        final PatientData<Disorder> data =
            new DictionaryPatientData<>(this.component.getName(), Collections.emptyMap());
        doReturn(data).when(this.patient).getData(this.component.getName());
        this.component.save(this.patient);
        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveDoesNothingWhenPatientDataIsNullAndPolicyIsUpdate()
    {
        when(this.patient.getData(this.component.getName())).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.UPDATE);

        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.xcontext);

        verifyNoMoreInteractions(this.doc);
        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveDoesNothingWhenPatientDataIsNullAndPolicyIsMerge()
    {
        when(this.patient.getData(this.component.getName())).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.MERGE);

        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.xcontext);

        verifyNoMoreInteractions(this.doc);
        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveNullsStoredDataWhenPatientDataIsNullAndPolicyIsReplace()
    {
        when(this.patient.getData(this.component.getName())).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.REPLACE);

        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.xcontext);

        verify(this.dataHolder, times(1)).set(DISORDER_PROPERTIES_OMIMID, null, this.xcontext);

        verifyNoMoreInteractions(this.doc, this.dataHolder);
    }

    @Test
    public void saveClearsExistingDataWhenPatientDataIsEmptyAndPolicyIsUpdate()
    {
        final PatientData<Disorder> data = new IndexedPatientData<>(this.component.getName(), Collections.emptyList());
        doReturn(data).when(this.patient).getData(this.component.getName());

        this.component.save(this.patient, PatientWritePolicy.UPDATE);

        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.xcontext);

        verify(this.dataHolder, times(1)).set(DISORDER_PROPERTIES_OMIMID, null, this.xcontext);

        verifyNoMoreInteractions(this.doc, this.dataHolder);
    }

    @Test
    public void saveKeepsExistingDataWhenPatientDataIsEmptyAndPolicyIsMerge() throws XWikiException
    {
        final PatientData<Disorder> data = new IndexedPatientData<>(this.component.getName(), Collections.emptyList());
        doReturn(data).when(this.patient).getData(this.component.getName());

        final ListProperty listProperty = mock(ListProperty.class);
        when(this.dataHolder.get(DISORDER_PROPERTIES_OMIMID)).thenReturn(listProperty);

        when(listProperty.getList()).thenReturn(Arrays.asList(OMIM_1, OMIM_2));

        this.component.save(this.patient, PatientWritePolicy.MERGE);

        final List<String> resultList = Arrays.asList(OMIM_1, OMIM_2);

        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.xcontext);
        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE);

        verify(this.dataHolder, times(1)).set(DISORDER_PROPERTIES_OMIMID, resultList, this.xcontext);
        verify(this.dataHolder, times(1)).get(DISORDER_PROPERTIES_OMIMID);

        verifyNoMoreInteractions(this.doc, this.dataHolder);
    }

    @Test
    public void saveClearsExistingDataWhenPatientDataIsEmptyAndPolicyIsReplace()
    {
        final PatientData<Disorder> data = new IndexedPatientData<>(this.component.getName(), Collections.emptyList());
        doReturn(data).when(this.patient).getData(this.component.getName());

        this.component.save(this.patient, PatientWritePolicy.REPLACE);

        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.xcontext);

        verify(this.dataHolder, times(1)).set(DISORDER_PROPERTIES_OMIMID, null, this.xcontext);

        verifyNoMoreInteractions(this.doc, this.dataHolder);
    }

    @Test
    public void saveReplacesOldDataWhenNewDataIsProvidedAndPolicyIsUpdate()
    {
        final Disorder disorder1 = new PhenoTipsDisorder(new JSONObject().put(ID, OMIM_1));
        final Disorder disorder2 = new PhenoTipsDisorder(new JSONObject().put(ID, OMIM_2));
        final List<Disorder> disorderArray = Arrays.asList(disorder1, disorder2);
        final PatientData<Disorder> data = new IndexedPatientData<>(this.component.getName(), disorderArray);
        doReturn(data).when(this.patient).getData(this.component.getName());

        this.component.save(this.patient, PatientWritePolicy.UPDATE);

        final List<String> resultList = Arrays.asList(OMIM_1, OMIM_2);

        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.xcontext);

        verify(this.dataHolder, times(1)).set(DISORDER_PROPERTIES_OMIMID, resultList, this.xcontext);

        verifyNoMoreInteractions(this.doc, this.dataHolder);
    }

    @Test
    public void saveMergesWithOldDataWhenNewDataIsProvidedAndPolicyIsMerge() throws XWikiException
    {
        final Disorder disorder1 = new PhenoTipsDisorder(new JSONObject().put(ID, OMIM_1));
        final Disorder disorder2 = new PhenoTipsDisorder(new JSONObject().put(ID, OMIM_2));
        final List<Disorder> disorderArray = Arrays.asList(disorder1, disorder2);
        final PatientData<Disorder> data = new IndexedPatientData<>(this.component.getName(), disorderArray);
        doReturn(data).when(this.patient).getData(this.component.getName());

        final ListProperty listProperty = mock(ListProperty.class);
        when(this.dataHolder.get(DISORDER_PROPERTIES_OMIMID)).thenReturn(listProperty);

        when(listProperty.getList()).thenReturn(Arrays.asList(OMIM_1, OMIM_3));

        this.component.save(this.patient, PatientWritePolicy.MERGE);

        final List<String> resultList = Arrays.asList(OMIM_1, OMIM_3, OMIM_2);

        // Once for save() and once for load().
        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.xcontext);
        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE);

        verify(this.dataHolder, times(1)).set(DISORDER_PROPERTIES_OMIMID, resultList, this.xcontext);
        verify(this.dataHolder, times(1)).get(DISORDER_PROPERTIES_OMIMID);

        verifyNoMoreInteractions(this.doc, this.dataHolder);
    }
}
