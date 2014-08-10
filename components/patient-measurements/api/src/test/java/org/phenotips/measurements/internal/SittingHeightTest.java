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
 * Tests for the {@link SittingHeightMeasurementHandler} component.
 *
 * @version $Id$
 * @since 1.0M3
 */
public class SittingHeightTest
{
    @Rule
    public final MockitoComponentMockingRule<MeasurementHandler> mocker =
        new MockitoComponentMockingRule<MeasurementHandler>(SittingHeightMeasurementHandler.class);

    @Test
    public void testValueToPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 0, 35));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(false, 0, 34.8));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToPercentile(true, 12, 0));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().valueToPercentile(true, 12, 1000));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToPercentile(false, 12, 0));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().valueToPercentile(false, 12, 1000));
        Assert.assertEquals(16, this.mocker.getComponentUnderTest().valueToPercentile(true, 36, 54.4));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 36, 56.5));
        Assert.assertEquals(84, this.mocker.getComponentUnderTest().valueToPercentile(true, 36, 58.6));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 30, 54.725));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 1000, 93));
        Assert.assertEquals(16, this.mocker.getComponentUnderTest().valueToPercentile(false, 36, 53.3));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(false, 36, 55.3));
        Assert.assertEquals(84, this.mocker.getComponentUnderTest().valueToPercentile(false, 36, 57.6));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(false, 30, 53.45));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(false, 1000, 87.5));
    }

    @Test
    public void testValueToStandardDeviation() throws ComponentLookupException
    {
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 0, 35), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(false, 0, 34.8), 1E-2);
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 36, 54.4), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 36, 56.5), 1E-2);
        Assert.assertEquals(1, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 36, 58.6), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 30, 54.725), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 1000, 93), 1E-2);
        Assert.assertEquals(-1, this.mocker.getComponentUnderTest().valueToStandardDeviation(false, 36, 53.3), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(false, 36, 55.3), 1E-2);
        Assert.assertEquals(1, this.mocker.getComponentUnderTest().valueToStandardDeviation(false, 36, 57.6), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(false, 30, 53.45), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(false, 1000, 87.5), 1E-2);
    }

    @Test
    public void testPercentileToValue() throws ComponentLookupException
    {
        Assert.assertEquals(35, this.mocker.getComponentUnderTest().percentileToValue(true, 0, 50), 1.0E-2);
        Assert.assertEquals(34.8, this.mocker.getComponentUnderTest().percentileToValue(false, 0, 50), 1.0E-2);
        Assert.assertEquals(54.4, this.mocker.getComponentUnderTest().percentileToValue(true, 36, 16), 1.0E-1);
        Assert.assertEquals(56.5, this.mocker.getComponentUnderTest().percentileToValue(true, 36, 50), 1.0E-2);
        Assert.assertEquals(58.6, this.mocker.getComponentUnderTest().percentileToValue(true, 36, 84), 1.0E-1);
        Assert.assertEquals(54.725, this.mocker.getComponentUnderTest().percentileToValue(true, 30, 50), 1.0E-2);
        Assert.assertEquals(93, this.mocker.getComponentUnderTest().percentileToValue(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(53.3, this.mocker.getComponentUnderTest().percentileToValue(false, 36, 16), 1.0E-1);
        Assert.assertEquals(55.3, this.mocker.getComponentUnderTest().percentileToValue(false, 36, 50), 1.0E-2);
        Assert.assertEquals(57.6, this.mocker.getComponentUnderTest().percentileToValue(false, 36, 84), 1.0E-1);
        Assert.assertEquals(53.45, this.mocker.getComponentUnderTest().percentileToValue(false, 30, 50), 1.0E-2);
        Assert.assertEquals(87.5, this.mocker.getComponentUnderTest().percentileToValue(false, 1000, 50), 1.0E-2);
    }

    @Test
    public void testStandardDeviationToValue() throws ComponentLookupException
    {
        Assert.assertEquals(35, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 0, 0), 1E-2);
        Assert.assertEquals(34.8, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 0, 0), 1E-2);
        Assert.assertEquals(54.4, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 36, -1), 1E-2);
        Assert.assertEquals(56.5, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 36, 0), 1E-2);
        Assert.assertEquals(58.6, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 36, 1), 1E-2);
        Assert.assertEquals(54.725, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 30, 0), 1E-2);
        Assert.assertEquals(93, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 1000, 0), 1E-2);
        Assert.assertEquals(53.3, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 36, -1), 1E-2);
        Assert.assertEquals(55.3, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 36, 0), 1E-2);
        Assert.assertEquals(57.6, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 36, 1), 1E-2);
        Assert.assertEquals(53.45, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 30, 0), 1E-2);
        Assert.assertEquals(87.5, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 1000, 0), 1E-2);
    }

    @Test
    public void testIsDoubleSided() throws ComponentLookupException
    {
        Assert.assertFalse(this.mocker.getComponentUnderTest().isDoubleSided());
    }
}
