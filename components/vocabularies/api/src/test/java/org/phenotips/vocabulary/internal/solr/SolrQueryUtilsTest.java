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
package org.phenotips.vocabulary.internal.solr;

import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.params.SpellingParams;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the {@link SolrQueryUtils} utility class.
 *
 * @version $Id$
 */
public class SolrQueryUtilsTest
{
    @Test
    public void testTransformQueryToSolrParams()
    {
        SolrParams output = SolrQueryUtils.transformQueryToSolrParams("field:value");
        Assert.assertEquals("field:value", output.get(CommonParams.Q));
    }

    @Test
    public void testTransformQueryToSolrParamsWithNullValue()
    {
        SolrParams output = SolrQueryUtils.transformQueryToSolrParams(null);
        Assert.assertNull(output.get(CommonParams.Q));
    }

    @Test
    public void testEnhanceParamsDefaultValues()
    {
        ModifiableSolrParams input = new ModifiableSolrParams();
        SolrParams output = SolrQueryUtils.enhanceParams(input);
        Assert.assertNull(output.get(CommonParams.Q));
        Assert.assertEquals("* score", output.get(CommonParams.FL));
        Assert.assertEquals(true, output.getBool(SpellingParams.SPELLCHECK_COLLATE));
        Assert.assertEquals(0, (int) output.getInt(CommonParams.START));
        Assert.assertTrue(output.getInt(CommonParams.ROWS) > 100);
    }

    @Test
    public void testEnhanceParamsDoesntReplaceExistingValues()
    {
        ModifiableSolrParams input = new ModifiableSolrParams();
        input.set(CommonParams.Q, "field:value");
        input.set(CommonParams.FL, "id");
        input.set(CommonParams.START, 30);
        input.set(CommonParams.ROWS, 10);
        SolrParams output = SolrQueryUtils.enhanceParams(input);
        Assert.assertEquals("field:value", output.get(CommonParams.Q));
        Assert.assertEquals("id", output.get(CommonParams.FL));
        Assert.assertEquals(true, output.getBool(SpellingParams.SPELLCHECK_COLLATE));
        Assert.assertEquals(30, (int) output.getInt(CommonParams.START));
        Assert.assertEquals(10, (int) output.getInt(CommonParams.ROWS));
    }

    @Test
    public void testEnhanceParamsWithNull()
    {
        Assert.assertNull(SolrQueryUtils.enhanceParams(null));
    }

    @Test
    public void testApplySpellcheckSuggestions()
    {
        ModifiableSolrParams input = new ModifiableSolrParams();
        input.set(CommonParams.Q, "original");
        SolrParams output = SolrQueryUtils.applySpellcheckSuggestion(input, "fixed");
        Assert.assertNotNull(output);
        Assert.assertEquals("fixed", output.get(CommonParams.Q));
    }

    @Test
    public void testApplySpellcheckSuggestionsWithBoostQuery()
    {
        ModifiableSolrParams input = new ModifiableSolrParams();
        input.set(CommonParams.Q, "original with text:stab*");
        input.set(DisMaxParams.BQ, "text:stab* name:stab*^5");
        SolrParams output = SolrQueryUtils.applySpellcheckSuggestion(input, "fixed with text:stub*");
        Assert.assertNotNull(output);
        Assert.assertEquals("fixed with text:stub* text:stab*^1.5", output.get(CommonParams.Q));
        Assert.assertArrayEquals(new String[] { "text:stab* name:stab*^5", "text:stub* name:stub*^5" },
            output.getParams(DisMaxParams.BQ));
    }

    @Test
    public void testApplySpellcheckSuggestionsWithNull()
    {
        SolrParams output = SolrQueryUtils.applySpellcheckSuggestion(null, null);
        Assert.assertNull(output);

        ModifiableSolrParams input = new ModifiableSolrParams();
        input.set(CommonParams.Q, "original");
        output = SolrQueryUtils.applySpellcheckSuggestion(input, null);
        Assert.assertNotNull(output);
        Assert.assertEquals("original", output.get(CommonParams.Q));

        output = SolrQueryUtils.applySpellcheckSuggestion(null, "fixed");
        Assert.assertNull(output);
    }

    @Test
    public void testGetCacheKey()
    {
        ModifiableSolrParams input = new ModifiableSolrParams();
        input.set(CommonParams.Q, "some value");
        input.add(DisMaxParams.BQ, "text:stub*");
        input.add(DisMaxParams.BQ, "stub*");
        input.set(CommonParams.FQ, "is_a:HP\\:0000108");
        String output = SolrQueryUtils.getCacheKey(input);
        Assert.assertEquals("{q:[some value]\nbq:[text:stub*, stub*]\nfq:[is_a:HP\\:0000108]\n}", output);
    }
}
