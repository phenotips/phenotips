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
import org.phenotips.data.permissions.EntityAccess;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.data.permissions.internal.access.AccessHelper;
import org.phenotips.data.permissions.internal.access.EditAccessLevel;
import org.phenotips.data.permissions.internal.access.ManageAccessLevel;
import org.phenotips.data.permissions.internal.access.NoAccessLevel;
import org.phenotips.data.permissions.internal.access.OwnerAccessLevel;
import org.phenotips.data.permissions.internal.access.ViewAccessLevel;
import org.phenotips.data.permissions.internal.visibility.HiddenVisibility;
import org.phenotips.data.permissions.internal.visibility.MockVisibility;
import org.phenotips.data.permissions.internal.visibility.PrivateVisibility;
import org.phenotips.data.permissions.internal.visibility.PublicVisibility;
import org.phenotips.data.permissions.internal.visibility.VisibilityHelper;
import org.phenotips.entities.PrimaryEntity;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.*;
import static org.mockito.Matchers.eq;
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
        new MockitoComponentMockingRule<>(DefaultPermissionsManager.class);

    private AccessLevel none = new NoAccessLevel();

    private AccessLevel view = new ViewAccessLevel();

    private AccessLevel edit = new EditAccessLevel();

    private AccessLevel manage = new ManageAccessLevel();

    private AccessLevel owner = new OwnerAccessLevel();

    private Visibility privateVisibility = new MockVisibility("private", 0, this.none);

    private Visibility publicVisibility = new MockVisibility("public", 50, this.view);

    private Visibility disabledOpenVisibility = new MockVisibility("open", 80, this.edit, true);

    private AccessHelper accessHelper;

    private VisibilityHelper visibilityHelper;

    private DefaultPermissionsManager component;

    @Before
    public void setUp() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        this.component = (DefaultPermissionsManager) this.mocker.getComponentUnderTest();

        this.accessHelper = this.mocker.getInstance(AccessHelper.class);
        this.visibilityHelper = this.mocker.getInstance(VisibilityHelper.class);

        final List<AccessLevel> levels = Arrays.asList(this.view, this.edit, this.manage);
        final List<AccessLevel> allLevels = Arrays.asList(this.edit, this.none, this.owner, this.view, this.manage);
        when(this.accessHelper.listAccessLevels()).thenReturn(levels);
        when(this.accessHelper.listAllAccessLevels()).thenReturn(allLevels);

        final List<Visibility> visibilities = Arrays.asList(this.privateVisibility, this.publicVisibility);
        final List<Visibility> allVisibilities = Arrays.asList(this.privateVisibility, this.publicVisibility,
            this.disabledOpenVisibility);
        when(this.visibilityHelper.listVisibilityOptions()).thenReturn(visibilities);
        when(this.visibilityHelper.listAllVisibilityOptions()).thenReturn(allVisibilities);
    }

    /** Basic tests for {@link PermissionsManager#listAccessLevels()}. */
    @Test
    public void listAccessLevels() throws ComponentLookupException
    {
        Collection<AccessLevel> returnedLevels = this.component.listAccessLevels();
        Assert.assertEquals(3, returnedLevels.size());
        Iterator<AccessLevel> it = returnedLevels.iterator();
        Assert.assertSame(this.view, it.next());
        Assert.assertSame(this.edit, it.next());
        Assert.assertSame(this.manage, it.next());
        Assert.assertFalse(returnedLevels.contains(this.none));
        Assert.assertFalse(returnedLevels.contains(this.owner));
    }

    /** {@link PermissionsManager#listAccessLevels()} returns an empty list when no implementations available. */
    @Test
    public void listAccessLevelsWithNoComponents() throws ComponentLookupException
    {
        when(this.accessHelper.listAccessLevels()).thenReturn(Collections.emptyList());
        Collection<AccessLevel> returnedLevels = this.component.listAccessLevels();
        Assert.assertTrue(returnedLevels.isEmpty());
    }

    /** {@link PermissionsManager#resolveAccessLevel(String)} returns the right implementation. */
    @Test
    public void resolveAccessLevel() throws ComponentLookupException
    {
        AccessLevel edit = mock(AccessLevel.class);
        when(this.accessHelper.resolveAccessLevel("edit")).thenReturn(edit);
        Assert.assertSame(edit, this.component.resolveAccessLevel("edit"));
    }

    /** {@link PermissionsManager#resolveAccessLevel(String)} returns null if an unknown level is requested. */
    @Test
    public void resolveAccessLevelWithUnknownAccess() throws ComponentLookupException
    {
        when(this.accessHelper.resolveAccessLevel("unknown")).thenReturn(null);
        Assert.assertNull(this.mocker.getComponentUnderTest().resolveAccessLevel("unknown"));
    }

    /** {@link PermissionsManager#resolveAccessLevel(String)} returns null if a null or blank level is requested. */
    @Test
    public void resolveAccessLevelWithNoAccess() throws ComponentLookupException
    {
        Assert.assertNull(this.component.resolveAccessLevel(null));
        Assert.assertNull(this.component.resolveAccessLevel(""));
        Assert.assertNull(this.component.resolveAccessLevel(" "));
    }

    /** Basic test for {@link PermissionsManager#listVisibilityOptions()}. */
    @Test
    public void listVisibilityOptionsSkipsDisabledVisibilitiesAndReordersByPriority() throws ComponentLookupException
    {
        Collection<Visibility> returnedVisibilities = this.component.listVisibilityOptions();
        Assert.assertEquals(2, returnedVisibilities.size());
        Iterator<Visibility> it = returnedVisibilities.iterator();
        Assert.assertSame(this.privateVisibility, it.next());
        Assert.assertSame(this.publicVisibility, it.next());
    }

    /** Basic test for {@link PermissionsManager#listAllVisibilityOptions()}. */
    @Test
    public void listAllVisibilityOptionsReordersByPriority() throws ComponentLookupException
    {
        Collection<Visibility> returnedVisibilities = this.component.listAllVisibilityOptions();
        Assert.assertEquals(3, returnedVisibilities.size());
        Iterator<Visibility> it = returnedVisibilities.iterator();
        Assert.assertSame(this.privateVisibility, it.next());
        Assert.assertSame(this.publicVisibility, it.next());
        Assert.assertSame(this.disabledOpenVisibility, it.next());
    }

    /** {@link PermissionsManager#listVisibilityOptions()} returns an empty list when no implementations available. */
    @Test
    public void listVisibilityOptionsWithNoComponentsEmptyList() throws ComponentLookupException
    {
        when(this.visibilityHelper.listVisibilityOptions()).thenReturn(Collections.emptyList());
        Collection<Visibility> returnedVisibilities = this.component.listVisibilityOptions();
        Assert.assertTrue(returnedVisibilities.isEmpty());
    }

    /**
     * {@link PermissionsManager#listAllVisibilityOptions()} returns an empty list when no implementations available.
     */
    @Test
    public void listAllVisibilityOptionsWithNoComponentsReturnsEmptyList() throws ComponentLookupException
    {
        when(this.visibilityHelper.listAllVisibilityOptions()).thenReturn(Collections.emptyList());
        Collection<Visibility> returnedVisibilities = this.component.listAllVisibilityOptions();
        Assert.assertTrue(returnedVisibilities.isEmpty());
    }

    @Test
    public void getDefaultVisibilityForwardsCalls() throws ComponentLookupException
    {
        when(this.visibilityHelper.getDefaultVisibility()).thenReturn(this.publicVisibility);
        Assert.assertSame(this.publicVisibility, this.component.getDefaultVisibility());
    }

    /** {@link PermissionsManager#resolveVisibility(String)} returns the right implementation. */
    @Test
    public void resolveVisibility() throws ComponentLookupException
    {
        when(this.visibilityHelper.resolveVisibility("public")).thenReturn(this.publicVisibility);
        Assert.assertSame(this.publicVisibility, this.mocker.getComponentUnderTest().resolveVisibility("public"));
    }

    /** {@link PermissionsManager#resolveVisibility(String)} returns private visibility if a null or blank visibility is
     * requested. */
    @Test
    public void resolveVisibilityWithNoAccess() throws ComponentLookupException
    {
        when(this.visibilityHelper.resolveVisibility(null)).thenReturn(this.privateVisibility);
        when(this.visibilityHelper.resolveVisibility("")).thenReturn(this.privateVisibility);
        when(this.visibilityHelper.resolveVisibility(" ")).thenReturn(this.privateVisibility);
        Assert.assertSame(this.privateVisibility, this.component.resolveVisibility(null));
        Assert.assertSame(this.privateVisibility, this.component.resolveVisibility(""));
        Assert.assertSame(this.privateVisibility, this.component.resolveVisibility(" "));
    }

    /** {@link PermissionsManager#resolveVisibility(String)} returns private visibility if an unknown visibility is
     * requested. */
    @Test
    public void resolveVisibilityWithUnknownVisibilityTest() throws ComponentLookupException
    {
        when(this.visibilityHelper.resolveVisibility("unknown")).thenReturn(this.privateVisibility);
        Assert.assertSame(this.privateVisibility, this.component.resolveVisibility("unknown"));
    }

    /** {@link PermissionsManager#getEntityAccess(PrimaryEntity)} returns a {@link DefaultEntityAccess}. */
    @Test
    public void getEntityAccess() throws ComponentLookupException
    {
        Patient patient = mock(Patient.class);
        EntityAccess result = this.component.getEntityAccess(patient);
        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof DefaultEntityAccess);
    }

    /** {@link PermissionsManager#getEntityAccess(PrimaryEntity)} returns a {@link DefaultEntityAccess}. */
    @Test
    public void getEntityAccessWithMissingHelper() throws ComponentLookupException
    {
        Patient patient = mock(Patient.class);
        EntityAccess result = this.component.getEntityAccess(patient);
        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof DefaultEntityAccess);
    }

    @Test
    public void filterCollectionByVisibilityWithEmptyInputReturnsEmptyCollection() throws ComponentLookupException
    {
        when(this.visibilityHelper.filterByVisibility(eq(Collections.emptyList()), any(PrivateVisibility.class)))
            .thenReturn(Collections.emptyList());
        Collection<Patient> result = this.component.filterByVisibility(Collections.emptyList(),
            new PrivateVisibility());
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void filterVisibleWithEmptyInputReturnsEmptyCollection() throws ComponentLookupException
    {
        when(this.visibilityHelper.filterByVisibility(eq(Collections.emptyList()), any(PrivateVisibility.class)))
            .thenReturn(Collections.emptyList());
        Collection<PrimaryEntity> result = this.component.filterVisible(Collections.emptyList(),
            new PrivateVisibility());
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void filterIteratorByVisibilityWithEmptyInputReturnsEmptyIterator() throws ComponentLookupException
    {
        when(this.visibilityHelper.filterByVisibility(eq(Collections.emptyIterator()), any(PrivateVisibility.class)))
            .thenReturn(Collections.emptyIterator());
        Iterator<Patient> result = this.component.filterByVisibility(Collections.emptyIterator(),
            new PrivateVisibility());
        Assert.assertNotNull(result);
        Assert.assertFalse(result.hasNext());
    }

    @Test
    public void filterVisibleIteratorWithEmptyInputReturnsEmptyIterator() throws ComponentLookupException
    {
        when(this.visibilityHelper.filterByVisibility(eq(Collections.emptyIterator()), any(PrivateVisibility.class)))
            .thenReturn(Collections.emptyIterator());
        Iterator<PrimaryEntity> result = this.component.filterVisible(Collections.emptyIterator(),
            new PrivateVisibility());
        Assert.assertNotNull(result);
        Assert.assertFalse(result.hasNext());
    }

    @Test
    public void filterCollectionByVisibilityWithNullInputReturnsEmptyCollection() throws ComponentLookupException
    {
        when(this.visibilityHelper.filterByVisibility((Collection<PrimaryEntity>) null, this.privateVisibility))
            .thenReturn(Collections.emptyList());
        Collection<Patient> result = this.component.filterByVisibility((Collection<Patient>) null,
           this.privateVisibility);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void filterVisibleWithNullInputReturnsEmptyCollection() throws ComponentLookupException
    {
        when(this.visibilityHelper.filterByVisibility((Collection<PrimaryEntity>) null, this.privateVisibility))
            .thenReturn(Collections.emptyList());
        Collection<PrimaryEntity> result = this.component.filterVisible((Collection<PrimaryEntity>) null,
            this.privateVisibility);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void filterIteratorByVisibilityWithNullInputReturnsEmptyIterator() throws ComponentLookupException
    {
        when(this.visibilityHelper.filterByVisibility((Iterator<PrimaryEntity>) null, this.privateVisibility))
            .thenReturn(Collections.emptyIterator());
        Iterator<Patient> result = this.component.filterByVisibility((Iterator<Patient>) null, this.privateVisibility);
        Assert.assertNotNull(result);
        Assert.assertFalse(result.hasNext());
    }

    @Test
    public void filterVisibleIteratorWithNullInputReturnsEmptyIterator() throws ComponentLookupException
    {
        when(this.visibilityHelper.filterByVisibility((Iterator<PrimaryEntity>) null, this.privateVisibility))
            .thenReturn(Collections.emptyIterator());
        Iterator<PrimaryEntity> result = this.component.filterVisible((Iterator<PrimaryEntity>) null,
            this.privateVisibility);
        Assert.assertNotNull(result);
        Assert.assertFalse(result.hasNext());
    }

    @Test
    public void filterCollectionByVisibilityWithValidInputFiltersPatients() throws ComponentLookupException
    {
        Collection<Patient> input = new ArrayList<>();
        Patient p1 = mock(Patient.class);
        when(this.visibilityHelper.getVisibility(p1)).thenReturn(new PublicVisibility());
        input.add(p1);
        Patient p2 = mock(Patient.class);
        when(this.visibilityHelper.getVisibility(p2)).thenReturn(new HiddenVisibility());
        input.add(p2);
        Patient p3 = mock(Patient.class);
        when(this.visibilityHelper.getVisibility(p3)).thenReturn(new PrivateVisibility());
        input.add(p3);

        final Collection<PrimaryEntity> recastInput = (Collection<PrimaryEntity>) (Collection<?>) input;
        when(this.visibilityHelper.filterByVisibility(recastInput, this.privateVisibility))
            .thenReturn(Arrays.asList(p1, p3));
        Collection<Patient> result = this.component.filterByVisibility(input, this.privateVisibility);
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        Iterator<Patient> it = result.iterator();
        Assert.assertSame(p1, it.next());
        Assert.assertSame(p3, it.next());
    }

    @Test
    public void filterVisibleWithValidInputFiltersPatients() throws ComponentLookupException
    {
        Collection<PrimaryEntity> input = new ArrayList<>();
        Patient p1 = mock(Patient.class);
        when(this.visibilityHelper.getVisibility(p1)).thenReturn(new PublicVisibility());
        input.add(p1);
        Patient p2 = mock(Patient.class);
        when(this.visibilityHelper.getVisibility(p2)).thenReturn(new HiddenVisibility());
        input.add(p2);
        Patient p3 = mock(Patient.class);
        when(this.visibilityHelper.getVisibility(p3)).thenReturn(new PrivateVisibility());
        input.add(p3);

        when(this.visibilityHelper.filterByVisibility(input, this.privateVisibility))
            .thenReturn(Arrays.asList(p1, p3));
        Collection<PrimaryEntity> result = this.component.filterVisible(input, this.privateVisibility);
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        Iterator<PrimaryEntity> it = result.iterator();
        Assert.assertSame(p1, it.next());
        Assert.assertSame(p3, it.next());
    }

    @Test
    public void filterIteratorByVisibilityWithValidInputFiltersPatients() throws ComponentLookupException
    {
        Collection<Patient> input = new ArrayList<>();
        Patient p1 = mock(Patient.class);
        when(this.visibilityHelper.getVisibility(p1)).thenReturn(new PublicVisibility());
        input.add(p1);
        Patient p2 = mock(Patient.class);
        when(this.visibilityHelper.getVisibility(p2)).thenReturn(new HiddenVisibility());
        input.add(p2);
        Patient p3 = mock(Patient.class);
        when(this.visibilityHelper.getVisibility(p3)).thenReturn(new PrivateVisibility());
        input.add(p3);

        final Iterator<Patient> inputIterator = input.iterator();
        final Iterator<PrimaryEntity> recastIterator = (Iterator<PrimaryEntity>) (Iterator<?>) inputIterator;
        when(this.visibilityHelper.filterByVisibility(recastIterator, this.privateVisibility))
            .thenReturn(Arrays.asList((PrimaryEntity) p1, (PrimaryEntity) p3).iterator());

        Iterator<Patient> result = this.component.filterByVisibility(inputIterator, this.privateVisibility);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.hasNext());
        Assert.assertSame(p1, result.next());
        Assert.assertTrue(result.hasNext());
        Assert.assertSame(p3, result.next());
        Assert.assertFalse(result.hasNext());
    }

    @Test
    public void filterVisibleIteratorWithValidInputFiltersPatients() throws ComponentLookupException
    {
        Collection<PrimaryEntity> input = new ArrayList<>();
        Patient p1 = mock(Patient.class);
        when(this.visibilityHelper.getVisibility(p1)).thenReturn(new PublicVisibility());
        input.add(p1);
        Patient p2 = mock(Patient.class);
        when(this.visibilityHelper.getVisibility(p2)).thenReturn(new HiddenVisibility());
        input.add(p2);
        Patient p3 = mock(Patient.class);
        when(this.visibilityHelper.getVisibility(p3)).thenReturn(new PrivateVisibility());
        input.add(p3);

        final Iterator<PrimaryEntity> inputIterator = input.iterator();
        when(this.visibilityHelper.filterByVisibility(inputIterator, this.privateVisibility))
            .thenReturn(Arrays.asList((PrimaryEntity) p1, (PrimaryEntity) p3).iterator());

        Iterator<PrimaryEntity> result = this.component.filterVisible(inputIterator, this.privateVisibility);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.hasNext());
        Assert.assertSame(p1, result.next());
        Assert.assertTrue(result.hasNext());
        Assert.assertSame(p3, result.next());
        Assert.assertFalse(result.hasNext());
    }
}
