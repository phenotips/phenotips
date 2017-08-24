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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Test for the {@link PrenatalPerinatalHistoryController} Component, only the overridden methods from
 * {@link AbstractComplexController} are tested here.
 */
public class PrenatalPerinatalHistoryControllerTest
{
    private static final String IVF = "ivf";

    private static final String ICSI = "icsi";

    private static final String ASSISTED_REPRODUCTION_IUI = "assistedReproduction_iui";

    private static final String ASSISTED_REPRODUCTION_FERTILITY_MEDS = "assistedReproduction_fertilityMeds";

    private static final String ASSISTED_REPRODUCTION_SURROGACY = "assistedReproduction_surrogacy";

    private static final String ASSISTED_REPRODUCTION_DONOR_EGG = "assistedReproduction_donoregg";

    private static final String ASSISTED_REPRODUCTION_DONOR_SPERM = "assistedReproduction_donorsperm";

    private static final String MULTIPLE_GESTATION = "multipleGestation";

    private static final String GESTATION_TWIN = "twinNumber";

    private static final String GESTATION = "gestation";

    private static final String TRUE = "true";

    private static final String FALSE = "false";

    @Rule
    public MockitoComponentMockingRule<PatientDataController<String>> mocker =
        new MockitoComponentMockingRule<>(PrenatalPerinatalHistoryController.class);

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject dataHolder;

    private PatientDataController<String> component;

    private XWikiContext context;

