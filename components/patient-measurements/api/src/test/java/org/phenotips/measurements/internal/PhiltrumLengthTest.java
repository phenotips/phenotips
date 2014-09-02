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
 * Tests for the {@link PhiltrumLengthMeasurementHandler} component. These tests are based on the Zankl charts.
 *
 * @version $Id$
 * @since 1.0M3
 */
public class PhiltrumLengthTest
{
    @Rule
    public final MockitoComponentMockingRule<MeasurementHandler> mocker =
        new MockitoComponentMockingRule<MeasurementHandler>(PhiltrumLengthMeasurementHandler.class);

    @Test
    public void testValueToPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(2, this.mocker.getComponentUnderTest().valueToPercentile(true, 0, 1.025));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 0, 1.41));
        Assert.assertEquals(98, this.mocker.getComponentUnderTest().valueToPercentile(true, 0, 1.775));
        Assert.assertEquals(2, this.mocker.getComponentUnderTest().valueToPercentile(false, 0, 0.995));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(false, 0, 1.334));
        Assert.assertEquals(98, this.mocker.getComponentUnderTest().valueToPercentile(false, 0, 1.625));
        Assert.assertEquals(2, this.mocker.getComponentUnderTest().valueToPercentile(true, 60, 1.19));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 60, 1.528));
        Assert.assertEquals(98, this.mocker.getComponentUnderTest().valueToPercentile(true, 60, 1.87));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 45, 1.496));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 1000, 1.86));
    }

    @Test
    public void testValueToStandardDeviation() throws ComponentLookupException
    {
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 0, 1.41), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(false, 0, 1.334), 1E-2);
        Assert.assertEquals(-2, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 60, 1.19), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 60, 1.528), 1E-2);
        Assert.assertEquals(2, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 60, 1.87), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 45, 1.496), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 1000, 1.86), 1E-2);
    }

    @Test
    public void testPercentileToValue() throws ComponentLookupException
    {
        Assert.assertEquals(1.014, this.mocker.getComponentUnderTest().percentileToValue(true, 0, 2), 1E-2);
        Assert.assertEquals(1.41, this.mocker.getComponentUnderTest().percentileToValue(true, 0, 50), 1E-2);
        Assert.assertEquals(1.775, this.mocker.getComponentUnderTest().percentileToValue(true, 0, 98), 1E-2);
        Assert.assertEquals(0.985, this.mocker.getComponentUnderTest().percentileToValue(false, 0, 2), 1E-2);
        Assert.assertEquals(1.334, this.mocker.getComponentUnderTest().percentileToValue(false, 0, 50), 1E-2);
        Assert.assertEquals(1.625, this.mocker.getComponentUnderTest().percentileToValue(false, 0, 98), 1E-2);
        Assert.assertEquals(1.19, this.mocker.getComponentUnderTest().percentileToValue(true, 60, 2), 1E-2);
        Assert.assertEquals(1.528, this.mocker.getComponentUnderTest().percentileToValue(true, 60, 50), 1E-2);
        Assert.assertEquals(1.87, this.mocker.getComponentUnderTest().percentileToValue(true, 60, 98), 1E-2);
        Assert.assertEquals(1.496, this.mocker.getComponentUnderTest().percentileToValue(true, 45, 50), 1E-2);
        Assert.assertEquals(1.86, this.mocker.getComponentUnderTest().percentileToValue(true, 1000, 50), 1E-2);
    }

    @Test
    public void testStandardDeviationToValue() throws ComponentLookupException
    {
        Assert.assertEquals(1.025, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 0, -2), 1E-2);
        Assert.assertEquals(1.41, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 0, 0), 1E-2);
        Assert.assertEquals(1.775, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 0, 2), 1E-2);
        Assert.assertEquals(0.995, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 0, -2), 1E-2);
        Assert.assertEquals(1.334, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 0, 0), 1E-2);
        Assert.assertEquals(1.625, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 0, 2), 1E-2);
        Assert.assertEquals(1.19, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 60, -2), 1E-2);
        Assert.assertEquals(1.528, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 60, 0), 1E-2);
        Assert.assertEquals(1.87, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 60, 2), 1E-2);
        Assert.assertEquals(1.496, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 45, 0), 1E-2);
        Assert.assertEquals(1.86, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 1000, 0), 1E-2);
    }

    @Test
    public void testIsDoubleSided() throws ComponentLookupException
    {
        Assert.assertFalse(this.mocker.getComponentUnderTest().isDoubleSided());
    }
}
