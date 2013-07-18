/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.data.similarity.internal;

import org.phenotips.data.PhenotypeMetadatum;
import org.phenotips.data.similarity.PhenotypeMetadatumSimilarityScorer;
import org.phenotips.data.similarity.internal.PaceOfProgressionPhenotypeMetadatumSimilarityScorer;
import org.phenotips.data.similarity.internal.mocks.MockPhenotypeMetadatum;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests for the custom {@link PhenotypeMetadatumSimilarityScorer} implementation for
 * {@link PaceOfProgressionPhenotypeMetadatumSimilarityScorerTest pace of progression}.
 * 
 * @version $Id$
 */
public class PaceOfProgressionPhenotypeMetadatumSimilarityScorerTest
{
    @Rule
    public final MockitoComponentMockingRule<PhenotypeMetadatumSimilarityScorer> mocker =
        new MockitoComponentMockingRule<PhenotypeMetadatumSimilarityScorer>(
            PaceOfProgressionPhenotypeMetadatumSimilarityScorer.class);

    /** Same term should get the maximum score. */
    @Test
    public void testEqualValues() throws ComponentLookupException
    {
        PhenotypeMetadatum match = new MockPhenotypeMetadatum("HP:0003680", "Nonprogressive disorder", "pace");
        PhenotypeMetadatum reference = new MockPhenotypeMetadatum("HP:0003680", "Nonprogressive disorder", "pace");
        Assert.assertEquals(1.0, this.mocker.getComponentUnderTest().getScore(match, reference), 1.0E-5);
    }

    /** Opposite terms should get the minimum score. */
    @Test
    public void testOppositeValues() throws ComponentLookupException
    {
        PhenotypeMetadatum match = new MockPhenotypeMetadatum("HP:0003680", "Nonprogressive disorder", "pace");
        PhenotypeMetadatum reference = new MockPhenotypeMetadatum("HP:0003678", "Rapidly progressive", "pace");
        Assert.assertEquals(-1.0, this.mocker.getComponentUnderTest().getScore(match, reference), 1.0E-5);
    }

    /** Very similar values should get a high score, but not maximum. */
    @Test
    public void testCloseValues() throws ComponentLookupException
    {
        PhenotypeMetadatum match = new MockPhenotypeMetadatum("HP:0003680", "Nonprogressive disorder", "pace");
        PhenotypeMetadatum reference = new MockPhenotypeMetadatum("HP:0003677", "Slow progression", "pace");
        Assert.assertEquals(0.6, this.mocker.getComponentUnderTest().getScore(match, reference), 0.1);

        match = new MockPhenotypeMetadatum("HP:0003676", "Progressive disorder", "pace");
        reference = new MockPhenotypeMetadatum("HP:0003677", "Progressive disorder", "pace");
        Assert.assertEquals(0.3, this.mocker.getComponentUnderTest().getScore(match, reference), 0.1);
    }

    /** Variable progression should give 0 score, no matter what the other value is. */
    @Test
    public void testVariableProgression() throws ComponentLookupException
    {
        PhenotypeMetadatum match = new MockPhenotypeMetadatum("HP:0003682", "Variable progression", "pace");
        PhenotypeMetadatum reference = new MockPhenotypeMetadatum("HP:0003677", "Slow progression", "pace");
        Assert.assertEquals(0.0, this.mocker.getComponentUnderTest().getScore(match, reference), 1.0E-5);
        Assert.assertEquals(0.0, this.mocker.getComponentUnderTest().getScore(reference, match), 1.0E-5);
    }

    /** Unknown progression terms should get a zero score. */
    @Test
    public void testUnknownValues() throws ComponentLookupException
    {
        PhenotypeMetadatum match = new MockPhenotypeMetadatum("HP:0123456", "Very slow progression", "pace");
        PhenotypeMetadatum reference = new MockPhenotypeMetadatum("HP:0003680", "Nonprogressive disorder", "pace");
        Assert.assertEquals(0.0, this.mocker.getComponentUnderTest().getScore(match, reference), 1.0E-5);
        Assert.assertEquals(0.0, this.mocker.getComponentUnderTest().getScore(reference, match), 1.0E-5);
    }

    /** Missing reference should get a zero score. */
    @Test
    public void testMissingReference() throws ComponentLookupException
    {
        PhenotypeMetadatum match = new MockPhenotypeMetadatum("HP:0003680", "Nonprogressive disorder", "pace");
        Assert.assertEquals(0.0, this.mocker.getComponentUnderTest().getScore(match, null), 1.0E-5);
    }

    /** Missing match should get a zero score. */
    @Test
    public void testMissingMatch() throws ComponentLookupException
    {
        PhenotypeMetadatum reference = new MockPhenotypeMetadatum("HP:0003680", "Nonprogressive disorder", "pace");
        Assert.assertEquals(0.0, this.mocker.getComponentUnderTest().getScore(null, reference), 1.0E-5);
    }

    /** Missing both match and reference should get a zero score. */
    @Test
    public void testMissingMatchAndReference() throws ComponentLookupException
    {
        Assert.assertEquals(0.0, this.mocker.getComponentUnderTest().getScore(null, null), 1.0E-5);
    }

    /** Empty values in the reference should get a zero score. */
    @Test
    public void testEmptyReference() throws ComponentLookupException
    {
        PhenotypeMetadatum match = new MockPhenotypeMetadatum("HP:0003680", "Nonprogressive disorder", "pace");
        PhenotypeMetadatum reference = new MockPhenotypeMetadatum(null, null, null);
        Assert.assertEquals(0.0, this.mocker.getComponentUnderTest().getScore(match, reference), 1.0E-5);
    }

    /** Empty values in the match should get a zero score. */
    @Test
    public void testEmptyMatch() throws ComponentLookupException
    {
        PhenotypeMetadatum match = new MockPhenotypeMetadatum(null, null, null);
        PhenotypeMetadatum reference = new MockPhenotypeMetadatum("HP:0003680", "Nonprogressive disorder", "pace");
        Assert.assertEquals(0.0, this.mocker.getComponentUnderTest().getScore(match, reference), 1.0E-5);
    }
}
