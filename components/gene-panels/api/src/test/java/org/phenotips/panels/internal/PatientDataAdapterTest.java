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

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.Feature;
import org.phenotips.data.Gene;
import org.phenotips.data.IndexedPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.SimpleValuePatientData;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.web.Utils;

import static org.mockito.Mockito.mock;
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

    private static final String HGNC = "hgnc";

    private static final String GENE_1_ID = "gene1";

    private static final String GENE_2_ID = "gene2";

    private static final String GENE_3_ID = "gene3";

    private static final String REJECTED_LABEL = "rejected";

    private static final String REJECTED_CANDIDATE_LABEL = "rejected_candidate";

    private static final String STATUS_KEY = "status";

    private static final String STRATEGY_KEY = "strategy";

    private static final List<String> STATUS_VALUES = Arrays.asList("candidate", "rejected", "rejected_candidate",
        "solved", "carrier", "candidate>novel_disease", "candidate>novel_phen", "umc", "umc>vus", "umc>msv");

    private static final List<String> STRATEGY_VALUES = Arrays.asList("sequencing", "deletion", "familial_mutation",
        "common_mutations");

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

    @Mock
    private Provider<XWikiContext> provider;

    @Mock
    private XWikiContext context;

    @Mock
    private ComponentManager cm;

    @Mock
    private Provider<ComponentManager> mockProvider;

    private PatientData<Gene> geneData;

    private PatientData<Object> qualifierData;

    private PatientDataAdapter.AdapterBuilder adapterBuilder;

    private Set<Feature> features;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        Utils.setComponentManager(this.cm);
        ReflectionUtils.setFieldValue(new ComponentManagerRegistry(), "cmProvider", this.mockProvider);
        when(this.mockProvider.get()).thenReturn(this.cm);
        when(this.cm.getInstance(XWikiContext.TYPE_PROVIDER)).thenReturn(this.provider);
        when(this.cm.getInstance(VocabularyManager.class)).thenReturn(this.vocabularyManager);
        when(this.provider.get()).thenReturn(this.context);
        XWiki x = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(x);

        XWikiDocument geneDoc = mock(XWikiDocument.class);
        when(x.getDocument(Gene.GENE_CLASS, this.context)).thenReturn(geneDoc);
        geneDoc.setNew(false);
        BaseClass c = mock(BaseClass.class);
        when(geneDoc.getXClass()).thenReturn(c);
        StaticListClass lc1 = mock(StaticListClass.class);
        StaticListClass lc2 = mock(StaticListClass.class);
        when(c.get(STATUS_KEY)).thenReturn(lc1);
        when(c.get(STRATEGY_KEY)).thenReturn(lc2);
        when(lc1.getList(this.context)).thenReturn(STATUS_VALUES);
        when(lc2.getList(this.context)).thenReturn(STRATEGY_VALUES);

        when(this.vocabularyManager.getVocabulary(HGNC)).thenReturn(this.hgnc);
        when(this.hgnc.getTerm(GENE_1_ID)).thenReturn(this.geneTerm1);
        when(this.hgnc.getTerm(GENE_2_ID)).thenReturn(null);

        final Gene geneDatum1 = mockGene(GENE_1_ID, REJECTED_LABEL);
        final Gene geneDatum2 = mockGene(GENE_2_ID, REJECTED_CANDIDATE_LABEL);
        final Gene geneDatum3 = mockGene(GENE_3_ID, "aa");

        this.geneData =
            new IndexedPatientData<>(GENES, Arrays.asList(geneDatum1, geneDatum2, geneDatum3));
        when(this.patient.<Gene>getData(GENES)).thenReturn(this.geneData);

        this.qualifierData = new SimpleValuePatientData<>(GLOBAL_QUALIFIERS, Arrays.asList(this.qualifierTerm1,
            this.qualifierTerm2));
        when(this.patient.getData(GLOBAL_QUALIFIERS)).thenReturn(this.qualifierData);

        this.features = new HashSet<>();
        this.features.add(this.positiveFeature1);
        this.features.add(this.positiveFeature2);
        this.features.add(this.negativeFeature1);
        when(this.positiveFeature1.getValue()).thenReturn(POS_FEATURE_1_ID);
        when(this.positiveFeature2.getValue()).thenReturn(POS_FEATURE_2_ID);
        when(this.negativeFeature1.getValue()).thenReturn(NEG_FEATURE_1_ID);
        when(this.vocabularyManager.resolveTerm(POS_FEATURE_1_ID)).thenReturn(this.positiveTerm1);
        when(this.vocabularyManager.resolveTerm(POS_FEATURE_2_ID)).thenReturn(this.positiveTerm2);
        when(this.vocabularyManager.resolveTerm(NEG_FEATURE_1_ID)).thenReturn(this.negativeTerm1);
        when(this.positiveFeature1.isPresent()).thenReturn(true);
        when(this.positiveFeature2.isPresent()).thenReturn(true);
        when(this.negativeFeature1.isPresent()).thenReturn(false);
        Mockito.doReturn(this.features).when(this.patient).getFeatures();

        this.adapterBuilder = new PatientDataAdapter.AdapterBuilder(this.patient, this.vocabularyManager);
    }

    @Test
    public void getPresentTermsReturnsEmptyCollectionWhenNoneStored()
    {
        when(this.patient.getFeatures()).thenReturn(null);
        when(this.patient.getData(GLOBAL_QUALIFIERS)).thenReturn(null);
        final PatientDataAdapter dataAdapter = this.adapterBuilder.build();
        Assert.assertTrue(dataAdapter.getPresentTerms().isEmpty());
    }

    @Test
    public void getPresentTermsReturnsTermsForAllPresentFeatures()
    {
        final PatientDataAdapter dataAdapter = this.adapterBuilder.build();
        final Set<VocabularyTerm> presentTerms = new HashSet<>();
        presentTerms.add(this.positiveTerm1);
        presentTerms.add(this.positiveTerm2);
        presentTerms.add(this.qualifierTerm1);
        presentTerms.add(this.qualifierTerm2);
        Assert.assertEquals(presentTerms, dataAdapter.getPresentTerms());
    }

    @Test
    public void getAbsentTermsReturnsEmptyCollectionWhenNoneStored()
    {
        when(this.patient.getFeatures()).thenReturn(null);
        when(this.patient.getData(GLOBAL_QUALIFIERS)).thenReturn(null);
        final PatientDataAdapter dataAdapter = this.adapterBuilder.build();
        Assert.assertTrue(dataAdapter.getAbsentTerms().isEmpty());
    }

    @Test
    public void getAbsentTermsReturnsTermsForAllAbsentFeatures()
    {
        final PatientDataAdapter dataAdapter = this.adapterBuilder.build();
        final Set<VocabularyTerm> absentTerms = new HashSet<>();
        absentTerms.add(this.negativeTerm1);
        Assert.assertEquals(absentTerms, dataAdapter.getAbsentTerms());
    }

    @Test
    public void getRejectedGenesReturnsEmptyCollectionWhenRejectedGenesNotRetrievedFromPatient()
    {
        when(this.patient.getFeatures()).thenReturn(null);
        when(this.patient.getData(GLOBAL_QUALIFIERS)).thenReturn(null);
        final PatientDataAdapter dataAdapter = this.adapterBuilder.build();
        Assert.assertTrue(dataAdapter.getRejectedGenes().isEmpty());
    }

    @Test
    public void getRejectedGenesReturnsEmptyCollectionWhenNoRejectedGenesStored()
    {
        when(this.patient.getFeatures()).thenReturn(null);
        when(this.patient.getData(GLOBAL_QUALIFIERS)).thenReturn(null);
        when(this.patient.getData(GENES)).thenReturn(null);
        final PatientDataAdapter dataAdapter = this.adapterBuilder.withRejectedGenes().build();
        Assert.assertTrue(dataAdapter.getRejectedGenes().isEmpty());
    }

    @Test
    public void getRejectedGenesReturnsTermsForAllNegativeGenes()
    {
        final PatientDataAdapter dataAdapter = this.adapterBuilder.withRejectedGenes().build();
        final Set<VocabularyTerm> rejectedGenes = new HashSet<>();
        rejectedGenes.add(this.geneTerm1);
        Assert.assertEquals(rejectedGenes, dataAdapter.getRejectedGenes());
    }

    private Gene mockGene(String id, String status)
    {
        Gene result = mock(Gene.class);
        when(result.getId()).thenReturn(id);
        when(result.getStatus()).thenReturn(status);
        return result;
    }
}
