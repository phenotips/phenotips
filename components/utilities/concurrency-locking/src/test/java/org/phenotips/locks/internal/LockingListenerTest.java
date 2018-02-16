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
package org.phenotips.locks.internal;

import org.phenotips.locks.DocumentLockManager;

import org.xwiki.bridge.event.ActionExecutedEvent;
import org.xwiki.bridge.event.ActionExecutingEvent;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.doc.XWikiDocument;
import net.jcip.annotations.NotThreadSafe;

/**
 * Tests for the {@link LockingListener} component.
 *
 * @version $Id$
 */
@NotThreadSafe
public class LockingListenerTest
{
    @Rule
    public final MockitoComponentMockingRule<EventListener> mocker =
        new MockitoComponentMockingRule<>(LockingListener.class);

    private DocumentLockManager lockManager;

    private EventListener listener;

    private DocumentReference docRef = new DocumentReference("xwiki", "data", "P0000001");

    @Mock
    private XWikiDocument doc;

    @Before
    public void setup() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        this.lockManager = this.mocker.getInstance(DocumentLockManager.class);
        this.listener = this.mocker.getComponentUnderTest();

        Mockito.when(this.doc.getDocumentReference()).thenReturn(this.docRef);
    }

    @Test
    public void hasRightName() throws ComponentLookupException
    {
        Assert.assertEquals("concurrency-locking", this.mocker.getComponentUnderTest().getName());
    }

    @Test
    public void listensToActionEvents()
    {
        List<Event> events = this.listener.getEvents();
        Assert.assertEquals(2, events.size());
        Assert.assertTrue(events.get(0).matches(new ActionExecutingEvent()));
        Assert.assertTrue(events.get(1).matches(new ActionExecutedEvent()));
    }

    @Test
    public void noActionOnEventWithNullParameters()
    {
        // Just call the method and see that no NPE is thrown
        this.listener.onEvent(null, null, null);
        this.listener.onEvent(new ActionExecutedEvent("save"), null, null);
        this.listener.onEvent(null, this.doc, null);
        Mockito.verifyZeroInteractions(this.lockManager);
    }

    @Test
    public void noActionOnEventWithNonLockableAction()
    {
        this.listener.onEvent(new ActionExecutingEvent("download"), this.doc, null);
        this.listener.onEvent(new ActionExecutedEvent("download"), this.doc, null);
        Mockito.verifyZeroInteractions(this.doc);
        Mockito.verifyZeroInteractions(this.lockManager);
    }

    @Test
    public void noActionOnOtherEvents()
    {
        this.listener.onEvent(new ApplicationStartedEvent(), this.doc, null);
        Mockito.verifyZeroInteractions(this.doc);
        Mockito.verifyZeroInteractions(this.lockManager);
    }

    @Test
    public void locksOnActionExecutingEvent()
    {
        this.listener.onEvent(new ActionExecutingEvent("save"), this.doc, null);
        Mockito.verify(this.lockManager).lock(this.docRef);
    }

    @Test
    public void unlocksOnActionExecutedEvent()
    {
        this.listener.onEvent(new ActionExecutedEvent("save"), this.doc, null);
        Mockito.verify(this.lockManager).unlock(this.docRef);
    }
}
