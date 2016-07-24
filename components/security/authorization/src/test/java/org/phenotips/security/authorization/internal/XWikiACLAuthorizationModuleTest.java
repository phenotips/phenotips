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
package org.phenotips.security.authorization.internal;

import org.phenotips.security.authorization.AuthorizationModule;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

/**
 * Tests for the {@link XWikiACLAuthorizationModule ACL-based} {@link AuthorizationModule} component.
 *
 * @version $Id$
 */
public class XWikiACLAuthorizationModuleTest
{
    @Rule
    public final MockitoComponentMockingRule<AuthorizationModule> mocker =
        new MockitoComponentMockingRule<AuthorizationModule>(XWikiACLAuthorizationModule.class);

    @Mock
    private User user;

    @Mock
    private Right right;

    @Mock
    private DocumentReference doc;

    @Mock
    private DocumentReference userProfile;

    @Before
    public void setupMocks()
    {
        MockitoAnnotations.initMocks(this);
        when(this.user.getProfileDocument()).thenReturn(this.userProfile);
    }

    @Test
    public void decisionIsForwarded() throws ComponentLookupException
    {
        AuthorizationManager am = this.mocker.getInstance(AuthorizationManager.class);
        when(am.hasAccess(this.right, this.userProfile, this.doc)).thenReturn(true);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, this.right, this.doc));
    }

    @Test(expected = NullPointerException.class)
    public void exceptionsAreForwarded() throws ComponentLookupException
    {
        AuthorizationManager am = this.mocker.getInstance(AuthorizationManager.class);
        when(am.hasAccess(this.right, this.userProfile, this.doc)).thenThrow(new NullPointerException());
        this.mocker.getComponentUnderTest().hasAccess(this.user, this.right, this.doc);
    }

    @Test
    public void nullUserIsAccepted() throws ComponentLookupException
    {
        AuthorizationManager am = this.mocker.getInstance(AuthorizationManager.class);
        when(am.hasAccess(this.right, null, this.doc)).thenReturn(true);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(null, this.right, this.doc));
    }

    @Test
    public void expectedPriority() throws ComponentLookupException
    {
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().getPriority());
    }
}
