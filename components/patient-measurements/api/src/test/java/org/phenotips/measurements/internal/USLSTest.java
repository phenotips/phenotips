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
 * Tests for the {@link USLSMeasurementHandler} component, and a few methods from the base @{link
 * {@link AbstractMeasurementHandler} class.
 *
 * @version $Id$
 * @since 1.4
 */
public class USLSTest
{
    @Rule
    public final MockitoComponentMockingRule<MeasurementHandler> mocker =
        new MockitoComponentMockingRule<MeasurementHandler>(USLSMeasurementHandler.class);

    private USLSMeasurementHandler getComponent() throws ComponentLookupException
    {
        return (USLSMeasurementHandler) this.mocker.getComponentUnderTest();
    }

    @Test
    public void nameIsUSLS() throws ComponentLookupException
    {
        Assert.assertSame("usls", getComponent().getName());
    }

    // Since the measurement is a ratio, it should be displayed as unitless
    @Test
    public void unitIsVoid() throws ComponentLookupException
    {
        Assert.assertSame("", getComponent().getUnit());
    }

    @Test
    public void isComputed() throws ComponentLookupException
    {
        Assert.assertTrue(getComponent().isComputed());
    }

    @Test
    public void checkComputationalDependencies() throws ComponentLookupException
    {
        Object[] x = getComponent().getComputationDependencies().toArray();
        Assert.assertSame("upperSeg", x[0]);
        Assert.assertSame("lowerSeg", x[1]);
        Assert.assertEquals(2, x.length);
    }

    @Test
    public void testComputeUSLS() throws ComponentLookupException
    {
        Assert.assertEquals(1.0, getComponent().computeUSLS(60, 60), 1.0E-2);
        Assert.assertEquals(0.75, getComponent().computeUSLS(48, 64), 1.0E-2);
        Assert.assertEquals(1.125, getComponent().computeUSLS(72, 64), 1.0E-2);
        Assert.assertEquals(0.0, getComponent().computeUSLS(80, 0), 1.0E-2);
        Assert.assertEquals(0.0, getComponent().computeUSLS(0, 120), 1.0E-2);
        Assert.assertEquals(0.0, getComponent().computeUSLS(-80, -160), 1.0E-2);
        Assert.assertEquals(Double.MAX_VALUE, getComponent().computeUSLS(Double.MAX_VALUE, 1), 1.0E-2);
        Assert.assertEquals(0.0, getComponent().computeUSLS(1, Double.MAX_VALUE), 1.0E-2);
    }

    @Test
    public void isNotDoubleSided() throws ComponentLookupException
    {
        Assert.assertFalse(getComponent().isDoubleSided());
    }

    @Test
    public void hasNoChartConfiguration() throws ComponentLookupException
    {
        Assert.assertEquals(0, getComponent().getChartsConfigurations().size());
    }

    @Test
    public void hasNoAssociatedTerms() throws ComponentLookupException
    {
        Assert.assertTrue(getComponent().getAssociatedTerms(3.0).isEmpty());
        Assert.assertTrue(getComponent().getAssociatedTerms(2.0).isEmpty());
        Assert.assertTrue(getComponent().getAssociatedTerms(-2.0).isEmpty());
        Assert.assertTrue(getComponent().getAssociatedTerms(-3.0).isEmpty());
        Assert.assertTrue(getComponent().getAssociatedTerms(null).isEmpty());
    }
}
