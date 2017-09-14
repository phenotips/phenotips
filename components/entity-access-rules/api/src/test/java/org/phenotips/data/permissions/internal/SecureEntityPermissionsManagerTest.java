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

import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.EntityAccess;
import org.phenotips.data.permissions.EntityPermissionsManager;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.data.permissions.internal.access.EditAccessLevel;
import org.phenotips.data.permissions.internal.visibility.PublicVisibility;
import org.phenotips.entities.PrimaryEntity;

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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the secure {@link EntityPermissionsManager} implementation, {@link SecureEntityPermissionsManager}.
 *
 * @version $Id$
 */
public class SecureEntityPermissionsManagerTest
{
    @Rule
    public final MockitoComponentMockingRule<EntityPermissionsManager> mocker =
        new MockitoComponentMockingRule<EntityPermissionsManager>(SecureEntityPermissionsManager.class);

    private AccessLevel edit = new EditAccessLevel();

    private Visibility publicVisibility = new PublicVisibility();

    @Test
    public void listAccessLevelsForwardsCalls() throws ComponentLookupException
    {
        EntityPermissionsManager internal = this.mocker.getInstance(EntityPermissionsManager.class);
        List<AccessLevel> levels = new ArrayList<>();
        when(internal.listAccessLevels()).thenReturn(levels);
        Collection<AccessLevel> returnedLevels = this.mocker.getComponentUnderTest().listAccessLevels();
        Assert.assertSame(levels, returnedLevels);
    }

    @Test
    public void resolveAccessLevelForwardsCalls() throws ComponentLookupException
    {
        EntityPermissionsManager internal = this.mocker.getInstance(EntityPermissionsManager.class);
        when(internal.resolveAccessLevel("edit")).thenReturn(this.edit);
        Assert.assertSame(this.edit, this.mocker.getComponentUnderTest().resolveAccessLevel("edit"));
    }

    @Test
    public void listVisibilityOptionsForwardsCalls() throws ComponentLookupException
    {
        EntityPermissionsManager internal = this.mocker.getInstance(EntityPermissionsManager.class);
        List<Visibility> visibilities = new ArrayList<>();
        when(internal.listVisibilityOptions()).thenReturn(visibilities);
        Collection<Visibility> returnedVisibilities = this.mocker.getComponentUnderTest().listVisibilityOptions();
        Assert.assertSame(visibilities, returnedVisibilities);
    }

    @Test
    public void listAllVisibilityOptionsForwardsCalls() throws ComponentLookupException
    {
        EntityPermissionsManager internal = this.mocker.getInstance(EntityPermissionsManager.class);
        List<Visibility> visibilities = new ArrayList<>();
        when(internal.listAllVisibilityOptions()).thenReturn(visibilities);
        Collection<Visibility> returnedVisibilities = this.mocker.getComponentUnderTest().listAllVisibilityOptions();
        Assert.assertSame(visibilities, returnedVisibilities);
    }

    @Test
    public void getDefaultVisibilityForwardsCalls() throws ComponentLookupException
    {
        EntityPermissionsManager internal = this.mocker.getInstance(EntityPermissionsManager.class);
        when(internal.getDefaultVisibility()).thenReturn(this.publicVisibility);
        Visibility result = this.mocker.getComponentUnderTest().getDefaultVisibility();
        Assert.assertSame(this.publicVisibility, result);
    }

    @Test
    public void resolveVisibilityForwardsCalls() throws ComponentLookupException
    {
        EntityPermissionsManager internal = this.mocker.getInstance(EntityPermissionsManager.class);
        when(internal.resolveVisibility("public")).thenReturn(this.publicVisibility);
        Assert.assertSame(this.publicVisibility, this.mocker.getComponentUnderTest().resolveVisibility("public"));
    }

    @Test
    public void getPrimaryEntityAccessReturnsSecureAccess() throws ComponentLookupException
    {
        EntityPermissionsManager internal = this.mocker.getInstance(EntityPermissionsManager.class);
        PrimaryEntity entity = mock(PrimaryEntity.class);
        EntityAccess internalAccess = mock(EntityAccess.class);
        when(internal.getEntityAccess(entity)).thenReturn(internalAccess);
        EntityAccess result = this.mocker.getComponentUnderTest().getEntityAccess(entity);
        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof SecureEntityAccess);
    }

    @Test
    public void filterCollectionByVisibilityForwardsCalls() throws ComponentLookupException
    {
        EntityPermissionsManager internal = this.mocker.getInstance(EntityPermissionsManager.class);
        Collection<? extends PrimaryEntity> input = new ArrayList<>();
        doReturn(input).when(internal).filterByVisibility(input, this.publicVisibility);

        Collection<? extends PrimaryEntity> result =
            this.mocker.getComponentUnderTest().filterByVisibility(input, this.publicVisibility);
        Mockito.verify(internal).filterByVisibility(input, this.publicVisibility);
        Assert.assertSame(input, result);
    }

    @Test
    public void filterIteratorByVisibilityForwardsCalls() throws ComponentLookupException
    {
        EntityPermissionsManager internal = this.mocker.getInstance(EntityPermissionsManager.class);
        Iterator<? extends PrimaryEntity> input = new ArrayList<PrimaryEntity>().iterator();
        doReturn(input).when(internal).filterByVisibility(input, this.publicVisibility);

        Iterator<? extends PrimaryEntity> result = this.mocker.getComponentUnderTest().filterByVisibility(input,
            this.publicVisibility);
        Mockito.verify(internal).filterByVisibility(input, this.publicVisibility);
        Assert.assertSame(input, result);
    }
}
