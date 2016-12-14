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
import org.phenotips.data.Patient;
import org.phenotips.panels.GenePanel;
import org.phenotips.panels.GenePanelFactory;
import org.phenotips.panels.TermsForGene;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultGenePanelImpl}.
 *
 * @version $Id$
 * @since 1.3M6
 */
public class DefaultGenePanelImplTest
{
    private static final String HPO_TERM1 = "HP:001";

    private static final String HPO_TERM2 = "HP:002";

    private static final String HPO_TERM3 = "HP:003";

    private static final String GENE1 = "gene1";

    private static final String GENE2 = "gene2";

    private static final String GENE3 = "gene3";

    private static final String GENE_ID1 = "gene-id1";

    private static final String GENE_ID2 = "gene-id2";

    private static final List<Object> GENE_ID_LIST1 = Collections.<Object>singletonList(GENE_ID1);

    private static final List<Object> GENE_ID_LIST2 = Collections.<Object>singletonList(GENE_ID2);

    private static final String ASSOCIATED_GENES = "associated_genes";

    private static final String ENSEMBL_ID = "ensembl_gene_id";

    private static final String HPO_LABEL = "hpo";

    private static final String HGNC_LABEL = "hgnc";

    private static final String SIZE_LABEL = "size";

    private static final String TOTAL_SIZE_LABEL = "totalSize";

    private static final String GENES_LABEL = "genes";

    private static final String TERMS_LABEL = "terms";

    private static final String GENE_SYMBOL_LABEL = "gene_symbol";

    private static final String GENE_ID_LABEL = "gene_id";

    private static final String COUNT_LABEL = "count";

    private static final String ID_LABEL = "id";

    @Rule
    public MockitoComponentMockingRule<GenePanelFactory> mocker =
        new MockitoComponentMockingRule<GenePanelFactory>(DefaultGenePanelFactoryImpl.class);

    private GenePanelFactory genePanelFactory;

    private Vocabulary hpo;

    private Vocabulary hgnc;

    private Patient patient;

    private List<VocabularyTerm> presentTerms;

    private List<VocabularyTerm> absentTerms;

    private GenePanel patientGenePanel;

    private GenePanel presentTermsGenePanel;

    private GenePanel termsGenePanel;

    private VocabularyTerm presentTerm1;

    private VocabularyTerm presentTerm2;

    private JSONObject termsForGeneJSON1;

    private JSONObject termsForGeneJSON2;

    private JSONObject termsForGeneJSON3;

    private JSONObject expectedJSON;

    @Before
    public void setUp() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        this.genePanelFactory = this.mocker.getComponentUnderTest();
        final VocabularyManager vocabularyManager = this.mocker.getInstance(VocabularyManager.class);
        this.hpo = mock(Vocabulary.class);
        this.hgnc = mock(Vocabulary.class);

        when(vocabularyManager.getVocabulary(HPO_LABEL)).thenReturn(this.hpo);
        when(vocabularyManager.getVocabulary(HGNC_LABEL)).thenReturn(this.hgnc);

        makeGenePanelMocks();
        makeExpectedTermsForGeneJSON();

