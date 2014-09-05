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
package org.phenotips.data.permissions.internal;

import org.phenotips.data.events.PatientChangingEvent;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Tests for the {@link RightsUpdateEventListener}
 *
 * @version $Id$
 */
public class RightsUpdateEventListenerTest
{
    @Rule
    public final MockitoComponentMockingRule<EventListener> mocker =
    new MockitoComponentMockingRule<EventListener>(RightsUpdateEventListener.class);

    @Mock
    private Event event;

    @Mock
    private XWikiDocument doc;

    @Mock
    private XWikiContext context;

    @Mock
    private BaseObject patientObject;

    @Mock
    private BaseObject manageRightsObject;

    @Mock
    private BaseObject editRightObject;

    @Mock
    private BaseObject viewRightObject;

    @Before
    public void setUp() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void listensForPatientChanges() throws ComponentLookupException
    {
        List<Event> events = this.mocker.getComponentUnderTest().getEvents();
        Assert.assertEquals(1, events.size());
        Assert.assertTrue(events.get(0) instanceof PatientChangingEvent);
    }

    @Test
    public void hasName() throws ComponentLookupException
    {
        String name = this.mocker.getComponentUnderTest().getName();
        Assert.assertTrue(StringUtils.isNotBlank(name));
        Assert.assertFalse("default".equals(name));
    }
}
