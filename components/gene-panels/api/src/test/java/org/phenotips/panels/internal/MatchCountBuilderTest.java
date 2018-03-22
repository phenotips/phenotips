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

import org.phenotips.panels.MatchCount;
import org.phenotips.vocabulary.VocabularyTerm;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MatchCountBuilder}.
 */
public class MatchCountBuilderTest
{
    private static final String GENE_1 = "gene1";

    private static final String GENE_1_SYNONYM = "gene1_synonym";

    private static final String GENE_2 = "gene2";

    private static final String GENE_3 = "gene3";

    private static final String HP01 = "HP:01";

    private static final String HP01_NAME = "Phenotype 01";

    private static final String HP02 = "HP:02";

    private static final String HP02_NAME = "Phenotype 02";

    private static final Collection<String> HP01_GENES = Arrays.asList(GENE_1, GENE_1_SYNONYM, GENE_2, GENE_3);

    private static final Collection<String> HP02_GENES = Arrays.asList(GENE_1, null, GENE_1_SYNONYM, GENE_2, GENE_3);

    @Mock
    private VocabularyTerm feature1;

    @Mock
    private VocabularyTerm feature2;

    private MatchCountBuilder component;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        when(this.feature1.getId()).thenReturn(HP01);
        when(this.feature1.getTranslatedName()).thenReturn(HP01_NAME);
        when(this.feature2.getId()).thenReturn(HP02);
        when(this.feature2.getTranslatedName()).thenReturn(HP02_NAME);

        this.component = new MatchCountBuilder();
    }

    @Test
    public void addNonNullTerm()
    {
        this.component.add(this.feature1, HP01_GENES);
    }

    @Test
    public void buildEmpty()
    {
        Assert.assertTrue(this.component.build().isEmpty());
    }

    @Test
    public void buildNonEmptyDifferentNumGenes()
    {
        this.component.add(this.feature1, null);
        this.component.add(this.feature2, HP01_GENES);
        final List<MatchCount> matchCounts = this.component.build();
        Assert.assertEquals(2, matchCounts.size());

        final MatchCount firstObj = matchCounts.get(0);
        Assert.assertEquals(HP02, firstObj.getId());
        Assert.assertEquals(HP02_NAME, firstObj.getName());
        Assert.assertEquals(Arrays.asList(GENE_1, GENE_1_SYNONYM, GENE_2, GENE_3), firstObj.getGenes());

        final MatchCount secondObj = matchCounts.get(1);
        Assert.assertEquals(HP01, secondObj.getId());
        Assert.assertEquals(HP01_NAME, secondObj.getName());
        Assert.assertTrue(secondObj.getGenes().isEmpty());
    }

    @Test
    public void buildNonEmptySameNumGenes()
    {
        this.component.add(this.feature2, HP02_GENES);
        this.component.add(this.feature1, HP01_GENES);
        final List<MatchCount> matchCounts = this.component.build();
        Assert.assertEquals(2, matchCounts.size());

        final MatchCount firstObj = matchCounts.get(0);
        Assert.assertEquals(HP01, firstObj.getId());
        Assert.assertEquals(HP01_NAME, firstObj.getName());
        Assert.assertEquals(Arrays.asList(GENE_1, GENE_1_SYNONYM, GENE_2, GENE_3), firstObj.getGenes());

        final MatchCount secondObj = matchCounts.get(1);
        Assert.assertEquals(HP02, secondObj.getId());
        Assert.assertEquals(HP02_NAME, secondObj.getName());
        Assert.assertEquals(Arrays.asList(GENE_1, GENE_1_SYNONYM, GENE_2, GENE_3), secondObj.getGenes());
    }

    @Test
    public void buildNonEmptySameNumGenesSameNameKeepOrder()
    {
        when(this.feature2.getTranslatedName()).thenReturn(HP01_NAME);

        this.component.add(this.feature2, HP02_GENES);
        this.component.add(this.feature1, HP01_GENES);
        final List<MatchCount> matchCounts = this.component.build();
        Assert.assertEquals(2, matchCounts.size());

        final MatchCount firstObj = matchCounts.get(0);
        Assert.assertEquals(HP02, firstObj.getId());
        Assert.assertEquals(HP01_NAME, firstObj.getName());
        Assert.assertEquals(Arrays.asList(GENE_1, GENE_1_SYNONYM, GENE_2, GENE_3), firstObj.getGenes());

        final MatchCount secondObj = matchCounts.get(1);
        Assert.assertEquals(HP01, secondObj.getId());
        Assert.assertEquals(HP01_NAME, secondObj.getName());
        Assert.assertEquals(Arrays.asList(GENE_1, GENE_1_SYNONYM, GENE_2, GENE_3), secondObj.getGenes());
    }
}
