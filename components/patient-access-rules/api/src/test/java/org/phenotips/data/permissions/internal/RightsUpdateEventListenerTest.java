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

import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    public void listensToDocumentCreation() throws ComponentLookupException
    {
        boolean found = false;
        for (Event e : this.mocker.getComponentUnderTest().getEvents()) {
            if (e instanceof DocumentCreatingEvent) {
                found = true;
                break;
            }
        }
        Assert.assertTrue(found);
    }

    @Test
    public void listensToDocumentUpdates() throws ComponentLookupException
    {
        boolean found = false;
        for (Event e : this.mocker.getComponentUnderTest().getEvents()) {
            if (e instanceof DocumentUpdatingEvent) {
                found = true;
                break;
            }
        }
        Assert.assertTrue(found);
    }

    @Test
    public void hasName() throws ComponentLookupException
    {
        String name = this.mocker.getComponentUnderTest().getName();
        Assert.assertTrue(StringUtils.isNotBlank(name));
        Assert.assertFalse("default".equals(name));
    }

    @Test
    public void ignoresNonPatients() throws ComponentLookupException, XWikiException
    {
        when(this.doc.getXObject(Patient.CLASS_REFERENCE)).thenReturn(null);
        this.mocker.getComponentUnderTest().onEvent(this.event, this.doc, this.context);
        verify(this.doc, never()).getXObjects(any(EntityReference.class));
        verify(this.doc, never()).newXObject(any(EntityReference.class), any(XWikiContext.class));
    }

    @Test
    public void ignoresTemplatePatient() throws ComponentLookupException, XWikiException
    {
        when(this.doc.getXObject(Patient.CLASS_REFERENCE)).thenReturn(this.patientObject);
        when(this.doc.getDocumentReference()).thenReturn(new DocumentReference("x", "PhenoTips", "PatientTemplate"));
        this.mocker.getComponentUnderTest().onEvent(this.event, this.doc, this.context);
        verify(this.doc, never()).getXObjects(any(EntityReference.class));
        verify(this.doc, never()).newXObject(any(EntityReference.class), any(XWikiContext.class));
    }
}
