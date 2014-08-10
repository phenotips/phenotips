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
 * Tests for the {@link OuterCanthalDistanceMeasurementHandler} component.
 *
 * @version $Id$
 * @since 1.0M3
 */
public class OCDTest
{
    @Rule
    public final MockitoComponentMockingRule<MeasurementHandler> mocker =
        new MockitoComponentMockingRule<MeasurementHandler>(OuterCanthalDistanceMeasurementHandler.class);

    @Test
    public void testValueToPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 0, 6.3));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToPercentile(true, 0, 0));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().valueToPercentile(true, 0, 1000));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 1000, 9.08));
        Assert.assertEquals(3, this.mocker.getComponentUnderTest().valueToPercentile(true, 16, 6.27));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 16, 7.305));
        Assert.assertEquals(97, this.mocker.getComponentUnderTest().valueToPercentile(true, 16, 8.33));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 30, 7.4725));
    }

    @Test
    public void testValueToStandardDeviation() throws ComponentLookupException
    {
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 0, 6.3), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 900, 9.08), 1E-2);
        Assert.assertEquals(-1.88, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 16, 6.27), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 16, 7.305), 1E-2);
        Assert.assertEquals(1.88, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 16, 8.33), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 30, 7.4725), 1E-2);
    }

    @Test
    public void testPercentileToValue() throws ComponentLookupException
    {
        Assert.assertEquals(6.3, this.mocker.getComponentUnderTest().percentileToValue(true, 0, 50), 1.0E-2);
        Assert.assertEquals(4.86, this.mocker.getComponentUnderTest().percentileToValue(true, 0, 0), 1.0E-2);
        Assert.assertEquals(7.98, this.mocker.getComponentUnderTest().percentileToValue(true, 0, 100), 1.0E-2);
        Assert.assertEquals(9.08, this.mocker.getComponentUnderTest().percentileToValue(true, 1000, 50), 1.0E-2);
        Assert.assertEquals(6.27, this.mocker.getComponentUnderTest().percentileToValue(true, 16, 3), 1.0E-2);
        Assert.assertEquals(7.305, this.mocker.getComponentUnderTest().percentileToValue(true, 16, 50), 1.0E-2);
        Assert.assertEquals(8.33, this.mocker.getComponentUnderTest().percentileToValue(true, 16, 97), 1.0E-2);
        Assert.assertEquals(7.4725, this.mocker.getComponentUnderTest().percentileToValue(true, 30, 50), 1.0E-2);
    }

    @Test
    public void testStandardDeviationToValue() throws ComponentLookupException
    {
        Assert.assertEquals(6.3, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 0, 0), 1.0E-2);
        Assert.assertEquals(9.08, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 900, 0), 1.0E-2);
        Assert.assertEquals(6.27, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 16, -1.88), 1E-2);
        Assert.assertEquals(7.305, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 16, 0), 1E-2);
        Assert.assertEquals(8.33, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 16, 1.88), 1E-2);
        Assert.assertEquals(7.4725, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 30, 0), 1E-2);
    }

    @Test
    public void testIsDoubleSided() throws ComponentLookupException
    {
        Assert.assertFalse(this.mocker.getComponentUnderTest().isDoubleSided());
    }
}
