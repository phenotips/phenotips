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
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.data.permissions.EntityAccess;
import org.phenotips.data.permissions.Owner;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.data.permissions.internal.access.AccessHelper;
import org.phenotips.data.permissions.internal.access.EditAccessLevel;
import org.phenotips.data.permissions.internal.access.ManageAccessLevel;
import org.phenotips.data.permissions.internal.access.NoAccessLevel;
import org.phenotips.data.permissions.internal.access.OwnerAccessLevel;
import org.phenotips.data.permissions.internal.access.ViewAccessLevel;
import org.phenotips.data.permissions.internal.visibility.VisibilityHelper;
import org.phenotips.entities.PrimaryEntity;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import java.util.Collection;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultEntityAccess}.
 */
public class DefaultEntityAccessTest
{
    /** The user used as the owner of the entity. */
    private static final DocumentReference OWNER = new DocumentReference("xwiki", "XWiki", "padams");

    /** The user used as the owner of the entity. */
    private static final Owner OWNER_OBJECT = new DefaultOwner(OWNER, mock(PermissionsHelper.class));

    /** The user used as a collaborator. */
    private static final DocumentReference COLLABORATOR = new DocumentReference("xwiki", "XWiki", "hmccoy");

    /** The user used as a non-collaborator. */
    private static final DocumentReference OTHER_USER = new DocumentReference("xwiki", "XWiki", "cxavier");

    @Mock
    private Visibility visibility;

    @Mock
    private PrimaryEntity entity;

    @Mock
    private PermissionsHelper permissionsHelper;

    @Mock
    private AccessHelper accessHelper;

    @Mock
    private VisibilityHelper visibilityHelper;

