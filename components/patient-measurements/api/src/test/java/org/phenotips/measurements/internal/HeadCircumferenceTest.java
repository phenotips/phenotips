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
package org.phenotips.measurements.internal;

import org.phenotips.measurements.MeasurementHandler;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests for the {@link HeadCircumferenceMeasurementHandler} component.
 *
 * @version $Id$
 * @since 1.0M3
 */
public class HeadCircumferenceTest
{
    @Rule
    public final MockitoComponentMockingRule<MeasurementHandler> mocker =
        new MockitoComponentMockingRule<MeasurementHandler>(HeadCircumferenceMeasurementHandler.class);

    @Test
    public void testValueToPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 0, 35.81));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(false, 0, 34.71));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToPercentile(true, 0, 0));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().valueToPercentile(true, 0, 1000));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToPercentile(false, 0, 0));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().valueToPercentile(false, 0, 1000));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 36, 50.22));
        Assert.assertEquals(1, this.mocker.getComponentUnderTest().valueToPercentile(true, 24, 46.01));
        Assert.assertEquals(92, this.mocker.getComponentUnderTest().valueToPercentile(false, 24, 49.80));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 192, 55.77));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(false, 192, 54.31));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 1000, 56.75));
    }

    @Test
    public void testValueToStandardDeviation() throws ComponentLookupException
    {
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 0, 35.81), 1.0E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(false, 0, 34.71), 1.0E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 36, 50.22), 1.0E-2);
        Assert.assertEquals(-2.21, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 24, 46.01),
            1.0E-2);
        Assert.assertEquals(1.39, this.mocker.getComponentUnderTest().valueToStandardDeviation(false, 24, 49.80),
            1.0E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 192, 55.77),
            1.0E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(false, 192, 54.31),
            1.0E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 1000, 56.75), 1.0E-2);
    }

    @Test
    public void testPercentileToValue() throws ComponentLookupException
    {
        Assert.assertEquals(35.81, this.mocker.getComponentUnderTest().percentileToValue(true, 0, 50), 1.0E-2);
        Assert.assertEquals(34.71, this.mocker.getComponentUnderTest().percentileToValue(false, 0, 50), 1.0E-2);
        Assert.assertEquals(28.30, this.mocker.getComponentUnderTest().percentileToValue(true, 0, 0), 1.0E-2);
        Assert.assertEquals(40.09, this.mocker.getComponentUnderTest().percentileToValue(true, 0, 100), 1.0E-2);
        Assert.assertEquals(30.74, this.mocker.getComponentUnderTest().percentileToValue(false, 0, 0), 1.0E-2);
        Assert.assertEquals(40.1, this.mocker.getComponentUnderTest().percentileToValue(false, 0, 100), 1.0E-2);
        Assert.assertEquals(50.22, this.mocker.getComponentUnderTest().percentileToValue(true, 36, 50), 1.0E-2);
        Assert.assertEquals(46.47, this.mocker.getComponentUnderTest().percentileToValue(true, 24, 3), 1.0E-2);
        Assert.assertEquals(50.11, this.mocker.getComponentUnderTest().percentileToValue(false, 24, 95), 1.0E-2);
        Assert.assertEquals(55.77, this.mocker.getComponentUnderTest().percentileToValue(true, 192, 50), 1.0E-2);
        Assert.assertEquals(54.31, this.mocker.getComponentUnderTest().percentileToValue(false, 192, 50), 1.0E-2);
        Assert.assertEquals(56.75, this.mocker.getComponentUnderTest().percentileToValue(true, 1000, 50), 1.0E-2);
    }

    @Test
    public void testStandardDeviationToValue() throws ComponentLookupException
    {
        Assert.assertEquals(35.81, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 0, 0), 1.0E-2);
        Assert.assertEquals(34.71, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 0, 0), 1.0E-2);
        Assert.assertEquals(50.22, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 36, 0), 1.0E-2);
        Assert.assertEquals(46.47, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 24, -1.881),
            1.0E-2);
        Assert.assertEquals(50.11, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 24, 1.645),
            1.0E-2);
        Assert.assertEquals(54.31, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 192, 0), 1.0E-2);
        Assert.assertEquals(54.31, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 192, 0), 1.0E-2);
        Assert.assertEquals(56.75, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 1000, 0), 1.0E-2);
    }

    @Test
    public void testIsDoubleSided() throws ComponentLookupException
    {
        Assert.assertFalse(this.mocker.getComponentUnderTest().isDoubleSided());
    }
}
