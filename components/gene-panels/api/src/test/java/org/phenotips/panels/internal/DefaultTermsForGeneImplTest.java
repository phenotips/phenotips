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
        this.defaultTermsForGene = new DefaultTermsForGeneImpl(GENE_SYMBOL, GENE_ID);
        when(this.term1.getName()).thenReturn(TERM_1_NAME);
        when(this.term1.getId()).thenReturn(TERM_1_ID);
        when(this.term2.getName()).thenReturn(null);
        when(this.term2.getId()).thenReturn(TERM_2_ID);

        this.term1Json = new JSONObject().put(ID_LABEL, TERM_1_ID).put(NAME_LABEL, TERM_1_NAME);
        this.term2Json = new JSONObject().put(ID_LABEL, TERM_2_ID);
        when(this.term1.toJSON()).thenReturn(this.term1Json);
        when(this.term2.toJSON()).thenReturn(this.term2Json);
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
        Assert.assertTrue(this.defaultTermsForGene.getTerms().isEmpty());
        this.defaultTermsForGene.addTerm(this.term1);
        this.defaultTermsForGene.addTerm(this.term2);
        final Collection<VocabularyTerm> terms = this.defaultTermsForGene.getTerms();
        Assert.assertEquals(2, terms.size());
        Assert.assertTrue(terms.contains(this.term2));
        Assert.assertTrue(terms.contains(this.term2));
    }

    @Test
    public void getCountReturnsZeroWhenNoTermsProvided()
    {
        Assert.assertEquals(0, this.defaultTermsForGene.getCount());
    }

    @Test
    public void getCountCountsNumberOfTermsCorrectly()
    {
        this.defaultTermsForGene.addTerm(this.term2);
        Assert.assertEquals(1, this.defaultTermsForGene.getCount());
        this.defaultTermsForGene.addTerm(this.term1);
        Assert.assertEquals(2, this.defaultTermsForGene.getCount());
    }

    @Test
    public void getGeneIdGetsExpectedId()
    {
        Assert.assertEquals(GENE_ID, this.defaultTermsForGene.getGeneId());
    }

    @Test
    public void getGeneSymbolGetsExpectedSymbol()
    {
        Assert.assertEquals(GENE_SYMBOL, this.defaultTermsForGene.getGeneSymbol());
    }

    @Test
    public void getTermsReturnsEmptyCollectionWhenNoTermsProvided()
    {
        Assert.assertTrue(this.defaultTermsForGene.getTerms().isEmpty());
    }

    @Test
    public void getTermsReturnsAllProvidedTermsInCorrectOrder()
    {
        this.defaultTermsForGene.addTerm(this.term1);
        this.defaultTermsForGene.addTerm(this.term2);
        final Collection<VocabularyTerm> terms = this.defaultTermsForGene.getTerms();
        Assert.assertEquals(2, terms.size());
        final Iterator<VocabularyTerm> iterator = terms.iterator();
        Assert.assertEquals(this.term2, iterator.next());
        Assert.assertEquals(this.term1, iterator.next());
    }

    @Test
    public void toJSONWithNoTermsBehavesAsExpected()
    {
        final JSONObject emptyJson = new JSONObject()
            .put(COUNT, 0)
            .put(TERMS, new JSONArray())
            .put(GENE_ID_LABEL, GENE_ID)
            .put(GENE_SYMBOL_LABEL, GENE_SYMBOL);

        Assert.assertTrue(emptyJson.similar(this.defaultTermsForGene.toJSON()));
    }

    @Test
    public void toJSONWithTermsBehavesAsExpected()
    {
        this.defaultTermsForGene.addTerm(this.term1);
        this.defaultTermsForGene.addTerm(this.term2);

        final JSONArray termsJson = new JSONArray()
            .put(this.term2Json)
            .put(this.term1Json);
        final JSONObject json = new JSONObject()
            .put(COUNT, 2)
            .put(TERMS, termsJson)
            .put(GENE_ID_LABEL, GENE_ID)
            .put(GENE_SYMBOL_LABEL, GENE_SYMBOL);

        Assert.assertTrue(json.similar(this.defaultTermsForGene.toJSON()));
    }
}
