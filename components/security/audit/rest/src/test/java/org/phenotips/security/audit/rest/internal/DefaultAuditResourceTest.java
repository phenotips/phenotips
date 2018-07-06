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
package org.phenotips.security.audit.rest.internal;

import org.phenotips.Constants;
import org.phenotips.security.audit.AuditEvent;
import org.phenotips.security.audit.AuditStore;
import org.phenotips.security.audit.rest.AuditResource;
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWikiContext;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link DefaultAuditResource} component.
 *
 * @version $Id$
 */
public class DefaultAuditResourceTest
{
    @Rule
    public final MockitoComponentMockingRule<AuditResource> mocker =
        new MockitoComponentMockingRule<>(DefaultAuditResource.class);

    @Mock
    private User user;

    @Mock
    private AuditEvent event;

    private DocumentReference doc = new DocumentReference("wiki", "Space", "Page");

    private DocumentReference xwikiPreferences = new DocumentReference("wiki", "XWiki", "XWikiPreferences");

    private AuditStore store;

    private AuthorizationService auth;

    private UserManager users;

    private AuditResource resource;

    private List<AuditEvent> events;

    @Before
    public void setup() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);

        // This is needed for the XWikiResource initialization
        Execution execution = mock(Execution.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);
        ComponentManager componentManager = this.mocker.getInstance(ComponentManager.class, "context");
        when(componentManager.getInstance(Execution.class)).thenReturn(execution);
        doReturn(executionContext).when(execution).getContext();
        doReturn(mock(XWikiContext.class)).when(executionContext).getProperty("xwikicontext");

        this.resource = this.mocker.getComponentUnderTest();
        this.store = this.mocker.getInstance(AuditStore.class);
        this.auth = this.mocker.getInstance(AuthorizationService.class);

        this.events = Arrays.asList(this.event);

        this.users = this.mocker.getInstance(UserManager.class);
        when(this.users.getCurrentUser()).thenReturn(this.user);
        when(this.users.getUser("user")).thenReturn(this.user);
        when(this.user.getId()).thenReturn("wiki:XWiki.user");

        DocumentReferenceResolver<EntityReference> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "currentmixed");
        when(resolver.resolve(Constants.XWIKI_SPACE_REFERENCE)).thenReturn(this.xwikiPreferences);
        DocumentReferenceResolver<String> resolverd =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        when(resolverd.resolve("Space.Page")).thenReturn(this.doc);
    }

    @Test
    public void listEventsRejectsRequestForUserWithoutAdminAccess()
    {
        when(this.auth.hasAccess(this.user, Right.ADMIN, this.doc)).thenReturn(false);
        Response response = this.resource.listEvents(0, 25, "get", this.user.getId(), "ip", "", "", "");
        Mockito.verifyZeroInteractions(this.store);
        Assert.assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    public void listEventsForwardsCalls()
    {
        Calendar from = Calendar.getInstance();
        from.clear();
        from.set(2010, 0, 1, 0, 0, 0);
        Calendar to = Calendar.getInstance();
        to.clear();
        to.set(2011, 0, 1, 0, 0, 0);
        AuditEvent template = new AuditEvent(this.user, "ip", "action", null, this.doc, null);
        when(this.store.getEvents(template, from, to, 20, 10)).thenReturn(this.events);
        when(this.auth.hasAccess(this.user, Right.ADMIN, this.xwikiPreferences)).thenReturn(true);
        JSONObject response = (JSONObject) this.resource
            .listEvents(20, 10, "action", "user", "ip", "Space.Page", "01/01/2010", "01/01/2011").getEntity();
        Mockito.verify(this.store).getEvents(template, from, to, 20, 10);
        JSONArray data = (JSONArray) response.get("data");
        Assert.assertSame(this.events.get(0).toJSON(), data.opt(0));
    }

    @Test
    public void listEventsAcceptsNullFilters()
    {
        AuditEvent template = new AuditEvent(null, null, null, null, null, null);
        when(this.store.getEvents(template, null, null, 0, 0)).thenReturn(this.events);
        when(this.auth.hasAccess(this.user, Right.ADMIN, this.xwikiPreferences)).thenReturn(true);
        JSONObject response =
            (JSONObject) this.resource.listEvents(0, 0, null, null, null, null, null, null).getEntity();
        Mockito.verify(this.store).getEvents(template, null, null, 0, 0);
        JSONArray data = (JSONArray) response.get("data");
        Assert.assertSame(this.events.get(0).toJSON(), data.opt(0));
    }

    @Test
    public void listEventsAcceptsBlankFilters()
    {
        AuditEvent template = new AuditEvent(null, null, null, null, null, null);
        when(this.store.getEvents(template, null, null, 0, 0)).thenReturn(this.events);
        when(this.auth.hasAccess(this.user, Right.ADMIN, this.xwikiPreferences)).thenReturn(true);
        JSONObject response = (JSONObject) this.resource.listEvents(0, 0, " ", " ", " ", " ", " ", " ").getEntity();
        Mockito.verify(this.store).getEvents(template, null, null, 0, 0);
        JSONArray data = (JSONArray) response.get("data");
        Assert.assertSame(this.events.get(0).toJSON(), data.opt(0));
    }
}
