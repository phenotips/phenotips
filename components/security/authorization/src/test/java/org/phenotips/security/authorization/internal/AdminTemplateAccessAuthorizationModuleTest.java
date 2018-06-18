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
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AdminTemplateAccessAuthorizationModule}.
 *
 * @version $Id$
 */
public class AdminTemplateAccessAuthorizationModuleTest
{
    private static final String PHENOTIPS = "PhenoTips";

    private static final String XWIKI = "xwiki";

    private static final String USERS = "Users";

    private static final DocumentReference PT_DOC = new DocumentReference(XWIKI, PHENOTIPS, "PatientTemplate");

    private static final DocumentReference FT_DOC = new DocumentReference(XWIKI, PHENOTIPS, "FamilyTemplate");

    private static final DocumentReference PS_DOC = new DocumentReference(XWIKI, PHENOTIPS, "PatientSheet");

    private static final DocumentReference NORMAL_USER = new DocumentReference(XWIKI, USERS, "padams");

    private static final DocumentReference ADMIN_USER = new DocumentReference(XWIKI, USERS, "Admin");

    @Rule
    public final MockitoComponentMockingRule<AuthorizationModule> mocker =
        new MockitoComponentMockingRule<AuthorizationModule>(AdminTemplateAccessAuthorizationModule.class);

    @Mock
    private User adminUser;

    @Mock
    private User nonAdminUser;

    @Mock
    private User guestUser;

    private AuthorizationModule component;

    @Before
    public void setUp() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);

        this.component = this.mocker.getComponentUnderTest();

        when(this.adminUser.getProfileDocument()).thenReturn(ADMIN_USER);
        when(this.nonAdminUser.getProfileDocument()).thenReturn(NORMAL_USER);
        when(this.guestUser.getProfileDocument()).thenReturn(null);

        final AuthorizationService auth = this.mocker.getInstance(AuthorizationService.class);
        when(auth.hasAccess(eq(this.adminUser), eq(Right.ADMIN), any(DocumentReference.class))).thenReturn(true);
        when(auth.hasAccess(eq(this.nonAdminUser), eq(Right.ADMIN), any(DocumentReference.class))).thenReturn(false);
    }

    @Test
    public void getPriority()
    {
        Assert.assertEquals(500, this.component.getPriority());
    }

    @Test
    public void hasAccessIsNullIfNotTemplate()
    {
        Assert.assertNull(this.component.hasAccess(this.adminUser, Right.EDIT, PS_DOC));
    }

    @Test
    public void hasAccessIsNullIfReadOnlyRight()
    {
        Assert.assertNull(this.component.hasAccess(this.adminUser, Right.VIEW, PT_DOC));
    }

    @Test
    public void hasAccessIsFalseIfNullUser()
    {
        Assert.assertFalse(this.component.hasAccess(null, Right.EDIT, PT_DOC));
    }

    @Test
    public void hasAccessIsFalseIfGuestUser()
    {
        Assert.assertFalse(this.component.hasAccess(this.guestUser, Right.EDIT, PT_DOC));
        Assert.assertFalse(this.component.hasAccess(this.guestUser, Right.EDIT, FT_DOC));
    }

    @Test
    public void hasAccessIsFalseIfNonAdminUser()
    {
        Assert.assertFalse(this.component.hasAccess(this.nonAdminUser, Right.EDIT, PT_DOC));
        Assert.assertFalse(this.component.hasAccess(this.nonAdminUser, Right.EDIT, FT_DOC));
    }

    @Test
    public void hasAccessIsTrueIfAdminUser()
    {
        Assert.assertTrue(this.component.hasAccess(this.adminUser, Right.EDIT, PT_DOC));
        Assert.assertTrue(this.component.hasAccess(this.adminUser, Right.EDIT, FT_DOC));
    }
}
