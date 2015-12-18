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
package org.phenotips.groups.internal.listeners;

import org.phenotips.groups.Group;

import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentDeletingEvent;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.query.QueryException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.List;

import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GroupCleanupEventListenerTest
{
    @Rule
    public final MockitoComponentMockingRule<EventListener> mocker = new MockitoComponentMockingRule<EventListener>(
        GroupCleanupEventListener.class);

    @Test
    public void getName() throws ComponentLookupException, QueryException
    {
        Assert.assertTrue(StringUtils.isNotEmpty(this.mocker.getComponentUnderTest().getName()));
    }

    @Test
    public void getEvents() throws ComponentLookupException, QueryException
    {
        List<Event> events = this.mocker.getComponentUnderTest().getEvents();
        Assert.assertFalse(events.isEmpty());
        Assert.assertEquals(1, events.size());
        Assert.assertTrue(events.get(0) instanceof DocumentDeletedEvent);
    }

    @Test
    public void onEvent() throws ComponentLookupException, XWikiException
    {
        Utils.setComponentManager(this.mocker);

        DocumentReference docReference = new DocumentReference("xwiki", "Groups", "Group1");
        Provider<XWikiContext> contextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        XWikiContext context = mock(XWikiContext.class);
        when(contextProvider.get()).thenReturn(context);
        XWiki xwiki = mock(XWiki.class);
        when(context.getWiki()).thenReturn(xwiki);

        XWikiDocument doc = mock(XWikiDocument.class);
        when(doc.getDocumentReference()).thenReturn(docReference);
        when(doc.getXObject(Group.CLASS_REFERENCE)).thenReturn(mock(BaseObject.class));
        XWikiDocument newDoc = mock(XWikiDocument.class);
        when(newDoc.getOriginalDocument()).thenReturn(doc);

        DocumentReference adminsDocReference = new DocumentReference("xwiki", "Groups", "Group1 Administrators");
        XWikiDocument adminsDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(adminsDocReference), eq(context))).thenReturn(adminsDoc);

        this.mocker.getComponentUnderTest().onEvent(new DocumentDeletingEvent(docReference), newDoc, context);

        Mockito.verify(xwiki).getDocument(adminsDocReference, context);
        Mockito.verify(xwiki).deleteDocument(adminsDoc, context);
        Mockito.verifyNoMoreInteractions(xwiki);
    }

    @Test
    public void onEventWithNoOriginalDocument() throws ComponentLookupException, XWikiException
    {
        Utils.setComponentManager(this.mocker);

        DocumentReference docReference = new DocumentReference("xwiki", "Groups", "Group1");
        Provider<XWikiContext> contextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        XWikiContext context = mock(XWikiContext.class);
        when(contextProvider.get()).thenReturn(context);
        XWikiDocument doc = mock(XWikiDocument.class);
        when(doc.getOriginalDocument()).thenReturn(null);

        this.mocker.getComponentUnderTest().onEvent(new DocumentDeletingEvent(docReference), doc, context);

        Mockito.verifyZeroInteractions(context);
        Mockito.verify(doc).getOriginalDocument();
        Mockito.verifyNoMoreInteractions(doc);
    }

    @Test
    public void onEventWithNonGroup() throws ComponentLookupException, XWikiException
    {
        Utils.setComponentManager(this.mocker);

        DocumentReference docReference = new DocumentReference("xwiki", "Groups", "Group1");
        Provider<XWikiContext> contextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        XWikiContext context = mock(XWikiContext.class);
        when(contextProvider.get()).thenReturn(context);
        XWikiDocument doc = mock(XWikiDocument.class);
        when(doc.getXObject(Group.CLASS_REFERENCE)).thenReturn(null);
        XWikiDocument newDoc = mock(XWikiDocument.class);
        when(newDoc.getOriginalDocument()).thenReturn(doc);

        this.mocker.getComponentUnderTest().onEvent(new DocumentDeletingEvent(docReference), newDoc, context);

        Mockito.verifyZeroInteractions(context);
        Mockito.verify(doc).getXObject(Group.CLASS_REFERENCE);
        Mockito.verifyNoMoreInteractions(doc);
    }

    @Test
    public void onEventWithExceptions() throws ComponentLookupException, XWikiException
    {
        Utils.setComponentManager(this.mocker);

        DocumentReference docReference = new DocumentReference("xwiki", "Groups", "Group1");
        Provider<XWikiContext> contextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        XWikiContext context = mock(XWikiContext.class);
        when(contextProvider.get()).thenReturn(context);
        XWiki xwiki = mock(XWiki.class);
        when(context.getWiki()).thenReturn(xwiki);

        XWikiDocument doc = mock(XWikiDocument.class);
        when(doc.getDocumentReference()).thenReturn(docReference);
        when(doc.getXObject(Group.CLASS_REFERENCE)).thenReturn(mock(BaseObject.class));
        XWikiDocument newDoc = mock(XWikiDocument.class);
        when(newDoc.getOriginalDocument()).thenReturn(doc);

        DocumentReference adminsDocReference = new DocumentReference("xwiki", "Groups", "Group1 Administrators");
        when(xwiki.getDocument(eq(adminsDocReference), eq(context))).thenThrow(new XWikiException(0, 0, "DB Error"));

        this.mocker.getComponentUnderTest().onEvent(new DocumentDeletingEvent(docReference), newDoc, context);

        Mockito.verify(doc).getXObject(Group.CLASS_REFERENCE);
        Mockito.verify(doc).getDocumentReference();
        Mockito.verifyNoMoreInteractions(doc);
    }
}
