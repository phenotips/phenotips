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
package org.phenotips.vocabulary.internal;

import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.phase.InitializationException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link DefaultVocabularyManager} class.
 */
public class DefaultVocabularyManagerTest
{
    private static final String CHEMICAL_CATEGORY = "chemical";

    private static final String ETHNICITY_CATEGORY = "ethnicity";

    private static final String GENE_CATEGORY = "gene";

    private static final String PHENOTYPE_CATEGORY = "phenotype";

    private static final String PHENOTYPE_Q_CATEGORY = "phenotype-qualifier";

    private static final String DISEASE_CATEGORY = "disease";

    private static final String HGNC_LABEL = "hgnc";

    private static final String HGNC_CAPS_LABEL = "HGNC";

    private static final String CHEBI_LABEL = "chebi";

    private static final String HPO_LABEL = "hpo";

    private static final String ETHNICITY_LABEL = ETHNICITY_CATEGORY;

    private static final String ETHNO_LABEL = "ETHNO";

    private static final String OMIM_LABEL = "omim";

    private static final String SCORE_LABEL = "score";

    private static final String SEARCH_QUERY_ABC_LABEL = "ABC";

    private static final String SEARCH_QUERY_A_LABEL = "A";

    private static final String CHEBI_CAPS_LABEL = "CHEBI";

    private static final String HP_LABEL = "HP";

    private static final String MIM_LABEL = "MIM";

    @Rule
    public final MockitoComponentMockingRule<VocabularyManager> mocker =
        new MockitoComponentMockingRule<>(DefaultVocabularyManager.class);

    private VocabularyManager vocabularyManager;

    private Map<String, Vocabulary> vocabularies;

    private Logger logger;

    @Mock
    private Vocabulary hgnc;

    @Mock
    private Vocabulary chebi;

    @Mock
    private Vocabulary hpo;

    @Mock
    private Vocabulary ethnicity;

    @Mock
    private Vocabulary omim;

    @Mock
    private VocabularyTerm result1;

    @Mock
    private VocabularyTerm result2;

    @Mock
    private VocabularyTerm result3;

    @Mock
    private VocabularyTerm result4;

    @Mock
    private VocabularyTerm result5;

    @Mock
    private VocabularyTerm result6;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        // Add the vocabularies to the map.
        this.vocabularies = new HashMap<>();
        this.vocabularies.put(HGNC_LABEL, this.hgnc);
        this.vocabularies.put(CHEBI_LABEL, this.chebi);
        this.vocabularies.put(HPO_LABEL, this.hpo);
        this.vocabularies.put(ETHNICITY_LABEL, this.ethnicity);
        this.vocabularies.put(OMIM_LABEL, this.omim);

        // Mock a set of aliases for the hgnc vocabulary.
        final Set<String> hgncAliases = new HashSet<>();
        hgncAliases.add(HGNC_LABEL);
        hgncAliases.add(HGNC_CAPS_LABEL);

        // Mock a set of aliases for the chebi vocabulary.
        final Set<String> chebiAliases = new HashSet<>();
        chebiAliases.add(CHEBI_LABEL);
        chebiAliases.add(CHEBI_CAPS_LABEL);

        // Mock a set of aliases for the hpo vocabulary.
        final Set<String> hpoAliases = new HashSet<>();
        hpoAliases.add(HPO_LABEL);
        hpoAliases.add(HP_LABEL);

        // Mock a set of aliases for the ethnicity vocabulary.
        final Set<String> ethnicityAliases = new HashSet<>();
        ethnicityAliases.add(ETHNICITY_LABEL);
        ethnicityAliases.add(ETHNO_LABEL);

        // Mock a set of aliases for the omim vocabulary.
        final Set<String> omimAliases = new HashSet<>();
        omimAliases.add(OMIM_LABEL);
        omimAliases.add(MIM_LABEL);

        // Mock the Vocabulary#getAliases calls.
        when(this.hgnc.getAliases()).thenReturn(hgncAliases);
        when(this.chebi.getAliases()).thenReturn(chebiAliases);
        when(this.hpo.getAliases()).thenReturn(hpoAliases);
        when(this.ethnicity.getAliases()).thenReturn(ethnicityAliases);
        when(this.omim.getAliases()).thenReturn(omimAliases);

        // Mock the Vocabulary#getSupportedCategories calls.
        when(this.hgnc.getSupportedCategories()).thenReturn(Collections.singletonList(GENE_CATEGORY));
        when(this.chebi.getSupportedCategories()).thenReturn(Collections.singletonList(CHEMICAL_CATEGORY));
        when(this.hpo.getSupportedCategories()).thenReturn(Arrays.asList(PHENOTYPE_CATEGORY, PHENOTYPE_Q_CATEGORY));
        when(this.ethnicity.getSupportedCategories()).thenReturn(Collections.singletonList(ETHNICITY_CATEGORY));
        when(this.omim.getSupportedCategories()).thenReturn(Arrays.asList(DISEASE_CATEGORY, GENE_CATEGORY));

