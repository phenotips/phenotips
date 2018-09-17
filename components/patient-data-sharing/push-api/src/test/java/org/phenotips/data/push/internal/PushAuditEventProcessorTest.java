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
package org.phenotips.data.push.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.push.PushPatientService;
import org.phenotips.security.audit.AuditEvent;
import org.phenotips.security.audit.spi.AuditEventProcessor;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;

import java.util.Calendar;

import javax.inject.Provider;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;

import static org.mockito.Mockito.when;

/**
 * Tests for the {@link PushAuditEventProcessor} {@link AuditEventProcessor} component.
 *
 * @version $Id$
 */
public class PushAuditEventProcessorTest
{
    @Rule
    public final MockitoComponentMockingRule<AuditEventProcessor> mocker =
        new MockitoComponentMockingRule<>(PushAuditEventProcessor.class);

    @Mock
    private User user;

    @Mock
    private XWikiRequest request;

    private DocumentReference patientDoc = new DocumentReference("wiki", "data", "P0000001");

    private DocumentReference pushServiceDoc = new DocumentReference("wiki", "PhenoTips", "PushPatientService");

    @Before
    public void setup() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);

        XWikiContext xcontext = this.mocker.<Provider<XWikiContext>>getInstance(XWikiContext.TYPE_PROVIDER).get();
        when(xcontext.getRequest()).thenReturn(this.request);
        when(this.request.getParameter("do")).thenReturn("push");
        when(this.request.getParameter("patientid")).thenReturn("P0000001");
        when(this.request.getParameter("serverid")).thenReturn("targetServer");
        when(this.request.getParameter("groupname")).thenReturn("targetGroup");
        when(this.request.getParameter("fields")).thenReturn("name,phenotype,disorder");
        when(this.request.getParameter("usr")).thenReturn("targetUser");

        PushPatientService pushService = this.mocker.getInstance(PushPatientService.class);
        when(pushService.getRemoteUsername("targetServer")).thenReturn("presetTargetUser");

        DocumentReferenceResolver<String> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "currentmixed");
        when(resolver.resolve("P0000001", Patient.DEFAULT_DATA_SPACE)).thenReturn(this.patientDoc);
    }

    @Test
    public void returnsInputIfNotOnPushPatientService() throws ComponentLookupException
    {
        final AuditEvent e = new AuditEvent(this.user, "ip", "get", null, this.patientDoc, Calendar.getInstance());
        Assert.assertSame(e, this.mocker.getComponentUnderTest().process(e));
    }

    @Test
    public void returnsInputIfNotOnTheRightPushPatientService() throws ComponentLookupException
    {
        final AuditEvent e = new AuditEvent(this.user, "ip", "get", null,
            new DocumentReference("wiki", "Sandbox", "PushPatientService"), Calendar.getInstance());
        Assert.assertSame(e, this.mocker.getComponentUnderTest().process(e));
    }

    @Test
    public void returnsInputIfNotAPushRequest() throws ComponentLookupException
    {
        final AuditEvent e =
            new AuditEvent(this.user, "ip", "get", null, this.pushServiceDoc, Calendar.getInstance());
        when(this.request.getParameter("do")).thenReturn("getuser");
        Assert.assertSame(e, this.mocker.getComponentUnderTest().process(e));
    }

    @Test
    public void returnsPushEventOnPatientWithExtraInformation() throws ComponentLookupException
    {
        final AuditEvent e = new AuditEvent(this.user, "ip", "get", null, this.pushServiceDoc, Calendar.getInstance());

        final AuditEvent result = this.mocker.getComponentUnderTest().process(e);

        Assert.assertSame(e.getUser(), result.getUser());
        Assert.assertSame(e.getIp(), result.getIp());
        Assert.assertSame("push", result.getAction());
        Assert.assertSame(this.patientDoc, result.getEntity());
        Assert.assertSame(e.getTime(), result.getTime());
        JSONObject extraInfo = new JSONObject(result.getExtraInformation());
        Assert.assertEquals("targetServer", extraInfo.getString("server"));
        Assert.assertEquals("targetGroup", extraInfo.getString("group"));
        Assert.assertEquals("name,phenotype,disorder", extraInfo.getString("fields"));
        Assert.assertEquals("targetUser", extraInfo.getString("remoteUser"));
    }

    @Test
    public void includesStoredRemoteUsernameWhenNotPresentInRequest() throws ComponentLookupException
    {
        final AuditEvent e = new AuditEvent(this.user, "ip", "get", null, this.pushServiceDoc, Calendar.getInstance());
        when(this.request.getParameter("usr")).thenReturn(null);

        final AuditEvent result = this.mocker.getComponentUnderTest().process(e);

        JSONObject extraInfo = new JSONObject(result.getExtraInformation());
        Assert.assertEquals("presetTargetUser", extraInfo.getString("remoteUser"));
    }

    @Test
    public void returnsPushEventOnPatientWithoutMissingExtraInformation() throws ComponentLookupException
    {
        final AuditEvent e = new AuditEvent(this.user, "ip", "get", null, this.pushServiceDoc, Calendar.getInstance());
        when(this.request.getParameter("groupname")).thenReturn(null);
        when(this.request.getParameter("fields")).thenReturn(null);

        final AuditEvent result = this.mocker.getComponentUnderTest().process(e);

        Assert.assertSame(e.getUser(), result.getUser());
        Assert.assertSame(e.getIp(), result.getIp());
        Assert.assertSame("push", result.getAction());
        Assert.assertSame(this.patientDoc, result.getEntity());
        Assert.assertSame(e.getTime(), result.getTime());
        JSONObject extraInfo = new JSONObject(result.getExtraInformation());
        Assert.assertEquals("targetServer", extraInfo.getString("server"));
        Assert.assertFalse(extraInfo.has("group"));
        Assert.assertFalse(extraInfo.has("fields"));
    }

    @Test
    public void replacesOriginalInformation() throws ComponentLookupException
    {
        final AuditEvent e =
            new AuditEvent(this.user, "ip", "get", "originalInformation", this.pushServiceDoc, Calendar.getInstance());

        final AuditEvent result = this.mocker.getComponentUnderTest().process(e);

        Assert.assertSame(e.getUser(), result.getUser());
        Assert.assertSame(e.getIp(), result.getIp());
        Assert.assertSame("push", result.getAction());
        Assert.assertSame(this.patientDoc, result.getEntity());
        Assert.assertSame(e.getTime(), result.getTime());
        JSONObject extraInfo = new JSONObject(result.getExtraInformation());
        Assert.assertEquals("targetServer", extraInfo.getString("server"));
        Assert.assertEquals("targetGroup", extraInfo.getString("group"));
        Assert.assertEquals("name,phenotype,disorder", extraInfo.getString("fields"));
        Assert.assertEquals("targetUser", extraInfo.getString("remoteUser"));
    }
}
