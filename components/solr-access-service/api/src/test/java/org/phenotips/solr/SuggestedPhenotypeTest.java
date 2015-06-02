package org.phenotips.solr;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class SuggestedPhenotypeTest
{

    private final String testID = "123";
    private final String testName = "Test Name";
    private final double testScore = 1.0;
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
        Assert.assertEquals(0, testInstance.compareTo(new SuggestedPhenotype("!!!!!", "!!!!!", testScore)));
        Assert.assertEquals(-1, testInstance.compareTo(new SuggestedPhenotype(testID, testName, (testScore - 0.001))));
        Assert.assertEquals(-1, testInstance.compareTo(new SuggestedPhenotype(testID, testName, -testScore)));
        Assert.assertEquals(-1, testInstance.compareTo(new SuggestedPhenotype(testID, testName, -999999)));
        Assert.assertEquals(1, testInstance.compareTo(new SuggestedPhenotype(testID, testName, 1.1)));
        Assert.assertEquals(1, testInstance.compareTo(new SuggestedPhenotype(testID, testName, 999999)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEquals()
    {
        Assert.assertTrue(testInstance.equals(testInstance));
        Assert.assertTrue(testInstance.equals(new SuggestedPhenotype(testID, testName, testScore)));
        Assert.assertFalse(testInstance.equals(null));
        Assert.assertFalse(testInstance.equals(new Object()));
        SuggestedPhenotype instanceWithNullID = new SuggestedPhenotype(null, testName, testScore);
        Assert.assertFalse(instanceWithNullID.equals(testInstance));
        Assert.assertFalse(testInstance.equals(instanceWithNullID));
        Assert.assertFalse(testInstance.equals(new SuggestedPhenotype("!!!!!", testName, testScore)));
        Assert.assertTrue(testInstance.equals(new SuggestedPhenotype(testID, "!!!!!", testScore)));
    }
}