        this.patientGenePanel = this.genePanelFactory.build(this.patient);
        this.termsGenePanel = this.genePanelFactory.build(Collections.unmodifiableList(this.presentTerms),
            Collections.unmodifiableList(this.absentTerms));
        this.presentTermsGenePanel = this.genePanelFactory.build(Collections.unmodifiableList(this.presentTerms),
            Collections.<VocabularyTerm>emptyList());
    }

    //---------------------------------Gene panel from patient---------------------------------//

    @Test
    public void confirmPanelIsEmptyIfEmptyPatient()
    {
        final GenePanel panel = this.genePanelFactory.build(mock(Patient.class));
        assertEquals(0, panel.size());
        assertEquals(Collections.<VocabularyTerm>emptySet(), panel.getPresentTerms());
        assertEquals(Collections.<VocabularyTerm>emptySet(), panel.getAbsentTerms());
        assertEquals(Collections.<TermsForGene>emptyList(), panel.getTermsForGeneList());

        final JSONObject expectedJSON = new JSONObject().put(SIZE_LABEL, 0).put(TOTAL_SIZE_LABEL, 0)
            .put(GENES_LABEL, new JSONArray());
        assertTrue(expectedJSON.similar(panel.toJSON()));
    }

    @Test
    public void testGetPresentTermsForPatientObjectThatOnlyHasFeatureData()
    {
        assertEquals(new HashSet<>(this.presentTerms), this.patientGenePanel.getPresentTerms());
    }

    @Test
    public void testGetAbsentTermsForPatientObjectThatOnlyHasFeatureData()
    {
        assertEquals(new HashSet<>(this.absentTerms), this.patientGenePanel.getAbsentTerms());
    }

    @Test
    public void testGetTermsForGeneListForPatientObjectThatOnlyHasFeatureData()
    {
        final List<TermsForGene> termsForGeneList = this.patientGenePanel.getTermsForGeneList();
        assertEquals(3, termsForGeneList.size());

        // The first term should be "gene2", since it has the most number of appearances.
        assertEquals(GENE2, termsForGeneList.get(0).getGeneSymbol());
        assertEquals(GENE_ID2, termsForGeneList.get(0).getGeneId());

        // "gene2" should appear in two terms.
        assertEquals(2, termsForGeneList.get(0).getCount());

        // "gene2" should be associated with both present terms.
        final Set<VocabularyTerm> terms1 = new HashSet<>();
        terms1.add(this.presentTerm1);
        terms1.add(this.presentTerm2);
        assertEquals(terms1, termsForGeneList.get(0).getTerms());

        // test expected json for "gene2" TermsForGene object.
        assertTrue(this.termsForGeneJSON1.similar(termsForGeneList.get(0).toJSON()));


        // The second term should be "gene1", since it appears only one time, and comes ahead of "gene3" w.r.t. natural
        // order of its first associated phenotype.
        assertEquals(GENE1, termsForGeneList.get(1).getGeneSymbol());
        assertEquals(GENE_ID1, termsForGeneList.get(1).getGeneId());

        // "gene1" should appear in one term.
        assertEquals(1, termsForGeneList.get(1).getCount());

        // "gene1" should be associated with first present term.
        final Set<VocabularyTerm> terms2 = new HashSet<>();
        terms2.add(this.presentTerm1);
        assertEquals(terms2, termsForGeneList.get(1).getTerms());

        // test expected json for "gene1" TermsForGene object.
        assertTrue(this.termsForGeneJSON2.similar(termsForGeneList.get(1).toJSON()));


        // The third term should be "gene3", since it appears only one time, and comes after of "gene1" w.r.t. natural
        // order of its first associated phenotype.
        assertEquals(GENE3, termsForGeneList.get(2).getGeneSymbol());
        assertEquals(GENE3, termsForGeneList.get(2).getGeneId());

        // "gene3" should appear in one term.
        assertEquals(1, termsForGeneList.get(2).getCount());

        // "gene3" should be associated with second present term.
        final Set<VocabularyTerm> terms3 = new HashSet<>();
        terms3.add(this.presentTerm2);
        assertEquals(terms3, termsForGeneList.get(2).getTerms());

        // test expected json for "gene3" TermsForGene object.
        assertTrue(this.termsForGeneJSON3.similar(termsForGeneList.get(2).toJSON()));
    }

    @Test
    public void testToJSONForPatientObjectThatOnlyHasFeatureData()
    {
        assertTrue(this.expectedJSON.similar(this.patientGenePanel.toJSON()));
    }


    @Test
    public void testSizeForPatientObjectThatOnlyHasFeatureData()
    {
        assertEquals(3, this.patientGenePanel.size());
    }

    //----------------------------------Gene panel from terms----------------------------------//

    @Test
    public void confirmPanelIsEmptyIfEmptyTerms()
    {
        final GenePanel panel = this.genePanelFactory.build(Collections.<VocabularyTerm>emptyList(),
            Collections.<VocabularyTerm>emptyList());
        assertEquals(0, panel.size());
        assertEquals(Collections.<VocabularyTerm>emptySet(), panel.getPresentTerms());
        assertEquals(Collections.<VocabularyTerm>emptySet(), panel.getAbsentTerms());
        assertEquals(Collections.<TermsForGene>emptyList(), panel.getTermsForGeneList());

        final JSONObject expectedJSON = new JSONObject().put(SIZE_LABEL, 0).put(TOTAL_SIZE_LABEL, 0)
            .put(GENES_LABEL, new JSONArray());
        assertTrue(expectedJSON.similar(panel.toJSON()));
    }

    @Test
    public void confirmPanelIsCorrectIfNonEmptyAbsentTermsAndEmptyPresentTerms()
    {
        final GenePanel panel = this.genePanelFactory.build(Collections.<VocabularyTerm>emptyList(),
            Collections.unmodifiableList(this.absentTerms));
        assertEquals(0, panel.size());
        assertEquals(Collections.<VocabularyTerm>emptySet(), panel.getPresentTerms());
        assertEquals(new HashSet<>(this.absentTerms), panel.getAbsentTerms());
        assertEquals(Collections.<TermsForGene>emptyList(), panel.getTermsForGeneList());

        final JSONObject expectedJSON = new JSONObject().put(SIZE_LABEL, 0).put(TOTAL_SIZE_LABEL, 0)
            .put(GENES_LABEL, new JSONArray());
        assertTrue(expectedJSON.similar(panel.toJSON()));
    }

    @Test
    public void testGetPresentTermsIfOnlyPresentTermsProvided()
    {
        assertEquals(new HashSet<>(this.presentTerms), this.presentTermsGenePanel.getPresentTerms());
    }

    @Test
    public void testGetAbsentTermsIfOnlyPresentTermsProvided()
    {
        assertEquals(Collections.emptySet(), this.presentTermsGenePanel.getAbsentTerms());
    }

    @Test
    public void testGetTermsForGeneListIfOnlyPresentTermsProvided()
    {
        final List<TermsForGene> termsForGeneList = this.presentTermsGenePanel.getTermsForGeneList();
        assertEquals(3, termsForGeneList.size());

        // The first term should be "gene2", since it has the most number of appearances.
        assertEquals(GENE2, termsForGeneList.get(0).getGeneSymbol());
        assertEquals(GENE_ID2, termsForGeneList.get(0).getGeneId());

        // "gene2" should appear in two terms.
        assertEquals(2, termsForGeneList.get(0).getCount());

        // "gene2" should be associated with both present terms.
        final Set<VocabularyTerm> terms1 = new HashSet<>();
        terms1.add(this.presentTerm1);
        terms1.add(this.presentTerm2);
        assertEquals(terms1, termsForGeneList.get(0).getTerms());

        // test expected json for "gene2" TermsForGene object.
        assertTrue(this.termsForGeneJSON1.similar(termsForGeneList.get(0).toJSON()));


        // The second term should be "gene1", since it appears only one time, and comes ahead of "gene3" w.r.t. natural
        // order of its first associated phenotype.
        assertEquals(GENE1, termsForGeneList.get(1).getGeneSymbol());
        assertEquals(GENE_ID1, termsForGeneList.get(1).getGeneId());

        // "gene1" should appear in one term.
        assertEquals(1, termsForGeneList.get(1).getCount());

        // "gene1" should be associated with first present term.
        final Set<VocabularyTerm> terms2 = new HashSet<>();
        terms2.add(this.presentTerm1);
        assertEquals(terms2, termsForGeneList.get(1).getTerms());

        // test expected json for "gene1" TermsForGene object.
        assertTrue(this.termsForGeneJSON2.similar(termsForGeneList.get(1).toJSON()));


        // The third term should be "gene3", since it appears only one time, and comes after of "gene1" w.r.t. natural
        // order of its first associated phenotype.
        assertEquals(GENE3, termsForGeneList.get(2).getGeneSymbol());
        assertEquals(GENE3, termsForGeneList.get(2).getGeneId());

        // "gene3" should appear in one term.
        assertEquals(1, termsForGeneList.get(2).getCount());

        // "gene3" should be associated with second present term.
        final Set<VocabularyTerm> terms3 = new HashSet<>();
        terms3.add(this.presentTerm2);
        assertEquals(terms3, termsForGeneList.get(2).getTerms());

        // test expected json for "gene3" TermsForGene object.
        assertTrue(this.termsForGeneJSON3.similar(termsForGeneList.get(2).toJSON()));
    }

    @Test
    public void testToJSONIfOnlyPresentTermsProvided()
    {
        assertTrue(this.expectedJSON.similar(this.patientGenePanel.toJSON()));
    }


    @Test
    public void testSizeIfOnlyPresentTermsProvided()
    {
        assertEquals(3, this.presentTermsGenePanel.size());
    }

    @Test
    public void testGetPresentTermsIfPresentAndAbsentTermsProvided()
    {
        assertEquals(new HashSet<>(this.presentTerms), this.termsGenePanel.getPresentTerms());
    }

    @Test
    public void testGetAbsentTermsIfPresentAndAbsentTermsProvided()
    {
        assertEquals(new HashSet<>(this.absentTerms), this.termsGenePanel.getAbsentTerms());

    }

    @Test
    public void testGetTermsForGeneListIfPresentAndAbsentTermsProvided()
    {
        final List<TermsForGene> termsForGeneList = this.termsGenePanel.getTermsForGeneList();
        assertEquals(3, termsForGeneList.size());

        // The first term should be "gene2", since it has the most number of appearances.
        assertEquals(GENE2, termsForGeneList.get(0).getGeneSymbol());
        assertEquals(GENE_ID2, termsForGeneList.get(0).getGeneId());

        // "gene2" should appear in two terms.
        assertEquals(2, termsForGeneList.get(0).getCount());

        // "gene2" should be associated with both present terms.
        final Set<VocabularyTerm> terms1 = new HashSet<>();
        terms1.add(this.presentTerm1);
        terms1.add(this.presentTerm2);
        assertEquals(terms1, termsForGeneList.get(0).getTerms());

        // test expected json for "gene2" TermsForGene object.
        assertTrue(this.termsForGeneJSON1.similar(termsForGeneList.get(0).toJSON()));


        // The second term should be "gene1", since it appears only one time, and comes ahead of "gene3" w.r.t. natural
        // order.
        assertEquals(GENE1, termsForGeneList.get(1).getGeneSymbol());
        assertEquals(GENE_ID1, termsForGeneList.get(1).getGeneId());

        // "gene1" should appear in one term.
        assertEquals(1, termsForGeneList.get(1).getCount());

        // "gene1" should be associated with first present term.
        final Set<VocabularyTerm> terms2 = new HashSet<>();
        terms2.add(this.presentTerm1);
        assertEquals(terms2, termsForGeneList.get(1).getTerms());

        // test expected json for "gene1" TermsForGene object.
        assertTrue(this.termsForGeneJSON2.similar(termsForGeneList.get(1).toJSON()));


        // The third term should be "gene3", since it appears only one time, and comes after of "gene1" w.r.t. natural
        // order.
        assertEquals(GENE3, termsForGeneList.get(2).getGeneSymbol());
        assertEquals(GENE3, termsForGeneList.get(2).getGeneId());

        // "gene3" should appear in one term.
        assertEquals(1, termsForGeneList.get(2).getCount());

        // "gene3" should be associated with second present term.
        final Set<VocabularyTerm> terms3 = new HashSet<>();
        terms3.add(this.presentTerm2);
        assertEquals(terms3, termsForGeneList.get(2).getTerms());

        // test expected json for "gene3" TermsForGene object.
        assertTrue(this.termsForGeneJSON3.similar(termsForGeneList.get(2).toJSON()));
    }

    @Test
    public void testToJSONIfPresentAndAbsentTermsProvided()
    {
        assertTrue(this.expectedJSON.similar(this.patientGenePanel.toJSON()));
    }


    @Test
    public void testSizeIfPresentAndAbsentTermsProvided()
    {
        assertEquals(3, this.termsGenePanel.size());
    }

    //--------------------------------Gene panel from features---------------------------------//

    @Test
    public void confirmPanelIsEmptyIfEmptyFeatures()
    {
        final GenePanel panel = this.genePanelFactory.build(Collections.<Feature>emptySet());
        assertEquals(0, panel.size());
        assertEquals(Collections.<VocabularyTerm>emptySet(), panel.getPresentTerms());
        assertEquals(Collections.<VocabularyTerm>emptySet(), panel.getAbsentTerms());
        assertEquals(Collections.<TermsForGene>emptyList(), panel.getTermsForGeneList());

        final JSONObject expectedJSON = new JSONObject().put(SIZE_LABEL, 0).put(TOTAL_SIZE_LABEL, 0)
            .put(GENES_LABEL, new JSONArray());
        assertTrue(expectedJSON.similar(panel.toJSON()));
    }

    //--------------------------------------Helper methods-------------------------------------//

    /**
     * Helper method for mocking all outside components for GenePanel
     */
    @SuppressWarnings("unchecked")
    private void makeGenePanelMocks()
    {
        this.patient = mock(Patient.class);
        final Set<Feature> features = new HashSet<>();
        this.presentTerms = new ArrayList<>();
        this.absentTerms = new ArrayList<>();

        final Feature presentFeature1 = mock(Feature.class);
        final Feature presentFeature2 = mock(Feature.class);
        final Feature absentFeature = mock(Feature.class);

        features.add(presentFeature1);
        features.add(presentFeature2);
        features.add(absentFeature);

        this.presentTerm1 = mock(VocabularyTerm.class);
        this.presentTerm2 = mock(VocabularyTerm.class);
        final VocabularyTerm absentTerm = mock(VocabularyTerm.class);

        this.presentTerms.add(this.presentTerm1);
        this.presentTerms.add(this.presentTerm2);
        this.absentTerms.add(absentTerm);

        final VocabularyTerm geneTerm1 = mock(VocabularyTerm.class);
        final VocabularyTerm geneTerm2 = mock(VocabularyTerm.class);
        final VocabularyTerm geneTerm3 = mock(VocabularyTerm.class);

        final Object associatedGenes1 = new ArrayList<>();
        ((List<String>) associatedGenes1).add(GENE2);
        ((List<String>) associatedGenes1).add(GENE1);

        final Object associatedGenes2 = new ArrayList<>();
        ((List<String>) associatedGenes2).add(GENE2);
        ((List<String>) associatedGenes2).add(GENE3);

        Mockito.<Set<? extends Feature>> when(this.patient.getFeatures()).thenReturn(features);

        when(presentFeature1.isPresent()).thenReturn(Boolean.TRUE);
        when(presentFeature2.isPresent()).thenReturn(Boolean.TRUE);
        when(absentFeature.isPresent()).thenReturn(Boolean.FALSE);

        when(presentFeature1.getValue()).thenReturn(HPO_TERM1);
        when(presentFeature2.getValue()).thenReturn(HPO_TERM2);
        when(absentFeature.getValue()).thenReturn(HPO_TERM3);

        when(this.hpo.getTerm(HPO_TERM1)).thenReturn(this.presentTerm1);
        when(this.hpo.getTerm(HPO_TERM2)).thenReturn(this.presentTerm2);
        when(this.hpo.getTerm(HPO_TERM3)).thenReturn(absentTerm);

        when(this.presentTerm1.get(ASSOCIATED_GENES)).thenReturn(associatedGenes1);
        when(this.presentTerm2.get(ASSOCIATED_GENES)).thenReturn(associatedGenes2);

        when(this.hgnc.getTerm(GENE1)).thenReturn(geneTerm1);
        when(this.hgnc.getTerm(GENE2)).thenReturn(geneTerm2);
        when(this.hgnc.getTerm(GENE3)).thenReturn(geneTerm3);

        when(geneTerm1.get(ENSEMBL_ID)).thenReturn(GENE_ID_LIST1);
        when(geneTerm2.get(ENSEMBL_ID)).thenReturn(GENE_ID_LIST2);
        when(geneTerm3.get(ENSEMBL_ID)).thenReturn(null);

        when(this.presentTerm1.toJSON()).thenReturn(new JSONObject().put(ID_LABEL, HPO_TERM1));
        when(this.presentTerm2.toJSON()).thenReturn(new JSONObject().put(ID_LABEL, HPO_TERM2));
        when(absentTerm.toJSON()).thenReturn(new JSONObject().put(ID_LABEL, HPO_TERM3));

        when(this.presentTerm1.getName()).thenReturn(HPO_TERM1);
        when(this.presentTerm2.getName()).thenReturn(HPO_TERM2);
        when(absentTerm.getName()).thenReturn(HPO_TERM3);
    }

    /**
     * Mock up expected JSON outputs.
     */
    private void makeExpectedTermsForGeneJSON()
    {
        this.termsForGeneJSON1 =
            new JSONObject()
                .put(TERMS_LABEL,
                    new JSONArray()
                        .put(new JSONObject().put(ID_LABEL, HPO_TERM1))
                        .put(new JSONObject().put(ID_LABEL, HPO_TERM2)))
                .put(GENE_SYMBOL_LABEL, GENE2)
                .put(GENE_ID_LABEL, GENE_ID2)
                .put(COUNT_LABEL, 2);

        this.termsForGeneJSON2 =
            new JSONObject()
                .put(TERMS_LABEL,
                    new JSONArray()
                        .put(new JSONObject().put(ID_LABEL, HPO_TERM1)))
                .put(GENE_SYMBOL_LABEL, GENE1)
                .put(GENE_ID_LABEL, GENE_ID1)
                .put(COUNT_LABEL, 1);

        this.termsForGeneJSON3 =
            new JSONObject()
                .put(TERMS_LABEL,
                    new JSONArray()
                        .put(new JSONObject().put(ID_LABEL, HPO_TERM2)))
                .put(GENE_SYMBOL_LABEL, GENE3)
                .put(GENE_ID_LABEL, GENE3)
                .put(COUNT_LABEL, 1);

        this.expectedJSON =
            new JSONObject()
                .put(SIZE_LABEL, 3)
                .put(TOTAL_SIZE_LABEL, 3)
                .put(GENES_LABEL,
                    new JSONArray()
                        .put(this.termsForGeneJSON1)
                        .put(this.termsForGeneJSON2)
                        .put(this.termsForGeneJSON3));
    }
}
