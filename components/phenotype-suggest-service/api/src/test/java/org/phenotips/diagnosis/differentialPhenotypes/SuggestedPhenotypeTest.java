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
package org.phenotips.diagnosis.differentialPhenotypes;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the {@link SuggestedPhenotype} class.
 *
 * @version $Id$
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
        this.testInstance = new SuggestedPhenotype(this.testID, this.testName, this.testScore);
    }

    @Test
    public void testConstructionAndGetters()
    {
        Assert.assertEquals(this.testID, this.testInstance.getId());
        Assert.assertEquals(this.testName, this.testInstance.getName());
        Assert.assertEquals(this.testScore, this.testInstance.getScore(), 0);
    }

    @Test
    public void testCompareTo()
    {
        Assert.assertEquals(0, this.testInstance.compareTo(null));
        Assert.assertEquals(0,
            this.testInstance.compareTo(new SuggestedPhenotype(this.testID, this.testName, this.testScore)));
        Assert.assertEquals(0,
            this.testInstance.compareTo(new SuggestedPhenotype(this.testID, this.testName, this.testScore)));
        Assert.assertEquals(0,
            this.testInstance.compareTo(new SuggestedPhenotype(this.otherID, this.otherName, this.testScore)));
        Assert.assertEquals(-1,
            this.testInstance.compareTo(new SuggestedPhenotype(this.testID, this.testName, (this.testScore - 0.001))));
        Assert.assertEquals(-1,
            this.testInstance.compareTo(new SuggestedPhenotype(this.testID, this.testName, -this.testScore)));
        Assert.assertEquals(-1,
            this.testInstance.compareTo(new SuggestedPhenotype(this.testID, this.testName, -999999)));
        Assert.assertEquals(1, this.testInstance.compareTo(new SuggestedPhenotype(this.testID, this.testName, 1.1)));
        Assert.assertEquals(1, this.testInstance.compareTo(new SuggestedPhenotype(this.testID, this.testName, 999999)));
    }

    @Test
    public void testEquals()
    {
        Assert.assertTrue(this.testInstance.equals(this.testInstance));
        Assert.assertTrue(this.testInstance.equals(new SuggestedPhenotype(this.testID, this.testName, this.testScore)));
        Assert.assertFalse(this.testInstance.equals(null));
        Assert.assertFalse(this.testInstance.equals(new Object()));
        SuggestedPhenotype instanceWithNullID = new SuggestedPhenotype(null, this.testName, this.testScore);
        Assert.assertFalse(instanceWithNullID.equals(this.testInstance));
        Assert.assertFalse(this.testInstance.equals(instanceWithNullID));
        Assert
            .assertFalse(this.testInstance.equals(new SuggestedPhenotype(this.otherID, this.testName, this.testScore)));
        Assert
            .assertFalse(this.testInstance.equals(new SuggestedPhenotype(this.testID, this.testName, this.otherScore)));
        Assert
            .assertTrue(this.testInstance.equals(new SuggestedPhenotype(this.testID, this.otherName, this.testScore)));
    }

    @Test
    public void checkHashCodeContractMet()
    {
        Assert.assertEquals(this.testInstance.hashCode(), this.testInstance.hashCode());
        Assert.assertEquals(this.testInstance.hashCode(),
            new SuggestedPhenotype(this.testID, this.testName, this.testScore).hashCode());
        Assert.assertEquals(this.testInstance.hashCode(),
            new SuggestedPhenotype(this.testID, this.otherName, this.testScore).hashCode());

        double randomScore = Math.random();
        String randomID = UUID.randomUUID().toString();
        String randomName = UUID.randomUUID().toString();
        Assert.assertEquals(new SuggestedPhenotype(randomID, randomName, randomScore).hashCode(),
            new SuggestedPhenotype(randomID, randomName, randomScore).hashCode());
        Assert.assertEquals(new SuggestedPhenotype(randomID, randomName, randomScore).hashCode(),
            new SuggestedPhenotype(randomID, this.otherID, randomScore).hashCode());
    }

    @Test
    public void testToString()
    {
        Assert.assertEquals(this.testInstance.toString(), this.testID + '\t' + this.testName + '\t' + this.testScore);
    }
}
