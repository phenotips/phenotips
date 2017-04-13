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

import org.phenotips.panels.TermsForGene;
import org.phenotips.vocabulary.VocabularyTerm;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TermsForGeneBuilder}.
 */
public class TermsForGeneBuilderTest
{
    private static final String EX_GENE_1_ID = "ex_gene1";

    private static final String EX_GENE_3_ID = "ex_gene3";

    private static final String EX_GENE_1_SYMBOL = "ex_symbol1";

    private static final String EX_GENE_2_SYMBOL = "ex_symbol2";

    private static final String EX_GENE_3_SYMBOL = "ex_symbol3";

    private static final String EX_GENE_1_ALIAS_A = "ex_alias_1a";

    private static final String EX_GENE_1_ALIAS_B = "ex_alias_1b";

    private static final String GENE_1_SYMBOL = "ex_alias_1b";

    private static final String GENE_2_SYMBOL = "symbol2";

    private static final String GENE_3_SYMBOL = "symbol3";

    private static final List<String> EX_GENE_1_ALIASES = Arrays.asList(EX_GENE_1_ALIAS_A, EX_GENE_1_ALIAS_B);

    private static final String TERM_1_NAME = "term1";

    private static final String TERM_2_NAME = "term2";

    private static final String TERM_3_NAME = "term3";

    private static final String SYMBOL_LABEL = "symbol";

    private static final String ENSEMBL_ID_LABEL = "ensembl_gene_id";

    private static final String SYMBOL_ALIAS_LABEL = "symbol_alias";

    @Mock
    private VocabularyTerm excludedGene1;

    @Mock
    private VocabularyTerm excludedGene2;

    @Mock
    private VocabularyTerm excludedGene3;

    @Mock
    private VocabularyTerm term1;

    @Mock
    private VocabularyTerm term2;

    @Mock
    private VocabularyTerm term3;

    private TermsForGeneBuilder builder;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        when(excludedGene1.get(SYMBOL_LABEL)).thenReturn(EX_GENE_1_SYMBOL);
        when(excludedGene1.get(ENSEMBL_ID_LABEL)).thenReturn(Collections.singletonList(EX_GENE_1_ID));
        when(excludedGene1.get(SYMBOL_ALIAS_LABEL)).thenReturn(EX_GENE_1_ALIASES);

        when(excludedGene2.get(SYMBOL_LABEL)).thenReturn(EX_GENE_2_SYMBOL);
        when(excludedGene2.get(ENSEMBL_ID_LABEL)).thenReturn(null);
        when(excludedGene2.get(SYMBOL_ALIAS_LABEL)).thenReturn(Collections.emptySet());

        when(excludedGene3.get(SYMBOL_LABEL)).thenReturn(EX_GENE_3_SYMBOL);
        when(excludedGene3.get(ENSEMBL_ID_LABEL)).thenReturn(Collections.singletonList(EX_GENE_3_ID));
        when(excludedGene3.get(SYMBOL_ALIAS_LABEL)).thenReturn(EX_GENE_1_ALIASES);

        when(term1.getName()).thenReturn(TERM_1_NAME);
        when(term2.getName()).thenReturn(TERM_2_NAME);
        when(term3.getName()).thenReturn(TERM_3_NAME);

