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

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.Cancer;
import org.phenotips.data.CancerQualifier;
import org.phenotips.data.DictionaryPatientData;
import org.phenotips.data.Feature;
import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.PatientWritePolicy;
import org.phenotips.vocabulary.VocabularyManager;

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.diff.DiffManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseStringProperty;
import com.xpn.xwiki.web.Utils;

import net.jcip.annotations.NotThreadSafe;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Test for the {@link CancersController} Component.
 */
@NotThreadSafe
public class CancersControllerTest
{
    private static final String CANCERS = "cancers";

    private static final String CANCER_1 = "HP:001";

    private static final String CANCER_1_NAME = "Cancer1";

    private static final String CANCER_2 = "nonStandard";

    private static final String CANCER_2_NAME = CANCER_2;

    private static final String CANCER_3 = "HP:003";

    private static final String CANCER_3_NAME = "Cancer3";

    private static final String ID_LABEL = "id";

    private static final String AFFECTED_LABEL = "affected";

    private static final String CANCER_LABEL = "cancer";

    private static final String AGE_AT_DIAGNOSIS_LABEL = "ageAtDiagnosis";

    private static final String NUMERIC_AGE_AT_DIAGNOSIS_LABEL = "numericAgeAtDiagnosis";

    private static final String PRIMARY_LABEL = "primary";

    private static final String LATERALITY_LABEL = "laterality";

    private static final String NOTES_LABEL = "notes";

    private static final String QUALIFIERS_LABEL = "qualifiers";

    @Rule
    public MockitoComponentMockingRule<PatientDataController<Cancer>> mocker =
        new MockitoComponentMockingRule<>(CancersController.class);

    @Mock
    private ComponentManager cm;

    @Mock
    private Provider<ComponentManager> mockProvider;

    @Mock
    private VocabularyManager vm;

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    @Mock
    private XWikiContext context;

    @Mock
    private BaseObject cancerData;

    @Mock
    private BaseObject qualifierData;

    @Mock
    private Cancer cancer1;

    @Mock
    private Cancer cancer2;

    @Mock
    private Cancer cancer3;

    @Mock
    private CancerQualifier cancerQualifier1;

    @Mock
    private CancerQualifier cancerQualifier2;

    @Mock
    private CancerQualifier cancerQualifier3;

    @Mock
    private CancerQualifier cancerQualifier4;

    @Mock
    private BaseStringProperty field;

    private PatientDataController<Cancer> component;

    private Logger logger;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        Utils.setComponentManager(this.cm);
        ReflectionUtils.setFieldValue(new ComponentManagerRegistry(), "cmProvider", this.mockProvider);
        when(this.mockProvider.get()).thenReturn(this.cm);
        when(this.cm.getInstance(DiffManager.class)).thenReturn(null);
        when(this.cm.getInstance(VocabularyManager.class)).thenReturn(this.vm);

        this.component = this.mocker.getComponentUnderTest();
        this.logger = this.mocker.getMockedLogger();

        final Provider<XWikiContext> xcontextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(this.context);

        final DocumentReference patientDocRef = new DocumentReference("wiki", "patient", "00000001");
        when(this.patient.getDocumentReference()).thenReturn(patientDocRef);
        when(this.patient.getXDocument()).thenReturn(this.doc);

        when(this.doc.newXObject(Cancer.CLASS_REFERENCE, this.context)).thenReturn(this.cancerData);
        when(this.doc.newXObject(CancerQualifier.CLASS_REFERENCE, this.context)).thenReturn(this.qualifierData);

        // Set up cancer and qualifier objects
        final Set<CancerQualifier> qualifiers1 = new HashSet<>();
        qualifiers1.add(this.cancerQualifier1);
        qualifiers1.add(this.cancerQualifier2);

        when(this.cancer1.getQualifiers()).thenReturn(qualifiers1);
        when(this.cancer1.getId()).thenReturn(CANCER_1);
        when(this.cancer1.getName()).thenReturn(CANCER_1_NAME);
        when(this.cancer1.getProperty(Cancer.CancerProperty.CANCER)).thenReturn(CANCER_1);
        when(this.cancer1.getProperty(Cancer.CancerProperty.AFFECTED)).thenReturn(true);

        final Set<CancerQualifier> qualifiers2 = new HashSet<>();
        qualifiers2.add(this.cancerQualifier3);

        when(this.cancer2.getQualifiers()).thenReturn(qualifiers2);
        when(this.cancer2.getId()).thenReturn(null);
        when(this.cancer2.getName()).thenReturn(CANCER_2_NAME);
        when(this.cancer2.getProperty(Cancer.CancerProperty.CANCER)).thenReturn(CANCER_2);
        when(this.cancer2.getProperty(Cancer.CancerProperty.AFFECTED)).thenReturn(true);

