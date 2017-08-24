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

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.List;

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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Test for the {@link TextNotesController} Component, only the overridden methods from {@link AbstractSimpleController}
 * are tested here.
 */
public class TextNotesControllerTest
{
    private static final String INDICATION_FOR_REFERRAL = "indication_for_referral";

    private static final String FAMILY_HISTORY = "family_history";

    private static final String PRENATAL_DEVELOPMENT = "prenatal_development";

    private static final String MEDICAL_HISTORY = "medical_history";

    private static final String DIAGNOSIS_NOTES = "diagnosis_notes";

    private static final String GENETIC_NOTES = "genetic_notes";

    @Rule
    public MockitoComponentMockingRule<PatientDataController<String>> mocker =
        new MockitoComponentMockingRule<>(TextNotesController.class);

    @Mock
    private BaseObject dataHolder;

    @Mock
    private PatientData<String> data;

    @Mock
    private XWikiDocument doc;

    @Mock
    private Patient patient;

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
        doReturn(this.data).when(this.patient).getData(this.component.getName());
        when(this.data.isNamed()).thenReturn(true);
    }

    @Test
    public void checkGetName()
    {
        Assert.assertEquals("notes", this.component.getName());
    }

    @Test
    public void checkGetJsonPropertyName()
    {
        Assert.assertEquals("notes",
            ((AbstractSimpleController) this.component).getJsonPropertyName());
    }

    @Test
    public void checkGetProperties()
    {
        List<String> result = ((AbstractSimpleController) this.component).getProperties();

        Assert.assertEquals(6, result.size());
        Assert.assertThat(result, Matchers.hasItem(INDICATION_FOR_REFERRAL));
        Assert.assertThat(result, Matchers.hasItem(FAMILY_HISTORY));
        Assert.assertThat(result, Matchers.hasItem(PRENATAL_DEVELOPMENT));
        Assert.assertThat(result, Matchers.hasItem(MEDICAL_HISTORY));
        Assert.assertThat(result, Matchers.hasItem(DIAGNOSIS_NOTES));
        Assert.assertThat(result, Matchers.hasItem(GENETIC_NOTES));
    }

    @Test
    public void saveDoesNothingWhenPatientHasNoPatientClass()
    {
        when(this.doc.getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any())).thenReturn(null);
        this.component.save(this.patient);
        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveDoesNothingWhenDataHasWrongFormat()
    {
        when(this.data.isNamed()).thenReturn(false);
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
    public void saveSetsAllPropertiesToNullWhenPatientDataIsNullAndPolicyIsReplace()
    {
        when(this.patient.getData(this.component.getName())).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.REPLACE);

        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());
        verifyNoMoreInteractions(this.doc);

        verify(this.dataHolder, times(1)).setStringValue(INDICATION_FOR_REFERRAL, null);
        verify(this.dataHolder, times(1)).setStringValue(FAMILY_HISTORY, null);
        verify(this.dataHolder, times(1)).setStringValue(PRENATAL_DEVELOPMENT, null);
        verify(this.dataHolder, times(1)).setStringValue(MEDICAL_HISTORY, null);
        verify(this.dataHolder, times(1)).setStringValue(DIAGNOSIS_NOTES, null);
        verify(this.dataHolder, times(1)).setStringValue(GENETIC_NOTES, null);

        verifyNoMoreInteractions(this.dataHolder);
    }

    @Test
    public void saveSetsOnlySpecifiedPropertiesWhenPolicyIsUpdate()
    {
        when(this.data.containsKey(INDICATION_FOR_REFERRAL)).thenReturn(true);
        when(this.data.containsKey(FAMILY_HISTORY)).thenReturn(false);
        when(this.data.containsKey(PRENATAL_DEVELOPMENT)).thenReturn(true);
        when(this.data.containsKey(MEDICAL_HISTORY)).thenReturn(false);
        when(this.data.containsKey(DIAGNOSIS_NOTES)).thenReturn(false);
        when(this.data.containsKey(GENETIC_NOTES)).thenReturn(true);

        when(this.data.get(INDICATION_FOR_REFERRAL)).thenReturn(INDICATION_FOR_REFERRAL);
        when(this.data.get(FAMILY_HISTORY)).thenReturn(null);
        when(this.data.get(PRENATAL_DEVELOPMENT)).thenReturn(PRENATAL_DEVELOPMENT);
        when(this.data.get(MEDICAL_HISTORY)).thenReturn(null);
        when(this.data.get(DIAGNOSIS_NOTES)).thenReturn(null);
        when(this.data.get(GENETIC_NOTES)).thenReturn(GENETIC_NOTES);

        this.component.save(this.patient, PatientWritePolicy.UPDATE);

        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());
        verifyNoMoreInteractions(this.doc);

        verify(this.data, times(1)).containsKey(INDICATION_FOR_REFERRAL);
        verify(this.data, times(1)).containsKey(FAMILY_HISTORY);
        verify(this.data, times(1)).containsKey(PRENATAL_DEVELOPMENT);
        verify(this.data, times(1)).containsKey(MEDICAL_HISTORY);
        verify(this.data, times(1)).containsKey(DIAGNOSIS_NOTES);
        verify(this.data, times(1)).containsKey(GENETIC_NOTES);
        verify(this.data, times(1)).get(INDICATION_FOR_REFERRAL);
        verify(this.data, times(1)).get(PRENATAL_DEVELOPMENT);
        verify(this.data, times(1)).get(GENETIC_NOTES);
        verify(this.data, times(1)).isNamed();
        verifyNoMoreInteractions(this.data);

        verify(this.dataHolder, times(1)).setStringValue(INDICATION_FOR_REFERRAL, INDICATION_FOR_REFERRAL);
        verify(this.dataHolder, times(1)).setStringValue(PRENATAL_DEVELOPMENT, PRENATAL_DEVELOPMENT);
        verify(this.dataHolder, times(1)).setStringValue(GENETIC_NOTES, GENETIC_NOTES);
        verifyNoMoreInteractions(this.dataHolder);
    }

    @Test
    public void saveSetsOnlySpecifiedPropertiesWhenPolicyIsMerge()
    {
        when(this.data.containsKey(INDICATION_FOR_REFERRAL)).thenReturn(true);
        when(this.data.containsKey(FAMILY_HISTORY)).thenReturn(false);
        when(this.data.containsKey(PRENATAL_DEVELOPMENT)).thenReturn(true);
        when(this.data.containsKey(MEDICAL_HISTORY)).thenReturn(false);
        when(this.data.containsKey(DIAGNOSIS_NOTES)).thenReturn(false);
        when(this.data.containsKey(GENETIC_NOTES)).thenReturn(true);

        when(this.data.get(INDICATION_FOR_REFERRAL)).thenReturn(INDICATION_FOR_REFERRAL);
        when(this.data.get(FAMILY_HISTORY)).thenReturn(null);
        when(this.data.get(PRENATAL_DEVELOPMENT)).thenReturn(PRENATAL_DEVELOPMENT);
        when(this.data.get(MEDICAL_HISTORY)).thenReturn(null);
        when(this.data.get(DIAGNOSIS_NOTES)).thenReturn(null);
        when(this.data.get(GENETIC_NOTES)).thenReturn(GENETIC_NOTES);

        this.component.save(this.patient, PatientWritePolicy.MERGE);

        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());
        verifyNoMoreInteractions(this.doc);

        verify(this.data, times(1)).containsKey(INDICATION_FOR_REFERRAL);
        verify(this.data, times(1)).containsKey(FAMILY_HISTORY);
        verify(this.data, times(1)).containsKey(PRENATAL_DEVELOPMENT);
        verify(this.data, times(1)).containsKey(MEDICAL_HISTORY);
        verify(this.data, times(1)).containsKey(DIAGNOSIS_NOTES);
        verify(this.data, times(1)).containsKey(GENETIC_NOTES);
        verify(this.data, times(1)).get(INDICATION_FOR_REFERRAL);
        verify(this.data, times(1)).get(PRENATAL_DEVELOPMENT);
        verify(this.data, times(1)).get(GENETIC_NOTES);
        verify(this.data, times(1)).isNamed();
        verifyNoMoreInteractions(this.data);

        verify(this.dataHolder, times(1)).setStringValue(INDICATION_FOR_REFERRAL, INDICATION_FOR_REFERRAL);
        verify(this.dataHolder, times(1)).setStringValue(PRENATAL_DEVELOPMENT, PRENATAL_DEVELOPMENT);
        verify(this.dataHolder, times(1)).setStringValue(GENETIC_NOTES, GENETIC_NOTES);
        verifyNoMoreInteractions(this.dataHolder);
    }

    @Test
    public void saveSetsOnlySpecifiedPropertiesAndNullsTheRestWhenPolicyIsReplace()
    {
        when(this.data.containsKey(INDICATION_FOR_REFERRAL)).thenReturn(true);
        when(this.data.containsKey(FAMILY_HISTORY)).thenReturn(false);
        when(this.data.containsKey(PRENATAL_DEVELOPMENT)).thenReturn(true);
        when(this.data.containsKey(MEDICAL_HISTORY)).thenReturn(false);
        when(this.data.containsKey(DIAGNOSIS_NOTES)).thenReturn(false);
        when(this.data.containsKey(GENETIC_NOTES)).thenReturn(true);

        when(this.data.get(INDICATION_FOR_REFERRAL)).thenReturn(INDICATION_FOR_REFERRAL);
        when(this.data.get(FAMILY_HISTORY)).thenReturn(null);
        when(this.data.get(PRENATAL_DEVELOPMENT)).thenReturn(PRENATAL_DEVELOPMENT);
        when(this.data.get(MEDICAL_HISTORY)).thenReturn(null);
        when(this.data.get(DIAGNOSIS_NOTES)).thenReturn(null);
        when(this.data.get(GENETIC_NOTES)).thenReturn(GENETIC_NOTES);

        this.component.save(this.patient, PatientWritePolicy.REPLACE);

        verify(this.doc, times(1)).getXObject(eq(Patient.CLASS_REFERENCE), eq(true), any());
        verifyNoMoreInteractions(this.doc);

        verify(this.data, times(1)).get(INDICATION_FOR_REFERRAL);
        verify(this.data, times(1)).get(FAMILY_HISTORY);
        verify(this.data, times(1)).get(PRENATAL_DEVELOPMENT);
        verify(this.data, times(1)).get(MEDICAL_HISTORY);
        verify(this.data, times(1)).get(DIAGNOSIS_NOTES);
        verify(this.data, times(1)).get(GENETIC_NOTES);
        verify(this.data, times(1)).isNamed();
        verifyNoMoreInteractions(this.data);

        verify(this.dataHolder, times(1)).setStringValue(INDICATION_FOR_REFERRAL, INDICATION_FOR_REFERRAL);
        verify(this.dataHolder, times(1)).setStringValue(FAMILY_HISTORY, null);
        verify(this.dataHolder, times(1)).setStringValue(PRENATAL_DEVELOPMENT, PRENATAL_DEVELOPMENT);
        verify(this.dataHolder, times(1)).setStringValue(MEDICAL_HISTORY, null);
        verify(this.dataHolder, times(1)).setStringValue(DIAGNOSIS_NOTES, null);
        verify(this.dataHolder, times(1)).setStringValue(GENETIC_NOTES, GENETIC_NOTES);
        verifyNoMoreInteractions(this.dataHolder);
    }
}
