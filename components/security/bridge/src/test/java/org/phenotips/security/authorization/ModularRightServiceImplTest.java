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
package org.phenotips.security.authorization;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiAuthService;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.web.Utils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

public class ModularRightServiceImplTest
{
    @Mock
    private ComponentManager cm;

    @Mock
    private UserManager userManager;

    @Mock
    private AuthorizationService internalService;

    @Mock
    private XWikiDocument document;

    private DocumentReference documentReference = new DocumentReference("xwiki", "Some", "Document");

    private DocumentReference userReference = new DocumentReference("xwiki", "XWiki", "jdoe");

    private DocumentReference guestReference = new DocumentReference("xwiki", "XWiki", "XWikiGuest");

    @Mock
    private DocumentReferenceResolver<String> userResolver;

    @Mock
    private DocumentReferenceResolver<String> docResolver;

    @Mock
    private XWikiContext context;

    @Mock
    private User user;

    @Mock
    private XWiki xwiki;

    @Mock
    private User guestUser;

    private ModularRightServiceImpl service;

    @Mock
    private XWikiAuthService authService;

    @Before
    public void setupMocks() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        Utils.setComponentManager(this.cm);

        when(this.cm.getInstance(AuthorizationService.class, "default")).thenReturn(this.internalService);
        when(this.internalService.hasAccess(this.user, Right.VIEW, this.documentReference)).thenReturn(true);
        when(this.internalService.hasAccess(this.guestUser, Right.VIEW, this.documentReference)).thenReturn(false);

        when(this.document.getDocumentReference()).thenReturn(this.documentReference);

        when(this.cm.getInstance(UserManager.class, "default")).thenReturn(this.userManager);
        when(this.userManager.getUser("jdoe", true)).thenReturn(this.user);
        when(this.userManager.getUser("xwiki:XWiki.jdoe", true)).thenReturn(this.user);
        when(this.userManager.getUser(null, true)).thenReturn(this.guestUser);

        when(this.context.getWiki()).thenReturn(this.xwiki);
        when(this.context.getDatabase()).thenReturn("xwiki");

        when(this.cm.getInstance(DocumentReferenceResolver.TYPE_STRING, "user")).thenReturn(this.userResolver);
        when(this.userResolver.resolve("jdoe", new WikiReference("xwiki"))).thenReturn(this.userReference);
        when(this.userResolver.resolve("XWiki.jdoe", new WikiReference("xwiki"))).thenReturn(this.userReference);
        when(this.userResolver.resolve("XWiki.XWikiGuest", new WikiReference("xwiki"))).thenReturn(this.guestReference);

        when(this.cm.getInstance(DocumentReferenceResolver.TYPE_STRING, "currentmixed")).thenReturn(this.docResolver);
        when(this.docResolver.resolve("Some.Document", new WikiReference("xwiki"))).thenReturn(this.documentReference);

        when(this.xwiki.getAuthService()).thenReturn(this.authService);

        this.service = new ModularRightServiceImpl();
    }

    @Test
    public void checkAccessBasicTest() throws XWikiException
    {
        when(this.context.getUserReference()).thenReturn(this.userReference);
        Assert.assertTrue(this.service.checkAccess("view", this.document, this.context));
        Mockito.verify(this.authService, never()).checkAuth(this.context);
    }

    @Test
    public void checkAccessWithGuestUser() throws XWikiException
    {
        when(this.context.getUserReference()).thenReturn(this.guestReference);
        Assert.assertFalse(this.service.checkAccess("view", this.document, this.context));
    }

    @Test
    public void checkAccessWithNullUserAndValidAuthentication() throws XWikiException
    {
        when(this.context.getUserReference()).thenReturn(null);
        XWikiUser oldcoreUser = mock(XWikiUser.class);
        when(this.xwiki.checkAuth(this.context)).thenReturn(oldcoreUser);
        when(oldcoreUser.getUser()).thenReturn("XWiki.jdoe");

        Assert.assertTrue(this.service.checkAccess("view", this.document, this.context));
    }

    @Test
    public void checkAccessWithNullUserDoesntShowLoginOnLoginAction() throws XWikiException
    {
        when(this.context.getUserReference()).thenReturn(null);
        XWikiUser oldcoreUser = mock(XWikiUser.class);
        when(this.xwiki.checkAuth(this.context)).thenReturn(oldcoreUser);
        when(oldcoreUser.getUser()).thenReturn("XWiki.XWikiGuest");
        when(this.context.getAction()).thenReturn("login");
        Assert.assertFalse(this.service.checkAccess("view", this.document, this.context));
        Mockito.verify(this.authService, never()).checkAuth(this.context);
    }

    @Test
    public void checkAccessWithNullUserAndGuestAuthentication() throws XWikiException
    {
        when(this.context.getUserReference()).thenReturn(null);
        XWikiUser oldcoreUser = mock(XWikiUser.class);
        when(this.xwiki.checkAuth(this.context)).thenReturn(oldcoreUser);
        when(oldcoreUser.getUser()).thenReturn("XWiki.XWikiGuest");

        Assert.assertFalse(this.service.checkAccess("view", this.document, this.context));
        // Will show the login screen
        Mockito.verify(this.authService).showLogin(this.context);
    }

    @Test
    public void checkAccessWithNullUserAndNoAuthentication() throws XWikiException
    {
        when(this.context.getUserReference()).thenReturn(null);
        when(this.xwiki.checkAuth(this.context)).thenReturn(null);
        Assert.assertFalse(this.service.checkAccess("view", this.document, this.context));
    }

    @Test
    public void checkAccessWithNullUserAndFailedAuthentication() throws XWikiException
    {
        when(this.context.getUserReference()).thenReturn(null);
        when(this.xwiki.checkAuth(this.context)).thenThrow(new XWikiException());
        Assert.assertFalse(this.service.checkAccess("view", this.document, this.context));
    }

    @Test
    public void hasAccessLevelBaseTest() throws XWikiException
    {
        Assert.assertTrue(this.service.hasAccessLevel("view", "jdoe", "Some.Document", this.context));
    }

    @Test
    public void hasAccessWithGuestUser() throws XWikiException
    {
        Assert.assertFalse(this.service.hasAccessLevel("view", "XWiki.XWikiGuest", "Some.Document", this.context));
    }
}
