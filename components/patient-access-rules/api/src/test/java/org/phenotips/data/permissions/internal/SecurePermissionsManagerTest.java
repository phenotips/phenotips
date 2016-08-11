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
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.data.permissions.internal.access.EditAccessLevel;
import org.phenotips.data.permissions.internal.visibility.PublicVisibility;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the secure {@link PermissionsManager} implementation, {@link SecurePermissionsManager}.
 *
 * @version $Id$
 */
public class SecurePermissionsManagerTest
{
    @Rule
    public final MockitoComponentMockingRule<PermissionsManager> mocker =
        new MockitoComponentMockingRule<PermissionsManager>(SecurePermissionsManager.class);

    private AccessLevel edit = new EditAccessLevel();

    private Visibility publicVisibility = new PublicVisibility();

    @Test
    public void listAccessLevelsForwardsCalls() throws ComponentLookupException
    {
        PermissionsManager internal = this.mocker.getInstance(PermissionsManager.class);
        List<AccessLevel> levels = new ArrayList<>();
        when(internal.listAccessLevels()).thenReturn(levels);
        Collection<AccessLevel> returnedLevels = this.mocker.getComponentUnderTest().listAccessLevels();
        Assert.assertSame(levels, returnedLevels);
    }

    @Test
    public void resolveAccessLevelForwardsCalls() throws ComponentLookupException
    {
        PermissionsManager internal = this.mocker.getInstance(PermissionsManager.class);
        when(internal.resolveAccessLevel("edit")).thenReturn(this.edit);
        Assert.assertSame(this.edit, this.mocker.getComponentUnderTest().resolveAccessLevel("edit"));
    }

    @Test
    public void listVisibilityOptionsForwardsCalls() throws ComponentLookupException
    {
        PermissionsManager internal = this.mocker.getInstance(PermissionsManager.class);
        List<Visibility> visibilities = new ArrayList<>();
        when(internal.listVisibilityOptions()).thenReturn(visibilities);
        Collection<Visibility> returnedVisibilities = this.mocker.getComponentUnderTest().listVisibilityOptions();
        Assert.assertSame(visibilities, returnedVisibilities);
    }

    @Test
    public void listAllVisibilityOptionsForwardsCalls() throws ComponentLookupException
    {
        PermissionsManager internal = this.mocker.getInstance(PermissionsManager.class);
        List<Visibility> visibilities = new ArrayList<>();
        when(internal.listAllVisibilityOptions()).thenReturn(visibilities);
        Collection<Visibility> returnedVisibilities = this.mocker.getComponentUnderTest().listAllVisibilityOptions();
        Assert.assertSame(visibilities, returnedVisibilities);
    }

    @Test
    public void getDefaultVisibilityForwardsCalls() throws ComponentLookupException
    {
        PermissionsManager internal = this.mocker.getInstance(PermissionsManager.class);
        when(internal.getDefaultVisibility()).thenReturn(this.publicVisibility);
        Visibility result = this.mocker.getComponentUnderTest().getDefaultVisibility();
        Assert.assertSame(this.publicVisibility, result);
    }

    @Test
    public void resolveVisibilityForwardsCalls() throws ComponentLookupException
    {
        PermissionsManager internal = this.mocker.getInstance(PermissionsManager.class);
        when(internal.resolveVisibility("public")).thenReturn(this.publicVisibility);
        Assert.assertSame(this.publicVisibility, this.mocker.getComponentUnderTest().resolveVisibility("public"));
    }

    @Test
    public void getPatientAccessReturnsSecureAccess() throws ComponentLookupException
    {
        PermissionsManager internal = this.mocker.getInstance(PermissionsManager.class);
        Patient patient = mock(Patient.class);
        PatientAccess internalAccess = mock(PatientAccess.class);
        when(internal.getPatientAccess(patient)).thenReturn(internalAccess);
        PatientAccess result = this.mocker.getComponentUnderTest().getPatientAccess(patient);
        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof SecurePatientAccess);
    }

    @Test
    public void filterCollectionByVisibilityForwardsCalls() throws ComponentLookupException
    {
        PermissionsManager internal = this.mocker.getInstance(PermissionsManager.class);
        Collection<Patient> input = new ArrayList<>();
        when(internal.filterByVisibility(input, this.publicVisibility)).thenReturn(input);

        Collection<Patient> result =
            this.mocker.getComponentUnderTest().filterByVisibility(input, this.publicVisibility);
        Mockito.verify(internal).filterByVisibility(input, this.publicVisibility);
        Assert.assertSame(input, result);
    }

    @Test
    public void filterIteratorByVisibilityForwardsCalls() throws ComponentLookupException
    {
        PermissionsManager internal = this.mocker.getInstance(PermissionsManager.class);
        Iterator<Patient> input = new ArrayList<Patient>().iterator();
        when(internal.filterByVisibility(input, this.publicVisibility)).thenReturn(input);

        Iterator<Patient> result = this.mocker.getComponentUnderTest().filterByVisibility(input, this.publicVisibility);
        Mockito.verify(internal).filterByVisibility(input, this.publicVisibility);
        Assert.assertSame(input, result);
    }
}