        final Set<CancerQualifier> qualifiers3 = new HashSet<>();
        qualifiers3.add(this.cancerQualifier4);

        when(this.cancer3.getQualifiers()).thenReturn(qualifiers3);
        when(this.cancer3.getId()).thenReturn(CANCER_3);
        when(this.cancer3.getName()).thenReturn(CANCER_3_NAME);
        when(this.cancer3.getProperty(Cancer.CancerProperty.CANCER)).thenReturn(CANCER_3);
        when(this.cancer3.getProperty(Cancer.CancerProperty.AFFECTED)).thenReturn(false);

        when(this.cancerData.getFieldList()).thenReturn(Arrays.asList(ID_LABEL, AFFECTED_LABEL));
        when(this.qualifierData.getFieldList()).thenReturn(Arrays.asList(CANCER_LABEL, AGE_AT_DIAGNOSIS_LABEL,
            NUMERIC_AGE_AT_DIAGNOSIS_LABEL, PRIMARY_LABEL, LATERALITY_LABEL));

        when(this.cancer1.mergeData(this.cancer1)).thenReturn(this.cancer1);
        when(this.cancer2.mergeData(this.cancer2)).thenReturn(this.cancer2);
        when(this.cancer3.mergeData(this.cancer3)).thenReturn(this.cancer3);
    }

    @Test
    public void saveWrongDataFormat()
    {
        final PatientData<Feature> data = new DictionaryPatientData<>(CANCERS, Collections.emptyMap());
        doReturn(data).when(this.patient).getData(CANCERS);

        this.component.save(this.patient);

        verify(this.patient, times(1)).getXDocument();
        verify(this.patient, times(1)).getData(CANCERS);

        verify(this.logger, times(1))
            .error(PatientDataController.ERROR_MESSAGE_DATA_IN_MEMORY_IN_WRONG_FORMAT);

        verifyNoMoreInteractions(this.patient);
        verifyZeroInteractions(this.doc, this.cancerData, this.qualifierData);
    }

    @Test
    public void savePatientDataIsNullWithUpdatePolicy()
    {
        when(this.patient.getData(CANCERS)).thenReturn(null);

        this.component.save(this.patient, PatientWritePolicy.UPDATE);

        verify(this.patient, times(1)).getData(CANCERS);
        verify(this.patient, times(1)).getXDocument();

        verifyNoMoreInteractions(this.patient);
        verifyZeroInteractions(this.doc, this.cancerData, this.qualifierData);
    }

    @Test
    public void savePatientDataIsNullWithMergePolicy()
    {
        when(this.patient.getData(CANCERS)).thenReturn(null);

        this.component.save(this.patient, PatientWritePolicy.MERGE);

        verify(this.patient, times(1)).getData(CANCERS);
        verify(this.patient, times(1)).getXDocument();

        verifyNoMoreInteractions(this.patient);
        verifyZeroInteractions(this.doc, this.cancerData, this.qualifierData);
    }

    @Test
    public void savePatientDataIsNullWithReplacePolicy()
    {
        when(this.patient.getData(CANCERS)).thenReturn(null);

        this.component.save(this.patient, PatientWritePolicy.REPLACE);

        verify(this.patient, times(1)).getData(CANCERS);
        verify(this.patient, times(1)).getXDocument();
        verify(this.doc, times(1)).removeXObjects(Cancer.CLASS_REFERENCE);
        verify(this.doc, times(1)).removeXObjects(CancerQualifier.CLASS_REFERENCE);

        verifyNoMoreInteractions(this.patient, this.doc);
        verifyZeroInteractions(this.cancerData, this.qualifierData);
    }

    @Test
    public void saveNonNullPatientDataWithUpdatePolicy()
    {
        final List<Cancer> cancerList = Arrays.asList(this.cancer1, this.cancer2);
        final PatientData<Cancer> data = new IndexedPatientData<>(CANCERS, cancerList);
        doReturn(data).when(this.patient).getData(CANCERS);

        this.component.save(this.patient, PatientWritePolicy.UPDATE);

        verify(this.patient, times(1)).getData(CANCERS);
        verify(this.patient, times(1)).getXDocument();

        verify(this.doc, times(1)).removeXObjects(Cancer.CLASS_REFERENCE);
        verify(this.doc, times(1)).removeXObjects(CancerQualifier.CLASS_REFERENCE);

        verify(this.cancer1, times(1)).write(this.doc, this.context);
        verify(this.cancer2, times(1)).write(this.doc, this.context);

        verifyNoMoreInteractions(this.patient, this.doc);
    }

    @Test
    public void saveNonNullPatientDataWithMergePolicyNoDataStored()
    {
        final List<Cancer> cancerList = Arrays.asList(this.cancer1, this.cancer2);
        final PatientData<Cancer> data = new IndexedPatientData<>(CANCERS, cancerList);
        doReturn(data).when(this.patient).getData(CANCERS);

        when(this.doc.getXObjects(Cancer.CLASS_REFERENCE)).thenReturn(null);

        this.component.save(this.patient, PatientWritePolicy.MERGE);

        verify(this.patient, times(1)).getData(CANCERS);
        // One on save, one on load.
        verify(this.patient, times(2)).getXDocument();

        verify(this.doc, times(1)).getXObjects(Cancer.CLASS_REFERENCE);
        verify(this.doc, times(1)).removeXObjects(Cancer.CLASS_REFERENCE);
        verify(this.doc, times(1)).removeXObjects(CancerQualifier.CLASS_REFERENCE);

        verify(this.cancer1, times(1)).write(this.doc, this.context);
        verify(this.cancer2, times(1)).write(this.doc, this.context);

        verifyNoMoreInteractions(this.patient, this.doc);
    }

    @Test
    public void saveNonNullPatientDataWithMergePolicyNoOverlap() throws XWikiException
    {
        final List<Cancer> cancerList = Arrays.asList(this.cancer1, this.cancer2);
        final PatientData<Cancer> data = new IndexedPatientData<>(CANCERS, cancerList);
        doReturn(data).when(this.patient).getData(CANCERS);

        final PatientDataController<Cancer> spyController = spy(this.component);
        doReturn(new IndexedPatientData<>(CANCERS, Collections.singletonList(this.cancer3))).when(spyController)
            .load(this.patient);

        spyController.save(this.patient, PatientWritePolicy.MERGE);

        verify(this.patient, times(1)).getData(CANCERS);
        verify(this.patient, times(1)).getXDocument();

        verify(this.doc, times(1)).removeXObjects(Cancer.CLASS_REFERENCE);
        verify(this.doc, times(1)).removeXObjects(CancerQualifier.CLASS_REFERENCE);

        verify(this.cancer1, times(1)).write(this.doc, this.context);
        verify(this.cancer2, times(1)).write(this.doc, this.context);
        verify(this.cancer3, times(1)).write(this.doc, this.context);

        verifyNoMoreInteractions(this.patient, this.doc);
    }

    @Test
    public void saveNonNullPatientDataWithMergePolicyWithOverlap()
    {
        final List<Cancer> cancerList = Arrays.asList(this.cancer1, this.cancer2);
        final PatientData<Cancer> data = new IndexedPatientData<>(CANCERS, cancerList);
        doReturn(data).when(this.patient).getData(CANCERS);

        final PatientDataController<Cancer> spyController = spy(this.component);
        final PatientData<Cancer> storedCancers = new IndexedPatientData<>(CANCERS,
            Collections.singletonList(this.cancer1));
        doReturn(storedCancers).when(spyController).load(this.patient);

        spyController.save(this.patient, PatientWritePolicy.MERGE);

        verify(this.patient, times(1)).getData(CANCERS);
        verify(this.patient, times(1)).getXDocument();

        verify(this.doc, times(1)).removeXObjects(Cancer.CLASS_REFERENCE);
        verify(this.doc, times(1)).removeXObjects(CancerQualifier.CLASS_REFERENCE);

        verify(this.cancer1, times(1)).write(this.doc, this.context);
        verify(this.cancer2, times(1)).write(this.doc, this.context);

        verifyNoMoreInteractions(this.patient, this.doc);
    }

    @Test
    public void saveNonNullPatientDataWithReplacePolicy() throws XWikiException
    {
        final List<Cancer> cancerList = Arrays.asList(this.cancer1, this.cancer2);
        final PatientData<Cancer> data = new IndexedPatientData<>(CANCERS, cancerList);
        doReturn(data).when(this.patient).getData(CANCERS);

        this.component.save(this.patient, PatientWritePolicy.REPLACE);

        verify(this.patient, times(1)).getData(CANCERS);
        verify(this.patient, times(1)).getXDocument();

        verify(this.doc, times(1)).removeXObjects(Cancer.CLASS_REFERENCE);
        verify(this.doc, times(1)).removeXObjects(CancerQualifier.CLASS_REFERENCE);

        verify(this.cancer1, times(1)).write(this.doc, this.context);
        verify(this.cancer2, times(1)).write(this.doc, this.context);

        verifyNoMoreInteractions(this.patient, this.doc);
    }

    @Test
    public void loadNoData()
    {
        when(this.doc.getXObjects(Cancer.CLASS_REFERENCE)).thenReturn(Collections.emptyList());
        Assert.assertNull(this.component.load(this.patient));
    }

    @Test
    public void loadCatchesExceptions()
    {
        when(this.doc.getXObjects(Cancer.CLASS_REFERENCE)).thenThrow(new RuntimeException(CANCERS));
        Assert.assertNull(this.component.load(this.patient));
        verify(this.logger, times(1)).error(PatientDataController.ERROR_MESSAGE_LOAD_FAILED, CANCERS);
    }

    @Test
    public void loadFiltersNulls()
    {
        when(this.qualifierData.getFieldList()).thenReturn(Collections.emptyList());

        when(this.cancerData.getStringValue(CANCER_LABEL)).thenReturn(CANCER_1);
        when(this.cancerData.getField(CANCER_LABEL)).thenReturn(this.field);

        when(this.qualifierData.getStringValue(CANCER_LABEL)).thenReturn(CANCER_2);
        when(this.qualifierData.getField(CANCER_LABEL)).thenReturn(this.field);

        when(this.field.getValue()).thenReturn(CANCER_1, CANCER_2);

        when(this.doc.getXObjects(Cancer.CLASS_REFERENCE)).thenReturn(Arrays.asList(null, this.cancerData,
            this.qualifierData));

        final PatientData<Cancer> data = this.component.load(this.patient);
        Assert.assertEquals(1, data.size());
        Assert.assertEquals(CANCER_1, data.get(0).getId());
    }

    @Test
    public void writeJSONNotASelectedField()
    {
        final JSONObject jsonObject = new JSONObject();
        this.component.writeJSON(this.patient, jsonObject, Collections.emptyList());
        verify(this.patient, Mockito.never()).getData(anyString());
        Assert.assertEquals(0, jsonObject.length());
    }

    @Test
    public void writeJSONIsASelectedFieldNullData()
    {
        when(this.patient.getData(CANCERS)).thenReturn(null);

        final JSONObject jsonObject = new JSONObject();
        this.component.writeJSON(this.patient, jsonObject, Collections.singletonList(CANCERS));
        verify(this.patient, times(1)).getData(CANCERS);
        Assert.assertEquals(1, jsonObject.length());
        Assert.assertTrue(new JSONArray().similar(jsonObject.getJSONArray(CANCERS)));
    }

    @Test
    public void writeJSONIsASelectedFieldNoData()
    {
        when(this.patient.getData(CANCERS)).thenReturn(new IndexedPatientData<>(CANCERS, Collections.emptyList()));

        final JSONObject jsonObject = new JSONObject();
        this.component.writeJSON(this.patient, jsonObject, Collections.singletonList(CANCERS));
        verify(this.patient, times(1)).getData(CANCERS);
        Assert.assertEquals(1, jsonObject.length());
        Assert.assertTrue(new JSONArray().similar(jsonObject.getJSONArray(CANCERS)));
    }

    @Test
    public void writeJSONIsASelectedFieldWrongData()
    {
        when(this.patient.getData(CANCERS)).thenReturn(new DictionaryPatientData<>(CANCERS, Collections.emptyMap()));

        final JSONObject jsonObject = new JSONObject();
        this.component.writeJSON(this.patient, jsonObject, Collections.singletonList(CANCERS));
        verify(this.patient, times(1)).getData(CANCERS);
        Assert.assertEquals(1, jsonObject.length());
        Assert.assertTrue(new JSONArray().similar(jsonObject.getJSONArray(CANCERS)));
    }

    @Test
    public void writeJSONIsASelectedFieldWithData()
    {
        when(this.patient.getData(CANCERS)).thenReturn(
            new IndexedPatientData<>(CANCERS, Collections.singletonList(this.cancer1)));
        when(this.cancer1.toJSON()).thenReturn(new JSONObject().put(ID_LABEL, CANCER_1));

        final JSONObject jsonObject = new JSONObject();
        this.component.writeJSON(this.patient, jsonObject, Collections.singletonList(CANCERS));
        verify(this.patient, times(1)).getData(CANCERS);
        Assert.assertEquals(1, jsonObject.length());
        Assert.assertTrue(new JSONArray().put(new JSONObject().put(ID_LABEL, CANCER_1))
            .similar(jsonObject.getJSONArray(CANCERS)));
    }


    @Test
    public void writeJSONIsASelectedFieldWrongDataNoId()
    {
        when(this.patient.getData(CANCERS)).thenReturn(
            new IndexedPatientData<>(CANCERS, Collections.singletonList(this.cancer1)));
        when(this.cancer1.getId()).thenReturn(null);
        when(this.cancer1.getName()).thenReturn(null);
        when(this.cancer1.toJSON()).thenReturn(new JSONObject().put(ID_LABEL, CANCER_1));

        final JSONObject jsonObject = new JSONObject();
        this.component.writeJSON(this.patient, jsonObject, Collections.singletonList(CANCERS));
        verify(this.patient, times(1)).getData(CANCERS);
        Assert.assertEquals(1, jsonObject.length());
        Assert.assertTrue(new JSONArray().similar(jsonObject.getJSONArray(CANCERS)));
    }

    @Test
    public void readJSONNull()
    {
        Assert.assertNull(this.component.readJSON(null));
    }

    @Test
    public void readJSONNoData()
    {
        final JSONObject data = new JSONObject().put(CANCERS, (String) null);
        Assert.assertNull(this.component.readJSON(data));
        Assert.assertNull(this.component.readJSON(new JSONObject()));
    }

    @Test
    public void readJSONHasData()
    {
        final JSONObject qualifier1Json = new JSONObject()
            .put(CANCER_LABEL, CANCER_1)
            .put(AGE_AT_DIAGNOSIS_LABEL, "22")
            .put(NUMERIC_AGE_AT_DIAGNOSIS_LABEL, 22)
            .put(PRIMARY_LABEL, true)
            .put(LATERALITY_LABEL, "l");
        final JSONObject qualifier2Json = new JSONObject()
            .put(CANCER_LABEL, CANCER_1)
            .put(PRIMARY_LABEL, false)
            .put(LATERALITY_LABEL, StringUtils.EMPTY)
            .put(NOTES_LABEL, NOTES_LABEL);
        final JSONObject cancer1Json =
            new JSONObject()
                .put(ID_LABEL, CANCER_1)
                .put(AFFECTED_LABEL, true)
                .put(QUALIFIERS_LABEL, new JSONArray().put(qualifier1Json).put(qualifier2Json));

        final JSONArray cancers = new JSONArray().put(cancer1Json);
        final JSONObject data = new JSONObject().put(CANCERS, cancers);

        final PatientData<Cancer> retrieved = this.component.readJSON(data);
        Assert.assertEquals(1, retrieved.size());

        final Cancer cancer = retrieved.get(0);
        Assert.assertEquals(CANCER_1, cancer.getId());
        Assert.assertEquals(CANCER_1, cancer.getName());
        Assert.assertTrue(cancer.isAffected());

        final Collection<CancerQualifier> qualifiers = cancer.getQualifiers();
        Assert.assertEquals(2, qualifiers.size());
        qualifiers.forEach(qualifier -> {
            final Integer ageNum =
                (Integer) qualifier.getProperty(CancerQualifier.CancerQualifierProperty.NUMERIC_AGE_AT_DIAGNOSIS);
            if (ageNum == null) {
                Assert.assertEquals(NOTES_LABEL, qualifier.getProperty(CancerQualifier.CancerQualifierProperty.NOTES));
                Assert.assertEquals(CANCER_1, qualifier.getProperty(CancerQualifier.CancerQualifierProperty.CANCER));
                Assert.assertEquals(StringUtils.EMPTY,
                    qualifier.getProperty(CancerQualifier.CancerQualifierProperty.LATERALITY));
                Assert.assertFalse((Boolean) qualifier.getProperty(CancerQualifier.CancerQualifierProperty.PRIMARY));
            } else if (ageNum.intValue() == 22) {
                Assert.assertEquals(22,
                    qualifier.getProperty(CancerQualifier.CancerQualifierProperty.NUMERIC_AGE_AT_DIAGNOSIS));
                Assert.assertEquals("22",
                    qualifier.getProperty(CancerQualifier.CancerQualifierProperty.AGE_AT_DIAGNOSIS));
                Assert.assertEquals(CANCER_1, qualifier.getProperty(CancerQualifier.CancerQualifierProperty.CANCER));
                Assert.assertEquals("l", qualifier.getProperty(CancerQualifier.CancerQualifierProperty.LATERALITY));
                Assert.assertTrue((Boolean) qualifier.getProperty(CancerQualifier.CancerQualifierProperty.PRIMARY));

            } else {
                Assert.fail();
            }
        });
    }
}
