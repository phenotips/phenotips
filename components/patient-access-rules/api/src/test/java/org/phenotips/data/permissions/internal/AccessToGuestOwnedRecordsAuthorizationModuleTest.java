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
package org.phenotips.data.permissions.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.permissions.EntityAccess;
import org.phenotips.data.permissions.EntityPermissionsManager;
import org.phenotips.data.permissions.Owner;
import org.phenotips.entities.PrimaryEntityResolver;
import org.phenotips.security.authorization.AuthorizationModule;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.ManageRight;
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
 * Tests for the {@link AccessToGuestOwnedRecordsAuthorizationModule} {@link AuthorizationModule} component.
 *
 * @version $Id$
 */
public class AccessToGuestOwnedRecordsAuthorizationModuleTest
{
    @Rule
    public final MockitoComponentMockingRule<AuthorizationModule> mocker =
        new MockitoComponentMockingRule<>(AccessToGuestOwnedRecordsAuthorizationModule.class);

    @Mock
    private User user;

    @Mock
    private Patient patient;

    private EntityPermissionsManager pm;

    private PrimaryEntityResolver resolver;

    @Mock
    private EntityAccess entityAccess;

    @Mock
    private Owner owner;

    private DocumentReference doc = new DocumentReference("xwiki", "data", "P01");

    @Mock
    private DocumentReference userProfile;

    @Before
    public void setupMocks() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.resolver = this.mocker.getInstance(PrimaryEntityResolver.class);
        when(this.resolver.resolveEntity("xwiki:data.P01")).thenReturn(this.patient);

        this.pm = this.mocker.getInstance(EntityPermissionsManager.class);
        when(this.pm.getEntityAccess(this.patient)).thenReturn(this.entityAccess);
        when(this.entityAccess.getOwner()).thenReturn(this.owner);
        when(this.owner.getUser()).thenReturn(null);
    }

    @Test
    public void allDocumentAccessGrantedForRegisteredUser() throws ComponentLookupException
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.VIEW, this.doc));
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.EDIT, this.doc));
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.COMMENT, this.doc));
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.DELETE, this.doc));
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, ManageRight.MANAGE, this.doc));
    }

    @Test
    public void allDocumentAccessGrantedForGuestUser() throws ComponentLookupException
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(null, Right.VIEW, this.doc));
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(null, Right.EDIT, this.doc));
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(null, Right.COMMENT, this.doc));
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(null, Right.DELETE, this.doc));
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(null, ManageRight.MANAGE, this.doc));
    }

    @Test
    public void noAccessGrantedWithRealOwner() throws ComponentLookupException
    {
        when(this.owner.getUser()).thenReturn(this.userProfile);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.VIEW, this.doc));
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(null, Right.VIEW, this.doc));
    }

    @Test
    public void noActionWithNonPatient() throws ComponentLookupException
    {
        when(this.resolver.resolveEntity("xwiki:data.P01")).thenReturn(null);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.VIEW, this.doc));
    }

    @Test
    public void noActionWithNonDocumentRight() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.REGISTER, this.doc));
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.PROGRAM, this.doc));
    }

    @Test
    public void noActionWithNullRight() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, null, this.doc));
    }

    @Test
    public void noActionWithNullDocument() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.VIEW, null));
    }

    @Test
    public void expectedPriority() throws ComponentLookupException
    {
        Assert.assertEquals(150, this.mocker.getComponentUnderTest().getPriority());
    }
}