    @Before
    public void setUp() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);

        this.component = this.mocker.getComponentUnderTest();

        final Provider<XWikiContext> provider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        this.context = provider.get();

        final DocumentReference patientDocRef = new DocumentReference("wiki", "patient", "00000001");
        when(this.patient.getDocumentReference()).thenReturn(patientDocRef);
        when(this.patient.getXDocument()).thenReturn(this.doc);
        when(this.doc.getXObject(Patient.CLASS_REFERENCE, true, this.context)).thenReturn(this.dataHolder);
    }

    @Test
    public void checkGetName()
    {
        Assert.assertEquals("prenatalPerinatalHistory", this.component.getName());
    }

    @Test
    public void checkGetJsonPropertyName()
    {
        Assert.assertEquals("prenatal_perinatal_history",
            ((AbstractComplexController<String>) this.component).getJsonPropertyName());
    }

    @Test
    public void checkGetProperties()
    {
        List<String> result =
            ((AbstractComplexController<String>) this.component).getProperties();

        Assert.assertEquals(10, result.size());
        Assert.assertThat(result, Matchers.hasItem("gestation"));
        Assert.assertThat(result, Matchers.hasItem(GESTATION_TWIN));
        Assert.assertThat(result, Matchers.hasItem(MULTIPLE_GESTATION));
        Assert.assertThat(result, Matchers.hasItem(IVF));
        Assert.assertThat(result, Matchers.hasItem(ICSI));
        Assert.assertThat(result, Matchers.hasItem(ASSISTED_REPRODUCTION_IUI));
        Assert.assertThat(result, Matchers.hasItem(ASSISTED_REPRODUCTION_FERTILITY_MEDS));
        Assert.assertThat(result, Matchers.hasItem(ASSISTED_REPRODUCTION_SURROGACY));
        Assert.assertThat(result, Matchers.hasItem(ASSISTED_REPRODUCTION_DONOR_EGG));
        Assert.assertThat(result, Matchers.hasItem(ASSISTED_REPRODUCTION_DONOR_SPERM));
    }

    @Test
    public void checkGetBooleanFields()
    {
        List<String> result =
            ((AbstractComplexController<String>) this.component).getBooleanFields();

        Assert.assertEquals(8, result.size());
        Assert.assertThat(result, Matchers.hasItem(MULTIPLE_GESTATION));
        Assert.assertThat(result, Matchers.hasItem(IVF));
        Assert.assertThat(result, Matchers.hasItem(ICSI));
        Assert.assertThat(result, Matchers.hasItem(ASSISTED_REPRODUCTION_IUI));
        Assert.assertThat(result, Matchers.hasItem(ASSISTED_REPRODUCTION_FERTILITY_MEDS));
        Assert.assertThat(result, Matchers.hasItem(ASSISTED_REPRODUCTION_SURROGACY));
        Assert.assertThat(result, Matchers.hasItem(ASSISTED_REPRODUCTION_DONOR_EGG));
        Assert.assertThat(result, Matchers.hasItem(ASSISTED_REPRODUCTION_DONOR_SPERM));
    }

    @Test
    public void checkGetCodeFields()
    {
        Assert.assertTrue(
            ((AbstractComplexController<String>) this.component).getCodeFields().isEmpty());
    }

    @Test
    public void saveThrowsExceptionWhenPatientHasNoPatientClass()
    {
        when(this.doc.getXObject(Patient.CLASS_REFERENCE, true, this.context)).thenReturn(null);
        this.component.save(this.patient);
        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveDoesNothingWhenPatientDataIsNullAndPolicyIsUpdate()
    {
        when(this.patient.getData(this.component.getName())).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.UPDATE);

        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.context);
        verifyNoMoreInteractions(this.doc);

        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveDoesNothingWhenPatientDataIsNullAndPolicyIsMerge()
    {
        when(this.patient.getData(this.component.getName())).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.MERGE);

        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.context);
        verifyNoMoreInteractions(this.doc);

        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveNullsAllSavedDataWhenPatientDataIsNullAndPolicyIsReplace()
    {
        when(this.patient.getData(this.component.getName())).thenReturn(null);
        this.component.save(this.patient, PatientWritePolicy.REPLACE);

        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.context);
        verifyNoMoreInteractions(this.doc);

        verify(this.dataHolder, times(1)).set(GESTATION, null, this.context);
        verify(this.dataHolder, times(1)).set(MULTIPLE_GESTATION, null, this.context);
        verify(this.dataHolder, times(1)).set(GESTATION_TWIN, null, this.context);
        verify(this.dataHolder, times(1)).set(ASSISTED_REPRODUCTION_IUI, null, this.context);
        verify(this.dataHolder, times(1)).set(IVF, null, this.context);
        verify(this.dataHolder, times(1)).set(ICSI, null, this.context);
        verify(this.dataHolder, times(1)).set(ASSISTED_REPRODUCTION_FERTILITY_MEDS, null, this.context);
        verify(this.dataHolder, times(1)).set(ASSISTED_REPRODUCTION_SURROGACY, null, this.context);
        verify(this.dataHolder, times(1)).set(ASSISTED_REPRODUCTION_DONOR_EGG, null, this.context);
        verify(this.dataHolder, times(1)).set(ASSISTED_REPRODUCTION_DONOR_SPERM, null, this.context);
        verifyNoMoreInteractions(this.dataHolder);
    }

    @Test
    public void saveUpdatesOnlySpecifiedPropertiesWhenPolicyIsUpdate()
    {
        final Map<String, String> result = new LinkedHashMap<>();
        result.put(MULTIPLE_GESTATION, TRUE);
        result.put(GESTATION_TWIN, TRUE);
        result.put(ASSISTED_REPRODUCTION_DONOR_EGG, FALSE);
        result.put(ASSISTED_REPRODUCTION_DONOR_SPERM, FALSE);
        final PatientData<String> data = spy(new DictionaryPatientData<>(this.component.getName(), result));

        doReturn(data).when(this.patient).getData(this.component.getName());
        this.component.save(this.patient, PatientWritePolicy.UPDATE);

        verify(data, times(1)).containsKey(GESTATION);
        verify(data, times(1)).containsKey(MULTIPLE_GESTATION);
        verify(data, times(1)).containsKey(GESTATION_TWIN);
        verify(data, times(1)).containsKey(ASSISTED_REPRODUCTION_IUI);
        verify(data, times(1)).containsKey(IVF);
        verify(data, times(1)).containsKey(ICSI);
        verify(data, times(1)).containsKey(ASSISTED_REPRODUCTION_FERTILITY_MEDS);
        verify(data, times(1)).containsKey(ASSISTED_REPRODUCTION_SURROGACY);
        verify(data, times(1)).containsKey(ASSISTED_REPRODUCTION_DONOR_EGG);
        verify(data, times(1)).containsKey(ASSISTED_REPRODUCTION_DONOR_SPERM);

        verify(data, times(1)).get(MULTIPLE_GESTATION);
        verify(data, times(1)).get(GESTATION_TWIN);
        verify(data, times(1)).get(ASSISTED_REPRODUCTION_DONOR_EGG);
        verify(data, times(1)).get(ASSISTED_REPRODUCTION_DONOR_SPERM);
        verify(data, times(1)).isNamed();
        verifyNoMoreInteractions(data);

        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.context);
        verifyNoMoreInteractions(this.doc);

        verify(this.dataHolder, times(1)).set(MULTIPLE_GESTATION, TRUE, this.context);
        verify(this.dataHolder, times(1)).set(GESTATION_TWIN, TRUE, this.context);
        verify(this.dataHolder, times(1)).set(ASSISTED_REPRODUCTION_DONOR_EGG, FALSE, this.context);
        verify(this.dataHolder, times(1)).set(ASSISTED_REPRODUCTION_DONOR_SPERM, FALSE, this.context);
        verifyNoMoreInteractions(this.dataHolder);
    }

    @Test
    public void saveUpdatesOnlySpecifiedPropertiesWhenPolicyIsMerge()
    {
        final Map<String, String> result = new LinkedHashMap<>();
        result.put(MULTIPLE_GESTATION, TRUE);
        result.put(GESTATION_TWIN, TRUE);
        result.put(ASSISTED_REPRODUCTION_DONOR_EGG, FALSE);
        result.put(ASSISTED_REPRODUCTION_DONOR_SPERM, FALSE);
        final PatientData<String> data = spy(new DictionaryPatientData<>(this.component.getName(), result));

        doReturn(data).when(this.patient).getData(this.component.getName());

        final BaseProperty baseProperty = mock(BaseProperty.class);
        when(baseProperty.getValue()).thenReturn(TRUE);
        when(this.dataHolder.getField(ASSISTED_REPRODUCTION_FERTILITY_MEDS)).thenReturn(baseProperty);
        this.component.save(this.patient, PatientWritePolicy.MERGE);

        verify(data, times(1)).containsKey(GESTATION);
        verify(data, times(1)).containsKey(MULTIPLE_GESTATION);
        verify(data, times(1)).containsKey(GESTATION_TWIN);
        verify(data, times(1)).containsKey(ASSISTED_REPRODUCTION_IUI);
        verify(data, times(1)).containsKey(IVF);
        verify(data, times(1)).containsKey(ICSI);
        verify(data, times(1)).containsKey(ASSISTED_REPRODUCTION_FERTILITY_MEDS);
        verify(data, times(1)).containsKey(ASSISTED_REPRODUCTION_SURROGACY);
        verify(data, times(1)).containsKey(ASSISTED_REPRODUCTION_DONOR_EGG);
        verify(data, times(1)).containsKey(ASSISTED_REPRODUCTION_DONOR_SPERM);

        verify(data, times(1)).get(MULTIPLE_GESTATION);
        verify(data, times(1)).get(GESTATION_TWIN);
        verify(data, times(1)).get(ASSISTED_REPRODUCTION_DONOR_EGG);
        verify(data, times(1)).get(ASSISTED_REPRODUCTION_DONOR_SPERM);
        verify(data, times(1)).isNamed();
        verifyNoMoreInteractions(data);

        // Once for save() and once for load().
        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE);
        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.context);
        verifyNoMoreInteractions(this.doc);

        // Nothing from load() since no stored data.
        // From save().
        verify(this.dataHolder, times(1)).set(MULTIPLE_GESTATION, TRUE, this.context);
        verify(this.dataHolder, times(1)).set(GESTATION_TWIN, TRUE, this.context);
        verify(this.dataHolder, times(1)).set(ASSISTED_REPRODUCTION_DONOR_EGG, FALSE, this.context);
        verify(this.dataHolder, times(1)).set(ASSISTED_REPRODUCTION_DONOR_SPERM, FALSE, this.context);
        verifyNoMoreInteractions(this.dataHolder);
    }

    @Test
    public void saveUpdatesOnlySpecifiedPropertiesAndNullsTheRestWhenPolicyIsReplace()
    {
        final Map<String, String> result = new LinkedHashMap<>();
        result.put(MULTIPLE_GESTATION, TRUE);
        result.put(GESTATION_TWIN, TRUE);
        result.put(ASSISTED_REPRODUCTION_DONOR_EGG, FALSE);
        result.put(ASSISTED_REPRODUCTION_DONOR_SPERM, FALSE);
        final PatientData<String> data = spy(new DictionaryPatientData<>(this.component.getName(), result));

        doReturn(data).when(this.patient).getData(this.component.getName());
        this.component.save(this.patient, PatientWritePolicy.REPLACE);

        verify(data, times(1)).get(GESTATION);
        verify(data, times(1)).get(MULTIPLE_GESTATION);
        verify(data, times(1)).get(GESTATION_TWIN);
        verify(data, times(1)).get(ASSISTED_REPRODUCTION_IUI);
        verify(data, times(1)).get(IVF);
        verify(data, times(1)).get(ICSI);
        verify(data, times(1)).get(ASSISTED_REPRODUCTION_FERTILITY_MEDS);
        verify(data, times(1)).get(ASSISTED_REPRODUCTION_SURROGACY);
        verify(data, times(1)).get(ASSISTED_REPRODUCTION_DONOR_EGG);
        verify(data, times(1)).get(ASSISTED_REPRODUCTION_DONOR_SPERM);
        verify(data, times(1)).isNamed();
        verifyNoMoreInteractions(data);

        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.context);
        verifyNoMoreInteractions(this.doc);

        verify(this.dataHolder, times(1)).set(GESTATION, null, this.context);
        verify(this.dataHolder, times(1)).set(MULTIPLE_GESTATION, TRUE, this.context);
        verify(this.dataHolder, times(1)).set(GESTATION_TWIN, TRUE, this.context);
        verify(this.dataHolder, times(1)).set(ASSISTED_REPRODUCTION_IUI, null, this.context);
        verify(this.dataHolder, times(1)).set(IVF, null, this.context);
        verify(this.dataHolder, times(1)).set(ICSI, null, this.context);
        verify(this.dataHolder, times(1)).set(ASSISTED_REPRODUCTION_FERTILITY_MEDS, null, this.context);
        verify(this.dataHolder, times(1)).set(ASSISTED_REPRODUCTION_SURROGACY, null, this.context);
        verify(this.dataHolder, times(1)).set(ASSISTED_REPRODUCTION_DONOR_EGG, FALSE, this.context);
        verify(this.dataHolder, times(1)).set(ASSISTED_REPRODUCTION_DONOR_SPERM, FALSE, this.context);
        verifyNoMoreInteractions(this.dataHolder);
    }
}
