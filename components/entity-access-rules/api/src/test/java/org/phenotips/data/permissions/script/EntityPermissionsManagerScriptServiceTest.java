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
package org.phenotips.data.permissions.script;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.EntityAccess;
import org.phenotips.data.permissions.EntityPermissionsManager;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.data.permissions.internal.SecureEntityPermissionsManager;
import org.phenotips.data.permissions.internal.access.EditAccessLevel;
import org.phenotips.data.permissions.internal.visibility.PublicVisibility;
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the secure {@link EntityPermissionsManager} implementation, {@link SecureEntityPermissionsManager}.
 *
 * @version $Id$
 */
public class EntityPermissionsManagerScriptServiceTest
{
    @Rule
    public final MockitoComponentMockingRule<EntityPermissionsManagerScriptService> mocker =
        new MockitoComponentMockingRule<>(EntityPermissionsManagerScriptService.class);

    private AccessLevel edit = new EditAccessLevel();

    private Visibility publicVisibility = new PublicVisibility();

    @Test
    public void listAccessLevelsForwardsCalls() throws ComponentLookupException
    {
        EntityPermissionsManager internal = this.mocker.getInstance(EntityPermissionsManager.class, "secure");
        List<AccessLevel> levels = new ArrayList<>();
        when(internal.listAccessLevels()).thenReturn(levels);
        Collection<AccessLevel> returnedLevels = this.mocker.getComponentUnderTest().listAccessLevels();
        Assert.assertSame(levels, returnedLevels);
    }

    @Test
    public void resolveAccessLevelForwardsCalls() throws ComponentLookupException
    {
        EntityPermissionsManager internal = this.mocker.getInstance(EntityPermissionsManager.class, "secure");
        when(internal.resolveAccessLevel("edit")).thenReturn(this.edit);
        Assert.assertSame(this.edit, this.mocker.getComponentUnderTest().resolveAccessLevel("edit"));
    }

    @Test
    public void listVisibilityOptionsForwardsCalls() throws ComponentLookupException
    {
        EntityPermissionsManager internal = this.mocker.getInstance(EntityPermissionsManager.class, "secure");
        List<Visibility> visibilities = new ArrayList<>();
        when(internal.listVisibilityOptions()).thenReturn(visibilities);
        Collection<Visibility> returnedVisibilities = this.mocker.getComponentUnderTest().listVisibilityOptions();
        Assert.assertSame(visibilities, returnedVisibilities);
    }

    @Test
    public void listAllVisibilityOptionsForwardsCalls() throws ComponentLookupException
    {
        EntityPermissionsManager internal = this.mocker.getInstance(EntityPermissionsManager.class, "secure");
        List<Visibility> visibilities = new ArrayList<>();
        when(internal.listAllVisibilityOptions()).thenReturn(visibilities);
        Collection<Visibility> returnedVisibilities = this.mocker.getComponentUnderTest().listAllVisibilityOptions();
        Assert.assertSame(visibilities, returnedVisibilities);
    }

    @Test
    public void getDefaultVisibilityForwardsCalls() throws ComponentLookupException
    {
        EntityPermissionsManager internal = this.mocker.getInstance(EntityPermissionsManager.class, "secure");
        when(internal.getDefaultVisibility()).thenReturn(this.publicVisibility);
        Visibility result = this.mocker.getComponentUnderTest().getDefaultVisibility();
        Assert.assertSame(this.publicVisibility, result);
    }

    @Test
    public void resolveVisibilityForwardsCalls() throws ComponentLookupException
    {
        EntityPermissionsManager internal = this.mocker.getInstance(EntityPermissionsManager.class, "secure");
        when(internal.resolveVisibility("public")).thenReturn(this.publicVisibility);
        Assert.assertSame(this.publicVisibility, this.mocker.getComponentUnderTest().resolveVisibility("public"));
    }

    @Test
    public void getPatientAccessForwardsCallsCorrectly() throws ComponentLookupException
    {
        EntityPermissionsManager internal = this.mocker.getInstance(EntityPermissionsManager.class, "secure");
        Patient patient = mock(Patient.class);
        DocumentReference patientReference = mock(DocumentReference.class);
        when(patient.getDocumentReference()).thenReturn(patientReference);

        EntityAccess internalAccess = mock(EntityAccess.class);
        when(internal.getPatientAccess(patient)).thenReturn(internalAccess);

        User currentUser = mock(User.class);
        UserManager userManager = this.mocker.getInstance(UserManager.class);
        when(userManager.getCurrentUser()).thenReturn(currentUser);

        String testID = "TESTID";
        PatientRepository patientRepo = this.mocker.getInstance(PatientRepository.class);
        when(patientRepo.get(testID)).thenReturn(patient);

        AuthorizationService access = this.mocker.getInstance(AuthorizationService.class);

        // test when has VIEW rights
        when(access.hasAccess(currentUser, Right.VIEW, patientReference)).thenReturn(true);
        EntityAccess result1 = this.mocker.getComponentUnderTest().getPatientAccess(testID);
        Assert.assertSame(internalAccess, result1);

        // test when no VIEW rights
        when(access.hasAccess(currentUser, Right.VIEW, patientReference)).thenReturn(false);
        EntityAccess result2 = this.mocker.getComponentUnderTest().getPatientAccess(testID);
        Assert.assertNull(result2);
    }
}
