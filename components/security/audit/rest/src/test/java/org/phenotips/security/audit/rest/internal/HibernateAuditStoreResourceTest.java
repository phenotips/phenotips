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
import org.phenotips.security.audit.rest.AuditStoreResource;
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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link HibernateAuditStoreResource} component.
 *
 * @version $Id$
 */
public class HibernateAuditStoreResourceTest
{
    @Rule
    public final MockitoComponentMockingRule<AuditStoreResource> mocker =
        new MockitoComponentMockingRule<>(HibernateAuditStoreResource.class);

    @Mock
    private User user;

    @Mock
    private AuditEvent event;

    private DocumentReference doc = new DocumentReference("wiki", "Space", "Page");

    private DocumentReference xwikiPreferences = new DocumentReference("wiki", "XWiki", "XWikiPreferences");

    private AuditStore store;

    private AuthorizationService auth;

    private UserManager users;

    private AuditStoreResource resource;

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
    public void getEvents()
    {
        when(this.store.getEvents(any(AuditEvent.class), any(Calendar.class), any(Calendar.class), anyInt(), anyInt()))
            .thenReturn(this.events);
        when(this.auth.hasAccess(this.user, Right.ADMIN, this.xwikiPreferences)).thenReturn(true);
        JSONObject response =
            (JSONObject) this.resource.listEvents(0, 25, "get", this.user.getId(), "ip", "", "", "").getEntity();
        JSONArray data = (JSONArray) response.get("data");
        Assert.assertSame(this.events.get(0).toJSON(), data.opt(0));
    }
}
