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
import org.phenotips.data.permissions.internal.access.ManageAccessLevel;
import org.phenotips.data.permissions.internal.access.NoAccessLevel;
import org.phenotips.data.permissions.internal.access.OwnerAccessLevel;
import org.phenotips.data.permissions.internal.access.ViewAccessLevel;
import org.phenotips.data.permissions.internal.visibility.HiddenVisibility;
import org.phenotips.data.permissions.internal.visibility.PrivateVisibility;
import org.phenotips.data.permissions.internal.visibility.PublicVisibility;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the default {@link PermissionsManager} implementation, {@link DefaultPermissionsManager}.
 *
 * @version $Id$
 */
public class DefaultPermissionsManagerTest
{
    @Rule
    public final MockitoComponentMockingRule<PermissionsManager> mocker =
        new MockitoComponentMockingRule<PermissionsManager>(DefaultPermissionsManager.class);

    /** Basic tests for {@link PermissionsManager#listAccessLevels()}. */
    @Test
    public void listAccessLevels() throws ComponentLookupException
    {
        ComponentManager cm = this.mocker.getInstance(ComponentManager.class, "context");
        AccessLevel none = new NoAccessLevel();
        AccessLevel view = new ViewAccessLevel();
        AccessLevel edit = new EditAccessLevel();
        AccessLevel manage = new ManageAccessLevel();
        AccessLevel owner = new OwnerAccessLevel();
        List<AccessLevel> levels = new ArrayList<AccessLevel>();
        levels.add(edit);
        levels.add(none);
        levels.add(owner);
        levels.add(view);
        levels.add(manage);
        when(cm.<AccessLevel>getInstanceList(AccessLevel.class)).thenReturn(levels);
        Collection<AccessLevel> returnedLevels = this.mocker.getComponentUnderTest().listAccessLevels();
        Assert.assertEquals(3, returnedLevels.size());
        Iterator<AccessLevel> it = returnedLevels.iterator();
        Assert.assertSame(view, it.next());
        Assert.assertSame(edit, it.next());
        Assert.assertSame(manage, it.next());
        Assert.assertFalse(returnedLevels.contains(none));
        Assert.assertFalse(returnedLevels.contains(owner));
    }

    /** {@link PermissionsManager#listAccessLevels()} returns an empty list when no implementations available. */
    @Test
    public void listAccessLevelsWithNoComponents() throws ComponentLookupException
    {
        ComponentManager cm = this.mocker.getInstance(ComponentManager.class, "context");
        when(cm.<AccessLevel>getInstanceList(AccessLevel.class)).thenReturn(Collections.<AccessLevel>emptyList());
        Collection<AccessLevel> returnedLevels = this.mocker.getComponentUnderTest().listAccessLevels();
        Assert.assertTrue(returnedLevels.isEmpty());
    }

    /** {@link PermissionsManager#listAccessLevels()} returns an empty list when looking up components fails. */
    @Test
    public void listAccessLevelsWithLookupExceptions() throws ComponentLookupException
    {
        ComponentManager cm = this.mocker.getInstance(ComponentManager.class, "context");
        when(cm.<AccessLevel>getInstanceList(AccessLevel.class)).thenThrow(new ComponentLookupException("None"));
        Collection<AccessLevel> returnedLevels = this.mocker.getComponentUnderTest().listAccessLevels();
        Assert.assertTrue(returnedLevels.isEmpty());
    }

    /** {@link PermissionsManager#resolveAccessLevel(String)} returns the right implementation. */
    @Test
    public void resolveAccessLevel() throws ComponentLookupException
    {
        ComponentManager cm = this.mocker.getInstance(ComponentManager.class, "context");
        AccessLevel edit = mock(AccessLevel.class);
        when(cm.getInstance(AccessLevel.class, "edit")).thenReturn(edit);
        Assert.assertSame(edit, this.mocker.getComponentUnderTest().resolveAccessLevel("edit"));
    }

    /** {@link PermissionsManager#resolveAccessLevel(String)} returns null if an unknown level is requested. */
    @Test
    public void resolveAccessLevelWithUnknownAccessTest() throws ComponentLookupException
    {
        ComponentManager cm = this.mocker.getInstance(ComponentManager.class, "context");
        when(cm.getInstance(AccessLevel.class, "unknown")).thenThrow(new ComponentLookupException("No such component"));
        Assert.assertNull(this.mocker.getComponentUnderTest().resolveAccessLevel("unknown"));
    }

