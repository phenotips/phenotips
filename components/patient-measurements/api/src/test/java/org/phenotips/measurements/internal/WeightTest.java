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
 * Tests for the {@link WeightMeasurementHandler} component.
 *
 * @version $Id$
 * @since 1.0M3
 */
public class WeightTest
{
    @Rule
    public final MockitoComponentMockingRule<MeasurementHandler> mocker =
    new MockitoComponentMockingRule<MeasurementHandler>(WeightMeasurementHandler.class);

    @Test
    public void testValueToPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 0, 3.35));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(false, 0, 3.23));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToPercentile(true, 0, 0));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().valueToPercentile(true, 0, 1000));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToPercentile(false, 0, 0));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().valueToPercentile(false, 0, 1000));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 1000, 70.6));
        Assert.assertEquals(37, this.mocker.getComponentUnderTest().valueToPercentile(true, 349, 67.0));
        Assert.assertEquals(12, this.mocker.getComponentUnderTest().valueToPercentile(false, 359, 49.0));
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().valueToPercentile(true, -1, 4.0));
    }

    @Test
    public void testValueToStandardDeviation() throws ComponentLookupException
    {
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 0, 3.35), 1.0E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(false, 0, 3.23), 1.0E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 1000, 70.6), 1.0E-2);
        Assert.assertEquals(-0.332, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 349, 67.0),
            1.0E-2);
        Assert.assertEquals(-1.175, this.mocker.getComponentUnderTest().valueToStandardDeviation(false, 359, 49.0),
            1.0E-2);
        Assert.assertTrue(Double.isNaN(this.mocker.getComponentUnderTest().valueToStandardDeviation(true, -1, 4.0)));
    }

    @Test
    public void testPercentileToValue() throws ComponentLookupException
    {
        Assert.assertEquals(3.35, this.mocker.getComponentUnderTest().percentileToValue(true, 0, 50), 1.0E-2);
        Assert.assertEquals(3.23, this.mocker.getComponentUnderTest().percentileToValue(false, 0, 50), 1.0E-2);
        Assert.assertEquals(2.15, this.mocker.getComponentUnderTest().percentileToValue(true, 0, 0), 1.0E-2);
        Assert.assertEquals(4.91, this.mocker.getComponentUnderTest().percentileToValue(true, 0, 100), 1.0E-2);
        Assert.assertEquals(2.1, this.mocker.getComponentUnderTest().percentileToValue(false, 0, 0), 1.0E-2);
        Assert.assertEquals(4.68, this.mocker.getComponentUnderTest().percentileToValue(false, 0, 100), 1.0E-2);
        Assert.assertEquals(70.6, this.mocker.getComponentUnderTest().percentileToValue(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(67.0, this.mocker.getComponentUnderTest().percentileToValue(true, 349, 37), 1.0E-2);
        Assert.assertEquals(49.04, this.mocker.getComponentUnderTest().percentileToValue(false, 359, 12), 1.0E-2);
    }

    @Test
    public void testStandardDeviationToValue() throws ComponentLookupException
    {
        Assert.assertEquals(3.35, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 0, 0), 1.0E-2);
        Assert.assertEquals(3.23, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 0, 0), 1.0E-2);
        Assert
        .assertEquals(2.15, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 0, -2.807), 1.0E-2);
        Assert.assertEquals(4.91, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 0, 2.807), 1.0E-2);
        Assert
        .assertEquals(2.1, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 0, -2.807), 1.0E-2);
        Assert
        .assertEquals(4.68, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 0, 2.807), 1.0E-2);
        Assert.assertEquals(70.6, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 1000, 0), 1.0E-2);
        Assert.assertEquals(67.0, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 349, -0.332),
            1.0E-2);
        Assert.assertEquals(49.04, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 359, -1.175),
            1.0E-2);
        Assert.assertEquals(0,
            this.mocker.getComponentUnderTest().standardDeviationToValue(false, 359, Integer.MIN_VALUE), 1.0E-2);
    }

    @Test
    public void testIsDoubleSided() throws ComponentLookupException
    {
        Assert.assertFalse(this.mocker.getComponentUnderTest().isDoubleSided());
    }
}
