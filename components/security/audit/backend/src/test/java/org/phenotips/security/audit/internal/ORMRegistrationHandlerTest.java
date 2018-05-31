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
package org.phenotips.security.audit.internal;

import org.phenotips.security.audit.AuditEvent;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.List;

import org.hibernate.cfg.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;

/**
 * Tests for the {@link ORMRegistrationHandler} component.
 *
 * @version $Id$
 */
public class ORMRegistrationHandlerTest
{
    @Rule
    public final MockitoComponentMockingRule<EventListener> mocker =
        new MockitoComponentMockingRule<>(ORMRegistrationHandler.class);

    @Mock
    private Configuration config;

    @Before
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        HibernateSessionFactory hsf = this.mocker.getInstance(HibernateSessionFactory.class);
        Mockito.when(hsf.getConfiguration()).thenReturn(this.config);
    }

    @Test
    public void hasProperName() throws ComponentLookupException
    {
        Assert.assertEquals("phenotips-audit-orm-registration", this.mocker.getComponentUnderTest().getName());
    }

    @Test
    public void listensToApplicationStartedEvent() throws ComponentLookupException
    {
        List<Event> events = this.mocker.getComponentUnderTest().getEvents();
        Assert.assertEquals(1, events.size());
        Event e = events.get(0);
        Assert.assertTrue(e.matches(new ApplicationStartedEvent()));
    }

    @Test
    public void registersAuditEventClass() throws ComponentLookupException
    {
        this.mocker.getComponentUnderTest().onEvent(new ApplicationStartedEvent(), null, null);
        Mockito.verify(this.config).addAnnotatedClass(AuditEvent.class);
    }
}
