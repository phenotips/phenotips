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
package org.phenotips.measurements.script;

import org.phenotips.measurements.MeasurementHandler;
import org.phenotips.measurements.internal.HeightMeasurementHandler;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;

/**
 * Tests for the {@link MeasurementsScriptService} component.
 *
 * @version $Id$
 * @since 1.0M1
 */
public class MeasurementsScriptServiceTest
{
    @Rule
    public final MockitoComponentMockingRule<MeasurementsScriptService> mocker =
        new MockitoComponentMockingRule<>(MeasurementsScriptService.class);

    @Test
    public void testGetWithNonExistentHint() throws ComponentLookupException
    {
        ComponentManager cm = this.mocker.getInstance(ComponentManager.class, "context");
        when(cm.getInstance(MeasurementHandler.class, "nothing")).thenThrow(new ComponentLookupException(""));
        Assert.assertNull(this.mocker.getComponentUnderTest().get("nothing"));
    }

    @Test
    public void testGetWithValidHint() throws ComponentLookupException
    {
        ComponentManager cm = this.mocker.getInstance(ComponentManager.class, "context");
        HeightMeasurementHandler handler = new HeightMeasurementHandler();
        when(cm.getInstance(MeasurementHandler.class, "height")).thenReturn(handler);
        Assert.assertEquals(handler, this.mocker.getComponentUnderTest().get("height"));
    }

    @Test
    public void testGetAvailableMeasurementHandlers() throws ComponentLookupException
    {
        ComponentManager cm = this.mocker.getInstance(ComponentManager.class, "context");
        List<MeasurementHandler> toReturn = new ArrayList<>();
        toReturn.add(Mockito.mock(MeasurementHandler.class));
        when(cm.<MeasurementHandler>getInstanceList(MeasurementHandler.class)).thenReturn(toReturn);
        List<MeasurementHandler> response = this.mocker.getComponentUnderTest().getAvailableMeasurementHandlers();
        Assert.assertEquals(toReturn, response);
    }

    @Test
    public void testGetAvailableMeasurementHandlersWithNull() throws ComponentLookupException
    {
        ComponentManager cm = this.mocker.getInstance(ComponentManager.class, "context");
        when(cm.getInstanceList(MeasurementHandler.class)).thenReturn(null);
        List<MeasurementHandler> response = this.mocker.getComponentUnderTest().getAvailableMeasurementHandlers();
        Assert.assertNotNull(response);
        Assert.assertTrue(response.isEmpty());
    }

    @Test
    public void testGetAvailableMeasurementHandlersWithException() throws ComponentLookupException
    {
        ComponentManager cm = this.mocker.getInstance(ComponentManager.class, "context");
        when(cm.getInstanceList(MeasurementHandler.class)).thenThrow(new ComponentLookupException(""));
        List<MeasurementHandler> response = this.mocker.getComponentUnderTest().getAvailableMeasurementHandlers();
        Assert.assertNotNull(response);
        Assert.assertTrue(response.isEmpty());
    }

    @Test
    public void testGetAvailableMeasurementNames() throws ComponentLookupException
    {
        ComponentManager cm = this.mocker.getInstance(ComponentManager.class, "context");
        Map<String, MeasurementHandler> toReturn = new HashMap<>();
        toReturn.put("hand", Mockito.mock(MeasurementHandler.class));
        when(cm.<MeasurementHandler>getInstanceMap(MeasurementHandler.class)).thenReturn(toReturn);
        Set<String> response = this.mocker.getComponentUnderTest().getAvailableMeasurementNames();
        Assert.assertEquals(toReturn.keySet(), response);
    }

    @Test
    public void testGetAvailableMeasurementNamesWithNull() throws ComponentLookupException
    {
        ComponentManager cm = this.mocker.getInstance(ComponentManager.class, "context");
        when(cm.getInstanceMap(MeasurementHandler.class)).thenReturn(null);
        Set<String> response = this.mocker.getComponentUnderTest().getAvailableMeasurementNames();
        Assert.assertNotNull(response);
        Assert.assertTrue(response.isEmpty());
    }

