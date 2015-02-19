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
package org.phenotips.measurements.internal;

import org.phenotips.measurements.MeasurementHandler;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests for the {@link BMIMeasurementHandler} component, and a few methods from the base @{link
 * {@link AbstractMeasurementHandler} class.
 *
 * @version $Id$
 * @since 1.0M3
 */
public class BMITest
{
    @Rule
    public final MockitoComponentMockingRule<MeasurementHandler> mocker =
        new MockitoComponentMockingRule<MeasurementHandler>(BMIMeasurementHandler.class);

    @Test
    public void testPercentileComputation() throws ComponentLookupException
    {
        Assert.assertEquals(0, getComponent().valueToPercentile(Double.MIN_VALUE, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(0, getComponent().valueToPercentile(-1, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(0, getComponent().valueToPercentile(0, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(0, getComponent().valueToPercentile(1, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(2, getComponent().valueToPercentile(2.114041, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(5, getComponent().valueToPercentile(2.179956, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(10, getComponent().valueToPercentile(2.250293, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(25, getComponent().valueToPercentile(2.374837, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(50, getComponent().valueToPercentile(2.5244, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(75, getComponent().valueToPercentile(2.686987, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(90, getComponent().valueToPercentile(2.84566, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(95, getComponent().valueToPercentile(2.946724, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(98, getComponent().valueToPercentile(3.050268, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(100, getComponent().valueToPercentile(3.5, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(100, getComponent().valueToPercentile(20, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(100, getComponent().valueToPercentile(1000, 2.5244, -0.3521, 0.09153));
        Assert.assertEquals(100, getComponent().valueToPercentile(Double.MAX_VALUE, 2.5244, -0.3521, 0.09153));
    }

    @Test
    public void testValueComputation() throws ComponentLookupException
    {
        // Values taken from the CDC data tables (Weight for age, boys, 0.5 months)
        double x = getComponent().percentileToValue(3, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(2.799548641, x, 1.0E-8);
        x = getComponent().percentileToValue(5, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(2.964655655, x, 1.0E-8);
        x = getComponent().percentileToValue(10, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(3.209510017, x, 1.0E-8);
        x = getComponent().percentileToValue(25, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(3.597395573, x, 1.0E-8);
        x = getComponent().percentileToValue(50, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(4.003106424, x, 1.0E-8);
        x = getComponent().percentileToValue(75, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(4.387422565, x, 1.0E-8);
        x = getComponent().percentileToValue(90, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(4.718161283, x, 1.0E-8);
        x = getComponent().percentileToValue(95, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(4.910130108, x, 1.0E-8);
        x = getComponent().percentileToValue(97, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(5.032624982, x, 1.0E-8);
        // Values taken from the CDC data tables (Weight for age, boys, 9.5 months)
        x = getComponent().percentileToValue(3, 9.476500305, -0.1600954, 0.11218624);
        Assert.assertEquals(7.700624405, x, 1.0E-8);
        x = getComponent().percentileToValue(90, 9.476500305, -0.1600954, 0.11218624);
        Assert.assertEquals(10.96017225, x, 1.0E-8);
        // Don't expect a child with +- Infinity kilograms...
        x = getComponent().percentileToValue(0, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(2.089641107, x, 1.0E-8);
        x = getComponent().percentileToValue(100, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(5.498638677, x, 1.0E-8);
        // Correct out of range percentiles
        x = getComponent().percentileToValue(Integer.MIN_VALUE, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(2.089641107, x, 1.0E-8);
        x = getComponent().percentileToValue(-50, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(2.089641107, x, 1.0E-8);
        x = getComponent().percentileToValue(1000, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(5.498638677, x, 1.0E-8);
        x = getComponent().percentileToValue(Integer.MAX_VALUE, 4.003106424, 1.547523128, 0.146025021);
        Assert.assertEquals(5.498638677, x, 1.0E-8);
    }

    @Test
    public void testGetBMI() throws ComponentLookupException
    {
        Assert.assertEquals(100.0, getComponent().computeBMI(100, 100), 1.0E-2);
        Assert.assertEquals(31.25, getComponent().computeBMI(80, 160), 1.0E-2);
        Assert.assertEquals(0.0, getComponent().computeBMI(0, 0), 1.0E-2);
        Assert.assertEquals(0.0, getComponent().computeBMI(80, 0), 1.0E-2);
        Assert.assertEquals(0.0, getComponent().computeBMI(0, 120), 1.0E-2);
        Assert.assertEquals(0.0, getComponent().computeBMI(-80, -160), 1.0E-2);
        Assert.assertEquals(Double.POSITIVE_INFINITY, getComponent().computeBMI(Double.MAX_VALUE, 1), 1.0E-2);
        Assert.assertEquals(0.0, getComponent().computeBMI(1, Double.MAX_VALUE), 1.0E-2);
    }

    @Test
    public void testValueToPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(50, getComponent().valueToPercentile(true, 0, 3.34, 49.9));
        Assert.assertEquals(50, getComponent().valueToPercentile(false, 0, 3.32, 49.9));
        Assert.assertEquals(0, getComponent().valueToPercentile(true, 0, 1, 1000));
        Assert.assertEquals(100, getComponent().valueToPercentile(true, 0, 1000, 1));
        Assert.assertEquals(0, getComponent().valueToPercentile(false, 0, 1, 1000));
        Assert.assertEquals(100, getComponent().valueToPercentile(false, 0, 1000, 1));
        Assert.assertEquals(0, getComponent().valueToPercentile(false, 0, 0, 0));
        Assert.assertEquals(21, getComponent().valueToPercentile(true, 42, 14.49, 100.0));
        Assert.assertEquals(92, getComponent().valueToPercentile(false, 42, 17.36, 100.0));
        Assert.assertEquals(0, getComponent().valueToPercentile(true, 100, 18, 130.0));
        Assert.assertEquals(100, getComponent().valueToPercentile(true, 100, 90, 110.0));
        Assert.assertEquals(16, getComponent().valueToPercentile(true, 349, 67.0, 181.0));
        Assert.assertEquals(0, getComponent().valueToPercentile(false, 359, 49.0, 173.0));
    }

    @Test
    public void testValueToStandardDeviation() throws ComponentLookupException
    {
        Assert.assertEquals(0, getComponent().valueToStandardDeviation(true, 0, 3.34, 49.9), 1.0E-2);
        Assert.assertEquals(0, getComponent().valueToStandardDeviation(false, 0, 3.32, 49.9), 1.0E-2);
        Assert.assertEquals(-1, getComponent().valueToStandardDeviation(true, 42, 14.26, 100.0), 1.0E-2);
        Assert.assertEquals(1, getComponent().valueToStandardDeviation(true, 42, 16.76, 100.0), 1.0E-2);
        Assert.assertEquals(-2, getComponent().valueToStandardDeviation(true, 42, 13.19, 100.0), 1.0E-2);
        Assert.assertEquals(2, getComponent().valueToStandardDeviation(true, 42, 18.21, 100.0), 1.0E-2);
        Assert.assertEquals(-3, getComponent().valueToStandardDeviation(true, 42, 12.22, 100.0), 1.0E-2);
        Assert.assertEquals(3, getComponent().valueToStandardDeviation(true, 42, 19.85, 100.0), 1.0E-2);
    }

    @Test
    public void testPercentileToValue() throws ComponentLookupException
    {
        Assert.assertEquals(13.4, this.mocker.getComponentUnderTest().percentileToValue(true, 0, 50), 1.0E-2);
        Assert.assertEquals(13.34, this.mocker.getComponentUnderTest().percentileToValue(false, 0, 50), 1.0E-2);
        Assert.assertEquals(10.36, this.mocker.getComponentUnderTest().percentileToValue(true, 0, 0), 1.0E-2);
        Assert.assertEquals(17.74, this.mocker.getComponentUnderTest().percentileToValue(true, 0, 100), 1.0E-2);
        Assert.assertEquals(10.3, this.mocker.getComponentUnderTest().percentileToValue(false, 0, 0), 1.0E-2);
        Assert.assertEquals(17.34, this.mocker.getComponentUnderTest().percentileToValue(false, 0, 100), 1.0E-2);
        Assert.assertEquals(23.04, this.mocker.getComponentUnderTest().percentileToValue(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(22.07, this.mocker.getComponentUnderTest().percentileToValue(true, 349, 37), 1.0E-2);
        Assert.assertEquals(18.7, this.mocker.getComponentUnderTest().percentileToValue(false, 359, 12), 1.0E-2);
    }

    @Test
    public void testStandardDeviationToValue() throws ComponentLookupException
    {
        Assert.assertEquals(13.4, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 0, 0), 1.0E-2);
        Assert.assertEquals(13.34, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 0, 0), 1.0E-2);
        Assert.assertEquals(10.36, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 0, -2.807),
            1.0E-2);
        Assert
            .assertEquals(17.74, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 0, 2.807), 1.0E-2);
        Assert.assertEquals(10.3, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 0, -2.807),
            1.0E-2);
        Assert.assertEquals(17.34, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 0, 2.807),
            1.0E-2);
        Assert.assertEquals(23.04, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 1000, 0), 1.0E-2);
        Assert.assertEquals(22.07, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 349, -0.332),
            1.0E-2);
        Assert.assertEquals(18.7, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 359, -1.175),
            1.0E-2);
    }

    private BMIMeasurementHandler getComponent() throws ComponentLookupException
    {
        return (BMIMeasurementHandler) this.mocker.getComponentUnderTest();
    }

    @Test
    public void testIsDoubleSided() throws ComponentLookupException
    {
        Assert.assertFalse(this.mocker.getComponentUnderTest().isDoubleSided());
    }
}
