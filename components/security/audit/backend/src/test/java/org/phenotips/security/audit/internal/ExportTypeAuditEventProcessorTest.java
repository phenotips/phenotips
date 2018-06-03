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
import org.phenotips.security.audit.spi.AuditEventProcessor;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;

import java.util.Calendar;

import javax.inject.Provider;

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
 * Tests for the {@link ExportTypeAuditEventProcessor} {@link AuditEventProcessor} component.
 *
 * @version $Id$
 */
public class ExportTypeAuditEventProcessorTest
{
    @Rule
    public final MockitoComponentMockingRule<AuditEventProcessor> mocker =
        new MockitoComponentMockingRule<>(ExportTypeAuditEventProcessor.class);

    @Mock
    private User user;

    @Mock
    private XWikiRequest request;

    private DocumentReference doc = new DocumentReference("wiki", "Space", "Page");

    @Before
    public void setup() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);

        XWikiContext xcontext = this.mocker.<Provider<XWikiContext>>getInstance(XWikiContext.TYPE_PROVIDER).get();
        when(xcontext.getRequest()).thenReturn(this.request);
        when(this.request.getParameter("format")).thenReturn("pdf");
    }

    @Test
    public void returnsInputIfNotExport() throws ComponentLookupException
    {
        final AuditEvent e = new AuditEvent(this.user, "ip", "view", null, this.doc, Calendar.getInstance());
        Assert.assertSame(e, this.mocker.getComponentUnderTest().process(e));
    }

    @Test
    public void returnsInputIfExtraInformationAlreadySet() throws ComponentLookupException
    {
        final AuditEvent e = new AuditEvent(this.user, "ip", "export", "json", this.doc, Calendar.getInstance());
        Assert.assertSame(e, this.mocker.getComponentUnderTest().process(e));
    }

    @Test
    public void addsModeIfExport() throws ComponentLookupException
    {
        final AuditEvent e = new AuditEvent(this.user, "ip", "export", null, this.doc, Calendar.getInstance());
        final AuditEvent result = this.mocker.getComponentUnderTest().process(e);
        Assert.assertSame(e.getUser(), result.getUser());
        Assert.assertSame(e.getIp(), result.getIp());
        Assert.assertSame(e.getAction(), result.getAction());
        Assert.assertEquals("pdf", result.getExtraInformation());
        Assert.assertSame(e.getEntity(), result.getEntity());
        Assert.assertSame(e.getTime(), result.getTime());
    }
}
