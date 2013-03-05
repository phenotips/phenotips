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
package edu.toronto.cs.phenotips.measurements;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
 * Tests for the {@link MeasurementsScriptService} component.
 * 
 * @version $Id$
 * @since 1.0M1
 */
public class MeasurementsScriptServiceTest
{
    @Rule
    public final MockitoComponentMockingRule<MeasurementsScriptService> mocker =
        new MockitoComponentMockingRule<MeasurementsScriptService>(MeasurementsScriptService.class);

    @Test
    public void testPercentileComputation() throws ComponentLookupException
    {
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToPercentile(Double.MIN_VALUE, 2.5244, -0.3521,
            0.09153));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToPercentile(-1, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToPercentile(0, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToPercentile(1, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(2, this.mocker.getComponentUnderTest()
            .valueToPercentile(2.114041, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(5, this.mocker.getComponentUnderTest()
            .valueToPercentile(2.179956, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(10, this.mocker.getComponentUnderTest().valueToPercentile(2.250293, 2.5244, -0.3521,
            0.09153));
        Assert.assertEquals(25, this.mocker.getComponentUnderTest().valueToPercentile(2.374837, 2.5244, -0.3521,
            0.09153));
        Assert
            .assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(2.5244, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(75, this.mocker.getComponentUnderTest().valueToPercentile(2.686987, 2.5244, -0.3521,
            0.09153));
        Assert.assertEquals(90, this.mocker.getComponentUnderTest()
            .valueToPercentile(2.84566, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(95, this.mocker.getComponentUnderTest().valueToPercentile(2.946724, 2.5244, -0.3521,
            0.09153));
        Assert.assertEquals(98, this.mocker.getComponentUnderTest().valueToPercentile(3.050268, 2.5244, -0.3521,
            0.09153));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().valueToPercentile(3.5, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().valueToPercentile(20, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().valueToPercentile(1000, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().valueToPercentile(Double.MAX_VALUE, 2.5244,
            -0.3521, 0.09153));
    }

    @Test
    public void testValueComputation() throws ComponentLookupException
    {
        // Values taken from the CDC data tables (Weight for age, boys, 0.5 months)
        double x = this.mocker.getComponentUnderTest().percentileToValue(3, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(2.799548641, x, 1.0E-8);
        x = this.mocker.getComponentUnderTest().percentileToValue(5, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(2.964655655, x, 1.0E-8);
        x = this.mocker.getComponentUnderTest().percentileToValue(10, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(3.209510017, x, 1.0E-8);
        x = this.mocker.getComponentUnderTest().percentileToValue(25, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(3.597395573, x, 1.0E-8);
        x = this.mocker.getComponentUnderTest().percentileToValue(50, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(4.003106424, x, 1.0E-8);
        x = this.mocker.getComponentUnderTest().percentileToValue(75, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(4.387422565, x, 1.0E-8);
        x = this.mocker.getComponentUnderTest().percentileToValue(90, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(4.718161283, x, 1.0E-8);
        x = this.mocker.getComponentUnderTest().percentileToValue(95, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(4.910130108, x, 1.0E-8);
        x = this.mocker.getComponentUnderTest().percentileToValue(97, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(5.032624982, x, 1.0E-8);
        // Values taken from the CDC data tables (Weight for age, boys, 9.5 months)
        x = this.mocker.getComponentUnderTest().percentileToValue(3, 9.476500305, -0.1600954, 0.11218624);
        Assert.assertEquals(7.700624405, x, 1.0E-8);
        x = this.mocker.getComponentUnderTest().percentileToValue(90, 9.476500305, -0.1600954, 0.11218624);
        Assert.assertEquals(10.96017225, x, 1.0E-8);
        // Don't expect a child with +- Infinity kilograms...
        x = this.mocker.getComponentUnderTest().percentileToValue(0, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(2.089641107, x, 1.0E-8);
        x = this.mocker.getComponentUnderTest().percentileToValue(100, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(5.498638677, x, 1.0E-8);
        // Correct out of range percentiles
        x =
            this.mocker.getComponentUnderTest().percentileToValue(Integer.MIN_VALUE, 4.003106424, 1.547523128,
                0.146025021);
        Assert.assertEquals(2.089641107, x, 1.0E-8);
        x = this.mocker.getComponentUnderTest().percentileToValue(-50, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(2.089641107, x, 1.0E-8);
        x = this.mocker.getComponentUnderTest().percentileToValue(1000, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(5.498638677, x, 1.0E-8);
        x =
            this.mocker.getComponentUnderTest().percentileToValue(Integer.MAX_VALUE, 4.003106424, 1.547523128,
                0.146025021);
        Assert.assertEquals(5.498638677, x, 1.0E-8);
    }

    @Test
    public void testGetBMI() throws ComponentLookupException
    {
        Assert.assertEquals(100.0, this.mocker.getComponentUnderTest().getBMI(100, 100));
        Assert.assertEquals(31.25, this.mocker.getComponentUnderTest().getBMI(80, 160));
        Assert.assertEquals(0.0, this.mocker.getComponentUnderTest().getBMI(0, 0));
        Assert.assertEquals(0.0, this.mocker.getComponentUnderTest().getBMI(80, 0));
        Assert.assertEquals(0.0, this.mocker.getComponentUnderTest().getBMI(0, 120));
        Assert.assertEquals(0.0, this.mocker.getComponentUnderTest().getBMI(-80, -160));
        Assert.assertEquals(Double.POSITIVE_INFINITY, this.mocker.getComponentUnderTest().getBMI(Double.MAX_VALUE, 1));
        Assert.assertEquals(0.0, this.mocker.getComponentUnderTest().getBMI(1, Double.MAX_VALUE));
    }

    @Test
    public void testGetBMIPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getBMIPercentile(true, 0, 3.34, 49.9));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getBMIPercentile(false, 0, 3.32, 49.9));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getBMIPercentile(true, 0, 1, 1000));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().getBMIPercentile(true, 0, 1000, 1));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getBMIPercentile(false, 0, 1, 1000));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().getBMIPercentile(false, 0, 1000, 1));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getBMIPercentile(false, 0, 0, 0));
        Assert.assertEquals(10, this.mocker.getComponentUnderTest().getBMIPercentile(true, 42, 14.49, 100.0));
        Assert.assertEquals(90, this.mocker.getComponentUnderTest().getBMIPercentile(false, 42, 17.36, 100.0));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getBMIPercentile(true, 100, 18, 130.0));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().getBMIPercentile(true, 100, 90, 110.0));
        Assert.assertEquals(16, this.mocker.getComponentUnderTest().getBMIPercentile(true, 349, 67.0, 181.0));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getBMIPercentile(false, 359, 49.0, 173.0));
    }

    @Test
    public void testGetBMIStandardDeviation() throws ComponentLookupException
    {
        Assert
            .assertEquals(0, this.mocker.getComponentUnderTest().getBMIStandardDeviation(true, 0, 3.34, 49.9), 1.0E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getBMIStandardDeviation(false, 0, 3.32, 49.9),
            1.0E-2);
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().getBMIStandardDeviation(true, 42, 14.76, 100.0),
            1.0E-2);
        Assert.assertEquals(1, this.mocker.getComponentUnderTest().getBMIStandardDeviation(true, 42, 17.02, 100.0),
            1.0E-2);
        Assert.assertEquals(-2, this.mocker.getComponentUnderTest().getBMIStandardDeviation(true, 42, 13.87, 100.0),
            1.0E-2);
        Assert.assertEquals(2, this.mocker.getComponentUnderTest().getBMIStandardDeviation(true, 42, 18.54, 100.0),
            1.0E-2);
        Assert.assertEquals(-3, this.mocker.getComponentUnderTest().getBMIStandardDeviation(true, 42, 13.10, 100.0),
            1.0E-2);
        Assert.assertEquals(3, this.mocker.getComponentUnderTest().getBMIStandardDeviation(true, 42, 20.40, 100.0),
            1.0E-2);
    }

    @Test
    public void testGetPercentileBMI() throws ComponentLookupException
    {
        Assert.assertEquals(13.4, this.mocker.getComponentUnderTest().getPercentileBMI(true, 0, 50), 1.0E-2);
        Assert.assertEquals(13.34, this.mocker.getComponentUnderTest().getPercentileBMI(false, 0, 50), 1.0E-2);
        Assert.assertEquals(10.36, this.mocker.getComponentUnderTest().getPercentileBMI(true, 0, 0), 1.0E-2);
        Assert.assertEquals(17.74, this.mocker.getComponentUnderTest().getPercentileBMI(true, 0, 100), 1.0E-2);
        Assert.assertEquals(10.3, this.mocker.getComponentUnderTest().getPercentileBMI(false, 0, 0), 1.0E-2);
        Assert.assertEquals(17.34, this.mocker.getComponentUnderTest().getPercentileBMI(false, 0, 100), 1.0E-2);
        Assert.assertEquals(23.04, this.mocker.getComponentUnderTest().getPercentileBMI(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(22.07, this.mocker.getComponentUnderTest().getPercentileBMI(true, 349, 37), 1.0E-2);
        Assert.assertEquals(18.7, this.mocker.getComponentUnderTest().getPercentileBMI(false, 359, 12), 1.0E-2);
    }

    @Test
    public void testGetStandardDeviationBMI() throws ComponentLookupException
    {
        Assert.assertEquals(13.4, this.mocker.getComponentUnderTest().getStandardDeviationBMI(true, 0, 0), 1.0E-2);
        Assert.assertEquals(13.34, this.mocker.getComponentUnderTest().getStandardDeviationBMI(false, 0, 0), 1.0E-2);
        Assert
            .assertEquals(10.36, this.mocker.getComponentUnderTest().getStandardDeviationBMI(true, 0, -2.807), 1.0E-2);
        Assert.assertEquals(17.74, this.mocker.getComponentUnderTest().getStandardDeviationBMI(true, 0, 2.807), 1.0E-2);
        Assert
            .assertEquals(10.3, this.mocker.getComponentUnderTest().getStandardDeviationBMI(false, 0, -2.807), 1.0E-2);
        Assert
            .assertEquals(17.34, this.mocker.getComponentUnderTest().getStandardDeviationBMI(false, 0, 2.807), 1.0E-2);
        Assert.assertEquals(23.04, this.mocker.getComponentUnderTest().getStandardDeviationBMI(true, 1000, 0), 1.0E-2);
        Assert.assertEquals(22.07, this.mocker.getComponentUnderTest().getStandardDeviationBMI(true, 349, -0.332),
            1.0E-2);
        Assert.assertEquals(18.7, this.mocker.getComponentUnderTest().getStandardDeviationBMI(false, 359, -1.175),
            1.0E-2);
    }

    @Test
    public void testGetWeightPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getWeightPercentile(true, 0, 4.0));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getWeightPercentile(false, 0, 3.8));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getWeightPercentile(true, 0, 0));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().getWeightPercentile(true, 0, 1000));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getWeightPercentile(false, 0, 0));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().getWeightPercentile(false, 0, 1000));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getWeightPercentile(true, 1000, 70.6));
        Assert.assertEquals(37, this.mocker.getComponentUnderTest().getWeightPercentile(true, 349, 67.0));
        Assert.assertEquals(12, this.mocker.getComponentUnderTest().getWeightPercentile(false, 359, 49.0));
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().getWeightPercentile(true, -1, 4.0));
    }

    @Test
    public void testGetWeightStandardDeviation() throws ComponentLookupException
    {
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getWeightStandardDeviation(true, 0, 4.0), 1.0E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getWeightStandardDeviation(false, 0, 3.8), 1.0E-2);
        Assert
            .assertEquals(0, this.mocker.getComponentUnderTest().getWeightStandardDeviation(true, 1000, 70.6), 1.0E-2);
        Assert.assertEquals(-0.332, this.mocker.getComponentUnderTest().getWeightStandardDeviation(true, 349, 67.0),
            1.0E-2);
        Assert.assertEquals(-1.175, this.mocker.getComponentUnderTest().getWeightStandardDeviation(false, 359, 49.0),
            1.0E-2);
        Assert.assertTrue(Double.isNaN(this.mocker.getComponentUnderTest().getWeightStandardDeviation(true, -1, 4.0)));
    }

    @Test
    public void testGetPercentileWeight() throws ComponentLookupException
    {
        Assert.assertEquals(4.0, this.mocker.getComponentUnderTest().getPercentileWeight(true, 0, 50), 1.0E-2);
        Assert.assertEquals(3.8, this.mocker.getComponentUnderTest().getPercentileWeight(false, 0, 50), 1.0E-2);
        Assert.assertEquals(2.09, this.mocker.getComponentUnderTest().getPercentileWeight(true, 0, 0), 1.0E-2);
        Assert.assertEquals(5.5, this.mocker.getComponentUnderTest().getPercentileWeight(true, 0, 100), 1.0E-2);
        Assert.assertEquals(2.2, this.mocker.getComponentUnderTest().getPercentileWeight(false, 0, 0), 1.0E-2);
        Assert.assertEquals(5.18, this.mocker.getComponentUnderTest().getPercentileWeight(false, 0, 100), 1.0E-2);
        Assert.assertEquals(70.6, this.mocker.getComponentUnderTest().getPercentileWeight(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(67.0, this.mocker.getComponentUnderTest().getPercentileWeight(true, 349, 37), 1.0E-2);
        Assert.assertEquals(49.04, this.mocker.getComponentUnderTest().getPercentileWeight(false, 359, 12), 1.0E-2);
    }

    @Test
    public void testGetStandardDeviationWeight() throws ComponentLookupException
    {
        Assert.assertEquals(4.0, this.mocker.getComponentUnderTest().getStandardDeviationWeight(true, 0, 0), 1.0E-2);
        Assert.assertEquals(3.8, this.mocker.getComponentUnderTest().getStandardDeviationWeight(false, 0, 0), 1.0E-2);
        Assert.assertEquals(2.09, this.mocker.getComponentUnderTest().getStandardDeviationWeight(true, 0, -2.807),
            1.0E-2);
        Assert
            .assertEquals(5.5, this.mocker.getComponentUnderTest().getStandardDeviationWeight(true, 0, 2.807), 1.0E-2);
        Assert.assertEquals(2.2, this.mocker.getComponentUnderTest().getStandardDeviationWeight(false, 0, -2.807),
            1.0E-2);
        Assert.assertEquals(5.18, this.mocker.getComponentUnderTest().getStandardDeviationWeight(false, 0, 2.807),
            1.0E-2);
        Assert
            .assertEquals(70.6, this.mocker.getComponentUnderTest().getStandardDeviationWeight(true, 1000, 0), 1.0E-2);
        Assert.assertEquals(67.0, this.mocker.getComponentUnderTest().getStandardDeviationWeight(true, 349, -0.332),
            1.0E-2);
        Assert.assertEquals(49.04, this.mocker.getComponentUnderTest().getStandardDeviationWeight(false, 359, -1.175),
            1.0E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getStandardDeviationWeight(false, 359,
            Integer.MIN_VALUE), 1.0E-2);
    }

    @Test
    public void testGetICDPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getInnerCanthalDistancePercentile(true, 0, 2));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getInnerCanthalDistancePercentile(true, 0, 0));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().getInnerCanthalDistancePercentile(true, 0, 1000));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getInnerCanthalDistancePercentile(true, 1000,
            3.1357));
        Assert.assertEquals(2, this.mocker.getComponentUnderTest().getInnerCanthalDistancePercentile(true, 16, 2.0475));
        Assert
            .assertEquals(50, this.mocker.getComponentUnderTest().getInnerCanthalDistancePercentile(true, 16, 2.5825));
        Assert
            .assertEquals(98, this.mocker.getComponentUnderTest().getInnerCanthalDistancePercentile(true, 16, 3.0485));
        Assert
            .assertEquals(50, this.mocker.getComponentUnderTest().getInnerCanthalDistancePercentile(true, 30, 2.6925));
    }

    @Test
    public void testGetICDStandardDeviation() throws ComponentLookupException
    {
        Assert.assertEquals(0,
            this.mocker.getComponentUnderTest().getInnerCanthalDistanceStandardDeviation(true, 0, 2), 1.0E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getInnerCanthalDistanceStandardDeviation(true, 900,
            3.135), 1.0E-2);
        Assert.assertEquals(-2, this.mocker.getComponentUnderTest().getInnerCanthalDistanceStandardDeviation(true, 16,
            2.047), 1.0E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getInnerCanthalDistanceStandardDeviation(true, 16,
            2.5825), 1.0E-2);
        Assert.assertEquals(2, this.mocker.getComponentUnderTest().getInnerCanthalDistanceStandardDeviation(true, 16,
            3.0485), 1.0E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getInnerCanthalDistanceStandardDeviation(true, 30,
            2.6925), 1.0E-2);
    }

    @Test
    public void testGetPercentileICD() throws ComponentLookupException
    {
        Assert.assertEquals(2, this.mocker.getComponentUnderTest().getPercentileInnerCanthalDistance(true, 0, 50),
            1.0E-2);
        Assert.assertEquals(1.30, this.mocker.getComponentUnderTest().getPercentileInnerCanthalDistance(true, 0, 0),
            1.0E-2);
        Assert.assertEquals(2.71, this.mocker.getComponentUnderTest().getPercentileInnerCanthalDistance(true, 0, 100),
            1.0E-2);
        Assert.assertEquals(3.1275, this.mocker.getComponentUnderTest().getPercentileInnerCanthalDistance(true, 1000,
            50), 1.0E-2);
        Assert.assertEquals(2.03, this.mocker.getComponentUnderTest().getPercentileInnerCanthalDistance(true, 16, 2),
            1.0E-2);
        Assert.assertEquals(2.5825,
            this.mocker.getComponentUnderTest().getPercentileInnerCanthalDistance(true, 16, 50), 1.0E-2);
        Assert.assertEquals(3.06, this.mocker.getComponentUnderTest().getPercentileInnerCanthalDistance(true, 16, 98),
            1.0E-2);
        Assert.assertEquals(2.6925,
            this.mocker.getComponentUnderTest().getPercentileInnerCanthalDistance(true, 30, 50), 1.0E-2);
    }

    @Test
    public void testGetStandardDeviationICD() throws ComponentLookupException
    {
        Assert.assertEquals(2,
            this.mocker.getComponentUnderTest().getStandardDeviationInnerCanthalDistance(true, 0, 0), 1.0E-2);
        Assert.assertEquals(3.127, this.mocker.getComponentUnderTest().getStandardDeviationInnerCanthalDistance(true,
            900, 0), 1.0E-2);
        Assert.assertEquals(2.05, this.mocker.getComponentUnderTest().getStandardDeviationInnerCanthalDistance(true,
            16, -2), 1.0E-2);
        Assert.assertEquals(2.5825, this.mocker.getComponentUnderTest().getStandardDeviationInnerCanthalDistance(true,
            16, 0), 1.0E-2);
        Assert.assertEquals(3.05, this.mocker.getComponentUnderTest().getStandardDeviationInnerCanthalDistance(true,
            16, 2), 1.0E-2);
        Assert.assertEquals(2.6925, this.mocker.getComponentUnderTest().getStandardDeviationInnerCanthalDistance(true,
            30, 0), 1.0E-2);
    }

    @Test
    public void testGetIPDPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getInterpupilaryDistancePercentile(true, 0, 3.91));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getInterpupilaryDistancePercentile(true, 0, 0));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().getInterpupilaryDistancePercentile(true, 0, 1000));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest()
            .getInterpupilaryDistancePercentile(true, 1000, 6.13));
        Assert.assertEquals(3, this.mocker.getComponentUnderTest().getInterpupilaryDistancePercentile(true, 36, 4.23));
        Assert
            .assertEquals(50, this.mocker.getComponentUnderTest().getInterpupilaryDistancePercentile(true, 36, 4.835));
        Assert.assertEquals(97, this.mocker.getComponentUnderTest().getInterpupilaryDistancePercentile(true, 36, 5.49));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest()
            .getInterpupilaryDistancePercentile(true, 30, 4.7825));
    }

    @Test
    public void testGetIPDStandardDeviation() throws ComponentLookupException
    {
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getInterpupilaryDistanceStandardDeviation(true, 0,
            3.91), 1.0E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getInterpupilaryDistanceStandardDeviation(true, 900,
            6.13), 1.0E-2);
        Assert.assertEquals(-1.881, this.mocker.getComponentUnderTest().getInterpupilaryDistanceStandardDeviation(true,
            36, 4.23), 1.0E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getInterpupilaryDistanceStandardDeviation(true, 36,
            4.835), 1.0E-2);
        Assert.assertEquals(1.881, this.mocker.getComponentUnderTest().getInterpupilaryDistanceStandardDeviation(true,
            36, 5.49), 1.0E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getInterpupilaryDistanceStandardDeviation(true, 30,
            4.782), 1.0E-2);
    }

    @Test
    public void testGetPercentileIPD() throws ComponentLookupException
    {
        Assert.assertEquals(3.91, this.mocker.getComponentUnderTest().getPercentileInterpupilaryDistance(true, 0, 50),
            1.0E-2);
        Assert.assertEquals(3.04, this.mocker.getComponentUnderTest().getPercentileInterpupilaryDistance(true, 0, 0),
            1.0E-2);
        Assert.assertEquals(4.98, this.mocker.getComponentUnderTest().getPercentileInterpupilaryDistance(true, 0, 100),
            1.0E-2);
        Assert.assertEquals(6.13, this.mocker.getComponentUnderTest()
            .getPercentileInterpupilaryDistance(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(4.23, this.mocker.getComponentUnderTest().getPercentileInterpupilaryDistance(true, 36, 3),
            1.0E-2);
        Assert.assertEquals(4.835,
            this.mocker.getComponentUnderTest().getPercentileInterpupilaryDistance(true, 36, 50), 1.0E-2);
        Assert.assertEquals(5.49, this.mocker.getComponentUnderTest().getPercentileInterpupilaryDistance(true, 36, 97),
            1.0E-2);
        Assert.assertEquals(4.7825, this.mocker.getComponentUnderTest()
            .getPercentileInterpupilaryDistance(true, 30, 50), 1.0E-2);
    }

    @Test
    public void testGetStandardDeviationIPD() throws ComponentLookupException
    {
        Assert.assertEquals(3.91, this.mocker.getComponentUnderTest().getStandardDeviationInterpupilaryDistance(true,
            0, 0), 1E-2);
        Assert.assertEquals(6.13, this.mocker.getComponentUnderTest().getStandardDeviationInterpupilaryDistance(true,
            900, 0), 1E-2);
        Assert.assertEquals(4.23, this.mocker.getComponentUnderTest().getStandardDeviationInterpupilaryDistance(true,
            36, -1.88), 1E-2);
        Assert.assertEquals(4.835, this.mocker.getComponentUnderTest().getStandardDeviationInterpupilaryDistance(true,
            36, 0), 1E-2);
        Assert.assertEquals(5.49, this.mocker.getComponentUnderTest().getStandardDeviationInterpupilaryDistance(true,
            36, 1.88), 1E-2);
        Assert.assertEquals(4.782, this.mocker.getComponentUnderTest().getStandardDeviationInterpupilaryDistance(true,
            30, 0), 1E-2);
    }

    @Test
    public void testGetOCDPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getOuterCanthalDistancePercentile(true, 0, 6.3));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getOuterCanthalDistancePercentile(true, 0, 0));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().getOuterCanthalDistancePercentile(true, 0, 1000));
        Assert
            .assertEquals(50, this.mocker.getComponentUnderTest().getOuterCanthalDistancePercentile(true, 1000, 9.08));
        Assert.assertEquals(3, this.mocker.getComponentUnderTest().getOuterCanthalDistancePercentile(true, 16, 6.27));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getOuterCanthalDistancePercentile(true, 16, 7.305));
        Assert.assertEquals(97, this.mocker.getComponentUnderTest().getOuterCanthalDistancePercentile(true, 16, 8.33));
        Assert
            .assertEquals(50, this.mocker.getComponentUnderTest().getOuterCanthalDistancePercentile(true, 30, 7.4725));
    }

    @Test
    public void testGetOCDStandardDeviation() throws ComponentLookupException
    {
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getOuterCanthalDistanceStandardDeviation(true, 0,
            6.3), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getOuterCanthalDistanceStandardDeviation(true, 900,
            9.08), 1E-2);
        Assert.assertEquals(-1.88, this.mocker.getComponentUnderTest().getOuterCanthalDistanceStandardDeviation(true,
            16, 6.27), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getOuterCanthalDistanceStandardDeviation(true, 16,
            7.305), 1E-2);
        Assert.assertEquals(1.88, this.mocker.getComponentUnderTest().getOuterCanthalDistanceStandardDeviation(true,
            16, 8.33), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getOuterCanthalDistanceStandardDeviation(true, 30,
            7.4725), 1E-2);
    }

    @Test
    public void testGetPercentileOCD() throws ComponentLookupException
    {
        Assert.assertEquals(6.3, this.mocker.getComponentUnderTest().getPercentileOuterCanthalDistance(true, 0, 50),
            1.0E-2);
        Assert.assertEquals(4.86, this.mocker.getComponentUnderTest().getPercentileOuterCanthalDistance(true, 0, 0),
            1.0E-2);
        Assert.assertEquals(7.98, this.mocker.getComponentUnderTest().getPercentileOuterCanthalDistance(true, 0, 100),
            1.0E-2);
        Assert.assertEquals(9.08,
            this.mocker.getComponentUnderTest().getPercentileOuterCanthalDistance(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(6.27, this.mocker.getComponentUnderTest().getPercentileOuterCanthalDistance(true, 16, 3),
            1.0E-2);
        Assert.assertEquals(7.305, this.mocker.getComponentUnderTest().getPercentileOuterCanthalDistance(true, 16, 50),
            1.0E-2);
        Assert.assertEquals(8.33, this.mocker.getComponentUnderTest().getPercentileOuterCanthalDistance(true, 16, 97),
            1.0E-2);
        Assert.assertEquals(7.4725,
            this.mocker.getComponentUnderTest().getPercentileOuterCanthalDistance(true, 30, 50), 1.0E-2);
    }

    @Test
    public void testGetStandardDeviationOCD() throws ComponentLookupException
    {
        Assert.assertEquals(6.3, this.mocker.getComponentUnderTest().getStandardDeviationOuterCanthalDistance(true, 0,
            0), 1.0E-2);
        Assert.assertEquals(9.08, this.mocker.getComponentUnderTest().getStandardDeviationOuterCanthalDistance(true,
            900, 0), 1.0E-2);
        Assert.assertEquals(6.27, this.mocker.getComponentUnderTest().getStandardDeviationOuterCanthalDistance(true,
            16, -1.88), 1E-2);
        Assert.assertEquals(7.305, this.mocker.getComponentUnderTest().getStandardDeviationOuterCanthalDistance(true,
            16, 0), 1E-2);
        Assert.assertEquals(8.33, this.mocker.getComponentUnderTest().getStandardDeviationOuterCanthalDistance(true,
            16, 1.88), 1E-2);
        Assert.assertEquals(7.4725, this.mocker.getComponentUnderTest().getStandardDeviationOuterCanthalDistance(true,
            30, 0), 1E-2);
    }

    @Test
    public void testGetEarLengthPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getEarLengthPercentile(true, 0, 4.04));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getEarLengthPercentile(true, 0, 0));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().getEarLengthPercentile(true, 0, 1000));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getEarLengthPercentile(true, 1000, 6.0825));
        Assert.assertEquals(2, this.mocker.getComponentUnderTest().getEarLengthPercentile(true, 36, 4.5));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getEarLengthPercentile(true, 36, 5.115));
        Assert.assertEquals(98, this.mocker.getComponentUnderTest().getEarLengthPercentile(true, 36, 5.89));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getEarLengthPercentile(true, 30, 5.015));
    }

    @Test
    public void testGetEarLengthStandardDeviation() throws ComponentLookupException
    {
        Assert
            .assertEquals(0, this.mocker.getComponentUnderTest().getEarLengthStandardDeviation(true, 0, 4.04), 1.0E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getEarLengthStandardDeviation(true, 1000, 6.0825),
            1.0E-2);
        Assert.assertEquals(-2, this.mocker.getComponentUnderTest().getEarLengthStandardDeviation(true, 36, 4.5),
            1.0E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getEarLengthStandardDeviation(true, 36, 5.115),
            1.0E-2);
        Assert.assertEquals(2, this.mocker.getComponentUnderTest().getEarLengthStandardDeviation(true, 36, 5.89),
            1.0E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getEarLengthStandardDeviation(true, 30, 5.015),
            1.0E-2);
    }

    @Test
    public void testGetPercentileEarLength() throws ComponentLookupException
    {
        Assert.assertEquals(4.04, this.mocker.getComponentUnderTest().getPercentileEarLength(true, 0, 50), 1.0E-2);
        Assert.assertEquals(3.15, this.mocker.getComponentUnderTest().getPercentileEarLength(true, 0, 0), 1.0E-2);
        Assert.assertEquals(5.02, this.mocker.getComponentUnderTest().getPercentileEarLength(true, 0, 100), 1.0E-2);
        Assert.assertEquals(6.0825, this.mocker.getComponentUnderTest().getPercentileEarLength(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(4.48, this.mocker.getComponentUnderTest().getPercentileEarLength(true, 36, 2), 1.0E-2);
        Assert.assertEquals(5.115, this.mocker.getComponentUnderTest().getPercentileEarLength(true, 36, 50), 1.0E-2);
        Assert.assertEquals(5.91, this.mocker.getComponentUnderTest().getPercentileEarLength(true, 36, 98), 1.0E-2);
        Assert.assertEquals(5.015, this.mocker.getComponentUnderTest().getPercentileEarLength(true, 30, 50), 1.0E-2);
    }

    @Test
    public void testGetStandardDeviationEarLength() throws ComponentLookupException
    {
        Assert.assertEquals(4.04, this.mocker.getComponentUnderTest().getStandardDeviationEarLength(true, 0, 0), 1E-2);
        Assert.assertEquals(6.0825, this.mocker.getComponentUnderTest().getStandardDeviationEarLength(true, 1000, 0),
            1E-2);
        Assert.assertEquals(4.5, this.mocker.getComponentUnderTest().getStandardDeviationEarLength(true, 36, -2), 1E-2);
        Assert
            .assertEquals(5.115, this.mocker.getComponentUnderTest().getStandardDeviationEarLength(true, 36, 0), 1E-2);
        Assert.assertEquals(5.89, this.mocker.getComponentUnderTest().getStandardDeviationEarLength(true, 36, 2), 1E-2);
        Assert
            .assertEquals(5.015, this.mocker.getComponentUnderTest().getStandardDeviationEarLength(true, 30, 0), 1E-2);
    }

    @Test
    public void testGetPalpebralFissureLengthPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getPalpebralFissureLengthPercentile(true, 0, 1.9));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getPalpebralFissureLengthPercentile(true, 0, 0));
        Assert
            .assertEquals(100, this.mocker.getComponentUnderTest().getPalpebralFissureLengthPercentile(true, 0, 1000));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getPalpebralFissureLengthPercentile(true, 1000,
            3.13));
        Assert
            .assertEquals(2, this.mocker.getComponentUnderTest().getPalpebralFissureLengthPercentile(true, 36, 2.215));
        Assert
            .assertEquals(50, this.mocker.getComponentUnderTest().getPalpebralFissureLengthPercentile(true, 36, 2.49));
        Assert
            .assertEquals(98, this.mocker.getComponentUnderTest().getPalpebralFissureLengthPercentile(true, 36, 2.78));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getPalpebralFissureLengthPercentile(true, 30,
            2.4325));
    }

    @Test
    public void testGetPalpebralFissureLengthStandardDeviation() throws ComponentLookupException
    {
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getPalpebralFissureLengthStandardDeviation(true, 0,
            1.9), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getPalpebralFissureLengthStandardDeviation(true,
            900, 3.13), 1E-2);
        Assert.assertEquals(-2, this.mocker.getComponentUnderTest().getPalpebralFissureLengthStandardDeviation(true,
            36, 2.215), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getPalpebralFissureLengthStandardDeviation(true, 36,
            2.49), 1E-2);
        Assert.assertEquals(2, this.mocker.getComponentUnderTest().getPalpebralFissureLengthStandardDeviation(true, 36,
            2.78), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getPalpebralFissureLengthStandardDeviation(true, 30,
            2.432), 1E-2);
    }

    @Test
    public void testGetPercentilePalpebralFissureLength() throws ComponentLookupException
    {
        Assert.assertEquals(1.9, this.mocker.getComponentUnderTest().getPercentilePalpebralFissureLength(true, 0, 50),
            1E-2);
        Assert.assertEquals(1.64, this.mocker.getComponentUnderTest().getPercentilePalpebralFissureLength(true, 0, 0),
            1E-2);
        Assert.assertEquals(2.21,
            this.mocker.getComponentUnderTest().getPercentilePalpebralFissureLength(true, 0, 100), 1E-2);
        Assert.assertEquals(3.13, this.mocker.getComponentUnderTest().getPercentilePalpebralFissureLength(true, 1000,
            50), 1E-2);
        Assert.assertEquals(2.215,
            this.mocker.getComponentUnderTest().getPercentilePalpebralFissureLength(true, 36, 2), 1E-2);
        Assert.assertEquals(2.49,
            this.mocker.getComponentUnderTest().getPercentilePalpebralFissureLength(true, 36, 50), 1E-2);
        Assert.assertEquals(2.78,
            this.mocker.getComponentUnderTest().getPercentilePalpebralFissureLength(true, 36, 98), 1E-2);
        Assert.assertEquals(2.4325, this.mocker.getComponentUnderTest().getPercentilePalpebralFissureLength(true, 30,
            50), 1E-2);
    }

    @Test
    public void testGetStandardDeviationPalpebralFissureLength() throws ComponentLookupException
    {
        Assert.assertEquals(1.9, this.mocker.getComponentUnderTest().getStandardDeviationPalpebralFissureLength(true,
            0, 0), 1E-2);
        Assert.assertEquals(3.13, this.mocker.getComponentUnderTest().getStandardDeviationPalpebralFissureLength(true,
            900, 0), 1E-2);
        Assert.assertEquals(2.215, this.mocker.getComponentUnderTest().getStandardDeviationPalpebralFissureLength(true,
            36, -2), 1E-2);
        Assert.assertEquals(2.49, this.mocker.getComponentUnderTest().getStandardDeviationPalpebralFissureLength(true,
            36, 0), 1E-2);
        Assert.assertEquals(2.78, this.mocker.getComponentUnderTest().getStandardDeviationPalpebralFissureLength(true,
            36, 2), 1E-2);
        Assert.assertEquals(2.432, this.mocker.getComponentUnderTest().getStandardDeviationPalpebralFissureLength(true,
            30, 0), 1E-2);
    }

    @Test
    public void testGetHandLengthPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().getHandLengthPercentile(true, 0, 6.3));
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().getHandLengthPercentile(true, 0, 0));
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().getHandLengthPercentile(true, 0, 1000));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getHandLengthPercentile(true, 24, 10.5));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getHandLengthPercentile(true, 24, 0));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().getHandLengthPercentile(true, 24, 1000));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getHandLengthPercentile(true, 1000, 19.25));
        Assert.assertEquals(3, this.mocker.getComponentUnderTest().getHandLengthPercentile(true, 36, 9.95));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getHandLengthPercentile(true, 36, 11.3));
        Assert.assertEquals(97, this.mocker.getComponentUnderTest().getHandLengthPercentile(true, 36, 12.45));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getHandLengthPercentile(true, 30, 10.9));
    }

    @Test
    public void testGetHandLengthStandardDeviation() throws ComponentLookupException
    {
        Assert.assertTrue(Double
            .isNaN(this.mocker.getComponentUnderTest().getHandLengthStandardDeviation(true, 0, 6.3)));
        Assert.assertTrue(Double.isNaN(this.mocker.getComponentUnderTest().getHandLengthStandardDeviation(true, 0, 0)));
        Assert.assertTrue(Double.isNaN(this.mocker.getComponentUnderTest()
            .getHandLengthStandardDeviation(true, 0, 1000)));
        Assert
            .assertEquals(0, this.mocker.getComponentUnderTest().getHandLengthStandardDeviation(true, 24, 10.5), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getHandLengthStandardDeviation(true, 1000, 19.25),
            1E-2);
        Assert.assertEquals(-1.881, this.mocker.getComponentUnderTest().getHandLengthStandardDeviation(true, 36, 9.95),
            1E-2);
        Assert
            .assertEquals(0, this.mocker.getComponentUnderTest().getHandLengthStandardDeviation(true, 36, 11.3), 1E-2);
        Assert.assertEquals(1.881, this.mocker.getComponentUnderTest().getHandLengthStandardDeviation(true, 36, 12.45),
            1E-2);
        Assert
            .assertEquals(0, this.mocker.getComponentUnderTest().getHandLengthStandardDeviation(true, 30, 10.9), 1E-2);
    }

    @Test
    public void testGetPercentileHandLength() throws ComponentLookupException
    {
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().getPercentileHandLength(true, 0, 50), 1.0E-2);
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().getPercentileHandLength(true, 0, 0), 1.0E-2);
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().getPercentileHandLength(true, 0, 100), 1.0E-2);
        Assert.assertEquals(10.5, this.mocker.getComponentUnderTest().getPercentileHandLength(true, 24, 50), 1.0E-2);
        Assert.assertEquals(8.33, this.mocker.getComponentUnderTest().getPercentileHandLength(true, 24, 0), 1.0E-2);
        Assert.assertEquals(12.08, this.mocker.getComponentUnderTest().getPercentileHandLength(true, 24, 100), 1.0E-2);
        Assert.assertEquals(19.25, this.mocker.getComponentUnderTest().getPercentileHandLength(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(9.95, this.mocker.getComponentUnderTest().getPercentileHandLength(true, 36, 3), 1.0E-2);
        Assert.assertEquals(11.3, this.mocker.getComponentUnderTest().getPercentileHandLength(true, 36, 50), 1.0E-2);
        Assert.assertEquals(12.45, this.mocker.getComponentUnderTest().getPercentileHandLength(true, 36, 97), 1.0E-2);
        Assert.assertEquals(10.9, this.mocker.getComponentUnderTest().getPercentileHandLength(true, 30, 50), 1.0E-2);
    }

    @Test
    public void testGetStandardDeviationHandLength() throws ComponentLookupException
    {
        Assert.assertTrue(Double.isNaN(this.mocker.getComponentUnderTest().getStandardDeviationHandLength(true, 0, 0)));
        Assert.assertTrue(Double.isNaN(this.mocker.getComponentUnderTest().getStandardDeviationHandLength(true, 0, 0)));
        Assert.assertTrue(Double
            .isNaN(this.mocker.getComponentUnderTest().getStandardDeviationHandLength(true, 0, 100)));
        Assert
            .assertEquals(10.5, this.mocker.getComponentUnderTest().getStandardDeviationHandLength(true, 24, 0), 1E-2);
        Assert.assertEquals(19.25, this.mocker.getComponentUnderTest().getStandardDeviationHandLength(true, 1000, 00),
            1E-2);
        Assert.assertEquals(9.95, this.mocker.getComponentUnderTest().getStandardDeviationHandLength(true, 36, -1.881),
            1E-2);
        Assert
            .assertEquals(11.3, this.mocker.getComponentUnderTest().getStandardDeviationHandLength(true, 36, 0), 1E-2);
        Assert.assertEquals(12.45, this.mocker.getComponentUnderTest().getStandardDeviationHandLength(true, 36, 1.881),
            1E-2);
        Assert
            .assertEquals(10.9, this.mocker.getComponentUnderTest().getStandardDeviationHandLength(true, 30, 0), 1E-2);
    }

    @Test
    public void testGetPalmLengthPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getPalmLengthPercentile(true, 0, 3.9));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getPalmLengthPercentile(true, 0, 0));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().getPalmLengthPercentile(true, 0, 1000));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getPalmLengthPercentile(true, 1000, 11.225));
        Assert.assertEquals(3, this.mocker.getComponentUnderTest().getPalmLengthPercentile(true, 36, 5.625));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getPalmLengthPercentile(true, 36, 6.475));
        Assert.assertEquals(97, this.mocker.getComponentUnderTest().getPalmLengthPercentile(true, 36, 7.3));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getPalmLengthPercentile(true, 30, 6.237));
    }

    @Test
    public void testGetPalmLengthStandardDeviation() throws ComponentLookupException
    {
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getPalmLengthStandardDeviation(true, 0, 3.9), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getPalmLengthStandardDeviation(true, 1000, 11.225),
            1E-2);
        Assert.assertEquals(-1.881,
            this.mocker.getComponentUnderTest().getPalmLengthStandardDeviation(true, 36, 5.625), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getPalmLengthStandardDeviation(true, 36, 6.475),
            1E-2);
        Assert.assertEquals(1.881, this.mocker.getComponentUnderTest().getPalmLengthStandardDeviation(true, 36, 7.3),
            1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getPalmLengthStandardDeviation(true, 30, 6.237),
            1E-2);
    }

    @Test
    public void testGetPercentilePalmLength() throws ComponentLookupException
    {
        Assert.assertEquals(3.9, this.mocker.getComponentUnderTest().getPercentilePalmLength(true, 0, 50), 1.0E-2);
        Assert.assertEquals(2.56, this.mocker.getComponentUnderTest().getPercentilePalmLength(true, 0, 0), 1.0E-2);
        Assert.assertEquals(5.24, this.mocker.getComponentUnderTest().getPercentilePalmLength(true, 0, 100), 1.0E-2);
        Assert
            .assertEquals(11.225, this.mocker.getComponentUnderTest().getPercentilePalmLength(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(5.625, this.mocker.getComponentUnderTest().getPercentilePalmLength(true, 36, 3), 1.0E-2);
        Assert.assertEquals(6.475, this.mocker.getComponentUnderTest().getPercentilePalmLength(true, 36, 50), 1.0E-2);
        Assert.assertEquals(7.3, this.mocker.getComponentUnderTest().getPercentilePalmLength(true, 36, 97), 1.0E-2);
        Assert.assertEquals(6.237, this.mocker.getComponentUnderTest().getPercentilePalmLength(true, 30, 50), 1.0E-2);
    }

    @Test
    public void testGetStandardDeviationPalmLength() throws ComponentLookupException
    {
        Assert.assertEquals(3.9, this.mocker.getComponentUnderTest().getStandardDeviationPalmLength(true, 0, 0), 1E-2);
        Assert.assertEquals(11.225, this.mocker.getComponentUnderTest().getStandardDeviationPalmLength(true, 1000, 0),
            1E-2);
        Assert.assertEquals(5.625,
            this.mocker.getComponentUnderTest().getStandardDeviationPalmLength(true, 36, -1.881), 1E-2);
        Assert.assertEquals(6.475, this.mocker.getComponentUnderTest().getStandardDeviationPalmLength(true, 36, 0),
            1E-2);
        Assert.assertEquals(7.3, this.mocker.getComponentUnderTest().getStandardDeviationPalmLength(true, 36, 1.881),
            1E-2);
        Assert.assertEquals(6.237, this.mocker.getComponentUnderTest().getStandardDeviationPalmLength(true, 30, 0),
            1E-2);
    }

    @Test
    public void testGetFootLengthPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getFootLengthPercentile(true, 0, 7.5));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getFootLengthPercentile(false, 0, 8.5));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getFootLengthPercentile(true, 0, 0));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().getFootLengthPercentile(true, 0, 1000));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getFootLengthPercentile(false, 0, 0));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().getFootLengthPercentile(false, 0, 1000));
        Assert.assertEquals(3, this.mocker.getComponentUnderTest().getFootLengthPercentile(true, 36, 13.4));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getFootLengthPercentile(true, 36, 15.2));
        Assert.assertEquals(97, this.mocker.getComponentUnderTest().getFootLengthPercentile(true, 36, 16.8));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getFootLengthPercentile(true, 30, 14.5125));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getFootLengthPercentile(true, 1000, 26.45));
        Assert.assertEquals(3, this.mocker.getComponentUnderTest().getFootLengthPercentile(false, 36, 13));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getFootLengthPercentile(false, 36, 15.075));
        Assert.assertEquals(97, this.mocker.getComponentUnderTest().getFootLengthPercentile(false, 36, 16.95));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getFootLengthPercentile(false, 30, 14.4875));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getFootLengthPercentile(false, 1000, 23.975));
    }

    @Test
    public void testGetFootLengthStandardDeviation() throws ComponentLookupException
    {
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getFootLengthStandardDeviation(true, 0, 7.5), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getFootLengthStandardDeviation(false, 0, 8.5), 1E-2);
        Assert.assertEquals(-1.881, this.mocker.getComponentUnderTest().getFootLengthStandardDeviation(true, 36, 13.4),
            1E-2);
        Assert
            .assertEquals(0, this.mocker.getComponentUnderTest().getFootLengthStandardDeviation(true, 36, 15.2), 1E-2);
        Assert.assertEquals(1.881, this.mocker.getComponentUnderTest().getFootLengthStandardDeviation(true, 36, 16.8),
            1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getFootLengthStandardDeviation(true, 30, 14.5125),
            1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getFootLengthStandardDeviation(true, 1000, 26.45),
            1E-2);
        Assert.assertEquals(-1.881, this.mocker.getComponentUnderTest().getFootLengthStandardDeviation(false, 36, 13),
            1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getFootLengthStandardDeviation(false, 36, 15.075),
            1E-2);
        Assert.assertEquals(1.881,
            this.mocker.getComponentUnderTest().getFootLengthStandardDeviation(false, 36, 16.95), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getFootLengthStandardDeviation(false, 30, 14.4875),
            1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getFootLengthStandardDeviation(false, 1000, 23.975),
            1E-2);
    }

    @Test
    public void testGetPercentileFootLength() throws ComponentLookupException
    {
        Assert.assertEquals(7.5, this.mocker.getComponentUnderTest().getPercentileFootLength(true, 0, 50), 1.0E-2);
        Assert.assertEquals(8.5, this.mocker.getComponentUnderTest().getPercentileFootLength(false, 0, 50), 1.0E-2);
        Assert.assertEquals(6.75, this.mocker.getComponentUnderTest().getPercentileFootLength(true, 0, 0), 1.0E-2);
        Assert.assertEquals(8.25, this.mocker.getComponentUnderTest().getPercentileFootLength(true, 0, 100), 1.0E-2);
        Assert.assertEquals(19.54, this.mocker.getComponentUnderTest().getPercentileFootLength(false, 300, 0), 1.0E-2);
        Assert
            .assertEquals(27.05, this.mocker.getComponentUnderTest().getPercentileFootLength(false, 300, 100), 1.0E-2);
        Assert.assertEquals(13.4, this.mocker.getComponentUnderTest().getPercentileFootLength(true, 36, 3), 1.0E-2);
        Assert.assertEquals(15.2, this.mocker.getComponentUnderTest().getPercentileFootLength(true, 36, 50), 1.0E-2);
        Assert.assertEquals(16.8, this.mocker.getComponentUnderTest().getPercentileFootLength(true, 36, 97), 1.0E-2);
        Assert.assertEquals(14.5125, this.mocker.getComponentUnderTest().getPercentileFootLength(true, 30, 50), 1.0E-2);
        Assert.assertEquals(26.45, this.mocker.getComponentUnderTest().getPercentileFootLength(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(13, this.mocker.getComponentUnderTest().getPercentileFootLength(false, 36, 3), 1.0E-2);
        Assert.assertEquals(15.075, this.mocker.getComponentUnderTest().getPercentileFootLength(false, 36, 50), 1.0E-2);
        Assert.assertEquals(16.95, this.mocker.getComponentUnderTest().getPercentileFootLength(false, 36, 97), 1.0E-2);
        Assert
            .assertEquals(14.4875, this.mocker.getComponentUnderTest().getPercentileFootLength(false, 30, 50), 1.0E-2);
        Assert.assertEquals(23.975, this.mocker.getComponentUnderTest().getPercentileFootLength(false, 1000, 50),
            1.0E-2);
    }

    @Test
    public void testGetStandardDeviationFootLength() throws ComponentLookupException
    {
        Assert.assertEquals(7.5, this.mocker.getComponentUnderTest().getStandardDeviationFootLength(true, 0, 0), 1E-2);
        Assert.assertEquals(8.5, this.mocker.getComponentUnderTest().getStandardDeviationFootLength(false, 0, 0), 1E-2);
        Assert.assertEquals(13.4, this.mocker.getComponentUnderTest().getStandardDeviationFootLength(true, 36, -1.881),
            1E-2);
        Assert
            .assertEquals(15.2, this.mocker.getComponentUnderTest().getStandardDeviationFootLength(true, 36, 0), 1E-2);
        Assert.assertEquals(16.8, this.mocker.getComponentUnderTest().getStandardDeviationFootLength(true, 36, 1.881),
            1E-2);
        Assert.assertEquals(14.5125, this.mocker.getComponentUnderTest().getStandardDeviationFootLength(true, 30, 0),
            1E-2);
        Assert.assertEquals(26.45, this.mocker.getComponentUnderTest().getStandardDeviationFootLength(true, 1000, 0),
            1E-2);
        Assert.assertEquals(13, this.mocker.getComponentUnderTest().getStandardDeviationFootLength(false, 36, -1.881),
            1E-2);
        Assert.assertEquals(15.075, this.mocker.getComponentUnderTest().getStandardDeviationFootLength(false, 36, 0),
            1E-2);
        Assert.assertEquals(16.95,
            this.mocker.getComponentUnderTest().getStandardDeviationFootLength(false, 36, 1.881), 1E-2);
        Assert.assertEquals(14.4875, this.mocker.getComponentUnderTest().getStandardDeviationFootLength(false, 30, 0),
            1E-2);
        Assert.assertEquals(23.975, this.mocker.getComponentUnderTest().getStandardDeviationFootLength(false, 1000, 0),
            1E-2);
    }

    @Test
    public void testGetHeightPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getHeightPercentile(true, 0, 52.7));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getHeightPercentile(false, 0, 51.68));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getHeightPercentile(true, 0, 0));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().getHeightPercentile(true, 0, 1000));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getHeightPercentile(false, 0, 0));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().getHeightPercentile(false, 0, 1000));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getHeightPercentile(true, 1000, 176.85));
        Assert.assertEquals(72, this.mocker.getComponentUnderTest().getHeightPercentile(true, 349, 181.0));
        Assert.assertEquals(93, this.mocker.getComponentUnderTest().getHeightPercentile(false, 359, 173.0));
    }

    @Test
    public void testGetHeightStandardDeviation() throws ComponentLookupException
    {
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getHeightStandardDeviation(true, 0, 52.7), 1.0E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getHeightStandardDeviation(false, 0, 51.68), 1.0E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getHeightStandardDeviation(true, 1000, 176.85),
            1.0E-2);
        Assert.assertEquals(0.583, this.mocker.getComponentUnderTest().getHeightStandardDeviation(true, 349, 181.0),
            1.0E-2);
        Assert.assertEquals(1.497, this.mocker.getComponentUnderTest().getHeightStandardDeviation(false, 359, 173.0),
            1.0E-2);
    }

    @Test
    public void testGetPercentileHeight() throws ComponentLookupException
    {
        Assert.assertEquals(52.7, this.mocker.getComponentUnderTest().getPercentileHeight(true, 0, 50), 1.0E-2);
        Assert.assertEquals(51.68, this.mocker.getComponentUnderTest().getPercentileHeight(false, 0, 50), 1.0E-2);
        Assert.assertEquals(45.73, this.mocker.getComponentUnderTest().getPercentileHeight(true, 0, 0), 1.0E-2);
        Assert.assertEquals(60.14, this.mocker.getComponentUnderTest().getPercentileHeight(true, 0, 100), 1.0E-2);
        Assert.assertEquals(45.61, this.mocker.getComponentUnderTest().getPercentileHeight(false, 0, 0), 1.0E-2);
        Assert.assertEquals(59.39, this.mocker.getComponentUnderTest().getPercentileHeight(false, 0, 100), 1.0E-2);
        Assert.assertEquals(176.85, this.mocker.getComponentUnderTest().getPercentileHeight(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(181.0, this.mocker.getComponentUnderTest().getPercentileHeight(true, 349, 72), 1.0E-2);
        Assert.assertEquals(172.86, this.mocker.getComponentUnderTest().getPercentileHeight(false, 359, 93), 1.0E-2);
    }

    @Test
    public void testGetStandardDeviationHeight() throws ComponentLookupException
    {
        Assert.assertEquals(52.7, this.mocker.getComponentUnderTest().getStandardDeviationHeight(true, 0, 0), 1.0E-2);
        Assert.assertEquals(51.68, this.mocker.getComponentUnderTest().getStandardDeviationHeight(false, 0, 0), 1.0E-2);
        Assert.assertEquals(176.85, this.mocker.getComponentUnderTest().getStandardDeviationHeight(true, 1000, 0),
            1.0E-2);
        Assert.assertEquals(181.0, this.mocker.getComponentUnderTest().getStandardDeviationHeight(true, 349, 0.583),
            1.0E-2);
        Assert.assertEquals(173.0, this.mocker.getComponentUnderTest().getStandardDeviationHeight(false, 359, 1.497),
            1.0E-2);
    }

    @Test
    public void testGetHCPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getHCPercentile(true, 0, 37.19));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getHCPercentile(false, 0, 36.03));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getHCPercentile(true, 0, 0));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().getHCPercentile(true, 0, 1000));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getHCPercentile(false, 0, 0));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().getHCPercentile(false, 0, 1000));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().getHCPercentile(true, 1000, 49.68));
        Assert.assertEquals(3, this.mocker.getComponentUnderTest().getHCPercentile(true, 24, 46.01));
        Assert.assertEquals(95, this.mocker.getComponentUnderTest().getHCPercentile(false, 24, 49.80));
    }

    @Test
    public void testGetHCStandardDeviation() throws ComponentLookupException
    {
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getHCStandardDeviation(true, 0, 37.19), 1.0E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getHCStandardDeviation(false, 0, 36.03), 1.0E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getHCStandardDeviation(true, 1000, 49.68), 1.0E-2);
        Assert
            .assertEquals(-1.881, this.mocker.getComponentUnderTest().getHCStandardDeviation(true, 24, 46.01), 1.0E-2);
        Assert
            .assertEquals(1.645, this.mocker.getComponentUnderTest().getHCStandardDeviation(false, 24, 49.80), 1.0E-2);
    }

    @Test
    public void testGetPercentileHC() throws ComponentLookupException
    {
        Assert.assertEquals(37.19, this.mocker.getComponentUnderTest().getPercentileHC(true, 0, 50), 1.0E-2);
        Assert.assertEquals(36.03, this.mocker.getComponentUnderTest().getPercentileHC(false, 0, 50), 1.0E-2);
        Assert.assertEquals(30.55, this.mocker.getComponentUnderTest().getPercentileHC(true, 0, 0), 1.0E-2);
        Assert.assertEquals(41.31, this.mocker.getComponentUnderTest().getPercentileHC(true, 0, 100), 1.0E-2);
        Assert.assertEquals(32.24, this.mocker.getComponentUnderTest().getPercentileHC(false, 0, 0), 1.0E-2);
        Assert.assertEquals(41.14, this.mocker.getComponentUnderTest().getPercentileHC(false, 0, 100), 1.0E-2);
        Assert.assertEquals(49.68, this.mocker.getComponentUnderTest().getPercentileHC(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(46.01, this.mocker.getComponentUnderTest().getPercentileHC(true, 24, 3), 1.0E-2);
        Assert.assertEquals(49.80, this.mocker.getComponentUnderTest().getPercentileHC(false, 24, 95), 1.0E-2);
    }

    @Test
    public void testGetStandardDeviationHC() throws ComponentLookupException
    {
        Assert.assertEquals(37.19, this.mocker.getComponentUnderTest().getStandardDeviationHC(true, 0, 0), 1.0E-2);
        Assert.assertEquals(36.03, this.mocker.getComponentUnderTest().getStandardDeviationHC(false, 0, 0), 1.0E-2);
        Assert.assertEquals(49.68, this.mocker.getComponentUnderTest().getStandardDeviationHC(true, 1000, 0), 1.0E-2);
        Assert
            .assertEquals(46.01, this.mocker.getComponentUnderTest().getStandardDeviationHC(true, 24, -1.881), 1.0E-2);
        Assert
            .assertEquals(49.80, this.mocker.getComponentUnderTest().getStandardDeviationHC(false, 24, 1.645), 1.0E-2);
    }

    @Test
    public void testGetFuzzyValueP() throws ComponentLookupException
    {
        Assert.assertEquals("extreme-below-normal", this.mocker.getComponentUnderTest()
            .getFuzzyValue(Integer.MIN_VALUE));
        Assert.assertEquals("extreme-below-normal", this.mocker.getComponentUnderTest().getFuzzyValue(-1));
        Assert.assertEquals("extreme-below-normal", this.mocker.getComponentUnderTest().getFuzzyValue(0));
        Assert.assertEquals("extreme-below-normal", this.mocker.getComponentUnderTest().getFuzzyValue(1));
        Assert.assertEquals("below-normal", this.mocker.getComponentUnderTest().getFuzzyValue(2));
        Assert.assertEquals("below-normal", this.mocker.getComponentUnderTest().getFuzzyValue(3));
        Assert.assertEquals("normal", this.mocker.getComponentUnderTest().getFuzzyValue(4));
        Assert.assertEquals("normal", this.mocker.getComponentUnderTest().getFuzzyValue(50));
        Assert.assertEquals("normal", this.mocker.getComponentUnderTest().getFuzzyValue(96));
        Assert.assertEquals("above-normal", this.mocker.getComponentUnderTest().getFuzzyValue(97));
        Assert.assertEquals("above-normal", this.mocker.getComponentUnderTest().getFuzzyValue(98));
        Assert.assertEquals("extreme-above-normal", this.mocker.getComponentUnderTest().getFuzzyValue(99));
        Assert.assertEquals("extreme-above-normal", this.mocker.getComponentUnderTest().getFuzzyValue(100));
        Assert.assertEquals("extreme-above-normal", this.mocker.getComponentUnderTest().getFuzzyValue(101));
        Assert.assertEquals("extreme-above-normal", this.mocker.getComponentUnderTest()
            .getFuzzyValue(Integer.MAX_VALUE));
    }

    @Test
    public void testGetFuzzyValueSD() throws ComponentLookupException
    {
        Assert.assertEquals("extreme-below-normal", this.mocker.getComponentUnderTest()
            .getFuzzyValue(-Double.MAX_VALUE));
        Assert.assertEquals("extreme-below-normal", this.mocker.getComponentUnderTest().getFuzzyValue(-3.1));
        Assert.assertEquals("extreme-below-normal", this.mocker.getComponentUnderTest().getFuzzyValue(-3.0));
        Assert.assertEquals("below-normal", this.mocker.getComponentUnderTest().getFuzzyValue(-2.99));
        Assert.assertEquals("below-normal", this.mocker.getComponentUnderTest().getFuzzyValue(-2.0));
        Assert.assertEquals("normal", this.mocker.getComponentUnderTest().getFuzzyValue(-1.99));
        Assert.assertEquals("normal", this.mocker.getComponentUnderTest().getFuzzyValue(0.0));
        Assert.assertEquals("normal", this.mocker.getComponentUnderTest().getFuzzyValue(1.99));
        Assert.assertEquals("above-normal", this.mocker.getComponentUnderTest().getFuzzyValue(2.0));
        Assert.assertEquals("above-normal", this.mocker.getComponentUnderTest().getFuzzyValue(2.99));
        Assert.assertEquals("extreme-above-normal", this.mocker.getComponentUnderTest().getFuzzyValue(3.0));
        Assert.assertEquals("extreme-above-normal", this.mocker.getComponentUnderTest().getFuzzyValue(3.1));
        Assert
            .assertEquals("extreme-above-normal", this.mocker.getComponentUnderTest().getFuzzyValue(Double.MAX_VALUE));
    }
}
