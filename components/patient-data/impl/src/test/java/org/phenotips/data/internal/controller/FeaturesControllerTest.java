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
import org.phenotips.data.Feature;
import org.phenotips.data.FeatureMetadatum;
import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.PatientWritePolicy;
import org.phenotips.data.internal.PhenoTipsFeature;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Test for the {@link FeaturesController} Component, only the overridden methods from {@link AbstractComplexController}
 * are tested here.
 */
public class FeaturesControllerTest
{
    private static final String PHENOTYPE_POSITIVE_PROPERTY = "phenotype";

    private static final String PHENOTYPE_NEGATIVE_PROPERTY = PhenoTipsFeature.NEGATIVE_PHENOTYPE_PREFIX
        + PHENOTYPE_POSITIVE_PROPERTY;

    private static final String PRENATAL_PHENOTYPE_PREFIX = "prenatal_";

    private static final String PRENATAL_PHENOTYPE_PROPERTY = PRENATAL_PHENOTYPE_PREFIX + PHENOTYPE_POSITIVE_PROPERTY;

    private static final String NEGATIVE_PRENATAL_PHENOTYPE_PROPERTY = PhenoTipsFeature.NEGATIVE_PHENOTYPE_PREFIX
        + PRENATAL_PHENOTYPE_PROPERTY;

    private static final String FEATURE_1 = "feature1";

    private static final String FEATURE_2 = "feature2";

    private static final String FEATURE_3 = "feature3";

    private static final String FEATURE_4 = "feature4";

    private static final String AGE_OF_ONSET = "age_of_onset";

    private static final String PACE_OF_PROGRESSION = "pace_of_progression";

    private static final String METADATUM_1 = "metadatum1";

    private static final String METADATUM_2 = "metadatum2";

    @Rule
    public MockitoComponentMockingRule<PatientDataController<Feature>> mocker =
        new MockitoComponentMockingRule<>(FeaturesController.class);

    @Mock
    private Patient patient;

    @Mock
    private XWikiDocument doc;

    @Mock
    private XWikiContext context;

    @Mock
    private BaseObject dataHolder;

    @Mock
    private Feature feature1;

    @Mock
    private Feature feature2;

    @Mock
    private Feature feature3;

    @Mock
    private Feature feature4;

    @Mock
    private FeatureMetadatum featureMetadatum1;

    @Mock
    private FeatureMetadatum featureMetadatum2;

    private PatientDataController<Feature> component;

    private Logger logger;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.component = this.mocker.getComponentUnderTest();
        this.logger = this.mocker.getMockedLogger();

        final Provider<XWikiContext> xcontextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(this.context);

        final DocumentReference patientDocRef = new DocumentReference("wiki", "patient", "00000001");
        doReturn(patientDocRef).when(this.patient).getDocumentReference();
        doReturn(this.doc).when(this.patient).getXDocument();
        when(this.doc.getXObject(Patient.CLASS_REFERENCE, true, this.context)).thenReturn(this.dataHolder);

        final Map<String, FeatureMetadatum> metadatum2 = new HashMap<>();
        metadatum2.put(AGE_OF_ONSET, this.featureMetadatum1);

        final Map<String, FeatureMetadatum> metadatum3 = new HashMap<>();
        metadatum3.put(PACE_OF_PROGRESSION, this.featureMetadatum2);

        when(this.featureMetadatum1.getType()).thenReturn(AGE_OF_ONSET);
        when(this.featureMetadatum1.getId()).thenReturn(METADATUM_1);

        when(this.featureMetadatum2.getType()).thenReturn(PACE_OF_PROGRESSION);
        when(this.featureMetadatum2.getId()).thenReturn(METADATUM_2);

        when(this.feature1.getType()).thenReturn(PRENATAL_PHENOTYPE_PROPERTY);
        when(this.feature1.isPresent()).thenReturn(true);
        when(this.feature1.getValue()).thenReturn(FEATURE_1);
        when(this.feature1.getPropertyName()).thenReturn(PRENATAL_PHENOTYPE_PROPERTY);

        when(this.feature2.getType()).thenReturn(PHENOTYPE_POSITIVE_PROPERTY);
        when(this.feature2.isPresent()).thenReturn(true);
        doReturn(metadatum2).when(this.feature2).getMetadata();
        when(this.feature2.getValue()).thenReturn(FEATURE_2);
        when(this.feature2.getPropertyName()).thenReturn(PHENOTYPE_POSITIVE_PROPERTY);

