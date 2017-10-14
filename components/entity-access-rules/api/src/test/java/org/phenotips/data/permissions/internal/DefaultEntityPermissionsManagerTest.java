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
import org.phenotips.data.permissions.EntityPermissionsManager;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.data.permissions.internal.access.EditAccessLevel;
import org.phenotips.data.permissions.internal.access.ViewAccessLevel;
import org.phenotips.data.permissions.internal.visibility.MockVisibility;
import org.phenotips.entities.PrimaryEntity;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the default {@link EntityPermissionsManager} implementation, {@link DefaultEntityPermissionsManager}.
 *
 * @version $Id$
 */
public class DefaultEntityPermissionsManagerTest
{
    private static final String PUBLIC = "public";

    private static final String OPEN = "open";

    private static final String EDIT = "edit";

    private static final AccessLevel VIEW_ACCESS = new ViewAccessLevel();

    private static final AccessLevel EDIT_ACCESS = new EditAccessLevel();

    private static final Visibility PUBLIC_VISIBILITY = new MockVisibility(PUBLIC, 50, VIEW_ACCESS, false);

    private static final Visibility DISABLED_OPEN_VISIBILITY = new MockVisibility(OPEN, 80, EDIT_ACCESS, true);

    @Rule
    public final MockitoComponentMockingRule<EntityPermissionsManager> mocker =
        new MockitoComponentMockingRule<>(DefaultEntityPermissionsManager.class);

    @Mock
    private Patient entity;

    private EntityPermissionsManager component;

    private EntityAccessManager accessManager;

    private EntityVisibilityManager visibilityManager;

    @Before
    public void setUp() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);

        this.component = this.mocker.getComponentUnderTest();

        this.accessManager = this.mocker.getInstance(EntityAccessManager.class);
        this.visibilityManager = this.mocker.getInstance(EntityVisibilityManager.class);
    }

    /** {@link EntityPermissionsManager#getEntityAccess(PrimaryEntity)} returns a {@link DefaultEntityAccess}. */
    @Test
    public void getPrimaryEntityAccess()
    {
        EntityAccess result = this.component.getEntityAccess(this.entity);
        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof DefaultEntityAccess);
    }

    /** {@link EntityPermissionsManager#getEntityAccess(PrimaryEntity)} returns a {@link DefaultEntityAccess}. */
    @Test
    public void getPrimaryEntityAccessWithMissingHelper()
    {
        EntityAccess result = this.component.getEntityAccess(this.entity);
        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof DefaultEntityAccess);
    }

    @Test
    public void listVisibilityOptionsForwardsCalls()
    {
        final List<Visibility> visibilities = Collections.singletonList(PUBLIC_VISIBILITY);
        when(this.visibilityManager.listVisibilityOptions()).thenReturn(visibilities);
        Assert.assertSame(visibilities, this.component.listVisibilityOptions());
        verify(this.visibilityManager, times(1)).listVisibilityOptions();
    }

    @Test
    public void listAllVisibilityOptionsForwardsCalls()
    {
        final List<Visibility> visibilities = Arrays.asList(PUBLIC_VISIBILITY, DISABLED_OPEN_VISIBILITY);
        when(this.visibilityManager.listAllVisibilityOptions()).thenReturn(visibilities);
        Assert.assertSame(visibilities, this.component.listAllVisibilityOptions());
        verify(this.visibilityManager, times(1)).listAllVisibilityOptions();
    }

    @Test
    public void getDefaultVisibilityForwardsCalls()
    {
        when(this.visibilityManager.getDefaultVisibility()).thenReturn(PUBLIC_VISIBILITY);
        Assert.assertSame(PUBLIC_VISIBILITY, this.component.getDefaultVisibility());
        verify(this.visibilityManager, times(1)).getDefaultVisibility();
    }

    @Test
    public void resolveVisibilityForwardsCalls()
    {
        when(this.visibilityManager.resolveVisibility(PUBLIC)).thenReturn(PUBLIC_VISIBILITY);
        Assert.assertSame(PUBLIC_VISIBILITY, this.component.resolveVisibility(PUBLIC));
        verify(this.visibilityManager, times(1)).resolveVisibility(PUBLIC);
    }

    @Test
    public void listAccessLevelsForwardsCalls()
    {
        final List<AccessLevel> levels = Collections.singletonList(VIEW_ACCESS);
        when(this.accessManager.listAccessLevels()).thenReturn(levels);
        Assert.assertSame(levels, this.component.listAccessLevels());
        verify(this.accessManager, times(1)).listAccessLevels();
    }

    @Test
    public void listAllAccessLevelsForwardsCalls()
    {
        final List<AccessLevel> levels = Arrays.asList(VIEW_ACCESS, EDIT_ACCESS);
        when(this.accessManager.listAllAccessLevels()).thenReturn(levels);
        Assert.assertSame(levels, this.component.listAllAccessLevels());
        verify(this.accessManager, times(1)).listAllAccessLevels();
    }

    @Test
    public void resolveAccessLevelForwardsCalls()
    {
        when(this.accessManager.resolveAccessLevel(EDIT)).thenReturn(EDIT_ACCESS);
        Assert.assertSame(EDIT_ACCESS, this.component.resolveAccessLevel(EDIT));
        verify(this.accessManager, times(1)).resolveAccessLevel(EDIT);
    }

    @Test
    public void filterCollectionByVisibilityForwardsCalls()
    {
        final Collection<PrimaryEntity> entities = Collections.singletonList(this.entity);
        when(this.visibilityManager.filterByVisibility(entities, PUBLIC_VISIBILITY))
            .thenReturn(Collections.emptyList());
        Assert.assertTrue(this.component.filterByVisibility(entities, PUBLIC_VISIBILITY).isEmpty());
        verify(this.visibilityManager, times(1)).filterByVisibility(entities, PUBLIC_VISIBILITY);
    }

    @Test
    public void filterIteratorByVisibilityForwardsCalls()
    {
        final Collection<PrimaryEntity> entities = Collections.singletonList(this.entity);
        final Iterator<PrimaryEntity> entityIterator = entities.iterator();
        when(this.visibilityManager.filterByVisibility(entityIterator, PUBLIC_VISIBILITY))
            .thenReturn(Collections.emptyIterator());
        Assert.assertFalse(this.component.filterByVisibility(entityIterator, PUBLIC_VISIBILITY).hasNext());
        verify(this.visibilityManager, times(1))
            .filterByVisibility(entityIterator, PUBLIC_VISIBILITY);

    }
}