    private EntityAccess entityAccess;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        this.entityAccess = new DefaultEntityAccess(this.entity, this.permissionsHelper, this.accessHelper,
            this.visibilityHelper);
        when(this.accessHelper.getOwner(this.entity)).thenReturn(OWNER_OBJECT);
    }

    /** Basic tests for {@link EntityAccess#getEntity()}. */
    @Test
    public void getPatient() throws ComponentLookupException
    {
        Assert.assertSame(this.entity, this.entityAccess.getEntity());
    }

    /** Basic tests for {@link EntityAccess#getOwner()}. */
    @Test
    public void getOwner() throws ComponentLookupException
    {
        Assert.assertSame(OWNER_OBJECT, this.entityAccess.getOwner());
        Assert.assertSame(OWNER, this.entityAccess.getOwner().getUser());
    }

    /** Basic tests for {@link EntityAccess#isOwner()}. */
    @Test
    public void isOwner() throws ComponentLookupException
    {
        when(this.permissionsHelper.getCurrentUser()).thenReturn(OWNER);
        Assert.assertTrue(this.entityAccess.isOwner());

        when(this.permissionsHelper.getCurrentUser()).thenReturn(OTHER_USER);
        Assert.assertFalse(this.entityAccess.isOwner());
    }

    /** {@link EntityAccess#isOwner()} with guest as the current user always returns false. */
    @Test
    public void isOwnerForGuests() throws ComponentLookupException
    {
        when(this.permissionsHelper.getCurrentUser()).thenReturn(null);
        Assert.assertFalse(this.entityAccess.isOwner());

        // False even if the owner is guest as well
        when(this.accessHelper.getOwner(this.entity)).thenReturn(null);
        Assert.assertFalse(this.entityAccess.isOwner());
    }

    /** Basic tests for {@link EntityAccess#isOwner(org.xwiki.model.reference.EntityReference)}. */
    @Test
    public void isOwnerWithSpecificUser() throws ComponentLookupException
    {
        Assert.assertTrue(this.entityAccess.isOwner(OWNER));
        Assert.assertFalse(this.entityAccess.isOwner(OTHER_USER));
        Assert.assertFalse(this.entityAccess.isOwner(null));
    }

    /** Basic tests for {@link EntityAccess#setOwner(org.xwiki.model.reference.EntityReference)}. */
    @Test
    public void setOwner() throws ComponentLookupException
    {
        when(this.accessHelper.setOwner(this.entity, OTHER_USER)).thenReturn(true);
        Assert.assertTrue(this.entityAccess.setOwner(OTHER_USER));
    }

    /** Basic tests for {@link EntityAccess#getVisibility()}. */
    @Test
    public void getVisibility() throws ComponentLookupException
    {
        when(this.visibilityHelper.getVisibility(this.entity)).thenReturn(this.visibility);
        Assert.assertSame(this.visibility, this.entityAccess.getVisibility());
    }

    /** Basic tests for {@link EntityAccess#setVisibility(Visibility)}. */
    @Test
    public void setVisibility() throws ComponentLookupException
    {
        Visibility v = mock(Visibility.class);
        when(this.visibilityHelper.setVisibility(this.entity, v)).thenReturn(true);
        Assert.assertTrue(this.entityAccess.setVisibility(v));
    }

    /** Basic tests for {@link EntityAccess#getCollaborators()}. */
    @Test
    public void getCollaborators() throws ComponentLookupException
    {
        Collection<Collaborator> collaborators = new HashSet<>();
        when(this.accessHelper.getCollaborators(this.entity)).thenReturn(collaborators);
        Assert.assertSame(collaborators, this.entityAccess.getCollaborators());
    }

    /** Basic tests for {@link EntityAccess#updateCollaborators(Collection)}. */
    @Test
    public void updateCollaborators() throws ComponentLookupException
    {
        Collection<Collaborator> collaborators = new HashSet<>();
        when(this.accessHelper.setCollaborators(this.entity, collaborators)).thenReturn(true);
        Assert.assertTrue(this.entityAccess.updateCollaborators(collaborators));
    }

    /** Basic tests for {@link EntityAccess#addCollaborator(org.xwiki.model.reference.EntityReference, AccessLevel)}. */
    @Test
    public void addCollaborator() throws ComponentLookupException
    {
        when(this.accessHelper.addCollaborator(Matchers.same(this.entity), Matchers.any(Collaborator.class)))
            .thenReturn(true);
        Assert.assertTrue(this.entityAccess.addCollaborator(COLLABORATOR, mock(AccessLevel.class)));
    }

    /** Basic tests for {@link EntityAccess#removeCollaborator(Collaborator)}. */
    @Test
    public void removeCollaborator() throws ComponentLookupException
    {
        when(this.accessHelper.removeCollaborator(Matchers.same(this.entity), Matchers.any(Collaborator.class)))
            .thenReturn(true);
        Assert.assertTrue(this.entityAccess.removeCollaborator(COLLABORATOR));

        Collaborator collaborator = mock(Collaborator.class);
        when(this.accessHelper.removeCollaborator(this.entity, collaborator)).thenReturn(true);
        Assert.assertTrue(this.entityAccess.removeCollaborator(collaborator));
    }

    /** {@link EntityAccess#getAccessLevel()} returns the default visibility access for guest users. */
    @Test
    public void getAccessLevelWithGuestUser() throws ComponentLookupException
    {
        when(this.accessHelper.getOwner(this.entity)).thenReturn(null);
        when(this.permissionsHelper.getCurrentUser()).thenReturn(null);
        Visibility publicV = mock(Visibility.class);
        when(this.visibilityHelper.getVisibility(this.entity)).thenReturn(publicV);
        AccessLevel view = new ViewAccessLevel();
        when(publicV.getDefaultAccessLevel()).thenReturn(view);
        Assert.assertSame(view, this.entityAccess.getAccessLevel());
    }

    /** {@link EntityAccess#getAccessLevel()} returns the default visibility access for guest users. */
    @Test
    public void getAccessLevelWithGuestUserAndPrivateVisibility() throws ComponentLookupException
    {
        when(this.accessHelper.getOwner(this.entity)).thenReturn(null);
        when(this.permissionsHelper.getCurrentUser()).thenReturn(null);
        Visibility privateV = mock(Visibility.class);
        when(this.visibilityHelper.getVisibility(this.entity)).thenReturn(privateV);
        AccessLevel none = new NoAccessLevel();
        when(privateV.getDefaultAccessLevel()).thenReturn(none);
        Assert.assertSame(none, this.entityAccess.getAccessLevel());
    }

    /** {@link EntityAccess#getAccessLevel()} returns Owner access for the owner. */
    @Test
    public void getAccessLevelWithOwner() throws ComponentLookupException
    {
        when(this.accessHelper.getOwner(this.entity)).thenReturn(OWNER_OBJECT);
        when(this.permissionsHelper.getCurrentUser()).thenReturn(OWNER);
        AccessLevel owner = new OwnerAccessLevel();
        when(this.accessHelper.resolveAccessLevel("owner")).thenReturn(owner);
        Assert.assertSame(owner, this.entityAccess.getAccessLevel());
    }

    /** {@link EntityAccess#getAccessLevel()} returns Owner access for site administrators. */
    @Test
    public void getAccessLevelWithAdministrator() throws ComponentLookupException
    {
        when(this.accessHelper.getOwner(this.entity)).thenReturn(OWNER_OBJECT);
        when(this.permissionsHelper.getCurrentUser()).thenReturn(OTHER_USER);
        when(this.accessHelper.isAdministrator(this.entity, OTHER_USER)).thenReturn(true);
        AccessLevel owner = new OwnerAccessLevel();
        when(this.accessHelper.resolveAccessLevel("owner")).thenReturn(owner);
        Assert.assertSame(owner, this.entityAccess.getAccessLevel());
    }

    /** {@link EntityAccess#getAccessLevel()} returns the specified collaborator access for a collaborator. */
    @Test
    public void getAccessLevelWithCollaborator() throws ComponentLookupException
    {
        when(this.accessHelper.getOwner(this.entity)).thenReturn(OWNER_OBJECT);
        AccessLevel edit = new EditAccessLevel();
        when(this.accessHelper.isAdministrator(this.entity, COLLABORATOR)).thenReturn(false);
        when(this.permissionsHelper.getCurrentUser()).thenReturn(COLLABORATOR);
        when(this.accessHelper.getAccessLevel(this.entity, COLLABORATOR)).thenReturn(edit);
        Visibility publicV = mock(Visibility.class);
        when(this.visibilityHelper.getVisibility(this.entity)).thenReturn(publicV);
        AccessLevel view = new ViewAccessLevel();
        when(publicV.getDefaultAccessLevel()).thenReturn(view);
        Assert.assertSame(edit, this.entityAccess.getAccessLevel());
    }

    /**
     * {@link EntityAccess#getAccessLevel(EntityReference)} returns the specified collaborator access for a
     * collaborator.
     */
    @Test
    public void getAccessLevelForCollaboratorAsAdministrator() throws ComponentLookupException
    {
        when(this.accessHelper.getOwner(this.entity)).thenReturn(OWNER_OBJECT);
        AccessLevel edit = new EditAccessLevel();
        when(this.accessHelper.isAdministrator(this.entity, COLLABORATOR)).thenReturn(false);
        when(this.permissionsHelper.getCurrentUser()).thenReturn(OTHER_USER);
        when(this.accessHelper.getAccessLevel(this.entity, COLLABORATOR)).thenReturn(edit);
        Visibility publicV = mock(Visibility.class);
        when(this.visibilityHelper.getVisibility(this.entity)).thenReturn(publicV);
        AccessLevel view = new ViewAccessLevel();
        when(publicV.getDefaultAccessLevel()).thenReturn(view);
        Assert.assertSame(edit, this.entityAccess.getAccessLevel(COLLABORATOR));
    }

    /**
     * {@link EntityAccess#getAccessLevel(EntityReference)} returns owner access for an administrator.
     */
    @Test
    public void getAccessLevelForAdministratorAsCollaborator() throws ComponentLookupException
    {
        when(this.accessHelper.getOwner(this.entity)).thenReturn(OWNER_OBJECT);
        AccessLevel edit = new EditAccessLevel();
        when(this.accessHelper.isAdministrator(this.entity, OTHER_USER)).thenReturn(true);
        when(this.permissionsHelper.getCurrentUser()).thenReturn(COLLABORATOR);
        when(this.accessHelper.getAccessLevel(this.entity, OTHER_USER)).thenReturn(edit);
        Visibility publicV = mock(Visibility.class);
        when(this.visibilityHelper.getVisibility(this.entity)).thenReturn(publicV);
        AccessLevel owner = new OwnerAccessLevel();
        when(accessHelper.resolveAccessLevel("owner")).thenReturn(owner);
        Assert.assertSame(owner, this.entityAccess.getAccessLevel(OTHER_USER));
    }

    /** {@link EntityAccess#getAccessLevel()} returns the default visibility access for non-collaborators. */
    @Test
    public void getAccessLevelWithOtherUser() throws ComponentLookupException
    {
        when(this.accessHelper.getOwner(this.entity)).thenReturn(OWNER_OBJECT);
        AccessLevel none = new NoAccessLevel();
        when(this.accessHelper.isAdministrator(this.entity, OTHER_USER)).thenReturn(false);
        when(this.permissionsHelper.getCurrentUser()).thenReturn(OTHER_USER);
        when(this.accessHelper.getAccessLevel(this.entity, OTHER_USER)).thenReturn(none);
        Visibility publicV = mock(Visibility.class);
        when(this.visibilityHelper.getVisibility(this.entity)).thenReturn(publicV);
        AccessLevel view = new ViewAccessLevel();
        when(publicV.getDefaultAccessLevel()).thenReturn(view);
        Assert.assertSame(view, this.entityAccess.getAccessLevel());
    }

    /**
     * {@link EntityAccess#hasAccessLevel(AccessLevel)} returns {@code true} for lower or same access levels than the
     * current user's access, {@code false} for higher levels.
     */
    @Test
    public void hasAccessLevel()
    {
        when(this.accessHelper.getOwner(this.entity)).thenReturn(OWNER_OBJECT);
        when(permissionsHelper.getCurrentUser()).thenReturn(COLLABORATOR);
        AccessLevel edit = new EditAccessLevel();
        when(this.accessHelper.getAccessLevel(this.entity, COLLABORATOR)).thenReturn(edit);
        Visibility publicV = mock(Visibility.class);
        when(this.visibilityHelper.getVisibility(this.entity)).thenReturn(publicV);
        AccessLevel view = new ViewAccessLevel();
        when(publicV.getDefaultAccessLevel()).thenReturn(view);
        Assert.assertTrue(this.entityAccess.hasAccessLevel(view));
        Assert.assertTrue(this.entityAccess.hasAccessLevel(edit));
        Assert.assertFalse(this.entityAccess.hasAccessLevel(new ManageAccessLevel()));
        Assert.assertFalse(this.entityAccess.hasAccessLevel(new OwnerAccessLevel()));
    }

    /**
     * {@link EntityAccess#hasAccessLevel(AccessLevel)} returns {@code true} for lower or same access levels than the
     * specified user's access, {@code false} for higher levels.
     */
    @Test
    public void hasAccessLevelForOtherUser()
    {
        when(this.accessHelper.getOwner(this.entity)).thenReturn(OWNER_OBJECT);
        AccessLevel edit = new EditAccessLevel();
        when(this.accessHelper.getAccessLevel(this.entity, COLLABORATOR)).thenReturn(edit);
        Visibility publicV = mock(Visibility.class);
        when(this.visibilityHelper.getVisibility(this.entity)).thenReturn(publicV);
        AccessLevel view = new ViewAccessLevel();
        when(publicV.getDefaultAccessLevel()).thenReturn(view);
        Assert.assertTrue(this.entityAccess.hasAccessLevel(COLLABORATOR, view));
        Assert.assertTrue(this.entityAccess.hasAccessLevel(COLLABORATOR, edit));
        Assert.assertFalse(this.entityAccess.hasAccessLevel(COLLABORATOR, new ManageAccessLevel()));
        Assert.assertFalse(this.entityAccess.hasAccessLevel(COLLABORATOR, new OwnerAccessLevel()));
    }

    /** {@link EntityAccess#toString()} is customized. */
    @Test
    public void toStringTest()
    {
        when(this.entity.getDocumentReference()).thenReturn(new DocumentReference("xwiki", "data", "P123"));
        Assert.assertEquals("Access rules for xwiki:data.P123", this.entityAccess.toString());
    }

    /** {@link EntityAccess#toString()} is customized. */
    @Test
    public void toStringWithNullPatient()
    {
        final EntityAccess obj = new DefaultEntityAccess(null, null, null, null);
        Assert.assertEquals("Access rules for <unknown entity>", obj.toString());
    }}
