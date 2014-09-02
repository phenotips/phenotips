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

import org.phenotips.data.Patient;
import org.phenotips.data.permissions.Owner;

import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link OwnerUpdateEventListener}
 *
 * @version $Id$
 */
public class OwnerUpdateEventListenerTest
{
    @Rule
    public final MockitoComponentMockingRule<EventListener> mocker =
        new MockitoComponentMockingRule<EventListener>(OwnerUpdateEventListener.class);

    private XWikiDocument doc = mock(XWikiDocument.class);

    private XWikiContext context = mock(XWikiContext.class);

    /** Sending an event with a non-patient document doesn't alter the document. */
    @Test
    public void onEventWithNonPatient() throws Exception
    {
        when(this.doc.getXObject(Patient.CLASS_REFERENCE)).thenReturn(null);
        this.mocker.getComponentUnderTest().onEvent(new DocumentCreatingEvent(), this.doc, this.context);
        verify(this.doc, never()).newXObject(Owner.CLASS_REFERENCE, this.context);
    }

    /** Sending an event with the template patient document doesn't alter the document. */
    @Test
    public void onEventWithTemplatePatient() throws Exception
    {
        when(this.doc.getXObject(Patient.CLASS_REFERENCE)).thenReturn(mock(BaseObject.class));
        when(this.doc.getDocumentReference())
            .thenReturn(new DocumentReference("xwiki", "PhenoTips", "PatientTemplate"));
        this.mocker.getComponentUnderTest().onEvent(new DocumentCreatingEvent(), this.doc, this.context);
        verify(this.doc, never()).newXObject(Owner.CLASS_REFERENCE, this.context);
    }

    /**
     * Sending an event with a new patient document adds a {@code PhenoTips.OwnerClass} object, with the document
     * creator as the owner.
     */
    @Test
    public void onEventWithNormalPatient() throws Exception
    {
        BaseObject patientObject = mock(BaseObject.class);
        BaseObject ownerObject = mock(BaseObject.class);
        when(this.doc.getXObject(Patient.CLASS_REFERENCE)).thenReturn(patientObject);
        when(this.doc.newXObject(Owner.CLASS_REFERENCE, this.context)).thenReturn(ownerObject);
        when(this.doc.getDocumentReference()).thenReturn(new DocumentReference("xwiki", "data", "P0000001"));
        when(this.doc.getCreatorReference()).thenReturn(new DocumentReference("xwiki", "XWiki", "jdoe"));
        this.mocker.getComponentUnderTest().onEvent(new DocumentCreatingEvent(), this.doc, this.context);
        verify(this.doc).newXObject(Owner.CLASS_REFERENCE, this.context);
        verify(ownerObject).setStringValue("owner", "xwiki:XWiki.jdoe");
    }

    /**
     * Sending an event with a new patient document created by a guest adds a {@code PhenoTips.OwnerClass} object, with
     * an empty owner.
     */
    @Test
    public void onEventWithNormalPatientAndGuestCreator() throws Exception
    {
        BaseObject patientObject = mock(BaseObject.class);
        BaseObject ownerObject = mock(BaseObject.class);
        when(this.doc.getXObject(Patient.CLASS_REFERENCE)).thenReturn(patientObject);
        when(this.doc.newXObject(Owner.CLASS_REFERENCE, this.context)).thenReturn(ownerObject);
        when(this.doc.getDocumentReference()).thenReturn(new DocumentReference("xwiki", "data", "P0000001"));
        when(this.doc.getCreatorReference()).thenReturn(null);
        this.mocker.getComponentUnderTest().onEvent(new DocumentCreatingEvent(), this.doc, this.context);
        verify(this.doc).newXObject(Owner.CLASS_REFERENCE, this.context);
        verify(ownerObject).setStringValue("owner", "");
    }

    /**
     * Sending an event with a new patient document created by a guest adds a {@code PhenoTips.OwnerClass} object, with
     * an empty owner.
     */
    @Test
    public void onEventWithException() throws Exception
    {
        BaseObject patientObject = mock(BaseObject.class);
        when(this.doc.getXObject(Patient.CLASS_REFERENCE)).thenReturn(patientObject);
        when(this.doc.newXObject(Owner.CLASS_REFERENCE, this.context)).thenThrow(
            new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_READING_DOC,
                "Exception while reading document [xwiki:PhenoTips.OwnerClass]"));
        when(this.doc.getDocumentReference()).thenReturn(new DocumentReference("xwiki", "data", "P0000001"));
        when(this.doc.getCreatorReference()).thenReturn(null);
        this.mocker.getComponentUnderTest().onEvent(new DocumentCreatingEvent(), this.doc, this.context);
        verify(this.mocker.getMockedLogger()).error(anyString(), anyString(), anyString(),
            Matchers.any(XWikiException.class));
    }

    /** Non empty name. */
    @Test
    public void getName() throws Exception
    {
        Assert.assertTrue(StringUtils.isNotBlank(this.mocker.getComponentUnderTest().getName()));
    }

    /** Only listes to new documents. */
    @Test
    public void getEvents() throws Exception
    {
        List<Event> events = this.mocker.getComponentUnderTest().getEvents();
        Assert.assertEquals(1, events.size());
        Assert.assertTrue(events.get(0) instanceof DocumentCreatingEvent);
    }
}
