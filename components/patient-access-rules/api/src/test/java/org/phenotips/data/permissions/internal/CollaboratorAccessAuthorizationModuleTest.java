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
package org.phenotips.data.permissions.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.security.authorization.AuthorizationModule;

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

import static org.mockito.Mockito.when;

/**
 * Tests for the {@link CollaboratorAccessAuthorizationModule owner granted access} {@link AuthorizationModule}
 * component.
 *
 * @version $Id$
 */
public class CollaboratorAccessAuthorizationModuleTest
{
    @Rule
    public final MockitoComponentMockingRule<AuthorizationModule> mocker =
        new MockitoComponentMockingRule<AuthorizationModule>(CollaboratorAccessAuthorizationModule.class);

    @Mock
    private User user;

    @Mock
    private Right right;

    @Mock
    private Patient patient;

    @Mock
    private PatientAccess pAccess;

    private DocumentReference doc = new DocumentReference("xwiki", "data", "P01");

    @Mock
    private DocumentReference userProfile;

    @Mock
    private AccessLevel grantedAccess;

    @Mock
    private AccessLevel requestedAccess;

    @Before
    public void setupMocks()
    {
        MockitoAnnotations.initMocks(this);
        when(this.user.getProfileDocument()).thenReturn(this.userProfile);
    }

    @Test
    public void noActionWithLowerLevelCollaborator() throws ComponentLookupException
    {
        PatientRepository repo = this.mocker.getInstance(PatientRepository.class);
        when(repo.getPatientById("xwiki:data.P01")).thenReturn(this.patient);
        PermissionsManager pm = this.mocker.getInstance(PermissionsManager.class);
        when(pm.getPatientAccess(this.patient)).thenReturn(this.pAccess);
        when(this.pAccess.getAccessLevel(this.userProfile)).thenReturn(this.grantedAccess);
        when(this.right.getName()).thenReturn("edit");
        when(pm.resolveAccessLevel("edit")).thenReturn(this.requestedAccess);
        when(this.grantedAccess.compareTo(this.requestedAccess)).thenReturn(-1);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, this.right, this.doc));
    }

    @Test
    public void accessGrantedWithSameLevelCollaborator() throws ComponentLookupException
    {
        PatientRepository repo = this.mocker.getInstance(PatientRepository.class);
        when(repo.getPatientById("xwiki:data.P01")).thenReturn(this.patient);
        PermissionsManager pm = this.mocker.getInstance(PermissionsManager.class);
        when(pm.getPatientAccess(this.patient)).thenReturn(this.pAccess);
        when(this.pAccess.getAccessLevel(this.userProfile)).thenReturn(this.grantedAccess);
        when(this.right.getName()).thenReturn("edit");
        when(pm.resolveAccessLevel("edit")).thenReturn(this.requestedAccess);
        when(this.grantedAccess.compareTo(this.requestedAccess)).thenReturn(0);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, this.right, this.doc));
    }

    @Test
    public void accessGrantedWithHigherLevelCollaborator() throws ComponentLookupException
    {
        PatientRepository repo = this.mocker.getInstance(PatientRepository.class);
        when(repo.getPatientById("xwiki:data.P01")).thenReturn(this.patient);
        PermissionsManager pm = this.mocker.getInstance(PermissionsManager.class);
        when(pm.getPatientAccess(this.patient)).thenReturn(this.pAccess);
        when(this.pAccess.getAccessLevel(this.userProfile)).thenReturn(this.grantedAccess);
        when(this.right.getName()).thenReturn("edit");
        when(pm.resolveAccessLevel("edit")).thenReturn(this.requestedAccess);
        when(this.grantedAccess.compareTo(this.requestedAccess)).thenReturn(1);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, this.right, this.doc));
    }

    @Test
    public void noActionWithNonPatient() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, this.right, this.doc));
    }

    @Test
    public void noActionWithNullArguments() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(null, this.right, this.doc));
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, this.right, null));
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, null, this.doc));
    }

    @Test
    public void noActionWithUnknownRight() throws ComponentLookupException
    {
        PatientRepository repo = this.mocker.getInstance(PatientRepository.class);
        when(repo.getPatientById("xwiki:data.P01")).thenReturn(this.patient);
        PermissionsManager pm = this.mocker.getInstance(PermissionsManager.class);
        when(pm.getPatientAccess(this.patient)).thenReturn(this.pAccess);
        when(this.pAccess.getAccessLevel(this.userProfile)).thenReturn(this.grantedAccess);
        when(this.right.getName()).thenReturn("manage");
        when(pm.resolveAccessLevel("manage")).thenReturn(null);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, this.right, this.doc));
    }

    @Test
    public void noActionWithNoGrantedAccess() throws ComponentLookupException
    {
        PatientRepository repo = this.mocker.getInstance(PatientRepository.class);
        when(repo.getPatientById("xwiki:data.P01")).thenReturn(this.patient);
        PermissionsManager pm = this.mocker.getInstance(PermissionsManager.class);
        when(pm.getPatientAccess(this.patient)).thenReturn(this.pAccess);
        when(this.pAccess.getAccessLevel(this.userProfile)).thenReturn(null);
        when(this.right.getName()).thenReturn("edit");
        when(pm.resolveAccessLevel("edit")).thenReturn(this.requestedAccess);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, this.right, this.doc));
    }

    @Test
    public void expectedPriority() throws ComponentLookupException
    {
        Assert.assertEquals(350, this.mocker.getComponentUnderTest().getPriority());
    }
}