        this.mocker.registerComponent(Vocabulary.class, "hgnc", this.hgnc);
        this.mocker.registerComponent(Vocabulary.class, "chebi", this.chebi);
        this.mocker.registerComponent(Vocabulary.class, "hpo", this.hpo);
        this.mocker.registerComponent(Vocabulary.class, "ethnicity", this.ethnicity);
        this.mocker.registerComponent(Vocabulary.class, "omim", this.omim);

        this.vocabularyManager = this.mocker.getComponentUnderTest();
        this.logger = this.mocker.getMockedLogger();
    }

    @Test
    public void resolveTermWhenTermIdIsNullReturnsNull() throws Exception
    {
        final VocabularyTerm term = this.vocabularyManager.resolveTerm(null);
        Assert.assertNull(term);
    }

    @Test
    public void resolveTermWhenTermIdIsBlankReturnsNull() throws Exception
    {
        final VocabularyTerm term1 = this.vocabularyManager.resolveTerm("");
        Assert.assertNull(term1);

        final VocabularyTerm term2 = this.vocabularyManager.resolveTerm(" ");
        Assert.assertNull(term2);
    }

    @Test
    public void resolveTermWhenInvalidVocabularyReturnsNull() throws Exception
    {
        final VocabularyTerm term = this.vocabularyManager.resolveTerm("WRONG:TERM");
        Assert.assertNull(term);
    }

    @Test
    public void resolveTermReturnsTermFromTermId() throws Exception
    {
        final String hpoId = "HP:01";
        final VocabularyTerm hpoTerm = mock(VocabularyTerm.class);

        final String hgncId = "HGNC:01";
        final VocabularyTerm hgncTerm = mock(VocabularyTerm.class);

        final String omimId = "MIM:01";
        final VocabularyTerm omimTerm = mock(VocabularyTerm.class);

        final String ethnicityId = "ETHNO:ABC";
        final VocabularyTerm ethnicityTerm = mock(VocabularyTerm.class);

        final String chebiId = "CHEBI:01";
        final VocabularyTerm chebiTerm = mock(VocabularyTerm.class);

        when(this.hpo.getTerm(hpoId)).thenReturn(hpoTerm);
        when(this.hgnc.getTerm(hgncId)).thenReturn(hgncTerm);
        when(this.omim.getTerm(omimId)).thenReturn(omimTerm);
        when(this.ethnicity.getTerm(ethnicityId)).thenReturn(ethnicityTerm);
        when(this.chebi.getTerm(chebiId)).thenReturn(chebiTerm);

        Assert.assertEquals(hpoTerm, this.vocabularyManager.resolveTerm(hpoId));
        Assert.assertEquals(hgncTerm, this.vocabularyManager.resolveTerm(hgncId));
        Assert.assertEquals(omimTerm, this.vocabularyManager.resolveTerm(omimId));
        Assert.assertEquals(ethnicityTerm, this.vocabularyManager.resolveTerm(ethnicityId));
        Assert.assertEquals(chebiTerm, this.vocabularyManager.resolveTerm(chebiId));
    }

    @Test
    public void getVocabulary() throws Exception
    {
        Assert.assertEquals(this.hpo, this.vocabularyManager.getVocabulary(HPO_LABEL));
        Assert.assertEquals(this.hpo, this.vocabularyManager.getVocabulary(HP_LABEL));
        Assert.assertEquals(this.hgnc, this.vocabularyManager.getVocabulary(HGNC_CAPS_LABEL));
        Assert.assertEquals(this.hgnc, this.vocabularyManager.getVocabulary(HGNC_LABEL));
        Assert.assertEquals(this.omim, this.vocabularyManager.getVocabulary(OMIM_LABEL));
        Assert.assertEquals(this.omim, this.vocabularyManager.getVocabulary(MIM_LABEL));
        Assert.assertEquals(this.ethnicity, this.vocabularyManager.getVocabulary(ETHNICITY_LABEL));
        Assert.assertEquals(this.ethnicity, this.vocabularyManager.getVocabulary(ETHNO_LABEL));
        Assert.assertEquals(this.chebi, this.vocabularyManager.getVocabulary(CHEBI_LABEL));
        Assert.assertEquals(this.chebi, this.vocabularyManager.getVocabulary(CHEBI_CAPS_LABEL));
    }

    @Test
    public void getAvailableVocabularies() throws Exception
    {
        final Set<String> vocabularyNameSet = this.vocabularies.keySet();
        final Set<String> result = new HashSet<>(this.vocabularyManager.getAvailableVocabularies());
        Assert.assertEquals(vocabularyNameSet, result);
        Assert.assertEquals(5, result.size());
    }

    @Test
    public void searchReturnsEmptyListIfInputIsNull()
    {
        final List<VocabularyTerm> terms = this.vocabularyManager.search(null, DISEASE_CATEGORY, 3);
        Assert.assertTrue(terms.isEmpty());
    }

    @Test
    public void searchReturnsEmptyListIfInputIsEmptyOrBlank()
    {
        final List<VocabularyTerm> emptyTerms = this.vocabularyManager.search(StringUtils.EMPTY, DISEASE_CATEGORY, 3);
        Assert.assertTrue(emptyTerms.isEmpty());

        final List<VocabularyTerm> blankTerms = this.vocabularyManager.search(" ", DISEASE_CATEGORY, 3);
        Assert.assertTrue(blankTerms.isEmpty());
    }

    @Test
    public void searchReturnsEmptyListIfCategoryIsNull()
    {
        final List<VocabularyTerm> terms = this.vocabularyManager.search(SEARCH_QUERY_ABC_LABEL, null, 3);
        Assert.assertTrue(terms.isEmpty());
    }

    @Test
    public void searchReturnsEmptyListIfCategoryIsEmptyOrBlank()
    {
        final List<VocabularyTerm> emptyTerms = this.vocabularyManager.search(SEARCH_QUERY_ABC_LABEL,
            StringUtils.EMPTY, 3);
        Assert.assertTrue(emptyTerms.isEmpty());

        final List<VocabularyTerm> blankTerms = this.vocabularyManager.search(SEARCH_QUERY_ABC_LABEL, " ", 3);
        Assert.assertTrue(blankTerms.isEmpty());
    }

    @Test
    public void searchReturnsEmptyListIfCategoryDoesNotExist() throws InitializationException
    {

        final List<VocabularyTerm> blankTerms = this.vocabularyManager.search(SEARCH_QUERY_ABC_LABEL, "No", 3);
        verify(this.logger).warn("No vocabularies associated with the specified category: {}", "No");
        Assert.assertTrue(blankTerms.isEmpty());
    }

    @Test
    public void searchReturnsSortedListOfTermsFromAllVocabulariesOfSpecifiedCategory() throws InitializationException
    {
        when(this.result1.get(SCORE_LABEL)).thenReturn((float) 1.22323);
        when(this.result2.get(SCORE_LABEL)).thenReturn((float) 2.23323);
        when(this.result3.get(SCORE_LABEL)).thenReturn((float) 0.2783);
        when(this.result4.get(SCORE_LABEL)).thenReturn((float) 3.2353);
        when(this.result5.get(SCORE_LABEL)).thenReturn((float) 3.27893);
        when(this.result6.get(SCORE_LABEL)).thenReturn((float) 1.28793);

        when(this.hgnc.search(SEARCH_QUERY_A_LABEL, GENE_CATEGORY, 3, null, null)).thenReturn(
            Arrays.asList(this.result1, this.result2, this.result3));
        when(this.omim.search(SEARCH_QUERY_A_LABEL, GENE_CATEGORY, 3, null, null)).thenReturn(
            Arrays.asList(this.result4, this.result5, this.result6));
        final List<VocabularyTerm> terms = this.vocabularyManager.search(SEARCH_QUERY_A_LABEL, GENE_CATEGORY, 3);
        Assert.assertEquals(3, terms.size());
        Assert.assertEquals(this.result5, terms.get(0));
        Assert.assertEquals(this.result4, terms.get(1));
        Assert.assertEquals(this.result2, terms.get(2));
    }

    @Test
    public void searchReturnsCorrectSortedListOfTermsIfOneVocabHasTermsWithNullForScore()
    {
        when(this.result1.get(SCORE_LABEL)).thenReturn(null);
        when(this.result2.get(SCORE_LABEL)).thenReturn(null);
        when(this.result3.get(SCORE_LABEL)).thenReturn(null);
        when(this.result4.get(SCORE_LABEL)).thenReturn((float) 3.2353);
        when(this.result5.get(SCORE_LABEL)).thenReturn((float) 3.27893);
        when(this.result6.get(SCORE_LABEL)).thenReturn((float) 1.28793);

        when(this.hgnc.search(SEARCH_QUERY_A_LABEL, GENE_CATEGORY, 3, null, null)).thenReturn(
            Arrays.asList(this.result1, this.result2, this.result3));
        when(this.omim.search(SEARCH_QUERY_A_LABEL, GENE_CATEGORY, 3, null, null)).thenReturn(
            Arrays.asList(this.result4, this.result5, this.result6));
        final List<VocabularyTerm> terms = this.vocabularyManager.search(SEARCH_QUERY_A_LABEL, GENE_CATEGORY, 3);
        Assert.assertEquals(3, terms.size());
        Assert.assertEquals(this.result5, terms.get(0));
        Assert.assertEquals(this.result4, terms.get(1));
        Assert.assertEquals(this.result6, terms.get(2));
    }
}
