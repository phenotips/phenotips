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
package org.phenotips.components;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests for the {@link ComponentManagerRegistry} component.
 *
 * @version $Id$
 */
public class ComponentManagerRegistryTest
{
    @Rule
    public final MockitoComponentMockingRule<ComponentManagerRegistry> mocker =
        new MockitoComponentMockingRule<ComponentManagerRegistry>(ComponentManagerRegistry.class);

    @SuppressWarnings("static-access")
    @Test
    public void testGetContextComponentManager() throws ComponentLookupException
    {
        ComponentManager cm = this.mocker.getInstance(ComponentManager.class, "context");
        Assert.assertSame(cm, this.mocker.getComponentUnderTest().getContextComponentManager());
    }

    @Test
    public void testGetName() throws ComponentLookupException
    {
        Assert.assertEquals("cmregistry", this.mocker.getComponentUnderTest().getName());
    }

    @Test
    public void testGetEvents() throws ComponentLookupException
    {
        List<Event> events = this.mocker.getComponentUnderTest().getEvents();
        Assert.assertEquals(1, events.size());
        Assert.assertTrue(events.get(0).matches(new ApplicationStartedEvent()));
    }

    @Test
    public void testOnEventWithNullParameters() throws ComponentLookupException
    {
        // Just call the method and see that no NPE is thrown
        this.mocker.getComponentUnderTest().onEvent(null, null, null);
    }
}
