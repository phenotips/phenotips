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
import org.phenotips.data.PatientRepository;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsManager;
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
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

/**
 * Tests for the {@link OwnerAccessAuthorizationModule owner granted access} {@link AuthorizationModule} component.
 *
 * @version $Id$
 */
public class OwnerAccessAuthorizationModuleTest
{
    @Rule
    public final MockitoComponentMockingRule<AuthorizationModule> mocker =
        new MockitoComponentMockingRule<>(OwnerAccessAuthorizationModule.class);

    @Mock
    private User user;

    @Mock
    private Patient patient;

    private PermissionsManager pm;

    private PatientRepository repo;

    @Mock
    private PatientAccess patientAccess;

    @Mock
    private AccessLevel userAccess;

    @Mock
    private AccessLevel ownerAccess;

    @Mock
    private AccessLevel noAccess;

    private DocumentReference doc = new DocumentReference("xwiki", "data", "P01");

    @Mock
    private DocumentReference userProfile;

    @Before
    public void setupMocks() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.repo = this.mocker.getInstance(PatientRepository.class);
        when(this.repo.get("xwiki:data.P01")).thenReturn(this.patient);

        this.pm = this.mocker.getInstance(PermissionsManager.class);
        when(this.pm.getPatientAccess(this.patient)).thenReturn(this.patientAccess);
        this.mocker.registerComponent(AccessLevel.class, "owner", this.ownerAccess);
        when(this.patientAccess.getAccessLevel(Matchers.any())).thenReturn(this.noAccess);
        when(this.patientAccess.getAccessLevel(this.userProfile)).thenReturn(this.userAccess);
        when(this.ownerAccess.compareTo(this.noAccess)).thenReturn(1);
        when(this.ownerAccess.compareTo(this.userAccess)).thenReturn(0);

        when(this.user.getProfileDocument()).thenReturn(this.userProfile);
    }

    @Test
    public void viewAccessGrantedForOwner() throws ComponentLookupException
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.VIEW, this.doc));
    }

    @Test
    public void editAccessGrantedForOwner() throws ComponentLookupException
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.EDIT, this.doc));
    }

    @Test
    public void commentAccessGrantedForOwner() throws ComponentLookupException
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.COMMENT, this.doc));
    }

    @Test
    public void deleteAccessGrantedForOwner() throws ComponentLookupException
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.DELETE, this.doc));
    }

    @Test
    public void manageAccessGrantedForOwner() throws ComponentLookupException
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, ManageRight.MANAGE, this.doc));
    }

    @Test
    public void viewAccessGrantedForGuestOwner() throws ComponentLookupException
    {
        when(this.patientAccess.getAccessLevel(null)).thenReturn(this.userAccess);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(null, Right.VIEW, this.doc));
    }

    @Test
    public void editAccessGrantedForGuestOwner() throws ComponentLookupException
    {
        when(this.patientAccess.getAccessLevel(null)).thenReturn(this.userAccess);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(null, Right.EDIT, this.doc));
    }

    @Test
    public void commentAccessGrantedForGuestOwner() throws ComponentLookupException
    {
        when(this.patientAccess.getAccessLevel(null)).thenReturn(this.userAccess);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(null, Right.COMMENT, this.doc));
    }

    @Test
    public void deleteAccessGrantedForGuestOwner() throws ComponentLookupException
    {
        when(this.patientAccess.getAccessLevel(null)).thenReturn(this.userAccess);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(null, Right.DELETE, this.doc));
    }

    @Test
    public void manageAccessGrantedForGuestOwner() throws ComponentLookupException
    {
        when(this.patientAccess.getAccessLevel(null)).thenReturn(this.userAccess);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(null, ManageRight.MANAGE, this.doc));
    }

    @Test
    public void noAccessGrantedForGuestsUserWithRealOwner() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(null, ManageRight.MANAGE, this.doc));
    }

    @Test
    public void editAccessGrantedForMoreThanOwner() throws ComponentLookupException
    {
        Mockito.doReturn(-1).when(this.ownerAccess).compareTo(this.userAccess);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.EDIT, this.doc));
    }

    @Test
    public void noActionForNonOwner() throws ComponentLookupException
    {
        Mockito.doReturn(1).when(this.ownerAccess).compareTo(this.userAccess);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.EDIT, this.doc));
    }

    @Test
    public void noActionWithNonPatient() throws ComponentLookupException
    {
        when(this.repo.get("xwiki:data.P01")).thenReturn(null);
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
        Assert.assertEquals(400, this.mocker.getComponentUnderTest().getPriority());
    }
}