        when(this.feature3.getType()).thenReturn(PHENOTYPE_POSITIVE_PROPERTY);
        when(this.feature3.isPresent()).thenReturn(true);
        doReturn(metadatum3).when(this.feature3).getMetadata();
        when(this.feature3.getValue()).thenReturn(FEATURE_3);
        when(this.feature3.getPropertyName()).thenReturn(PHENOTYPE_POSITIVE_PROPERTY);

        when(this.feature4.getType()).thenReturn(PHENOTYPE_POSITIVE_PROPERTY);
        when(this.feature4.isPresent()).thenReturn(false);
        when(this.feature4.getValue()).thenReturn(FEATURE_4);
        when(this.feature4.getPropertyName()).thenReturn(PHENOTYPE_NEGATIVE_PROPERTY);
    }

    @Test
    public void saveDoesNothingWhenPatientHasNoPatientClass()
    {
        when(this.doc.getXObject(Patient.CLASS_REFERENCE)).thenReturn(null);

        this.component.save(this.patient);
        verifyZeroInteractions(this.dataHolder);
    }

    @Test
    public void saveDoesNothingWhenDataIsInWrongFormat()
    {
        final PatientData<Feature> data = new DictionaryPatientData<>(this.component.getName(), Collections.emptyMap());
        doReturn(data).when(this.patient).getData(this.component.getName());

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
    public void saveNullsExistingDataWhenPatientDataIsNullAndPolicyIsReplace()
    {
        when(this.patient.getData(this.component.getName())).thenReturn(null);

        this.component.save(this.patient, PatientWritePolicy.REPLACE);

        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.context);
        verify(this.doc, times(1)).removeXObjects(FeatureMetadatum.CLASS_REFERENCE);
        verify(this.doc, times(1)).removeXObjects(PhenoTipsFeature.CATEGORY_CLASS_REFERENCE);

        verify(this.dataHolder, times(1)).set(PHENOTYPE_POSITIVE_PROPERTY, null, this.context);
        verify(this.dataHolder, times(1)).set(PHENOTYPE_NEGATIVE_PROPERTY, null, this.context);
        verify(this.dataHolder, times(1)).set(PRENATAL_PHENOTYPE_PROPERTY, null, this.context);
        verify(this.dataHolder, times(1)).set(NEGATIVE_PRENATAL_PHENOTYPE_PROPERTY, null, this.context);

        verifyNoMoreInteractions(this.doc, this.dataHolder);
    }

    @Test
    public void saveReplacesFeaturesWhenPolicyIsUpdate() throws XWikiException
    {
        final List<Feature> featureList = Arrays.asList(this.feature1, this.feature2, this.feature3, this.feature4);
        final PatientData<Feature> data = new IndexedPatientData<>(this.component.getName(), featureList);
        doReturn(data).when(this.patient).getData(this.component.getName());

        this.component.save(this.patient, PatientWritePolicy.UPDATE);

        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.context);
        verify(this.doc, times(1)).removeXObjects(FeatureMetadatum.CLASS_REFERENCE);
        verify(this.doc, times(1)).removeXObjects(PhenoTipsFeature.CATEGORY_CLASS_REFERENCE);
        verify(this.doc, times(2)).newXObject(FeatureMetadatum.CLASS_REFERENCE, this.context);

        final List<String> phenotypeIds = Arrays.asList(FEATURE_2, FEATURE_3);
        final List<String> negPhenotypeIds = Collections.singletonList(FEATURE_4);
        final List<String> prenatalPhenotype = Collections.singletonList(FEATURE_1);

        verify(this.dataHolder, times(1)).set(PHENOTYPE_POSITIVE_PROPERTY, null, this.context);
        verify(this.dataHolder, times(1)).set(PHENOTYPE_NEGATIVE_PROPERTY, null, this.context);
        verify(this.dataHolder, times(1)).set(PRENATAL_PHENOTYPE_PROPERTY, null, this.context);
        verify(this.dataHolder, times(1)).set(NEGATIVE_PRENATAL_PHENOTYPE_PROPERTY, null, this.context);

        verify(this.dataHolder, times(1)).set(PHENOTYPE_POSITIVE_PROPERTY, phenotypeIds, this.context);
        verify(this.dataHolder, times(1)).set(PHENOTYPE_NEGATIVE_PROPERTY, negPhenotypeIds, this.context);
        verify(this.dataHolder, times(1)).set(PRENATAL_PHENOTYPE_PROPERTY, prenatalPhenotype, this.context);

        verifyNoMoreInteractions(this.doc, this.dataHolder);
    }

    @Test
    public void saveMergesFeaturesWhenPolicyIsMerge() throws XWikiException
    {
        final PatientDataController<Feature> controller = spy(this.component);

        when(this.feature4.getValue()).thenReturn(FEATURE_2);

        final List<Feature> featureList = Arrays.asList(this.feature1, this.feature2);
        final IndexedPatientData<Feature> data = new IndexedPatientData<>(this.component.getName(), featureList);
        doReturn(data).when(this.patient).getData(this.component.getName());

        final List<Feature> storedFeatureList = Arrays.asList(this.feature3, this.feature4);
        final PatientData<Feature> storedData = new IndexedPatientData<>(this.component.getName(), storedFeatureList);
        doReturn(storedData).when(controller).load(this.patient);

        controller.save(this.patient, PatientWritePolicy.MERGE);

        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.context);
        verify(this.doc, times(1)).removeXObjects(FeatureMetadatum.CLASS_REFERENCE);
        verify(this.doc, times(1)).removeXObjects(PhenoTipsFeature.CATEGORY_CLASS_REFERENCE);
        verify(this.doc, times(2)).newXObject(FeatureMetadatum.CLASS_REFERENCE, this.context);

        final List<String> phenotypeIds = Arrays.asList(FEATURE_3, FEATURE_2);
        final List<String> prenatalPhenotype = Collections.singletonList(FEATURE_1);

        verify(this.dataHolder, times(1)).set(PHENOTYPE_POSITIVE_PROPERTY, null, this.context);
        verify(this.dataHolder, times(1)).set(PHENOTYPE_NEGATIVE_PROPERTY, null, this.context);
        verify(this.dataHolder, times(1)).set(PRENATAL_PHENOTYPE_PROPERTY, null, this.context);
        verify(this.dataHolder, times(1)).set(NEGATIVE_PRENATAL_PHENOTYPE_PROPERTY, null, this.context);

        verify(this.dataHolder, times(1)).set(PHENOTYPE_POSITIVE_PROPERTY, phenotypeIds, this.context);
        verify(this.dataHolder, times(1)).set(PRENATAL_PHENOTYPE_PROPERTY, prenatalPhenotype, this.context);

        verifyNoMoreInteractions(this.doc, this.dataHolder);
    }

    @Test
    public void saveReplacesFeaturesWhenPolicyIsReplace() throws XWikiException
    {
        final List<Feature> featureList = Arrays.asList(this.feature1, this.feature2, this.feature3, this.feature4);
        final PatientData<Feature> data = new IndexedPatientData<>(this.component.getName(), featureList);
        doReturn(data).when(this.patient).getData(this.component.getName());

        this.component.save(this.patient, PatientWritePolicy.REPLACE);

        verify(this.doc, times(1)).getXObject(Patient.CLASS_REFERENCE, true, this.context);
        verify(this.doc, times(1)).removeXObjects(FeatureMetadatum.CLASS_REFERENCE);
        verify(this.doc, times(1)).removeXObjects(PhenoTipsFeature.CATEGORY_CLASS_REFERENCE);
        verify(this.doc, times(2)).newXObject(FeatureMetadatum.CLASS_REFERENCE, this.context);

        final List<String> phenotypeIds = Arrays.asList(FEATURE_2, FEATURE_3);
        final List<String> negPhenotypeIds = Collections.singletonList(FEATURE_4);
        final List<String> prenatalPhenotype = Collections.singletonList(FEATURE_1);

        verify(this.dataHolder, times(1)).set(PHENOTYPE_POSITIVE_PROPERTY, null, this.context);
        verify(this.dataHolder, times(1)).set(PHENOTYPE_NEGATIVE_PROPERTY, null, this.context);
        verify(this.dataHolder, times(1)).set(PRENATAL_PHENOTYPE_PROPERTY, null, this.context);
        verify(this.dataHolder, times(1)).set(NEGATIVE_PRENATAL_PHENOTYPE_PROPERTY, null, this.context);

        verify(this.dataHolder, times(1)).set(PHENOTYPE_POSITIVE_PROPERTY, phenotypeIds, this.context);
        verify(this.dataHolder, times(1)).set(PHENOTYPE_NEGATIVE_PROPERTY, negPhenotypeIds, this.context);
        verify(this.dataHolder, times(1)).set(PRENATAL_PHENOTYPE_PROPERTY, prenatalPhenotype, this.context);

        verifyNoMoreInteractions(this.doc, this.dataHolder);
    }
}
