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
import org.phenotips.data.PatientRepository;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsManager;

import org.xwiki.bridge.event.ActionExecutingEvent;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
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
import com.xpn.xwiki.web.XWikiServletRequest;

import static org.mockito.Mockito.when;

/**
 * Tests for the {@link VCFAccessRestrictionEventListener}
 *
 * @version $Id$
 */
public class VCFAccessRestrictionEventListenerTest
{
    @Rule
    public final MockitoComponentMockingRule<EventListener> mocker =
    new MockitoComponentMockingRule<EventListener>(VCFAccessRestrictionEventListener.class);

    @Mock
    private Event event;

    @Mock
    private XWikiDocument doc;

    private DocumentReference docRef = new DocumentReference("xwiki", "data", "P0000001");

    @Mock
    private XWikiContext context;

    @Mock
    private XWikiServletRequest request;

    private PatientRepository patients;

    @Mock
    private Patient patient;

    private PermissionsManager permissions;

    private AccessLevel edit;

    @Mock
    private PatientAccess access;

    @Before
    public void setUp() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        this.patients = this.mocker.getInstance(PatientRepository.class);
        when(this.patients.getPatientById("xwiki:data.P0000001")).thenReturn(this.patient);
        this.permissions = this.mocker.getInstance(PermissionsManager.class);
        when(this.permissions.getPatientAccess(this.patient)).thenReturn(this.access);
        this.edit = this.mocker.getInstance(AccessLevel.class, "edit");

        when(this.context.getRequest()).thenReturn(this.request);
        when(this.context.getDoc()).thenReturn(this.doc);
        when(this.doc.getDocumentReference()).thenReturn(this.docRef);
    }

    @Test
    public void listensForDownloadActions() throws ComponentLookupException
    {
        List<Event> events = this.mocker.getComponentUnderTest().getEvents();
        Assert.assertTrue(events.contains(new ActionExecutingEvent("download")));
        Assert.assertTrue(events.contains(new ActionExecutingEvent("downloadrev")));
    }

    @Test
    public void hasName() throws ComponentLookupException
    {
        String name = this.mocker.getComponentUnderTest().getName();
        Assert.assertTrue(StringUtils.isNotBlank(name));
        Assert.assertFalse("default".equals(name));
    }

    @Test
    public void forbidsDownloadingVcfAttachmentsIfAccessIsLowerThanEdit() throws ComponentLookupException
    {
        ActionExecutingEvent event = new ActionExecutingEvent("download");
        when(this.request.getRequestURI()).thenReturn("/bin/download/data/P0000001/file.vcf");
        when(this.access.hasAccessLevel(this.edit)).thenReturn(false);
        this.mocker.getComponentUnderTest().onEvent(event, this.doc, this.context);
        Assert.assertTrue(event.isCanceled());
    }

    @Test
    public void allowsDownloadingVcfAttachmentsWhenHasEditAccess() throws ComponentLookupException
    {
        ActionExecutingEvent event = new ActionExecutingEvent("download");
        when(this.request.getRequestURI()).thenReturn("/bin/download/data/P0000001/file.vcf");
        when(this.access.hasAccessLevel(this.edit)).thenReturn(true);
        this.mocker.getComponentUnderTest().onEvent(event, this.doc, this.context);
        Assert.assertFalse(event.isCanceled());
    }

    @Test
    public void alwaysAllowsDownloadingNonVcfAttachments() throws ComponentLookupException
    {
        ActionExecutingEvent event = new ActionExecutingEvent("download");
        when(this.request.getRequestURI()).thenReturn("/bin/download/data/P0000001/file.png");
        when(this.access.hasAccessLevel(this.edit)).thenReturn(false);
        this.mocker.getComponentUnderTest().onEvent(event, this.doc, this.context);
        Assert.assertFalse(event.isCanceled());
    }

    @Test
    public void ignoresExtraFileparts() throws ComponentLookupException
    {
        ActionExecutingEvent event = new ActionExecutingEvent("download");
        when(this.request.getRequestURI()).thenReturn("/bin/download/data/P0000001/file.vcf/hacked");
        when(this.access.hasAccessLevel(this.edit)).thenReturn(false);
        this.mocker.getComponentUnderTest().onEvent(event, this.doc, this.context);
        Assert.assertTrue(event.isCanceled());
    }
}
