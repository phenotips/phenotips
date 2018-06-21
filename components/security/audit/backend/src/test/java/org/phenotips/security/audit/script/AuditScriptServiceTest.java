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
package org.phenotips.security.audit.script;

import org.phenotips.Constants;
import org.phenotips.security.audit.AuditEvent;
import org.phenotips.security.audit.AuditStore;
import org.phenotips.security.audit.internal.HibernateAuditStore;
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.component.manager.ComponentLookupException;
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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link HibernateAuditStore} component.
 *
 * @version $Id$
 */
public class AuditScriptServiceTest
{
    @Rule
    public final MockitoComponentMockingRule<AuditScriptService> mocker =
        new MockitoComponentMockingRule<>(AuditScriptService.class);

    @Mock
    private User user;

    private DocumentReference doc = new DocumentReference("wiki", "Space", "Page");

    private DocumentReference xwikiPreferences = new DocumentReference("wiki", "XWiki", "XWikiPreferences");

    private AuditScriptService scriptService;

    private AuditStore store;

    private AuthorizationService auth;

    @Mock
    private AuditEvent event;

    private List<AuditEvent> events;

    @Before
    public void setup() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        this.scriptService = this.mocker.getComponentUnderTest();
        this.store = this.mocker.getInstance(AuditStore.class);
        this.auth = this.mocker.getInstance(AuthorizationService.class);
        when(this.user.getId()).thenReturn("wiki:XWiki.user");
        this.events = Arrays.asList(this.event);

        UserManager users = this.mocker.getInstance(UserManager.class);
        when(users.getCurrentUser()).thenReturn(this.user);
        when(users.getUser("user")).thenReturn(this.user);

        DocumentReferenceResolver<EntityReference> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "currentmixed");
        when(resolver.resolve(Constants.XWIKI_SPACE_REFERENCE)).thenReturn(this.xwikiPreferences);
    }

    @Test
    public void getEventsForEntityWithoutEditAccessReturnsEmptyList()
    {
        when(this.auth.hasAccess(this.user, Right.EDIT, this.doc)).thenReturn(false);
        Assert.assertTrue(this.scriptService.getEventsForEntity(this.doc).isEmpty());
        Mockito.verifyZeroInteractions(this.store);
    }

    @Test
    public void getEventsForEntityForwardsCall()
    {
        when(this.store.getEventsForEntity(this.doc)).thenReturn(this.events);
        when(this.auth.hasAccess(this.user, Right.EDIT, this.doc)).thenReturn(true);
        Assert.assertSame(this.events, this.scriptService.getEventsForEntity(this.doc));
    }

    @Test
    public void getEventsForUserWithoutAdminAccessReturnsEmptyList()
    {
        when(this.auth.hasAccess(this.user, Right.ADMIN, this.xwikiPreferences)).thenReturn(false);
        Assert.assertTrue(this.scriptService.getEventsForUser("user").isEmpty());
        Mockito.verifyZeroInteractions(this.store);
    }

    @Test
    public void getEventsForUserForwardsCall()
    {
        when(this.store.getEventsForUser(this.user)).thenReturn(this.events);
        when(this.auth.hasAccess(this.user, Right.ADMIN, this.xwikiPreferences)).thenReturn(true);
        Assert.assertSame(this.events, this.scriptService.getEventsForUser("user"));
    }

    @Test
    public void getEventsForUserAndIPWithoutAdminAccessReturnsEmptyList()
    {
        when(this.auth.hasAccess(this.user, Right.ADMIN, this.xwikiPreferences)).thenReturn(false);
        Assert.assertTrue(this.scriptService.getEventsForUser("user", "ip").isEmpty());
        Mockito.verifyZeroInteractions(this.store);
    }

    @Test
    public void getEventsForUserAndIPForwardsCall()
    {
        when(this.store.getEventsForUser(this.user, "ip")).thenReturn(this.events);
        when(this.auth.hasAccess(this.user, Right.ADMIN, this.xwikiPreferences)).thenReturn(true);
        Assert.assertSame(this.events, this.scriptService.getEventsForUser("user", "ip"));
    }

    @Test
    public void getEvents()
    {
        when(this.store.getEvents(any(AuditEvent.class), any(Calendar.class), any(Calendar.class), anyInt(), anyInt()))
            .thenReturn(this.events);
        when(this.auth.hasAccess(this.user, Right.ADMIN, this.xwikiPreferences)).thenReturn(true);
        Assert.assertSame(this.events, this.scriptService.getEvents(0, 25, "get", this.user.getId(), "ip", "", "", ""));
    }
}
