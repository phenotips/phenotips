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
import org.phenotips.security.audit.AuditStore;
import org.phenotips.security.audit.spi.AuditEventProcessor;

import org.xwiki.bridge.event.ActionExecutedEvent;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Arrays;
import java.util.Calendar;

import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiRequest;

/**
 * Tests for the {@link AuditEventListener} component.
 *
 * @version $Id$
 */
public class AuditEventListenerTest
{
    @Rule
    public final MockitoComponentMockingRule<EventListener> mocker =
        new MockitoComponentMockingRule<>(AuditEventListener.class);

    private DocumentReference doc = new DocumentReference("wiki", "Space", "Page");

    @Mock
    private XWikiDocument xdoc;

    @Mock
    private User user;

    private AuditStore store;

    @Mock
    private XWikiRequest request;

    private XWikiContext context;

    @Mock
    private AuditEventProcessor processor;

    @Before
    public void setup() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        Mockito.when(this.xdoc.getDocumentReference()).thenReturn(this.doc);

        this.store = this.mocker.getInstance(AuditStore.class);

        UserManager users = this.mocker.getInstance(UserManager.class);
        Mockito.when(users.getCurrentUser()).thenReturn(this.user);

        this.context = this.mocker.<Provider<XWikiContext>>getInstance(XWikiContext.TYPE_PROVIDER).get();
        Mockito.when(this.context.getRequest()).thenReturn(this.request);
        Mockito.when(this.request.getRemoteAddr()).thenReturn("ip");
    }

    @Test
    public void hasProperName() throws ComponentLookupException
    {
        Assert.assertEquals("auditor", this.mocker.getComponentUnderTest().getName());
    }

    @Test
    public void listensToActionExecutedEvent() throws ComponentLookupException
    {
        Assert.assertEquals(Arrays.asList(new ActionExecutedEvent()), this.mocker.getComponentUnderTest().getEvents());
    }

    @Test
    public void createsAuditEvents() throws ComponentLookupException
    {
        this.mocker.getComponentUnderTest().onEvent(new ActionExecutedEvent("action"), this.xdoc, null);
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        Mockito.verify(this.store).store(captor.capture());
        AuditEvent e = captor.getValue();
        Assert.assertSame(this.user, e.getUser());
        Assert.assertEquals("ip", e.getIp());
        Assert.assertEquals("action", e.getAction());
        Assert.assertNull(e.getExtraInformation());
        Assert.assertEquals(this.doc, e.getEntity());
        Assert.assertTrue(Calendar.getInstance().getTimeInMillis() - e.getTime().getTimeInMillis() < 1000);
    }

    @Test
    public void createsAuditEventsWithNoRequest() throws ComponentLookupException
    {
        Mockito.when(this.context.getRequest()).thenReturn(null);
        this.mocker.getComponentUnderTest().onEvent(new ActionExecutedEvent("action"), this.xdoc, null);
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        Mockito.verify(this.store).store(captor.capture());
        AuditEvent e = captor.getValue();
        Assert.assertNull(e.getIp());
    }

    @Test
    public void invokesProcessors() throws Exception
    {
        this.mocker.registerComponent(AuditEventProcessor.class, this.processor);
        AuditEvent e = new AuditEvent();
        Mockito.when(this.processor.process(Matchers.any())).thenReturn(e);

        this.mocker.getComponentUnderTest().onEvent(new ActionExecutedEvent("action"), this.xdoc, null);
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        Mockito.verify(this.store).store(captor.capture());
        Assert.assertSame(e, captor.getValue());
    }
}
