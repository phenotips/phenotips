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

import org.phenotips.vocabulary.VocabularyTerm;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultTermsForGeneImpl}.
 */
public class DefaultTermsForGeneImplTest
{
    private static final String GENE_ID = "gene";

    private static final String GENE_SYMBOL = "symbol";

    private static final String TERM_1_NAME = "term1";

    private static final String TERM_1_ID = "id1";

    private static final String TERM_2_ID = "id2";

    private static final String COUNT = "count";

    private static final String TERMS = "terms";

    private static final String GENE_ID_LABEL = "gene_id";

    private static final String GENE_SYMBOL_LABEL = "gene_symbol";

    private static final String ID_LABEL = "id";

    private static final String NAME_LABEL = "name";

    @Mock
    private VocabularyTerm term1;

    @Mock
    private VocabularyTerm term2;

    private JSONObject term1Json;

    private JSONObject term2Json;

    private DefaultTermsForGeneImpl defaultTermsForGene;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        defaultTermsForGene = new DefaultTermsForGeneImpl(GENE_SYMBOL, GENE_ID);
        when(term1.getName()).thenReturn(TERM_1_NAME);
        when(term1.getId()).thenReturn(TERM_1_ID);
        when(term2.getName()).thenReturn(null);
        when(term2.getId()).thenReturn(TERM_2_ID);

        term1Json = new JSONObject().put(ID_LABEL, TERM_1_ID).put(NAME_LABEL, TERM_1_NAME);
        term2Json = new JSONObject().put(ID_LABEL, TERM_2_ID);
        when(term1.toJSON()).thenReturn(term1Json);
        when(term2.toJSON()).thenReturn(term2Json);
    }

    @Test(expected = IllegalArgumentException.class)
    public void providingBlankSymbolThrowsException()
    {
        new DefaultTermsForGeneImpl(StringUtils.EMPTY, GENE_ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void providingBlankIdThrowsException()
    {
        new DefaultTermsForGeneImpl(GENE_SYMBOL, StringUtils.EMPTY);
    }

    @Test
    public void addTermSeveralTermsAreAddedAsExpected()
    {
        Assert.assertTrue(defaultTermsForGene.getTerms().isEmpty());
        defaultTermsForGene.addTerm(term1);
        defaultTermsForGene.addTerm(term2);
        final Collection<VocabularyTerm> terms = defaultTermsForGene.getTerms();
        Assert.assertEquals(2, terms.size());
        Assert.assertTrue(terms.contains(term2));
        Assert.assertTrue(terms.contains(term2));
    }

    @Test
    public void getCountReturnsZeroWhenNoTermsProvided()
    {
        Assert.assertEquals(0, defaultTermsForGene.getCount());
    }

    @Test
    public void getCountCountsNumberOfTermsCorrectly()
    {
        defaultTermsForGene.addTerm(term2);
        Assert.assertEquals(1, defaultTermsForGene.getCount());
        defaultTermsForGene.addTerm(term1);
        Assert.assertEquals(2, defaultTermsForGene.getCount());
    }

    @Test
    public void getGeneIdGetsExpectedId()
    {
        Assert.assertEquals(GENE_ID, defaultTermsForGene.getGeneId());
    }

    @Test
    public void getGeneSymbolGetsExpectedSymbol()
    {
        Assert.assertEquals(GENE_SYMBOL, defaultTermsForGene.getGeneSymbol());
    }

    @Test
    public void getTermsReturnsEmptyCollectionWhenNoTermsProvided()
    {
        Assert.assertTrue(defaultTermsForGene.getTerms().isEmpty());
    }

    @Test
    public void getTermsReturnsAllProvidedTermsInCorrectOrder()
    {
        defaultTermsForGene.addTerm(term1);
        defaultTermsForGene.addTerm(term2);
        final Collection<VocabularyTerm> terms = defaultTermsForGene.getTerms();
        Assert.assertEquals(2, terms.size());
        final Iterator<VocabularyTerm> iterator = terms.iterator();
        Assert.assertEquals(term2, iterator.next());
        Assert.assertEquals(term1, iterator.next());
    }

    @Test
    public void toJSONWithNoTermsBehavesAsExpected()
    {
        final JSONObject emptyJson = new JSONObject()
            .put(COUNT, 0)
            .put(TERMS, new JSONArray())
            .put(GENE_ID_LABEL, GENE_ID)
            .put(GENE_SYMBOL_LABEL, GENE_SYMBOL);

        Assert.assertTrue(emptyJson.similar(defaultTermsForGene.toJSON()));
    }

    @Test
    public void toJSONWithTermsBehavesAsExpected()
    {
        defaultTermsForGene.addTerm(term1);
        defaultTermsForGene.addTerm(term2);

        final JSONArray termsJson = new JSONArray()
            .put(term2Json)
            .put(term1Json);
        final JSONObject json = new JSONObject()
            .put(COUNT, 2)
            .put(TERMS, termsJson)
            .put(GENE_ID_LABEL, GENE_ID)
            .put(GENE_SYMBOL_LABEL, GENE_SYMBOL);

        Assert.assertTrue(json.similar(defaultTermsForGene.toJSON()));
    }
}