        builder = new TermsForGeneBuilder(Arrays.asList(excludedGene1, excludedGene2, excludedGene3));
    }

    @Test(expected = NullPointerException.class)
    public void updateGeneSymbolIsNullThrowsException()
    {
        builder.update(null, term1);
    }

    @Test(expected = NullPointerException.class)
    public void updateTermIsNullThrowsException()
    {
        builder.update(GENE_2_SYMBOL, null);
    }

    @Test(expected = NullPointerException.class)
    public void updateGeneSymbolNotYetStoredThrowsException()
    {
        builder.update(GENE_2_SYMBOL, term1);
    }

    @Test
    public void updateAndAddTermIsAddedForSymbol()
    {
        // Test that add works as expected.
        builder.add(GENE_2_SYMBOL, GENE_2_SYMBOL, term1);
        Collection<TermsForGene> termsForGenes = builder.build();
        Assert.assertEquals(1, termsForGenes.size());
        TermsForGene firstItem = termsForGenes.iterator().next();
        Assert.assertEquals(1, firstItem.getCount());
        Assert.assertEquals(GENE_2_SYMBOL, firstItem.getGeneId());
        Assert.assertEquals(GENE_2_SYMBOL, firstItem.getGeneSymbol());
        Assert.assertEquals(1, firstItem.getTerms().size());
        Assert.assertTrue(firstItem.getTerms().contains(term1));

        // Test that duplicate terms are not added.
        builder.update(GENE_2_SYMBOL, term1);
        termsForGenes = builder.build();
        Assert.assertEquals(1, termsForGenes.size());
        firstItem = termsForGenes.iterator().next();
        Assert.assertEquals(1, firstItem.getCount());
        Assert.assertEquals(GENE_2_SYMBOL, firstItem.getGeneId());
        Assert.assertEquals(GENE_2_SYMBOL, firstItem.getGeneSymbol());
        Assert.assertEquals(1, firstItem.getTerms().size());
        Assert.assertTrue(firstItem.getTerms().contains(term1));

        // Test that update works as expected for non-duplicate terms.
        builder.update(GENE_2_SYMBOL, term2);
        termsForGenes = builder.build();
        Assert.assertEquals(1, termsForGenes.size());
        firstItem = termsForGenes.iterator().next();
        Assert.assertEquals(2, firstItem.getCount());
        Assert.assertEquals(GENE_2_SYMBOL, firstItem.getGeneId());
        Assert.assertEquals(GENE_2_SYMBOL, firstItem.getGeneSymbol());
        Assert.assertEquals(2, firstItem.getTerms().size());
        Assert.assertTrue(firstItem.getTerms().contains(term1));
        Assert.assertTrue(firstItem.getTerms().contains(term2));
    }

    @Test(expected = NullPointerException.class)
    public void addGeneIdNullThrowsException()
    {
        builder.add(GENE_2_SYMBOL, null, term2);
    }

    @Test(expected = NullPointerException.class)
    public void addGeneSymbolNullThrowsException()
    {
        builder.add(null, GENE_2_SYMBOL, term2);
    }

    @Test(expected = NullPointerException.class)
    public void addTermNullThrowsException()
    {
        builder.add(GENE_2_SYMBOL, GENE_2_SYMBOL, null);
    }

    @Test
    public void addIfGeneSymbolAlreadyStoredItGetsOverwritten()
    {
        // Add first item.
        builder.add(GENE_2_SYMBOL, GENE_2_SYMBOL, term1);
        Collection<TermsForGene> termsForGenes = builder.build();
        Assert.assertEquals(1, termsForGenes.size());
        TermsForGene firstItem = termsForGenes.iterator().next();
        Assert.assertEquals(1, firstItem.getCount());
        Assert.assertEquals(GENE_2_SYMBOL, firstItem.getGeneId());
        Assert.assertEquals(GENE_2_SYMBOL, firstItem.getGeneSymbol());
        Assert.assertEquals(1, firstItem.getTerms().size());
        Assert.assertTrue(firstItem.getTerms().contains(term1));

        // Add the same gene symbol.
        builder.add(GENE_2_SYMBOL, GENE_2_SYMBOL, term2);
        termsForGenes = builder.build();
        Assert.assertEquals(1, termsForGenes.size());
        firstItem = termsForGenes.iterator().next();
        Assert.assertEquals(1, firstItem.getCount());
        Assert.assertEquals(GENE_2_SYMBOL, firstItem.getGeneId());
        Assert.assertEquals(GENE_2_SYMBOL, firstItem.getGeneSymbol());
        Assert.assertEquals(1, firstItem.getTerms().size());
        Assert.assertTrue(firstItem.getTerms().contains(term2));
    }

    @Test
    public void addANewTermIsNotAddedIfItShouldBeExcluded()
    {
        // EX_GENE_1_ALIAS_A is in exclusions, it shouldn't be added.
        builder.add(GENE_2_SYMBOL, EX_GENE_1_ALIAS_A, term1);
        Collection<TermsForGene> termsForGenes = builder.build();
        Assert.assertEquals(0, termsForGenes.size());

        builder.add(EX_GENE_1_ALIAS_A, GENE_2_SYMBOL, term1);
        termsForGenes = builder.build();
        Assert.assertEquals(0, termsForGenes.size());
    }

    @Test
    public void containsReturnsFalseForNull()
    {
        Assert.assertFalse(builder.contains(null));
    }

    @Test
    public void containsReturnsFalseIfGeneSymbolNotYetStored()
    {
        Assert.assertFalse(builder.contains(EX_GENE_1_ALIAS_A));
    }

    @Test
    public void containsReturnsTrueIfGeneSymbolIsStored()
    {
        builder.add(GENE_2_SYMBOL, GENE_2_SYMBOL, term1);
        Assert.assertTrue(builder.contains(GENE_2_SYMBOL));
    }

    @Test
    public void buildMakesEmptyListIfNoTermsStored()
    {
        final List<TermsForGene> termsForGenes = builder.build();
        Assert.assertTrue(termsForGenes.isEmpty());
    }

    @Test
    public void buildMakesExpectedList()
    {
        // GENE_1_SYMBOL won't be added since it's an exclusion.
        builder.add(GENE_1_SYMBOL, GENE_1_SYMBOL, term1);
        builder.add(GENE_2_SYMBOL, GENE_2_SYMBOL, term1);
        builder.update(GENE_2_SYMBOL, term2);
        builder.add(GENE_3_SYMBOL, GENE_3_SYMBOL, term3);
        final List<TermsForGene> termsForGenes = builder.build();
        Assert.assertEquals(2, termsForGenes.size());
        final Iterator<TermsForGene> iterator = termsForGenes.iterator();
        final TermsForGene firstTerm = iterator.next();
        Assert.assertEquals(2, firstTerm.getCount());
        Assert.assertEquals(GENE_2_SYMBOL, firstTerm.getGeneSymbol());
        Assert.assertEquals(GENE_2_SYMBOL, firstTerm.getGeneId());
        Assert.assertEquals(2, firstTerm.getTerms().size());
        Assert.assertTrue(firstTerm.getTerms().contains(term1));
        Assert.assertTrue(firstTerm.getTerms().contains(term2));

        final TermsForGene secondTerm = iterator.next();
        Assert.assertEquals(1, secondTerm.getCount());
        Assert.assertEquals(GENE_3_SYMBOL, secondTerm.getGeneSymbol());
        Assert.assertEquals(GENE_3_SYMBOL, secondTerm.getGeneId());
        Assert.assertEquals(1, secondTerm.getTerms().size());
        Assert.assertTrue(secondTerm.getTerms().contains(term3));
    }
}
