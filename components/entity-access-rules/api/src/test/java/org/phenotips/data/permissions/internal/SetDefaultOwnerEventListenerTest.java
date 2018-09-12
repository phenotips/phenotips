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
package org.phenotips.data.permissions.internal;

import org.phenotips.data.events.PatientCreatingEvent;
import org.phenotips.data.permissions.Owner;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
//import org.junit.Before;
import org.junit.Rule;
//import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link SetDefaultOwnerEventListener}.
 *
 * @version $Id$
 */
public class SetDefaultOwnerEventListenerTest
{
    @Rule
    public final MockitoComponentMockingRule<EventListener> mocker =
        new MockitoComponentMockingRule<>(SetDefaultOwnerEventListener.class);

    private XWikiDocument doc = mock(XWikiDocument.class);

    private XWikiContext context = mock(XWikiContext.class);

    private DocumentAccessBridge bridge;

    private DocumentReferenceResolver<String> userOrGroupResolver;

    // @Before
    public void setup() throws ComponentLookupException
    {
        Execution e = this.mocker.getInstance(Execution.class);
        ExecutionContext ec = mock(ExecutionContext.class);
        when(e.getContext()).thenReturn(ec);
        when(ec.getProperty("xwikicontext")).thenReturn(this.context);
    }

    /**
     * Sending an event with a new patient document adds a {@code PhenoTips.OwnerClass} object, retrieved from the
     * configured defaultOwner for the user.
     */
    // @Test
    public void onEventAddsOwnerFromUserDocConfigSettings() throws Exception
    {
        this.bridge = this.mocker.getInstance(DocumentAccessBridge.class);
        this.userOrGroupResolver = this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "userOrGroup");
        DocumentReference userRef = new DocumentReference("xwiki", "XWiki", "jdoe");
        when(this.bridge.getCurrentUserReference()).thenReturn(userRef);
        when(this.bridge.getObjectNumber(any(DocumentReference.class), any(DocumentReference.class), eq("property"),
            eq("defaultOwner"))).thenReturn(0);
        when(this.bridge.getProperty(any(DocumentReference.class), any(DocumentReference.class), eq(0), eq("value")))
            .thenReturn("xwiki:XWiki.jdoe");
        when(this.userOrGroupResolver.resolve(eq("xwiki:XWiki.jdoe"), eq(EntityType.DOCUMENT))).thenReturn(userRef);
        BaseObject ownerObject = mock(BaseObject.class);
        when(this.doc.newXObject(Owner.CLASS_REFERENCE, this.context)).thenReturn(ownerObject);
        this.mocker.getComponentUnderTest().onEvent(new PatientCreatingEvent(), this.doc, null);
        verify(this.doc).newXObject(Owner.CLASS_REFERENCE, this.context);
        verify(ownerObject).setStringValue("owner", "xwiki:XWiki.jdoe");
    }

    /**
     * Sending an event with a new patient document adds a {@code PhenoTips.OwnerClass} object, with the document
     * creator as the owner.
     */
    // @Test
    public void onEventAddsOwnerObject() throws Exception
    {
        BaseObject ownerObject = mock(BaseObject.class);
        when(this.doc.newXObject(Owner.CLASS_REFERENCE, this.context)).thenReturn(ownerObject);
        when(this.doc.getCreatorReference()).thenReturn(new DocumentReference("xwiki", "XWiki", "jdoe"));
        this.mocker.getComponentUnderTest().onEvent(new PatientCreatingEvent(), this.doc, null);
        verify(this.doc).newXObject(Owner.CLASS_REFERENCE, this.context);
        verify(ownerObject).setStringValue("owner", "xwiki:XWiki.jdoe");
    }

    /**
     * Sending an event with a new patient document created by a guest adds a {@code PhenoTips.OwnerClass} object, with
     * an empty owner.
     */
    // @Test
    public void onEventWithGuestCreatorAddsEmptyOwnerObject() throws Exception
    {
        BaseObject ownerObject = mock(BaseObject.class);
        when(this.doc.newXObject(Owner.CLASS_REFERENCE, this.context)).thenReturn(ownerObject);
        when(this.doc.getDocumentReference()).thenReturn(new DocumentReference("xwiki", "data", "P0000001"));
        when(this.doc.getCreatorReference()).thenReturn(null);
        this.mocker.getComponentUnderTest().onEvent(new PatientCreatingEvent(), this.doc, this.context);
        verify(this.doc).newXObject(Owner.CLASS_REFERENCE, this.context);
        verify(ownerObject).setStringValue("owner", "");
    }

    /**
     * Sending an event with a new patient document adds a {@code PhenoTips.OwnerClass} object, with the document
     * creator as the owner.
     */
    // @Test
    public void onEventWithExistingOwnerDoesNothing() throws Exception
    {
        BaseObject ownerObject = mock(BaseObject.class);
        when(this.doc.getXObject(Owner.CLASS_REFERENCE)).thenReturn(ownerObject);
        this.mocker.getComponentUnderTest().onEvent(new PatientCreatingEvent(), this.doc, null);
        verify(this.doc, Mockito.never()).newXObject(Owner.CLASS_REFERENCE, this.context);
        verifyZeroInteractions(ownerObject);
    }

    /**
     * Sending an event with a new patient document created by a guest adds a {@code PhenoTips.OwnerClass} object, with
     * an empty owner.
     */
    // @Test
    public void onEventWithException() throws Exception
    {
        when(this.doc.newXObject(Owner.CLASS_REFERENCE, this.context)).thenThrow(
            new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_READING_DOC,
                "Exception while reading document [xwiki:PhenoTips.OwnerClass]"));
        when(this.doc.getDocumentReference()).thenReturn(new DocumentReference("xwiki", "data", "P0000001"));
        when(this.doc.getCreatorReference()).thenReturn(null);
        this.mocker.getComponentUnderTest().onEvent(new PatientCreatingEvent(), this.doc, this.context);
        verify(this.mocker.getMockedLogger()).error(anyString(), anyString(), anyString(),
            Matchers.any(XWikiException.class));
    }

    /** Non empty name. */
    // @Test
    public void hasName() throws Exception
    {
        Assert.assertTrue(StringUtils.isNotBlank(this.mocker.getComponentUnderTest().getName()));
    }

    /** Only listens to new patients. */
    // @Test
    public void listensForPatientCreation() throws Exception
    {
        List<Event> events = this.mocker.getComponentUnderTest().getEvents();
        Assert.assertEquals(1, events.size());
        Assert.assertTrue(events.get(0) instanceof PatientCreatingEvent);
    }
}
