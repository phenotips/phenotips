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
 * @version $Id: 62f81d2705cfef5b9315c7bcaff5fe3d15aa82b3 $
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
        Assert.assertEquals(10, this.tool.getBMIPercentile(true, 42, 14.49, 100.0));
        Assert.assertEquals(90, this.tool.getBMIPercentile(false, 42, 17.36, 100.0));
        Assert.assertEquals(0, this.tool.getBMIPercentile(true, 100, 18, 130.0));
        Assert.assertEquals(100, this.tool.getBMIPercentile(true, 100, 90, 110.0));
    }

    @Test
    public void testGetHeightPercentile()
    {
        Assert.assertEquals(50, this.tool.getHeightPercentile(true, 0, 52.7));
        Assert.assertEquals(50, this.tool.getHeightPercentile(false, 0, 51.68));
        Assert.assertEquals(50, this.tool.getHeightPercentile(true, 1000, 176.85));
    }
}
