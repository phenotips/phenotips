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

import org.phenotips.Constants;
import org.phenotips.data.DictionaryPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.PatientWritePolicy;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Provider;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ParentalAgeControllerTest
{
    private static final String MATERNAL_AGE = "maternal_age";

    private static final String PATERNAL_AGE = "paternal_age";

    // Arbitrary age
    private static final Integer AGE_NON_ZERO = 25;

    private static final Integer AGE_ZERO = 0;

    private static final Integer AGE_UNSET = Integer.MIN_VALUE;

    private static final String TEST_PATIENT_ID = "00000001";

    private static final EntityReference CLASS_REFERENCE =
        new EntityReference("ParentalInformationClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    @Rule
    public MockitoComponentMockingRule<PatientDataController<Integer>> mocker =
        new MockitoComponentMockingRule<>(ParentalAgeController.class);

    @Mock
    private Logger logger;

    private XWikiContext xWikiContext;

    private ParentalAgeController parentalAgeController;

    @Mock
    private XWiki xwiki;

    @Mock
    private Patient patient;

    @Mock
    private PatientData<Integer> patientData;

    @Mock
    private XWikiDocument doc;

    @Mock
    private BaseObject dataHolder;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.parentalAgeController = (ParentalAgeController) this.mocker.getComponentUnderTest();
        this.logger = this.mocker.getMockedLogger();

        final Provider<XWikiContext> provider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        this.xWikiContext = provider.get();
        doReturn(this.xwiki).when(this.xWikiContext).getWiki();

        DocumentReference patientDocRef = new DocumentReference("wiki", "patient", TEST_PATIENT_ID);
        doReturn(patientDocRef).when(this.patient).getDocumentReference();
        doReturn(this.doc).when(this.patient).getXDocument();
        doReturn(patientDocRef.getName()).when(this.patient).getId();

        when(this.doc.getXObject(CLASS_REFERENCE, true, this.xWikiContext)).thenReturn(this.dataHolder);
        doReturn(AGE_UNSET).when(this.dataHolder).getIntValue(any(String.class), eq(AGE_UNSET));
    }

    @Test
    public void loadEmptyPatientTest()
    {
        doReturn(null).when(this.doc).getXObject(any(EntityReference.class));
        PatientData<Integer> testData = this.parentalAgeController.load(this.patient);
        Assert.assertNull(testData);
        verify(this.logger).debug("No parental information for patient [{}]", TEST_PATIENT_ID);
    }

    @Test
    public void loadMaternalAndPaternalAgeNonZero()
    {
        BaseObject data = mock(BaseObject.class);
        doReturn(data).when(this.doc).getXObject(any(EntityReference.class));

        doReturn(AGE_NON_ZERO).when(data).getIntValue(MATERNAL_AGE, AGE_UNSET);
        doReturn(AGE_NON_ZERO).when(data).getIntValue(PATERNAL_AGE, AGE_UNSET);

        PatientData<Integer> testData = this.parentalAgeController.load(this.patient);

        Assert.assertEquals("parentalAge", testData.getName());
        Assert.assertEquals(AGE_NON_ZERO, testData.get(MATERNAL_AGE));
        Assert.assertEquals(AGE_NON_ZERO, testData.get(PATERNAL_AGE));
    }

    @Test
    public void loadMaternalAgeNonZero()
    {
        BaseObject data = mock(BaseObject.class);
        doReturn(data).when(this.doc).getXObject(any(EntityReference.class));

        doReturn(AGE_NON_ZERO).when(data).getIntValue(MATERNAL_AGE, AGE_UNSET);
        doReturn(AGE_ZERO).when(data).getIntValue(PATERNAL_AGE, AGE_UNSET);

        PatientData<Integer> testData = this.parentalAgeController.load(this.patient);

        Assert.assertEquals("parentalAge", testData.getName());
        Assert.assertEquals(AGE_NON_ZERO, testData.get(MATERNAL_AGE));
        Assert.assertEquals(AGE_ZERO, testData.get(PATERNAL_AGE));
    }

    @Test
    public void loadPaternalAgeNonZero()
    {
        BaseObject data = mock(BaseObject.class);
        doReturn(data).when(this.doc).getXObject(any(EntityReference.class));

        doReturn(AGE_UNSET).when(data).getIntValue(MATERNAL_AGE, AGE_UNSET);
        doReturn(AGE_NON_ZERO).when(data).getIntValue(PATERNAL_AGE, AGE_UNSET);

        PatientData<Integer> testData = this.parentalAgeController.load(this.patient);

        Assert.assertEquals("parentalAge", testData.getName());
        Assert.assertNull(testData.get(MATERNAL_AGE));
        Assert.assertEquals(AGE_NON_ZERO, testData.get(PATERNAL_AGE));
    }

    @Test
    public void loadMaternalAndPaternalAgeZero()
    {
        BaseObject data = mock(BaseObject.class);
        doReturn(data).when(this.doc).getXObject(any(EntityReference.class));
        doReturn(AGE_ZERO).when(data).getIntValue(MATERNAL_AGE, AGE_UNSET);
        doReturn(AGE_ZERO).when(data).getIntValue(PATERNAL_AGE, AGE_UNSET);

        PatientData<Integer> testData = this.parentalAgeController.load(this.patient);
        Assert.assertEquals("parentalAge", testData.getName());
        Assert.assertEquals(AGE_ZERO, testData.get(MATERNAL_AGE));
        Assert.assertEquals(AGE_ZERO, testData.get(PATERNAL_AGE));
    }

    @Test
    public void loadMaternalAndPaternalAgeUnset()
    {
        BaseObject data = mock(BaseObject.class);
        doReturn(AGE_UNSET).when(data).getIntValue(MATERNAL_AGE, AGE_UNSET);
        doReturn(AGE_UNSET).when(data).getIntValue(PATERNAL_AGE, AGE_UNSET);
        doReturn(data).when(this.doc).getXObject(any(EntityReference.class));

        Assert.assertNull(this.parentalAgeController.load(this.patient));
    }

    @Test
    public void loadHandlesExceptions() throws Exception
    {
        Exception testException = new RuntimeException("Test Exception");
        doThrow(testException).when(this.patient).getXDocument();

        this.parentalAgeController.load(this.patient);

        verify(this.logger).error(eq(PatientDataController.ERROR_MESSAGE_LOAD_FAILED), anyString());
    }

    @Test
    public void saveDoesNothingIfParentalInformationClassDoesNotExist()
    {
        when(this.doc.getXObject(this.parentalAgeController.getXClassReference(), true, this.xWikiContext))
            .thenReturn(null);
        this.parentalAgeController.save(this.patient);
        verify(this.doc, times(1)).getXObject(this.parentalAgeController.getXClassReference(), true,
            this.xWikiContext);
        verifyNoMoreInteractions(this.doc);
        verifyZeroInteractions(this.patientData);
    }

    @Test
    public void saveDoesNothingWithWrongTypeOfPatient()
    {
        doReturn(this.patientData).when(this.patient).getData(this.parentalAgeController.getName());
        doReturn(false).when(this.patientData).isNamed();
        this.parentalAgeController.save(this.patient);
        verify(this.doc, times(1)).getXObject(this.parentalAgeController.getXClassReference(), true,
            this.xWikiContext);
        verify(this.patientData, times(1)).isNamed();
        verifyNoMoreInteractions(this.doc);
        verifyNoMoreInteractions(this.patientData);
    }

    @Test
    public void saveHandlesEmptyPatientDataTestWithUpdatePolicy()
    {
        doReturn(null).when(this.patient).getData(this.parentalAgeController.getName());

        this.parentalAgeController.save(this.patient, PatientWritePolicy.UPDATE);

        verify(this.doc, times(1)).getXObject(this.parentalAgeController.getXClassReference(), true,
            this.xWikiContext);
        verifyNoMoreInteractions(this.doc);
        verifyZeroInteractions(this.patientData);
    }

    @Test
    public void saveHandlesEmptyPatientDataTestWithMergePolicy()
    {
        doReturn(null).when(this.patient).getData(this.parentalAgeController.getName());

        this.parentalAgeController.save(this.patient, PatientWritePolicy.MERGE);

        verify(this.doc, times(1)).getXObject(this.parentalAgeController.getXClassReference(), true,
            this.xWikiContext);
        verifyNoMoreInteractions(this.doc);
        verifyZeroInteractions(this.patientData);
    }

    @Test
    public void saveHandlesEmptyPatientDataTestWithReplacePolicy()
    {
        doReturn(null).when(this.patient).getData(this.parentalAgeController.getName());

        this.parentalAgeController.save(this.patient, PatientWritePolicy.REPLACE);

        verify(this.doc, times(1)).getXObject(this.parentalAgeController.getXClassReference(), true,
            this.xWikiContext);
        verify(this.dataHolder, times(1)).set(MATERNAL_AGE, null, this.xWikiContext);
        verify(this.dataHolder, times(1)).set(PATERNAL_AGE, null, this.xWikiContext);
        verifyNoMoreInteractions(this.doc);
        verifyZeroInteractions(this.patientData);
    }

    @Test
    public void saveDefaultBehaviourTestWithUpdatePolicy()
    {
        doReturn(this.patientData).when(this.patient).getData(this.parentalAgeController.getName());
        doReturn(true).when(this.patientData).isNamed();

        when(this.patientData.containsKey(MATERNAL_AGE)).thenReturn(false);
        when(this.patientData.containsKey(PATERNAL_AGE)).thenReturn(true);
        when(this.patientData.get(PATERNAL_AGE)).thenReturn(AGE_NON_ZERO);
        when(this.patientData.get(MATERNAL_AGE)).thenReturn(null);

        this.parentalAgeController.save(this.patient, PatientWritePolicy.UPDATE);

        verify(this.doc, times(1)).getXObject(this.parentalAgeController.getXClassReference(), true,
            this.xWikiContext);
        verify(this.dataHolder, never()).set(eq(MATERNAL_AGE), any(), eq(this.xWikiContext));
        verify(this.dataHolder, times(1)).set(PATERNAL_AGE, AGE_NON_ZERO, this.xWikiContext);
        verify(this.patientData, times(1)).isNamed();
        verify(this.patientData, times(1)).containsKey(MATERNAL_AGE);
        verify(this.patientData, times(1)).containsKey(PATERNAL_AGE);
        verify(this.patientData, times(1)).get(PATERNAL_AGE);
        verify(this.patientData, never()).get(MATERNAL_AGE);

        verifyNoMoreInteractions(this.doc);
        verifyNoMoreInteractions(this.patientData);
    }

    @Test
    public void saveDefaultBehaviourTestWithMergePolicy()
    {
        doReturn(this.patientData).when(this.patient).getData(this.parentalAgeController.getName());
        doReturn(true).when(this.patientData).isNamed();

        when(this.patientData.containsKey(MATERNAL_AGE)).thenReturn(false);
        when(this.patientData.containsKey(PATERNAL_AGE)).thenReturn(true);
        when(this.patientData.get(PATERNAL_AGE)).thenReturn(AGE_NON_ZERO);
        when(this.patientData.get(MATERNAL_AGE)).thenReturn(null);

        this.parentalAgeController.save(this.patient, PatientWritePolicy.MERGE);

        verify(this.doc, times(1)).getXObject(this.parentalAgeController.getXClassReference(), true,
            this.xWikiContext);
        verify(this.dataHolder, never()).set(eq(MATERNAL_AGE), any(), eq(this.xWikiContext));
        verify(this.dataHolder, times(1)).set(PATERNAL_AGE, AGE_NON_ZERO, this.xWikiContext);
        verify(this.patientData, times(1)).isNamed();
        verify(this.patientData, times(1)).containsKey(MATERNAL_AGE);
        verify(this.patientData, times(1)).containsKey(PATERNAL_AGE);
        verify(this.patientData, times(1)).get(PATERNAL_AGE);
        verify(this.patientData, never()).get(MATERNAL_AGE);

        verifyNoMoreInteractions(this.doc);
        verifyNoMoreInteractions(this.patientData);
    }

    @Test
    public void saveDefaultBehaviourTestWithReplacePolicy()
    {
        doReturn(this.patientData).when(this.patient).getData(this.parentalAgeController.getName());
        doReturn(true).when(this.patientData).isNamed();

        when(this.patientData.get(PATERNAL_AGE)).thenReturn(AGE_NON_ZERO);
        when(this.patientData.get(MATERNAL_AGE)).thenReturn(null);

        this.parentalAgeController.save(this.patient, PatientWritePolicy.REPLACE);

        verify(this.doc, times(1)).getXObject(this.parentalAgeController.getXClassReference(), true,
            this.xWikiContext);
        verify(this.dataHolder, times(1)).set(MATERNAL_AGE, null, this.xWikiContext);
        verify(this.dataHolder, times(1)).set(PATERNAL_AGE, AGE_NON_ZERO, this.xWikiContext);
        verify(this.patientData, times(1)).isNamed();
        verify(this.patientData, never()).containsKey(MATERNAL_AGE);
        verify(this.patientData, never()).containsKey(PATERNAL_AGE);
        verify(this.patientData, times(1)).get(PATERNAL_AGE);
        verify(this.patientData, times(1)).get(MATERNAL_AGE);

        verifyNoMoreInteractions(this.doc);
        verifyNoMoreInteractions(this.patientData);
    }

    @Test
    public void writeJSONPatientWithNoData()
    {
        JSONObject json = new JSONObject();
        this.parentalAgeController.writeJSON(this.patient, json, null);
        Assert.assertEquals(0, json.length());
    }

    @Test
    public void writeJSONDefaultBehaviour()
    {
        JSONObject json = new JSONObject();
        Map<String, Integer> testData = new LinkedHashMap<>();
        testData.put(MATERNAL_AGE, AGE_NON_ZERO);
        testData.put(PATERNAL_AGE, AGE_NON_ZERO);
        PatientData<Integer> testPatientData =
            new DictionaryPatientData<>(this.parentalAgeController.getName(), testData);
        doReturn(testPatientData).when(this.patient).getData(this.parentalAgeController.getName());
        JSONObject jsonTestData = new JSONObject();
        jsonTestData.put("prenatal_perinatal_history", testData);

        this.parentalAgeController.writeJSON(this.patient, json);

        Assert.assertNotNull(json);
        Assert.assertTrue(jsonTestData.getJSONObject("prenatal_perinatal_history").similar(
            json.getJSONObject("prenatal_perinatal_history")));
    }

    @Test
    public void writeJSONSelectedFieldsWithoutParentalAge()
    {
        JSONObject json = new JSONObject();
        Collection<String> fieldList = new ArrayList<>();
        fieldList.add("test field");

        this.parentalAgeController.writeJSON(this.patient, json, fieldList);
        Assert.assertEquals(0, json.length());
    }

    @Test
    public void readEmptyJSONObject()
    {
        JSONObject json = new JSONObject();
        PatientData<Integer> readData = this.parentalAgeController.readJSON(json);
        Assert.assertNull(readData);
    }

    @Test
    public void readJSONObjectWithNoData()
    {
        JSONObject json = new JSONObject();
        json.put("prenatal_perinatal_history", (Object) null);
        PatientData<Integer> readData = this.parentalAgeController.readJSON(json);
        Assert.assertNull(readData);
    }

    @Test
    public void readJSONDefaultBehaviour()
    {
        JSONObject json = new JSONObject();
        JSONObject data = new JSONObject();
        data.put(MATERNAL_AGE, AGE_NON_ZERO);
        data.put(PATERNAL_AGE, AGE_NON_ZERO);
        json.put("prenatal_perinatal_history", data);

        PatientData<Integer> readData = this.parentalAgeController.readJSON(json);
        Assert.assertNotNull(readData);
        Assert.assertEquals(AGE_NON_ZERO, readData.get(MATERNAL_AGE));
        Assert.assertEquals(AGE_NON_ZERO, readData.get(PATERNAL_AGE));
        Assert.assertEquals(this.parentalAgeController.getName(), readData.getName());
    }
}
