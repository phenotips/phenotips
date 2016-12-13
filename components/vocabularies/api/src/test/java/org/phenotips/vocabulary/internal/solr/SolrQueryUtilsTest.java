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

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;
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
    public void testEnhanceParamsDefaultValues()
    {
        SolrQuery input = new SolrQuery();
        SolrQuery output = SolrQueryUtils.generateQuery(input, null);
        Assert.assertNull(output.get(CommonParams.Q));
        Assert.assertEquals("*,score", output.get(CommonParams.FL));
        Assert.assertEquals(true, output.getBool(SpellingParams.SPELLCHECK_COLLATE));
        Assert.assertEquals(0, (int) output.getInt(CommonParams.START));
        Assert.assertTrue(output.getInt(CommonParams.ROWS) > 100);
    }

    @Test
    public void testEnhanceParamsDoesntReplaceExistingValues()
    {
        SolrQuery input = new SolrQuery("field:value");
        input.set(CommonParams.FL, "id");
        input.setStart(30);
        input.setRows(10);
        SolrQuery output = SolrQueryUtils.generateQuery(input, null);
        Assert.assertEquals("field:value", output.get(CommonParams.Q));
        Assert.assertEquals("id", output.get(CommonParams.FL));
        Assert.assertEquals(true, output.getBool(SpellingParams.SPELLCHECK_COLLATE));
        Assert.assertEquals(30, (int) output.getInt(CommonParams.START));
        Assert.assertEquals(10, (int) output.getInt(CommonParams.ROWS));
    }

    @Test
    public void testEnhanceParamsWithNull()
    {
        Assert.assertNull(SolrQueryUtils.generateQuery(null, null));
    }

    @Test
    public void testApplySpellcheckSuggestions()
    {
        SolrQuery input = new SolrQuery("original");
        SolrQuery output = SolrQueryUtils.applySpellcheckSuggestion(input, "fixed");
        Assert.assertNotNull(output);
        Assert.assertEquals("fixed", output.get(CommonParams.Q));
    }

    @Test
    public void testApplySpellcheckSuggestionsWithBoostQuery()
    {
        SolrQuery input = new SolrQuery("original with text:stab*");
        input.set(DisMaxParams.BQ, "text:stab* name:stab*^5");
        SolrQuery output = SolrQueryUtils.applySpellcheckSuggestion(input, "fixed with text:stub*");
        Assert.assertNotNull(output);
        Assert.assertEquals("fixed with text:stub* text:stab*^1.5", output.get(CommonParams.Q));
        Assert.assertArrayEquals(new String[] { "text:stab* name:stab*^5", "text:stub* name:stub*^5" },
            output.getParams(DisMaxParams.BQ));
    }

    @Test
    public void testApplySpellcheckSuggestionsWithNull()
    {
        SolrQuery output = SolrQueryUtils.applySpellcheckSuggestion(null, null);
        Assert.assertNull(output);

        SolrQuery input = new SolrQuery("original");
        output = SolrQueryUtils.applySpellcheckSuggestion(input, null);
        Assert.assertNotNull(output);
        Assert.assertEquals("original", output.get(CommonParams.Q));

        output = SolrQueryUtils.applySpellcheckSuggestion(null, "fixed");
        Assert.assertNull(output);
    }
}
