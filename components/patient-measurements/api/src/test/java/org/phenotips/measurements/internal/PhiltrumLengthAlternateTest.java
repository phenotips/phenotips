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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests for the {@link PhiltrumLengthMeasurementHandler} component. These tests are based on the Feingold charts.
 *
 * @version $Id$
 * @since 1.0M3
 */
@Ignore
public class PhiltrumLengthAlternateTest
{
    @Rule
    public final MockitoComponentMockingRule<MeasurementHandler> mocker =
        new MockitoComponentMockingRule<MeasurementHandler>(PhiltrumLengthMeasurementHandler.class);

    @Test
    public void testValueToPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(3, this.mocker.getComponentUnderTest().valueToPercentile(true, 0, 0.6));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 0, 0.854));
        Assert.assertEquals(97, this.mocker.getComponentUnderTest().valueToPercentile(true, 0, 2.57));
        Assert.assertEquals(3, this.mocker.getComponentUnderTest().valueToPercentile(false, 0, 0.6));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(false, 0, 0.854));
        Assert.assertEquals(97, this.mocker.getComponentUnderTest().valueToPercentile(false, 0, 2.57));
        Assert.assertEquals(3, this.mocker.getComponentUnderTest().valueToPercentile(true, 36, 0.81));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 36, 1.37));
        Assert.assertEquals(97, this.mocker.getComponentUnderTest().valueToPercentile(true, 36, 5.155));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 30, 1.34));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 1000, 1.73));
    }

    @Test
    public void testValueToStandardDeviation() throws ComponentLookupException
    {
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 0, 0.854), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(false, 0, 0.854), 1E-2);
        Assert.assertEquals(-1.881, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 36, 0.81), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 36, 1.37), 1E-2);
        Assert.assertEquals(1.881, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 36, 5.155), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 30, 1.34), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 1000, 1.73), 1E-2);
    }

    @Test
    public void testPercentileToValue() throws ComponentLookupException
    {
        Assert.assertEquals(0.6, this.mocker.getComponentUnderTest().percentileToValue(true, 0, 3), 1.0E-2);
        Assert.assertEquals(0.854, this.mocker.getComponentUnderTest().percentileToValue(true, 0, 50), 1.0E-2);
        Assert.assertEquals(2.57, this.mocker.getComponentUnderTest().percentileToValue(true, 0, 97), 1.0E-2);
        Assert.assertEquals(0.6, this.mocker.getComponentUnderTest().percentileToValue(false, 0, 3), 1.0E-2);
        Assert.assertEquals(0.854, this.mocker.getComponentUnderTest().percentileToValue(false, 0, 50), 1.0E-2);
        Assert.assertEquals(2.57, this.mocker.getComponentUnderTest().percentileToValue(false, 0, 97), 1.0E-2);
        Assert.assertEquals(0.81, this.mocker.getComponentUnderTest().percentileToValue(true, 36, 3), 1.0E-2);
        Assert.assertEquals(1.37, this.mocker.getComponentUnderTest().percentileToValue(true, 36, 50), 1.0E-2);
        Assert.assertEquals(5.155, this.mocker.getComponentUnderTest().percentileToValue(true, 36, 97), 1.0E-2);
        Assert.assertEquals(1.34, this.mocker.getComponentUnderTest().percentileToValue(true, 30, 50), 1.0E-2);
        Assert.assertEquals(1.73, this.mocker.getComponentUnderTest().percentileToValue(true, 1000, 50), 1.0E-2);
    }

    @Test
    public void testStandardDeviationToValue() throws ComponentLookupException
    {
        Assert.assertEquals(0.6, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 0, -1.881), 1E-2);
        Assert.assertEquals(0.854, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 0, 0), 1E-2);
        Assert.assertEquals(2.57, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 0, 1.881), 1E-2);
        Assert.assertEquals(0.6, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 0, -1.881), 1E-2);
        Assert.assertEquals(0.854, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 0, 0), 1E-2);
        Assert.assertEquals(2.57, this.mocker.getComponentUnderTest().standardDeviationToValue(false, 0, 1.881), 1E-2);
        Assert.assertEquals(0.81, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 36, -1.881), 1E-2);
        Assert.assertEquals(1.37, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 36, 0), 1E-2);
        Assert.assertEquals(5.155, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 36, 1.881), 1E-2);
        Assert.assertEquals(1.34, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 30, 0), 1E-2);
        Assert.assertEquals(1.73, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 1000, 0), 1E-2);
    }

    @Test
    public void testIsDoubleSided() throws ComponentLookupException
    {
        Assert.assertFalse(this.mocker.getComponentUnderTest().isDoubleSided());
    }
}
