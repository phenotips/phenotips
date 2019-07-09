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
import org.phenotips.security.audit.SecurityTestUtils;
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
        DocumentReferenceResolver<String> resolverd =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        when(resolverd.resolve("Space.Page")).thenReturn(this.doc);
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
    public void getEventsWithoutAdminAccessReturnsEmptyList()
    {
        when(this.auth.hasAccess(this.user, Right.ADMIN, this.xwikiPreferences)).thenReturn(false);
        Assert.assertTrue(this.scriptService.getEvents(0, 0, null, null, null, null, null, null).isEmpty());
        Mockito.verifyZeroInteractions(this.store);
    }

    @Test
    public void getEventsForwardsCall()
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
        List<AuditEvent> result =
            this.scriptService.getEvents(20, 10, "action", "user", "ip", "Space.Page", "01/01/2010", "01/01/2011");
        Mockito.verify(this.store).getEvents(template, from, to, 20, 10);
        Assert.assertSame(this.events, result);
    }

    @Test
    public void getEventsAcceptsNullFilters()
    {
        AuditEvent template = new AuditEvent(null, null, null, null, null, null);
        when(this.store.getEvents(template, null, null, 0, 0)).thenReturn(this.events);
        when(this.auth.hasAccess(this.user, Right.ADMIN, this.xwikiPreferences)).thenReturn(true);
        List<AuditEvent> result =
            this.scriptService.getEvents(0, 0, null, null, null, null, null, null);
        Mockito.verify(this.store).getEvents(template, null, null, 0, 0);
        Assert.assertSame(this.events, result);
    }

    @Test
    public void getEventsAcceptsBlankFilters()
    {
        AuditEvent template = new AuditEvent(null, null, null, null, null, null);
        when(this.store.getEvents(template, null, null, 0, 0)).thenReturn(this.events);
        when(this.auth.hasAccess(this.user, Right.ADMIN, this.xwikiPreferences)).thenReturn(true);
        List<AuditEvent> result =
            this.scriptService.getEvents(0, 0, " ", " ", " ", " ", " ", " ");
        Mockito.verify(this.store).getEvents(template, null, null, 0, 0);
        Assert.assertSame(this.events, result);
    }

    @Test
    public void countEventsWithoutAdminAccessReturnsEmptyList()
    {
        when(this.auth.hasAccess(this.user, Right.ADMIN, this.xwikiPreferences)).thenReturn(false);
        Assert.assertEquals(-1, this.scriptService.countEvents(null, null, null, null, null, null));
        Mockito.verifyZeroInteractions(this.store);
    }

    @Test
    public void countEventsForwardsCall()
    {
        Calendar from = Calendar.getInstance();
        from.clear();
        from.set(2010, 0, 1, 0, 0, 0);
        Calendar to = SecurityTestUtils.getCalendar(10);
        to.clear();
        to.set(2011, 0, 1, 0, 0, 0);
        AuditEvent template = new AuditEvent(this.user, "ip", "action", null, this.doc, null);
        when(this.store.countEvents(template, from, to)).thenReturn(42L);
        when(this.auth.hasAccess(this.user, Right.ADMIN, this.xwikiPreferences)).thenReturn(true);
        long result =
            this.scriptService.countEvents("action", "user", "ip", "Space.Page", "01/01/2010", "01/01/2011");
        Mockito.verify(this.store).countEvents(template, from, to);
        Assert.assertEquals(42L, result);
    }

    @Test
    public void countEventsAcceptsNullFilters()
    {
        AuditEvent template = new AuditEvent(null, null, null, null, null, null);
        when(this.store.countEvents(template, null, null)).thenReturn(42L);
        when(this.auth.hasAccess(this.user, Right.ADMIN, this.xwikiPreferences)).thenReturn(true);
        long result = this.scriptService.countEvents(null, null, null, null, null, null);
        Mockito.verify(this.store).countEvents(template, null, null);
        Assert.assertEquals(42L, result);
    }

    @Test
    public void countEventsAcceptsBlankFilters()
    {
        AuditEvent template = new AuditEvent(null, null, null, null, null, null);
        when(this.store.countEvents(template, null, null)).thenReturn(42L);
        when(this.auth.hasAccess(this.user, Right.ADMIN, this.xwikiPreferences)).thenReturn(true);
        long result = this.scriptService.countEvents(" ", " ", " ", " ", " ", " ");
        Mockito.verify(this.store).countEvents(template, null, null);
        Assert.assertEquals(42L, result);
    }
}
