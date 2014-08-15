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
 * Tests for the {@link PalpebralFissureLengthMeasurementHandler} component.
 *
 * @version $Id$
 * @since 1.0M3
 */
public class PFLTest
{
    @Rule
    public final MockitoComponentMockingRule<MeasurementHandler> mocker =
        new MockitoComponentMockingRule<MeasurementHandler>(PalpebralFissureLengthMeasurementHandler.class);

    @Test
    public void testValueToPercentile() throws ComponentLookupException
    {
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 0, 1.9));
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToPercentile(true, 0, 0));
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().valueToPercentile(true, 0, 1000));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 1000, 3.13));
        Assert.assertEquals(2, this.mocker.getComponentUnderTest().valueToPercentile(true, 36, 2.215));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 36, 2.49));
        Assert.assertEquals(98, this.mocker.getComponentUnderTest().valueToPercentile(true, 36, 2.78));
        Assert.assertEquals(50, this.mocker.getComponentUnderTest().valueToPercentile(true, 30, 2.4325));
    }

    @Test
    public void testValueToStandardDeviation() throws ComponentLookupException
    {
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 0, 1.9), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 900, 3.13), 1E-2);
        Assert.assertEquals(-2, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 36, 2.215), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 36, 2.49), 1E-2);
        Assert.assertEquals(2, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 36, 2.78), 1E-2);
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().valueToStandardDeviation(true, 30, 2.432), 1E-2);
    }

    @Test
    public void testPercentileToValue() throws ComponentLookupException
    {
        Assert.assertEquals(1.9, this.mocker.getComponentUnderTest().percentileToValue(true, 0, 50), 1E-2);
        Assert.assertEquals(1.64, this.mocker.getComponentUnderTest().percentileToValue(true, 0, 0), 1E-2);
        Assert.assertEquals(2.21, this.mocker.getComponentUnderTest().percentileToValue(true, 0, 100), 1E-2);
        Assert.assertEquals(3.13, this.mocker.getComponentUnderTest().percentileToValue(true, 1000, 50), 1E-2);
        Assert.assertEquals(2.215, this.mocker.getComponentUnderTest().percentileToValue(true, 36, 2), 1E-2);
        Assert.assertEquals(2.49, this.mocker.getComponentUnderTest().percentileToValue(true, 36, 50), 1E-2);
        Assert.assertEquals(2.78, this.mocker.getComponentUnderTest().percentileToValue(true, 36, 98), 1E-2);
        Assert.assertEquals(2.4325, this.mocker.getComponentUnderTest().percentileToValue(true, 30, 50), 1E-2);
    }

    @Test
    public void testStandardDeviationToValue() throws ComponentLookupException
    {
        Assert.assertEquals(1.9, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 0, 0), 1E-2);
        Assert.assertEquals(3.13, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 900, 0), 1E-2);
        Assert.assertEquals(2.215, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 36, -2), 1E-2);
        Assert.assertEquals(2.49, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 36, 0), 1E-2);
        Assert.assertEquals(2.78, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 36, 2), 1E-2);
        Assert.assertEquals(2.432, this.mocker.getComponentUnderTest().standardDeviationToValue(true, 30, 0), 1E-2);
    }

    @Test
    public void testIsDoubleSided() throws ComponentLookupException
    {
        Assert.assertFalse(this.mocker.getComponentUnderTest().isDoubleSided());
    }
}
