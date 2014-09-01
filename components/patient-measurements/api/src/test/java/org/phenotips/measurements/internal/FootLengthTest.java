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
 * Tests for the {@link FootLengthMeasurementHandler} component.
 *
 * @version $Id$
 * @since 1.0M3
 */
public class FootLengthTest
{
    @Rule
    public final MockitoComponentMockingRule<MeasurementHandler> mocker =
        new MockitoComponentMockingRule<MeasurementHandler>(FootLengthMeasurementHandler.class);

    @Test
    public void testValueToPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 0, 7.5));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(false, 0, 8.5));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToPercentile(true, 0, 0));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().valueToPercentile(true, 0, 1000));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToPercentile(false, 0, 0));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().valueToPercentile(false, 0, 1000));
        Assert.assertEquals(3, this.mocker.getComponentUnderTest().valueToPercentile(true, 36, 13.4));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 36, 15.2));
        Assert.assertEquals(97, this.mocker.getComponentUnderTest().valueToPercentile(true, 36, 16.8));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 30, 14.5125));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 1000, 26.45));
        Assert.assertEquals(3, this.mocker.getComponentUnderTest().valueToPercentile(false, 36, 13));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(false, 36, 15.075));
        Assert.assertEquals(97, this.mocker.getComponentUnderTest().valueToPercentile(false, 36, 16.95));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(false, 30, 14.4875));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(false, 1000, 23.975));
    }

    @Test
    public void testValueToStandardDeviation() throws ComponentLookupException
    {
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 0, 7.5), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(false, 0, 8.5), 1E-2);
        Assert.assertEquals(-1.881, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 36, 13.4), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 36, 15.2), 1E-2);
        Assert.assertEquals(1.881, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 36, 16.8), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 30, 14.5125), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 1000, 26.45), 1E-2);
        Assert.assertEquals(-1.881, this.mocker.getComponentUnderTest().valueToStandardDeviation(false, 36, 13), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(false, 36, 15.075), 1E-2);
        Assert.assertEquals(1.88, this.mocker.getComponentUnderTest().valueToStandardDeviation(false, 36, 16.95), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(false, 30, 14.4875), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(false, 1000, 23.975), 1E-2);
    }

    @Test
    public void testPercentileToValue() throws ComponentLookupException
    {
        Assert.assertEquals(7.5, this.mocker.getComponentUnderTest().percentileToValue(true, 0, 50), 1.0E-2);
        Assert.assertEquals(8.5, this.mocker.getComponentUnderTest().percentileToValue(false, 0, 50), 1.0E-2);
        Assert.assertEquals(6.75, this.mocker.getComponentUnderTest().percentileToValue(true, 0, 0), 1.0E-2);
        Assert.assertEquals(8.25, this.mocker.getComponentUnderTest().percentileToValue(true, 0, 100), 1.0E-2);
        Assert.assertEquals(19.54, this.mocker.getComponentUnderTest().percentileToValue(false, 300, 0), 1.0E-2);
        Assert.assertEquals(27.05, this.mocker.getComponentUnderTest().percentileToValue(false, 300, 100), 1.0E-2);
        Assert.assertEquals(13.4, this.mocker.getComponentUnderTest().percentileToValue(true, 36, 3), 1.0E-2);
        Assert.assertEquals(15.2, this.mocker.getComponentUnderTest().percentileToValue(true, 36, 50), 1.0E-2);
        Assert.assertEquals(16.8, this.mocker.getComponentUnderTest().percentileToValue(true, 36, 97), 1.0E-2);
        Assert.assertEquals(14.5125, this.mocker.getComponentUnderTest().percentileToValue(true, 30, 50), 1.0E-2);
        Assert.assertEquals(26.45, this.mocker.getComponentUnderTest().percentileToValue(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(13, this.mocker.getComponentUnderTest().percentileToValue(false, 36, 3), 1.0E-2);
        Assert.assertEquals(15.075, this.mocker.getComponentUnderTest().percentileToValue(false, 36, 50), 1.0E-2);
        Assert.assertEquals(16.95, this.mocker.getComponentUnderTest().percentileToValue(false, 36, 97), 1.0E-2);
        Assert.assertEquals(14.4875, this.mocker.getComponentUnderTest().percentileToValue(false, 30, 50), 1.0E-2);
        Assert.assertEquals(23.975, this.mocker.getComponentUnderTest().percentileToValue(false, 1000, 50), 1.0E-2);
    }

    @Test
    public void testStandardDeviationToValue() throws ComponentLookupException
    {
        Assert.assertEquals(7.5, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 0, 0), 1E-2);
        Assert.assertEquals(8.5, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 0, 0), 1E-2);
        Assert.assertEquals(13.4, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 36, -1.881), 1E-2);
        Assert.assertEquals(15.2, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 36, 0), 1E-2);
        Assert.assertEquals(16.8, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 36, 1.881), 1E-2);
        Assert.assertEquals(14.5125, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 30, 0), 1E-2);
        Assert.assertEquals(26.45, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 1000, 0), 1E-2);
        Assert.assertEquals(13, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 36, -1.881), 1E-2);
        Assert.assertEquals(15.075, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 36, 0), 1E-2);
        Assert.assertEquals(16.95, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 36, 1.88), 1E-2);
        Assert.assertEquals(14.4875, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 30, 0), 1E-2);
        Assert.assertEquals(23.975, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 1000, 0), 1E-2);
    }

    @Test
    public void testIsDoubleSided() throws ComponentLookupException
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().isDoubleSided());
    }
}
