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
package org.phenotips.solr;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

/**
 *  Tests for the {@link SuggestedPhenotype} class
 */
public class SuggestedPhenotypeTest
{

    private final String testID = "HP:0000118";
    private final String testName = "Test Name";
    private final double testScore = 1.0;
    private final String otherID = "!!!!!";
    private final String otherName = "!!!!!";
    private final double otherScore = -1.0;
    private SuggestedPhenotype testInstance;

    @Before
    public void setUp() throws Exception
    {
        testInstance = new SuggestedPhenotype(testID, testName, testScore);
    }

    @Test
    public void testConstructionAndGetters()
    {
        Assert.assertEquals(testID, testInstance.getId());
        Assert.assertEquals(testName, testInstance.getName());
        Assert.assertEquals(testScore, testInstance.getScore(), 0);
    }

    @Test
    public void testCompareTo()
    {
        Assert.assertEquals(0, testInstance.compareTo(null));
        Assert.assertEquals(0, testInstance.compareTo(new SuggestedPhenotype(testID, testName, testScore)));
        Assert.assertEquals(0, testInstance.compareTo(new SuggestedPhenotype(testID, testName, testScore)));
        Assert.assertEquals(0, testInstance.compareTo(new SuggestedPhenotype(otherID, otherName, testScore)));
        Assert.assertEquals(-1, testInstance.compareTo(new SuggestedPhenotype(testID, testName, (testScore - 0.001))));
        Assert.assertEquals(-1, testInstance.compareTo(new SuggestedPhenotype(testID, testName, -testScore)));
        Assert.assertEquals(-1, testInstance.compareTo(new SuggestedPhenotype(testID, testName, -999999)));
        Assert.assertEquals(1, testInstance.compareTo(new SuggestedPhenotype(testID, testName, 1.1)));
        Assert.assertEquals(1, testInstance.compareTo(new SuggestedPhenotype(testID, testName, 999999)));
    }

    @Test
    public void testEquals()
    {
        Assert.assertTrue(testInstance.equals(testInstance));
        Assert.assertTrue(testInstance.equals(new SuggestedPhenotype(testID, testName, testScore)));
        Assert.assertFalse(testInstance.equals(null));
        Assert.assertFalse(testInstance.equals(new Object()));
        SuggestedPhenotype instanceWithNullID = new SuggestedPhenotype(null, testName, testScore);
        Assert.assertFalse(instanceWithNullID.equals(testInstance));
        Assert.assertFalse(testInstance.equals(instanceWithNullID));
        Assert.assertFalse(testInstance.equals(new SuggestedPhenotype(otherID, testName, testScore)));
        Assert.assertFalse(testInstance.equals(new SuggestedPhenotype(testID, testName, otherScore)));
        Assert.assertTrue(testInstance.equals(new SuggestedPhenotype(testID, otherName, testScore)));
    }

    @Test
    public void checkHashCodeContractMet() {
        Assert.assertEquals(testInstance.hashCode(), testInstance.hashCode());
        Assert.assertEquals(testInstance.hashCode(), new SuggestedPhenotype(testID, testName, testScore).hashCode());
        Assert.assertEquals(testInstance.hashCode(), new SuggestedPhenotype(testID, otherName, testScore).hashCode());

        double randomScore = Math.random();
        String randomID = UUID.randomUUID().toString();
        String randomName = UUID.randomUUID().toString();
        Assert.assertEquals(new SuggestedPhenotype(randomID, randomName, randomScore).hashCode(),
            new SuggestedPhenotype(randomID, randomName, randomScore).hashCode());
        Assert.assertEquals(new SuggestedPhenotype(randomID, randomName, randomScore).hashCode(),
            new SuggestedPhenotype(randomID, otherID, randomScore).hashCode());
    }

    @Test
    public void testToString() {
        Assert.assertEquals(this.testInstance.toString(), this.testID + '\t' + this.testName + '\t' + this.testScore);
    }
}