    @Test
    public void testGetAvailableMeasurementNamesWithException() throws ComponentLookupException
    {
        ComponentManager cm = this.mocker.getInstance(ComponentManager.class, "context");
        when(cm.getInstanceMap(MeasurementHandler.class)).thenThrow(new ComponentLookupException(""));
        Set<String> response = this.mocker.getComponentUnderTest().getAvailableMeasurementNames();
        Assert.assertNotNull(response);
        Assert.assertTrue(response.isEmpty());
    }

    @Test
    public void testGetFuzzyValueP() throws ComponentLookupException
    {
        Assert.assertEquals("extreme-below-normal", this.mocker.getComponentUnderTest()
            .getFuzzyValue(Integer.MIN_VALUE));
        Assert.assertEquals("extreme-below-normal", this.mocker.getComponentUnderTest().getFuzzyValue(-1));
        Assert.assertEquals("extreme-below-normal", this.mocker.getComponentUnderTest().getFuzzyValue(0));
        Assert.assertEquals("extreme-below-normal", this.mocker.getComponentUnderTest().getFuzzyValue(1));
        Assert.assertEquals("below-normal", this.mocker.getComponentUnderTest().getFuzzyValue(2));
        Assert.assertEquals("below-normal", this.mocker.getComponentUnderTest().getFuzzyValue(3));
        Assert.assertEquals("normal", this.mocker.getComponentUnderTest().getFuzzyValue(4));
        Assert.assertEquals("normal", this.mocker.getComponentUnderTest().getFuzzyValue(50));
        Assert.assertEquals("normal", this.mocker.getComponentUnderTest().getFuzzyValue(96));
        Assert.assertEquals("above-normal", this.mocker.getComponentUnderTest().getFuzzyValue(97));
        Assert.assertEquals("above-normal", this.mocker.getComponentUnderTest().getFuzzyValue(98));
        Assert.assertEquals("extreme-above-normal", this.mocker.getComponentUnderTest().getFuzzyValue(99));
        Assert.assertEquals("extreme-above-normal", this.mocker.getComponentUnderTest().getFuzzyValue(100));
        Assert.assertEquals("extreme-above-normal", this.mocker.getComponentUnderTest().getFuzzyValue(101));
        Assert.assertEquals("extreme-above-normal", this.mocker.getComponentUnderTest()
            .getFuzzyValue(Integer.MAX_VALUE));
    }

    @Test
    public void testGetFuzzyValueSD() throws ComponentLookupException
    {
        Assert.assertEquals("extreme-below-normal", this.mocker.getComponentUnderTest()
            .getFuzzyValue(-Double.MAX_VALUE));
        Assert.assertEquals("extreme-below-normal", this.mocker.getComponentUnderTest().getFuzzyValue(-3.1));
        Assert.assertEquals("extreme-below-normal", this.mocker.getComponentUnderTest().getFuzzyValue(-3.0));
        Assert.assertEquals("below-normal", this.mocker.getComponentUnderTest().getFuzzyValue(-2.99));
        Assert.assertEquals("below-normal", this.mocker.getComponentUnderTest().getFuzzyValue(-2.0));
        Assert.assertEquals("normal", this.mocker.getComponentUnderTest().getFuzzyValue(-1.99));
        Assert.assertEquals("normal", this.mocker.getComponentUnderTest().getFuzzyValue(0.0));
        Assert.assertEquals("normal", this.mocker.getComponentUnderTest().getFuzzyValue(1.99));
        Assert.assertEquals("above-normal", this.mocker.getComponentUnderTest().getFuzzyValue(2.0));
        Assert.assertEquals("above-normal", this.mocker.getComponentUnderTest().getFuzzyValue(2.99));
        Assert.assertEquals("extreme-above-normal", this.mocker.getComponentUnderTest().getFuzzyValue(3.0));
        Assert.assertEquals("extreme-above-normal", this.mocker.getComponentUnderTest().getFuzzyValue(3.1));
        Assert
            .assertEquals("extreme-above-normal", this.mocker.getComponentUnderTest().getFuzzyValue(Double.MAX_VALUE));
    }
}
