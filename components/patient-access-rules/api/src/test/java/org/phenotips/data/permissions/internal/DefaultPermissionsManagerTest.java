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
import org.phenotips.data.permissions.EntityPermissionsManager;
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsConfiguration;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.data.permissions.internal.access.EditAccessLevel;
import org.phenotips.data.permissions.internal.access.ManageAccessLevel;
import org.phenotips.data.permissions.internal.access.NoAccessLevel;
import org.phenotips.data.permissions.internal.access.OwnerAccessLevel;
import org.phenotips.data.permissions.internal.access.ViewAccessLevel;
import org.phenotips.data.permissions.internal.visibility.MockVisibility;
import org.phenotips.data.permissions.internal.visibility.PrivateVisibility;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
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
        new MockitoComponentMockingRule<PermissionsManager>(DefaultPermissionsManager.class, PermissionsManager.class);

    private AccessLevel none = new NoAccessLevel();

    private AccessLevel view = new ViewAccessLevel();

    private AccessLevel edit = new EditAccessLevel();

    private AccessLevel manage = new ManageAccessLevel();

    private AccessLevel owner = new OwnerAccessLevel();

    private Visibility privateVisibility = new MockVisibility("private", 0, this.none);

    private Visibility publicVisibility = new MockVisibility("public", 50, this.view);

    private EntityPermissionsManager entityPermissionsManager;

    @Before
    public void setUp() throws ComponentLookupException
    {
        this.entityPermissionsManager = this.mocker.getInstance(EntityPermissionsManager.class);
    }

    /** Basic tests for {@link PermissionsManager#listAccessLevels()}. */
    @Test
    public void listAccessLevels() throws ComponentLookupException
    {
        List<AccessLevel> levels = new ArrayList<>();
        levels.add(this.view);
        levels.add(this.edit);
        levels.add(this.manage);
        when(this.entityPermissionsManager.listAccessLevels()).thenReturn(levels);
        Collection<AccessLevel> returnedLevels = this.mocker.getComponentUnderTest().listAccessLevels();
        Assert.assertEquals(3, returnedLevels.size());
        Iterator<AccessLevel> it = returnedLevels.iterator();
        Assert.assertSame(this.view, it.next());
        Assert.assertSame(this.edit, it.next());
        Assert.assertSame(this.manage, it.next());
        Assert.assertFalse(returnedLevels.contains(this.none));
        Assert.assertFalse(returnedLevels.contains(this.owner));
    }

    /** {@link PermissionsManager#listAccessLevels()} returns an empty list when looking up components fails. */
    @Test
    public void listAccessLevelsWithNoAccessLevels() throws ComponentLookupException
    {
        when(this.entityPermissionsManager.listAccessLevels()).thenReturn(Collections.emptyList());
        Collection<AccessLevel> returnedLevels = this.mocker.getComponentUnderTest().listAccessLevels();
        Assert.assertTrue(returnedLevels.isEmpty());
    }

    /** {@link PermissionsManager#resolveAccessLevel(String)} returns the right implementation. */
    @Test
    public void resolveAccessLevel() throws ComponentLookupException
    {
        when(this.entityPermissionsManager.resolveAccessLevel("edit")).thenReturn(edit);
        Assert.assertSame(edit, this.mocker.getComponentUnderTest().resolveAccessLevel("edit"));
    }

    /** {@link PermissionsManager#resolveAccessLevel(String)} returns null if an unknown level is requested. */
    @Test
    public void resolveAccessLevelWithUnknownAccess() throws ComponentLookupException
    {
        when(this.entityPermissionsManager.resolveAccessLevel("unknown")).thenReturn(null);
        Assert.assertNull(this.mocker.getComponentUnderTest().resolveAccessLevel("unknown"));
    }

    /** {@link PermissionsManager#resolveAccessLevel(String)} returns null if a null or blank level is requested. */
    @Test
    public void resolveAccessLevelWithNoAccess() throws ComponentLookupException
    {
        when(this.entityPermissionsManager.resolveAccessLevel(null)).thenReturn(null);
        when(this.entityPermissionsManager.resolveAccessLevel("")).thenReturn(null);
        when(this.entityPermissionsManager.resolveAccessLevel(" ")).thenReturn(null);

        Assert.assertNull(this.mocker.getComponentUnderTest().resolveAccessLevel(null));
        Assert.assertNull(this.mocker.getComponentUnderTest().resolveAccessLevel(""));
        Assert.assertNull(this.mocker.getComponentUnderTest().resolveAccessLevel(" "));
    }

    /** Basic test for {@link PermissionsManager#listVisibilityOptions()}. */
    @Test
    public void listVisibilityOptionsWorksAsExpected() throws ComponentLookupException
    {
        List<Visibility> visibilities = new ArrayList<>();
        visibilities.add(this.privateVisibility);
        visibilities.add(this.publicVisibility);
        when(this.entityPermissionsManager.listVisibilityOptions()).thenReturn(visibilities);
        Collection<Visibility> returnedVisibilities = this.mocker.getComponentUnderTest().listVisibilityOptions();
        Assert.assertEquals(2, returnedVisibilities.size());
        Iterator<Visibility> it = returnedVisibilities.iterator();
        Assert.assertSame(this.privateVisibility, it.next());
        Assert.assertSame(this.publicVisibility, it.next());
    }

    /** {@link PermissionsManager#listVisibilityOptions()} returns an empty list when no implementations available. */
    @Test
    public void listVisibilityOptionsWithNoComponentsEmptyList() throws ComponentLookupException
    {
        when(this.entityPermissionsManager.listVisibilityOptions()).thenReturn(Collections.emptyList());
        Collection<Visibility> returnedVisibilities = this.mocker.getComponentUnderTest().listVisibilityOptions();
        Assert.assertTrue(returnedVisibilities.isEmpty());
    }

    @Test
    public void getDefaultVisibilityForwardsCalls() throws ComponentLookupException
    {
        PermissionsConfiguration config = this.mocker.getInstance(PermissionsConfiguration.class);
        when(config.getDefaultVisibility()).thenReturn("public");
        when(this.entityPermissionsManager.getDefaultVisibility()).thenReturn(this.publicVisibility);
        when(this.entityPermissionsManager.resolveVisibility("public")).thenReturn(this.publicVisibility);
        Assert.assertSame(this.publicVisibility, this.mocker.getComponentUnderTest().getDefaultVisibility());
    }

    /** {@link PermissionsManager#resolveVisibility(String)} returns the right implementation. */
    @Test
    public void resolveVisibility() throws ComponentLookupException
    {
        when(this.entityPermissionsManager.resolveVisibility("public")).thenReturn(this.publicVisibility);
        Assert.assertSame(this.publicVisibility, this.mocker.getComponentUnderTest().resolveVisibility("public"));
    }

    /** {@link PermissionsManager#resolveVisibility(String)} returns null if a null or blank visibility is requested. */
    @Test
    public void resolveVisibilityWithNoAccess() throws ComponentLookupException
    {
        when(this.entityPermissionsManager.resolveVisibility(null)).thenReturn(null);
        when(this.entityPermissionsManager.resolveVisibility("")).thenReturn(null);
        when(this.entityPermissionsManager.resolveVisibility(" ")).thenReturn(null);

        Assert.assertNull(this.mocker.getComponentUnderTest().resolveVisibility(null));
        Assert.assertNull(this.mocker.getComponentUnderTest().resolveVisibility(""));
        Assert.assertNull(this.mocker.getComponentUnderTest().resolveVisibility(" "));
    }

    /** {@link PermissionsManager#resolveVisibility(String)} returns null if an unknown visibility is requested. */
    @Test
    public void resolveVisibilityWithUnknownVisibilityTest() throws ComponentLookupException
    {
        when(this.entityPermissionsManager.resolveVisibility("unknown")).thenReturn(null);
        Assert.assertNull(this.mocker.getComponentUnderTest().resolveVisibility("unknown"));
    }

    /** {@link PermissionsManager#getPatientAccess(Patient)} returns a {@link DefaultPatientAccess}. */
    @Test
    public void getPatientAccess() throws ComponentLookupException
    {
        ComponentManager cm = this.mocker.getInstance(ComponentManager.class, "context");
        Patient patient = mock(Patient.class);
        EntityAccessHelper helper = mock(EntityAccessHelper.class);
        when(cm.getInstance(EntityAccessHelper.class)).thenReturn(helper);
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
        when(cm.getInstance(EntityAccessHelper.class)).thenThrow(new ComponentLookupException("Missing"));
        PatientAccess result = this.mocker.getComponentUnderTest().getPatientAccess(patient);
        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof DefaultPatientAccess);
    }

    @Test
    public void filterCollectionByVisibilityWithEmptyInputReturnsEmptyCollection() throws ComponentLookupException
    {
        final Visibility visibility = new PrivateVisibility();
        when(this.entityPermissionsManager.filterByVisibility(Collections.emptyList(), visibility))
            .thenReturn(Collections.emptyList());
        Collection<Patient> result = this.mocker.getComponentUnderTest()
            .filterByVisibility(Collections.emptyList(), visibility);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void filterIteratorByVisibilityWithEmptyInputReturnsEmptyIterator() throws ComponentLookupException
    {
        final Visibility visibility = new PrivateVisibility();
        when(this.entityPermissionsManager.filterByVisibility(Collections.emptyIterator(), visibility))
            .thenReturn(Collections.emptyIterator());
        Iterator<Patient> result = this.mocker.getComponentUnderTest()
            .filterByVisibility(Collections.emptyIterator(), visibility);
        Assert.assertNotNull(result);
        Assert.assertFalse(result.hasNext());
    }

    @Test
    public void filterCollectionByVisibilityWithNullInputReturnsEmptyCollection() throws ComponentLookupException
    {
        Collection<Patient> result = this.mocker.getComponentUnderTest()
            .filterByVisibility((Collection<Patient>) null, new PrivateVisibility());
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }
}
