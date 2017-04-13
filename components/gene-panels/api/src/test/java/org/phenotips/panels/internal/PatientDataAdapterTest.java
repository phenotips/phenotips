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
package org.phenotips.panels.internal;

import org.phenotips.data.Feature;
import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PatientDataAdapter}.
 */
public class PatientDataAdapterTest
{
    private static final String GENES = "genes";

    private static final String GLOBAL_QUALIFIERS = "global-qualifiers";

    private static final String POS_FEATURE_1_ID = "HP:1";

    private static final String POS_FEATURE_2_ID = "HP:2";

    private static final String NEG_FEATURE_1_ID = "-HP:1";

    private static final String GENE = "gene";

    private static final String STATUS = "status";

    private static final String HGNC = "hgnc";

    private static final String GENE_1_ID = "gene1";

    private static final String GENE_2_ID = "gene2";

    private static final String GENE_3_ID = "gene3";

    private static final String REJECTED_LABEL = "rejected";

    @Mock
    private Patient patient;

    @Mock
    private VocabularyManager vocabularyManager;

    @Mock
    private Vocabulary hgnc;

    @Mock
    private Feature positiveFeature1;

    @Mock
    private Feature positiveFeature2;

    @Mock
    private Feature negativeFeature1;

    @Mock
    private VocabularyTerm positiveTerm1;

    @Mock
    private VocabularyTerm positiveTerm2;

    @Mock
    private VocabularyTerm negativeTerm1;

    @Mock
    private VocabularyTerm qualifierTerm1;

    @Mock
    private VocabularyTerm qualifierTerm2;

    @Mock
    private VocabularyTerm geneTerm1;

    private PatientData<Object> geneData;

    private PatientData<Object> qualifierData;

    private PatientDataAdapter.AdapterBuilder adapterBuilder;

    private Set<Feature> features;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        when(vocabularyManager.getVocabulary(HGNC)).thenReturn(hgnc);
        when(hgnc.getTerm(GENE_1_ID)).thenReturn(geneTerm1);
        when(hgnc.getTerm(GENE_2_ID)).thenReturn(null);

        final Map<String, String> geneDatum1 = new HashMap<>();
        geneDatum1.put(GENE, GENE_1_ID);
        geneDatum1.put(STATUS, REJECTED_LABEL);
        final Map<String, String> geneDatum2 = new HashMap<>();
        geneDatum2.put(GENE, GENE_2_ID);
        geneDatum2.put(STATUS, REJECTED_LABEL);
        final Map<String, String> geneDatum3 = new HashMap<>();
        geneDatum3.put(GENE, GENE_3_ID);
        geneDatum3.put(STATUS, "aa");
        geneData = new IndexedPatientData<>(GENES, Arrays.asList((Object) geneDatum1, geneDatum2, geneDatum3));
        when(patient.getData(GENES)).thenReturn(geneData);

        final List<VocabularyTerm> qualifierDatum1 = Collections.singletonList(qualifierTerm1);
        final List<VocabularyTerm> qualifierDatum2 = Collections.singletonList(qualifierTerm2);
        qualifierData = new IndexedPatientData<>(GLOBAL_QUALIFIERS, Arrays.asList((Object) qualifierDatum1,
            qualifierDatum2));
        when(patient.getData(GLOBAL_QUALIFIERS)).thenReturn(qualifierData);

        features = new HashSet<>();
        features.add(positiveFeature1);
        features.add(positiveFeature2);
        features.add(negativeFeature1);
        when(positiveFeature1.getValue()).thenReturn(POS_FEATURE_1_ID);
        when(positiveFeature2.getValue()).thenReturn(POS_FEATURE_2_ID);
        when(negativeFeature1.getValue()).thenReturn(NEG_FEATURE_1_ID);
        when(vocabularyManager.resolveTerm(POS_FEATURE_1_ID)).thenReturn(positiveTerm1);
        when(vocabularyManager.resolveTerm(POS_FEATURE_2_ID)).thenReturn(positiveTerm2);
        when(vocabularyManager.resolveTerm(NEG_FEATURE_1_ID)).thenReturn(negativeTerm1);
        when(positiveFeature1.isPresent()).thenReturn(true);
        when(positiveFeature2.isPresent()).thenReturn(true);
        when(negativeFeature1.isPresent()).thenReturn(false);
        when((Set<Feature>) patient.getFeatures()).thenReturn(features);

        this.adapterBuilder = new PatientDataAdapter.AdapterBuilder(patient, vocabularyManager);
    }

    @Test
    public void getPresentTermsReturnsEmptyCollectionWhenNoneStored()
    {
        when(patient.getFeatures()).thenReturn(null);
        when(patient.getData(GLOBAL_QUALIFIERS)).thenReturn(null);
        final PatientDataAdapter dataAdapter = adapterBuilder.build();
        Assert.assertTrue(dataAdapter.getPresentTerms().isEmpty());
    }

    @Test
    public void getPresentTermsReturnsTermsForAllPresentFeatures()
    {
        final PatientDataAdapter dataAdapter = adapterBuilder.build();
        final Set<VocabularyTerm> presentTerms = new HashSet<>();
        presentTerms.add(positiveTerm1);
        presentTerms.add(positiveTerm2);
        presentTerms.add(qualifierTerm1);
        presentTerms.add(qualifierTerm2);
        Assert.assertEquals(presentTerms, dataAdapter.getPresentTerms());
    }

    @Test
    public void getAbsentTermsReturnsEmptyCollectionWhenNoneStored()
    {
        when(patient.getFeatures()).thenReturn(null);
        when(patient.getData(GLOBAL_QUALIFIERS)).thenReturn(null);
        final PatientDataAdapter dataAdapter = adapterBuilder.build();
        Assert.assertTrue(dataAdapter.getAbsentTerms().isEmpty());
    }

    @Test
    public void getAbsentTermsReturnsTermsForAllAbsentFeatures()
    {
        final PatientDataAdapter dataAdapter = adapterBuilder.build();
        final Set<VocabularyTerm> absentTerms = new HashSet<>();
        absentTerms.add(negativeTerm1);
        Assert.assertEquals(absentTerms, dataAdapter.getAbsentTerms());
    }

    @Test
    public void getRejectedGenesReturnsEmptyCollectionWhenRejectedGenesNotRetrievedFromPatient()
    {
        when(patient.getFeatures()).thenReturn(null);
        when(patient.getData(GLOBAL_QUALIFIERS)).thenReturn(null);
        final PatientDataAdapter dataAdapter = adapterBuilder.build();
        Assert.assertTrue(dataAdapter.getRejectedGenes().isEmpty());
    }

    @Test
    public void getRejectedGenesReturnsEmptyCollectionWhenNoRejectedGenesStored()
    {
        when(patient.getFeatures()).thenReturn(null);
        when(patient.getData(GLOBAL_QUALIFIERS)).thenReturn(null);
        when(patient.getData(GENES)).thenReturn(null);
        final PatientDataAdapter dataAdapter = adapterBuilder.withRejectedGenes().build();
        Assert.assertTrue(dataAdapter.getRejectedGenes().isEmpty());
    }

    @Test
    public void getRejectedGenesReturnsTermsForAllNegativeGenes()
    {
        final PatientDataAdapter dataAdapter = adapterBuilder.withRejectedGenes().build();
        final Set<VocabularyTerm> rejectedGenes = new HashSet<>();
        rejectedGenes.add(geneTerm1);
        Assert.assertEquals(rejectedGenes, dataAdapter.getRejectedGenes());
    }
}
