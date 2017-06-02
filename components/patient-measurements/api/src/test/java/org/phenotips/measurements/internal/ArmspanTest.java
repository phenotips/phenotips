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
 * Tests for the {@link ArmspanMeasurementHandler} component, and a few methods from the base @{link
 * {@link AbstractMeasurementHandler} class.
 *
 * @version $Id$
 */
public class ArmspanTest
{
    @Rule
    public final MockitoComponentMockingRule<MeasurementHandler> mocker =
        new MockitoComponentMockingRule<MeasurementHandler>(ArmspanMeasurementHandler.class);

    @Test
    public void nameIsArmspan() throws ComponentLookupException
    {
        Assert.assertSame("armspan", this.mocker.getComponentUnderTest().getName());
    }

    @Test
    public void unitIsCm() throws ComponentLookupException
    {
        Assert.assertSame("cm", this.mocker.getComponentUnderTest().getUnit());
    }

    @Test
    public void isNotComputed() throws ComponentLookupException
    {
        Assert.assertFalse(this.mocker.getComponentUnderTest().isComputed());
    }

    @Test
    public void isNotDoubleSided() throws ComponentLookupException
    {
        Assert.assertFalse(this.mocker.getComponentUnderTest().isDoubleSided());
    }

    @Test
    public void hasNoComputationalDependencies() throws ComponentLookupException
    {
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getComputationDependencies().size());
        Assert.assertTrue(this.mocker.getComponentUnderTest().getComputationDependencies().isEmpty());
    }

    @Test
    public void hasNoChartConfiguration() throws ComponentLookupException
    {
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().getChartsConfigurations().size());
    }

    @Test
    public void hasNoAssociatedTerms() throws ComponentLookupException
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().getAssociatedTerms(3.0).isEmpty());
        Assert.assertTrue(this.mocker.getComponentUnderTest().getAssociatedTerms(-3.0).isEmpty());
        Assert.assertTrue(this.mocker.getComponentUnderTest().getAssociatedTerms(null).isEmpty());
    }
}