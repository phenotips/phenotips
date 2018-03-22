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

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultMatchCountImpl}.
 */
public class DefaultMatchCountImplTest
{
    private static final String ID_LABEL = "id";

    private static final String NAME_LABEL = "label";

    private static final String COUNT_LABEL = "count";

    private static final String GENE_1 = "gene1";

    private static final String GENE_1_SYNONYM = "gene1_synonym";

    private static final String GENE_2 = "gene2";

    private static final String GENE_3 = "gene3";

    private static final String HP01 = "HP:01";

    private static final String HP01_NAME = "Phenotype 01";

    private static final String HP02 = "HP:02";

    private static final String HP02_NAME = "Phenotype 02";

    private static final Collection<String> HP01_GENES = Arrays.asList(GENE_1, null, GENE_1_SYNONYM, GENE_2, GENE_3);

    @Mock
    private VocabularyTerm feature1;

    @Mock
    private VocabularyTerm feature2;

    private MatchCount component;

    private MatchCount noGenesComponent;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        when(this.feature1.getId()).thenReturn(HP01);
        when(this.feature1.getTranslatedName()).thenReturn(HP01_NAME);

        when(this.feature2.getId()).thenReturn(HP02);
        when(this.feature2.getTranslatedName()).thenReturn(HP02_NAME);

        this.component = new DefaultMatchCountImpl(this.feature1, HP01_GENES);
        this.noGenesComponent = new DefaultMatchCountImpl(this.feature1, null);
    }

    @Test(expected = NullPointerException.class)
    public void initializingClassWithNullFeature()
    {
        new DefaultMatchCountImpl(null, HP01_GENES);
    }

    @Test
    public void getCountWithNoGenes()
    {
        Assert.assertEquals(0, this.noGenesComponent.getCount());
    }

    @Test
    public void getCountWithGenes()
    {
        Assert.assertEquals(4, this.component.getCount());
    }

    @Test
    public void getIdIsNull()
    {
        when(this.feature1.getId()).thenReturn(null);
        Assert.assertNull(this.component.getId());
    }

    @Test
    public void getIdIsNotNull()
    {
        Assert.assertEquals(HP01, this.component.getId());
    }

    @Test
    public void getNameIsNull()
    {
        when(this.feature1.getTranslatedName()).thenReturn(null);
        Assert.assertNull(this.component.getName());
    }

    @Test
    public void getNameIsNotNull()
    {
        Assert.assertEquals(HP01_NAME, this.component.getName());
    }

    @Test
    public void getGenesNoGenes()
    {
        Assert.assertTrue(this.noGenesComponent.getGenes().isEmpty());
    }

    @Test
    public void getGenesWithGenes()
    {
        Assert.assertEquals(Arrays.asList(GENE_1, GENE_1_SYNONYM, GENE_2, GENE_3), this.component.getGenes());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getGenesResultIsUnmodifiable()
    {
        this.component.getGenes().add(GENE_3);
    }

    @Test
    public void toJSONWithNullId()
    {
        when(this.feature1.getId()).thenReturn(null);
        final JSONObject actual = this.component.toJSON();
        final JSONObject expected = new JSONObject().put(NAME_LABEL, HP01_NAME).put(COUNT_LABEL, 4);
        Assert.assertTrue(expected.similar(actual));
    }

    @Test
    public void toJSONWithNullName()
    {
        when(this.feature1.getTranslatedName()).thenReturn(null);
        final JSONObject actual = this.component.toJSON();
        final JSONObject expected = new JSONObject().put(ID_LABEL, HP01).put(COUNT_LABEL, 4);
        Assert.assertTrue(expected.similar(actual));
    }

    @Test
    public void toJSONWithNoGenes()
    {
        final JSONObject actual = this.noGenesComponent.toJSON();
        final JSONObject expected = new JSONObject().put(ID_LABEL, HP01).put(NAME_LABEL, HP01_NAME).put(COUNT_LABEL, 0);
        Assert.assertTrue(expected.similar(actual));
    }

    @Test
    public void toJSONWithGenes()
    {
        final JSONObject actual = this.component.toJSON();
        final JSONObject expected = new JSONObject().put(ID_LABEL, HP01).put(NAME_LABEL, HP01_NAME).put(COUNT_LABEL, 4);
        Assert.assertTrue(expected.similar(actual));
    }

    @Test
    public void equalsSameIdDifferentGenes()
    {
        Assert.assertFalse(this.component.equals(this.noGenesComponent));
    }

    @Test
    public void equalsSelf()
    {
        Assert.assertTrue(this.component.equals(this.component));
    }

    @Test
    public void equalsSame()
    {
        Assert.assertTrue(this.component.equals(new DefaultMatchCountImpl(this.feature1, HP01_GENES)));
    }

    @Test
    public void equalsDifferentIds()
    {
        when(this.feature2.getTranslatedName()).thenReturn(HP01_NAME);
        Assert.assertFalse(this.component.equals(new DefaultMatchCountImpl(this.feature2, HP01_GENES)));
    }

    @Test
    public void equalsDifferentNames()
    {
        when(this.feature2.getId()).thenReturn(HP01);
        Assert.assertFalse(this.component.equals(new DefaultMatchCountImpl(this.feature2, HP01_GENES)));
    }

    @Test
    public void hashCodeSame()
    {
        Assert.assertEquals(this.component.hashCode(), new DefaultMatchCountImpl(this.feature1, HP01_GENES).hashCode());
    }

    @Test
    public void hashCodeDifferent()
    {
        Assert.assertNotEquals(this.component.hashCode(), this.noGenesComponent.hashCode());
    }

    @Test
    public void compareToSelf()
    {
        Assert.assertEquals(0, this.component.compareTo(this.component));
    }

    @Test
    public void compareToSame()
    {
        Assert.assertEquals(0, this.component.compareTo(new DefaultMatchCountImpl(this.feature1, HP01_GENES)));
    }

    @Test
    public void compareToGreaterCount()
    {
        Assert.assertEquals(-1, this.component.compareTo(this.noGenesComponent));
    }

    @Test
    public void compareToSmallerCount()
    {
        Assert.assertEquals(1, this.noGenesComponent.compareTo(this.component));
    }

    @Test
    public void compareToSameCountGreaterNameVal()
    {
        Assert.assertEquals(-1, this.component.compareTo(new DefaultMatchCountImpl(this.feature2, HP01_GENES)));
    }

    @Test
    public void compareToSameCountSameNameDifferentId()
    {
        when(this.feature2.getTranslatedName()).thenReturn(HP01_NAME);
        Assert.assertEquals(0, this.component.compareTo(new DefaultMatchCountImpl(this.feature2, HP01_GENES)));
    }
}