    /** {@link PermissionsManager#resolveAccessLevel(String)} returns null if a null or blank level is requested. */
    @Test
    public void resolveAccessLevelWithNoAccess() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().resolveAccessLevel(null));
        Assert.assertNull(this.mocker.getComponentUnderTest().resolveAccessLevel(""));
        Assert.assertNull(this.mocker.getComponentUnderTest().resolveAccessLevel(" "));
    }

    /** Basic tests for {@link PermissionsManager#listVisibilityOptions()}. */
    @Test
    public void listVisibilityOptions() throws ComponentLookupException
    {
        ComponentManager cm = this.mocker.getInstance(ComponentManager.class, "context");
        Visibility privateV = new PrivateVisibility();
        Visibility publicV = new PublicVisibility();
        List<Visibility> visibilities = new ArrayList<Visibility>();
        visibilities.add(publicV);
        visibilities.add(privateV);
        when(cm.<Visibility>getInstanceList(Visibility.class)).thenReturn(visibilities);
        Collection<Visibility> returnedVisibilities = this.mocker.getComponentUnderTest().listVisibilityOptions();
        Assert.assertEquals(2, returnedVisibilities.size());
        Iterator<Visibility> it = returnedVisibilities.iterator();
        Assert.assertSame(privateV, it.next());
        Assert.assertSame(publicV, it.next());
    }

    /** {@link PermissionsManager#listVisibilityOptions()} returns an empty list when no implementations available. */
    @Test
    public void listVisibilityOptionsWithNoComponents() throws ComponentLookupException
    {
        ComponentManager cm = this.mocker.getInstance(ComponentManager.class, "context");
        when(cm.<Visibility>getInstanceList(Visibility.class)).thenReturn(Collections.<Visibility>emptyList());
        Collection<Visibility> returnedVisibilities = this.mocker.getComponentUnderTest().listVisibilityOptions();
        Assert.assertTrue(returnedVisibilities.isEmpty());
    }

    /** {@link PermissionsManager#listVisibilityOptions()} returns an empty list when looking up components fails. */
    @Test
    public void listVisibilityOptionsWithLookupExceptions() throws ComponentLookupException
    {
        ComponentManager cm = this.mocker.getInstance(ComponentManager.class, "context");
        when(cm.<Visibility>getInstanceList(Visibility.class)).thenThrow(new ComponentLookupException("None"));
        Collection<Visibility> returnedVisibilities = this.mocker.getComponentUnderTest().listVisibilityOptions();
        Assert.assertTrue(returnedVisibilities.isEmpty());
    }

    /** {@link PermissionsManager#resolveVisibility(String)} returns the right implementation. */
    @Test
    public void resolveVisibility() throws ComponentLookupException
    {
        ComponentManager cm = this.mocker.getInstance(ComponentManager.class, "context");
        Visibility publicV = mock(Visibility.class);
        when(cm.getInstance(Visibility.class, "public")).thenReturn(publicV);
        Assert.assertSame(publicV, this.mocker.getComponentUnderTest().resolveVisibility("public"));
    }

    /** {@link PermissionsManager#resolveVisibility(String)} returns null if a null or blank visibility is requested. */
    @Test
    public void resolveVisibilityWithNoAccess() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().resolveVisibility(null));
        Assert.assertNull(this.mocker.getComponentUnderTest().resolveVisibility(""));
        Assert.assertNull(this.mocker.getComponentUnderTest().resolveVisibility(" "));
    }

    /** {@link PermissionsManager#resolveVisibility(String)} returns null if an unknown visibility is requested. */
    @Test
    public void resolveVisibilityWithUnknownVisibilityTest() throws ComponentLookupException
    {
        ComponentManager cm = this.mocker.getInstance(ComponentManager.class, "context");
        when(cm.getInstance(Visibility.class, "unknown")).thenThrow(new ComponentLookupException("No such component"));
        Assert.assertNull(this.mocker.getComponentUnderTest().resolveVisibility("unknown"));
    }

    /** {@link PermissionsManager#getPatientAccess(Patient)} returns a {@link DefaultPatientAccess}. */
    @Test
    public void getPatientAccess() throws ComponentLookupException
    {
        ComponentManager cm = this.mocker.getInstance(ComponentManager.class, "context");
        Patient patient = mock(Patient.class);
        PatientAccessHelper helper = mock(PatientAccessHelper.class);
        when(cm.getInstance(PatientAccessHelper.class)).thenReturn(helper);
        PatientAccess result = this.mocker.getComponentUnderTest().getPatientAccess(patient);
        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof DefaultPatientAccess);
    }

    /** {@link PermissionsManager#getPatientAccess(Patient)} returns a {@link DefaultPatientAccess}. */
    @Test
    public void getPatientAccessWithMissingHelper() throws ComponentLookupException
    {
        ComponentManager cm = this.mocker.getInstance(ComponentManager.class, "context");
        Patient patient = mock(Patient.class);
        when(cm.getInstance(PatientAccessHelper.class)).thenThrow(new ComponentLookupException("Missing"));
        PatientAccess result = this.mocker.getComponentUnderTest().getPatientAccess(patient);
        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof DefaultPatientAccess);
    }

    @Test
    public void filterByVisibilityWithEmptyInputReturnsEmptyList() throws ComponentLookupException
    {
        Collection<Patient> result = this.mocker.getComponentUnderTest()
            .filterByVisibility(Collections.<Patient>emptyList(), new PrivateVisibility());
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void filterByVisibilityWithNullInputReturnsEmptyList() throws ComponentLookupException
    {
        Collection<Patient> result = this.mocker.getComponentUnderTest()
            .filterByVisibility(null, new PrivateVisibility());
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void filterByVisibilityWithValidInputFiltersPatients() throws ComponentLookupException
    {
        ComponentManager cm = this.mocker.getInstance(ComponentManager.class, "context");
        PatientAccessHelper helper = mock(PatientAccessHelper.class);
        when(cm.getInstance(PatientAccessHelper.class)).thenReturn(helper);

        Collection<Patient> input = new ArrayList<>();
        Patient p1 = mock(Patient.class);
        when(helper.getVisibility(p1)).thenReturn(new PublicVisibility());
        input.add(p1);
        Patient p2 = mock(Patient.class);
        when(helper.getVisibility(p2)).thenReturn(new HiddenVisibility());
        input.add(p2);
        Patient p3 = mock(Patient.class);
        when(helper.getVisibility(p3)).thenReturn(new PrivateVisibility());
        input.add(p3);

        Collection<Patient> result = this.mocker.getComponentUnderTest()
            .filterByVisibility(input, new PrivateVisibility());
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        Iterator<Patient> it = result.iterator();
        Assert.assertSame(p1, it.next());
        Assert.assertSame(p3, it.next());
    }

    @Test
    public void filterByVisibilityWithNullInInputFiltersValidPatients() throws ComponentLookupException
    {
        ComponentManager cm = this.mocker.getInstance(ComponentManager.class, "context");
        PatientAccessHelper helper = mock(PatientAccessHelper.class);
        when(cm.getInstance(PatientAccessHelper.class)).thenReturn(helper);

        Collection<Patient> input = new ArrayList<>();
        Patient p1 = mock(Patient.class);
        when(helper.getVisibility(p1)).thenReturn(new PublicVisibility());
        input.add(p1);
        Patient p2 = mock(Patient.class);
        when(helper.getVisibility(p2)).thenReturn(new HiddenVisibility());
        input.add(p2);
        input.add(null);
        Patient p3 = mock(Patient.class);
        when(helper.getVisibility(p3)).thenReturn(new PrivateVisibility());
        input.add(p3);

        Collection<Patient> result = this.mocker.getComponentUnderTest()
            .filterByVisibility(input, new PrivateVisibility());
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        Iterator<Patient> it = result.iterator();
        Assert.assertSame(p1, it.next());
        Assert.assertSame(p3, it.next());
    }

    @Test
    public void filterByVisibilityWithNonMatchingInputReturnsEmptyList() throws ComponentLookupException
    {
        ComponentManager cm = this.mocker.getInstance(ComponentManager.class, "context");
        PatientAccessHelper helper = mock(PatientAccessHelper.class);
        when(cm.getInstance(PatientAccessHelper.class)).thenReturn(helper);

        Collection<Patient> input = new ArrayList<>();
        Patient p1 = mock(Patient.class);
        when(helper.getVisibility(p1)).thenReturn(new HiddenVisibility());
        input.add(p1);
        Patient p2 = mock(Patient.class);
        when(helper.getVisibility(p2)).thenReturn(new HiddenVisibility());
        input.add(p2);
        input.add(null);
        Patient p3 = mock(Patient.class);
        when(helper.getVisibility(p3)).thenReturn(new PrivateVisibility());
        input.add(p3);

        Collection<Patient> result = this.mocker.getComponentUnderTest()
            .filterByVisibility(input, new PublicVisibility());
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void filterByVisibilityWithNullThresholdReturnsUnfilteredList() throws ComponentLookupException
    {
        ComponentManager cm = this.mocker.getInstance(ComponentManager.class, "context");
        PatientAccessHelper helper = mock(PatientAccessHelper.class);
        when(cm.getInstance(PatientAccessHelper.class)).thenReturn(helper);

        Collection<Patient> input = new ArrayList<>();
        Patient p1 = mock(Patient.class);
        when(helper.getVisibility(p1)).thenReturn(new PublicVisibility());
        input.add(p1);
        Patient p2 = mock(Patient.class);
        when(helper.getVisibility(p2)).thenReturn(new HiddenVisibility());
        input.add(p2);
        input.add(null);
        Patient p3 = mock(Patient.class);
        when(helper.getVisibility(p3)).thenReturn(new PrivateVisibility());
        input.add(p3);

        Collection<Patient> result = this.mocker.getComponentUnderTest()
            .filterByVisibility(input, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(4, result.size());
        Iterator<Patient> it = result.iterator();
        Assert.assertSame(p1, it.next());
        Assert.assertSame(p2, it.next());
        Assert.assertNull(it.next());
        Assert.assertSame(p3, it.next());
    }
}
