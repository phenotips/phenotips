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
package edu.toronto.cs.cidb.tools;

import junit.framework.Assert;

import org.junit.Test;
import org.xwiki.csrf.internal.DefaultCSRFToken;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

/**
 * Tests for the {@link DefaultCSRFToken} component.
 * 
 * @version $Id$
 * @since 2.5M2
 */
public class PercentileToolsTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement
    private PercentileTools tool;

    @Test
    public void testPercentileComputation()
    {
        Assert.assertEquals(0, this.tool.valueToPercentile(Double.MIN_VALUE, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(0, this.tool.valueToPercentile(-1, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(0, this.tool.valueToPercentile(0, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(0, this.tool.valueToPercentile(1, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(2, this.tool.valueToPercentile(2.114041, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(5, this.tool.valueToPercentile(2.179956, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(10, this.tool.valueToPercentile(2.250293, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(25, this.tool.valueToPercentile(2.374837, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(50, this.tool.valueToPercentile(2.5244, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(75, this.tool.valueToPercentile(2.686987, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(90, this.tool.valueToPercentile(2.84566, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(95, this.tool.valueToPercentile(2.946724, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(98, this.tool.valueToPercentile(3.050268, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(100, this.tool.valueToPercentile(3.5, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(100, this.tool.valueToPercentile(20, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(100, this.tool.valueToPercentile(1000, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(100, this.tool.valueToPercentile(Double.MAX_VALUE, 2.5244, -0.3521, 0.09153));
    }

    @Test
    public void testValueComputation()
    {
        // Values taken from the CDC data tables (Weight for age, boys, 0.5 months)
        double x = this.tool.percentileToValue(3, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(2.799548641, x, 1.0E-8);
        x = this.tool.percentileToValue(5, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(2.964655655, x, 1.0E-8);
        x = this.tool.percentileToValue(10, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(3.209510017, x, 1.0E-8);
        x = this.tool.percentileToValue(25, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(3.597395573, x, 1.0E-8);
        x = this.tool.percentileToValue(50, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(4.003106424, x, 1.0E-8);
        x = this.tool.percentileToValue(75, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(4.387422565, x, 1.0E-8);
        x = this.tool.percentileToValue(90, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(4.718161283, x, 1.0E-8);
        x = this.tool.percentileToValue(95, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(4.910130108, x, 1.0E-8);
        x = this.tool.percentileToValue(97, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(5.032624982, x, 1.0E-8);
        // Values taken from the CDC data tables (Weight for age, boys, 9.5 months)
        x = this.tool.percentileToValue(3, 9.476500305, -0.1600954, 0.11218624);
        Assert.assertEquals(7.700624405, x, 1.0E-8);
        x = this.tool.percentileToValue(90, 9.476500305, -0.1600954, 0.11218624);
        Assert.assertEquals(10.96017225, x, 1.0E-8);
        // Don't expect a child with +- Infinity kilograms...
        x = this.tool.percentileToValue(0, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(2.089641107, x, 1.0E-8);
        x = this.tool.percentileToValue(100, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(5.498638677, x, 1.0E-8);
        // Correct out of range percentiles
        x = this.tool.percentileToValue(Integer.MIN_VALUE, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(2.089641107, x, 1.0E-8);
        x = this.tool.percentileToValue(-50, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(2.089641107, x, 1.0E-8);
        x = this.tool.percentileToValue(1000, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(5.498638677, x, 1.0E-8);
        x = this.tool.percentileToValue(Integer.MAX_VALUE, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(5.498638677, x, 1.0E-8);
    }

    @Test
    public void testGetBMI()
    {
        Assert.assertEquals(100.0, this.tool.getBMI(100, 100));
        Assert.assertEquals(31.25, this.tool.getBMI(80, 160));
        Assert.assertEquals(0.0, this.tool.getBMI(0, 0));
        Assert.assertEquals(0.0, this.tool.getBMI(80, 0));
        Assert.assertEquals(0.0, this.tool.getBMI(0, 120));
        Assert.assertEquals(0.0, this.tool.getBMI(-80, -160));
    }

    @Test
    public void testGetBMIPercentile()
    {
        Assert.assertEquals(50, this.tool.getBMIPercentile(true, 0, 3.34, 49.9));
        Assert.assertEquals(50, this.tool.getBMIPercentile(false, 0, 3.32, 49.9));
        Assert.assertEquals(0, this.tool.getBMIPercentile(true, 0, 1, 1000));
        Assert.assertEquals(100, this.tool.getBMIPercentile(true, 0, 1000, 1));
        Assert.assertEquals(0, this.tool.getBMIPercentile(false, 0, 1, 1000));
        Assert.assertEquals(100, this.tool.getBMIPercentile(false, 0, 1000, 1));
        Assert.assertEquals(0, this.tool.getBMIPercentile(false, 0, 0, 0));
        Assert.assertEquals(10, this.tool.getBMIPercentile(true, 42, 14.49, 100.0));
        Assert.assertEquals(90, this.tool.getBMIPercentile(false, 42, 17.36, 100.0));
        Assert.assertEquals(0, this.tool.getBMIPercentile(true, 100, 18, 130.0));
        Assert.assertEquals(100, this.tool.getBMIPercentile(true, 100, 90, 110.0));
        Assert.assertEquals(16, this.tool.getBMIPercentile(true, 349, 67.0, 181.0));
        Assert.assertEquals(0, this.tool.getBMIPercentile(false, 359, 49.0, 173.0));
    }

    @Test
    public void testGetPercentileBMI()
    {
        Assert.assertEquals(13.4, this.tool.getPercentileBMI(true, 0, 50), 1.0E-2);
        Assert.assertEquals(13.34, this.tool.getPercentileBMI(false, 0, 50), 1.0E-2);
        Assert.assertEquals(10.36, this.tool.getPercentileBMI(true, 0, 0), 1.0E-2);
        Assert.assertEquals(17.74, this.tool.getPercentileBMI(true, 0, 100), 1.0E-2);
        Assert.assertEquals(10.3, this.tool.getPercentileBMI(false, 0, 0), 1.0E-2);
        Assert.assertEquals(17.34, this.tool.getPercentileBMI(false, 0, 100), 1.0E-2);
        Assert.assertEquals(23.04, this.tool.getPercentileBMI(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(22.07, this.tool.getPercentileBMI(true, 349, 37), 1.0E-2);
        Assert.assertEquals(18.7, this.tool.getPercentileBMI(false, 359, 12), 1.0E-2);
    }

    @Test
    public void testGetWeightPercentile()
    {
        Assert.assertEquals(50, this.tool.getWeightPercentile(true, 0, 4.0));
        Assert.assertEquals(50, this.tool.getWeightPercentile(false, 0, 3.8));
        Assert.assertEquals(0, this.tool.getWeightPercentile(true, 0, 0));
        Assert.assertEquals(100, this.tool.getWeightPercentile(true, 0, 1000));
        Assert.assertEquals(0, this.tool.getWeightPercentile(false, 0, 0));
        Assert.assertEquals(100, this.tool.getWeightPercentile(false, 0, 1000));
        Assert.assertEquals(50, this.tool.getWeightPercentile(true, 1000, 70.6));
        Assert.assertEquals(37, this.tool.getWeightPercentile(true, 349, 67.0));
        Assert.assertEquals(12, this.tool.getWeightPercentile(false, 359, 49.0));
    }

    @Test
    public void testGetPercentileWeight()
    {
        Assert.assertEquals(4.0, this.tool.getPercentileWeight(true, 0, 50), 1.0E-2);
        Assert.assertEquals(3.8, this.tool.getPercentileWeight(false, 0, 50), 1.0E-2);
        Assert.assertEquals(2.09, this.tool.getPercentileWeight(true, 0, 0), 1.0E-2);
        Assert.assertEquals(5.5, this.tool.getPercentileWeight(true, 0, 100), 1.0E-2);
        Assert.assertEquals(2.2, this.tool.getPercentileWeight(false, 0, 0), 1.0E-2);
        Assert.assertEquals(5.18, this.tool.getPercentileWeight(false, 0, 100), 1.0E-2);
        Assert.assertEquals(70.6, this.tool.getPercentileWeight(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(67.0, this.tool.getPercentileWeight(true, 349, 37), 1.0E-2);
        Assert.assertEquals(49.04, this.tool.getPercentileWeight(false, 359, 12), 1.0E-2);
    }

    @Test
    public void testGetHeightPercentile()
    {
        Assert.assertEquals(50, this.tool.getHeightPercentile(true, 0, 52.7));
        Assert.assertEquals(50, this.tool.getHeightPercentile(false, 0, 51.68));
        Assert.assertEquals(0, this.tool.getHeightPercentile(true, 0, 0));
        Assert.assertEquals(100, this.tool.getHeightPercentile(true, 0, 1000));
        Assert.assertEquals(0, this.tool.getHeightPercentile(false, 0, 0));
        Assert.assertEquals(100, this.tool.getHeightPercentile(false, 0, 1000));
        Assert.assertEquals(50, this.tool.getHeightPercentile(true, 1000, 176.85));
        Assert.assertEquals(72, this.tool.getHeightPercentile(true, 349, 181.0));
        Assert.assertEquals(93, this.tool.getHeightPercentile(false, 359, 173.0));
    }

    @Test
    public void testGetPercentileHeight()
    {
        Assert.assertEquals(52.7, this.tool.getPercentileHeight(true, 0, 50), 1.0E-2);
        Assert.assertEquals(51.68, this.tool.getPercentileHeight(false, 0, 50), 1.0E-2);
        Assert.assertEquals(45.73, this.tool.getPercentileHeight(true, 0, 0), 1.0E-2);
        Assert.assertEquals(60.14, this.tool.getPercentileHeight(true, 0, 100), 1.0E-2);
        Assert.assertEquals(45.61, this.tool.getPercentileHeight(false, 0, 0), 1.0E-2);
        Assert.assertEquals(59.39, this.tool.getPercentileHeight(false, 0, 100), 1.0E-2);
        Assert.assertEquals(176.85, this.tool.getPercentileHeight(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(181.0, this.tool.getPercentileHeight(true, 349, 72), 1.0E-2);
        Assert.assertEquals(172.86, this.tool.getPercentileHeight(false, 359, 93), 1.0E-2);
    }

    @Test
    public void testGetFuzzyValue()
    {
        Assert.assertEquals("extreme-below-normal", this.tool.getFuzzyValue(Integer.MIN_VALUE));
        Assert.assertEquals("extreme-below-normal", this.tool.getFuzzyValue(-1));
        Assert.assertEquals("extreme-below-normal", this.tool.getFuzzyValue(0));
        Assert.assertEquals("extreme-below-normal", this.tool.getFuzzyValue(3));
        Assert.assertEquals("below-normal", this.tool.getFuzzyValue(4));
        Assert.assertEquals("below-normal", this.tool.getFuzzyValue(7));
        Assert.assertEquals("below-normal", this.tool.getFuzzyValue(10));
        Assert.assertEquals("normal", this.tool.getFuzzyValue(11));
        Assert.assertEquals("normal", this.tool.getFuzzyValue(50));
        Assert.assertEquals("normal", this.tool.getFuzzyValue(89));
        Assert.assertEquals("above-normal", this.tool.getFuzzyValue(90));
        Assert.assertEquals("above-normal", this.tool.getFuzzyValue(93));
        Assert.assertEquals("above-normal", this.tool.getFuzzyValue(96));
        Assert.assertEquals("extreme-above-normal", this.tool.getFuzzyValue(97));
        Assert.assertEquals("extreme-above-normal", this.tool.getFuzzyValue(99));
        Assert.assertEquals("extreme-above-normal", this.tool.getFuzzyValue(100));
        Assert.assertEquals("extreme-above-normal", this.tool.getFuzzyValue(101));
        Assert.assertEquals("extreme-above-normal", this.tool.getFuzzyValue(Integer.MAX_VALUE));
    }
}
