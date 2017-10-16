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
import org.phenotips.data.permissions.internal.access.EditAccessLevel;
import org.phenotips.data.permissions.internal.access.ManageAccessLevel;
import org.phenotips.data.permissions.internal.access.NoAccessLevel;
import org.phenotips.data.permissions.internal.access.OwnerAccessLevel;
import org.phenotips.data.permissions.internal.access.ViewAccessLevel;
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
 * Tests for the default {@link EntityAccess} implementation, {@link DefaultEntityAccess}.
 *
 * @version $Id$
 */
public class DefaultEntityAccessTest
{
    /** The user used as the owner of the patient. */
    private static final DocumentReference OWNER = new DocumentReference("xwiki", "XWiki", "padams");

    /** The owner of the patient, when owned by OWNER. */
    private static final Owner OWNER_OBJECT = new DefaultOwner(OWNER, mock(EntityAccessHelper.class));

    /** The owner of the patient, when owned by guest. */
    private static final Owner GUEST_OWNER_OBJECT = new DefaultOwner(null, mock(EntityAccessHelper.class));

    /** The user used as a collaborator. */
    private static final DocumentReference COLLABORATOR = new DocumentReference("xwiki", "XWiki", "hmccoy");

    /** The user used as a non-collaborator. */
    private static final DocumentReference OTHER_USER = new DocumentReference("xwiki", "XWiki", "cxavier");

    private static final AccessLevel EDIT_ACCESS = new EditAccessLevel();

    private static final AccessLevel VIEW_ACCESS = new ViewAccessLevel();

    private static final AccessLevel OWNER_ACCESS = new OwnerAccessLevel();

    private static final AccessLevel NO_ACCESS = new NoAccessLevel();

    private static final AccessLevel MANAGE_ACCESS = new ManageAccessLevel();

    private static final String OWNER_LABEL = "owner";

    private static final String NONE_LABEL = "none";

    @Mock
    private Visibility visibility;

    @Mock
    private PrimaryEntity entity;

    @Mock
    private Collaborator collaborator;

    @Mock
    private EntityAccessHelper permissionsHelper;

    @Mock
    private EntityAccessManager accessManager;

    @Mock
    private EntityVisibilityManager visibilityManager;

    private EntityAccess entityAccess;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        this.entityAccess = new DefaultEntityAccess(this.entity, this.permissionsHelper, this.accessManager,
            this.visibilityManager);

        when(this.accessManager.getOwner(this.entity)).thenReturn(OWNER_OBJECT);
    }

    /** Basic tests for {@link EntityAccess#getEntity()}. */
    @Test
    public void getPatient()
    {
        Assert.assertSame(this.entity, this.entityAccess.getEntity());
    }

    /** Basic tests for {@link EntityAccess#getOwner()}. */
    @Test
    public void getOwner()
    {
        Assert.assertSame(OWNER_OBJECT, this.entityAccess.getOwner());
        Assert.assertSame(OWNER, this.entityAccess.getOwner().getUser());
    }

    @Test
    public void getOwnerWithGuestOwner()
    {
        when(this.accessManager.getOwner(this.entity)).thenReturn(GUEST_OWNER_OBJECT);
        EntityAccess pa = new DefaultEntityAccess(this.entity, this.permissionsHelper, this.accessManager,
            this.visibilityManager);
        Assert.assertSame(GUEST_OWNER_OBJECT, pa.getOwner());
        Assert.assertSame(null, pa.getOwner().getUser());
    }

    /** Basic tests for {@link EntityAccess#isOwner()}. */
    @Test
    public void isOwner()
    {
        when(this.permissionsHelper.getCurrentUser()).thenReturn(OWNER);
        Assert.assertTrue(this.entityAccess.isOwner());

        when(this.permissionsHelper.getCurrentUser()).thenReturn(OTHER_USER);
        Assert.assertFalse(this.entityAccess.isOwner());
    }

    /** {@link EntityAccess#isOwner()} with guest as the current user always returns false. */
    @Test
    public void isOwnerForGuests()
    {
        when(this.permissionsHelper.getCurrentUser()).thenReturn(null);
        Assert.assertFalse(this.entityAccess.isOwner());

        // False even if the owner cannot be computed
        when(this.accessManager.getOwner(this.entity)).thenReturn(null);
        Assert.assertFalse(this.entityAccess.isOwner());
    }

    /** {@link EntityAccess#isOwner()} with guest as the current user returns true if the owner is also guest. */
    @Test
    public void isOwnerWithGuestOwnerForGuests()
    {
        when(this.accessManager.getOwner(this.entity)).thenReturn(GUEST_OWNER_OBJECT);
        when(this.permissionsHelper.getCurrentUser()).thenReturn(null);
        Assert.assertTrue(this.entityAccess.isOwner());
    }

    /** {@link EntityAccess#isOwner()} for guest owners and a non-guest user returns false. */
    @Test
    public void isOwnerWithGuestOwnerForOtherUsers()
    {
        when(this.accessManager.getOwner(this.entity)).thenReturn(GUEST_OWNER_OBJECT);
        when(this.permissionsHelper.getCurrentUser()).thenReturn(OTHER_USER);
        Assert.assertFalse(this.entityAccess.isOwner());
    }

    /** Basic tests for {@link EntityAccess#isOwner(org.xwiki.model.reference.EntityReference)}. */
    @Test
    public void isOwnerWithSpecificUser()
    {
        Assert.assertTrue(this.entityAccess.isOwner(OWNER));
        Assert.assertFalse(this.entityAccess.isOwner(OTHER_USER));
        Assert.assertFalse(this.entityAccess.isOwner(null));
    }

    /** Basic tests for {@link EntityAccess#setOwner(org.xwiki.model.reference.EntityReference)}. */
    @Test
    public void setOwner()
    {
        when(this.accessManager.setOwner(this.entity, OTHER_USER)).thenReturn(true);
        Assert.assertTrue(this.entityAccess.setOwner(OTHER_USER));
    }

    /** Basic tests for {@link EntityAccess#getVisibility()}. */
    @Test
    public void getVisibility()
    {
        when(this.visibilityManager.getVisibility(this.entity)).thenReturn(this.visibility);
        Assert.assertSame(this.visibility, this.entityAccess.getVisibility());
    }

    /** Basic tests for {@link EntityAccess#setVisibility(Visibility)}. */
    @Test
    public void setVisibility()
    {
        when(this.visibilityManager.setVisibility(this.entity, this.visibility)).thenReturn(true);
        Assert.assertTrue(this.entityAccess.setVisibility(this.visibility));
    }

    /** Basic tests for {@link EntityAccess#getCollaborators()}. */
    @Test
    public void getCollaborators()
    {
        Collection<Collaborator> collaborators = new HashSet<>();
        when(this.accessManager.getCollaborators(this.entity)).thenReturn(collaborators);
        Assert.assertSame(collaborators, this.entityAccess.getCollaborators());
    }

    /** Basic tests for {@link EntityAccess#updateCollaborators(Collection)}. */
    @Test
    public void updateCollaborators()
    {
        Collection<Collaborator> collaborators = new HashSet<>();
        when(this.accessManager.setCollaborators(this.entity, collaborators)).thenReturn(true);
        Assert.assertTrue(this.entityAccess.updateCollaborators(collaborators));
    }

    /**
     * Basic tests for {@link EntityAccess#addCollaborator(org.xwiki.model.reference.EntityReference, AccessLevel)}.
     */
    @Test
    public void addCollaborator()
    {
        when(this.accessManager.addCollaborator(Matchers.same(this.entity), Matchers.any(Collaborator.class)))
            .thenReturn(true);
        Assert.assertTrue(this.entityAccess.addCollaborator(COLLABORATOR, mock(AccessLevel.class)));
    }

    /** Basic tests for {@link EntityAccess#removeCollaborator(Collaborator)}. */
    @Test
    public void removeCollaborator()
    {
        when(this.accessManager.removeCollaborator(Matchers.same(this.entity), Matchers.any(Collaborator.class)))
            .thenReturn(true);
        Assert.assertTrue(this.entityAccess.removeCollaborator(COLLABORATOR));

        when(this.accessManager.removeCollaborator(this.entity, this.collaborator)).thenReturn(true);
        Assert.assertTrue(this.entityAccess.removeCollaborator(this.collaborator));
    }

    /** {@link EntityAccess#getAccessLevel()} returns no access for guest users. */
    @Test
    public void getAccessLevelWithGuestUser()
    {
        when(this.permissionsHelper.getCurrentUser()).thenReturn(null);
        when(this.visibilityManager.getVisibility(this.entity)).thenReturn(this.visibility);
        AccessLevel view = new ViewAccessLevel();
        when(this.visibility.getDefaultAccessLevel()).thenReturn(view);
        AccessLevel none = new NoAccessLevel();
        when(this.accessManager.resolveAccessLevel(NONE_LABEL)).thenReturn(none);
        Assert.assertSame(none, this.entityAccess.getAccessLevel());
    }

    /** {@link EntityAccess#getAccessLevel()} returns the default visibility access for guest users. */
    @Test
    public void getAccessLevelWithGuestUserAndPrivateVisibility()
    {
        when(this.permissionsHelper.getCurrentUser()).thenReturn(null);
        when(this.visibilityManager.getVisibility(this.entity)).thenReturn(this.visibility);
        AccessLevel none = new NoAccessLevel();
        when(this.visibility.getDefaultAccessLevel()).thenReturn(none);
        when(this.accessManager.resolveAccessLevel(NONE_LABEL)).thenReturn(none);
        Assert.assertSame(none, this.entityAccess.getAccessLevel());
    }

    /** {@link EntityAccess#getAccessLevel()} returns owner access for guest users and guest owner. */
    @Test
    public void getAccessLevelWithGuestUserAndGuestOwner()
    {
        when(this.accessManager.getOwner(this.entity)).thenReturn(GUEST_OWNER_OBJECT);
        when(this.permissionsHelper.getCurrentUser()).thenReturn(null);
        Visibility privateV = mock(Visibility.class);
        when(this.visibilityManager.getVisibility(this.entity)).thenReturn(privateV);
        AccessLevel none = new NoAccessLevel();
        when(privateV.getDefaultAccessLevel()).thenReturn(none);
        AccessLevel owner = new OwnerAccessLevel();
        when(this.accessManager.resolveAccessLevel(OWNER_LABEL)).thenReturn(owner);
        Assert.assertSame(owner, this.entityAccess.getAccessLevel());
    }

    /** {@link EntityAccess#getAccessLevel()} returns Owner access for the owner. */
    @Test
    public void getAccessLevelWithOwner()
    {
        when(this.permissionsHelper.getCurrentUser()).thenReturn(OWNER);
        AccessLevel owner = new OwnerAccessLevel();
        when(this.accessManager.resolveAccessLevel(OWNER_LABEL)).thenReturn(owner);
        Assert.assertSame(owner, this.entityAccess.getAccessLevel());
    }

    /** {@link EntityAccess#getAccessLevel()} returns Owner access for site administrators. */
    @Test
    public void getAccessLevelWithAdministrator()
    {
        when(this.permissionsHelper.getCurrentUser()).thenReturn(OTHER_USER);
        when(this.accessManager.isAdministrator(this.entity, OTHER_USER)).thenReturn(true);
        AccessLevel owner = new OwnerAccessLevel();
        when(this.accessManager.resolveAccessLevel(OWNER_LABEL)).thenReturn(owner);
        Assert.assertSame(owner, this.entityAccess.getAccessLevel());
    }

    /** {@link EntityAccess#getAccessLevel()} returns the specified collaborator access for a collaborator. */
    @Test
    public void getAccessLevelWithCollaborator()
    {
        when(this.accessManager.isAdministrator(this.entity, COLLABORATOR)).thenReturn(false);
        when(this.permissionsHelper.getCurrentUser()).thenReturn(COLLABORATOR);
        when(this.accessManager.getAccessLevel(this.entity, COLLABORATOR)).thenReturn(EDIT_ACCESS);
        when(this.visibilityManager.getVisibility(this.entity)).thenReturn(this.visibility);
        when(this.visibility.getDefaultAccessLevel()).thenReturn(VIEW_ACCESS);
        Assert.assertSame(EDIT_ACCESS, this.entityAccess.getAccessLevel());
    }

    /**
     * {@link EntityAccess#getAccessLevel(EntityReference)} returns the specified collaborator access for a
     * collaborator.
     */
    @Test
    public void getAccessLevelForCollaboratorAsAdministrator() throws ComponentLookupException
    {
        when(this.accessManager.isAdministrator(this.entity, COLLABORATOR)).thenReturn(false);
        when(this.permissionsHelper.getCurrentUser()).thenReturn(OTHER_USER);
        when(this.accessManager.getAccessLevel(this.entity, COLLABORATOR)).thenReturn(EDIT_ACCESS);
        when(this.visibilityManager.getVisibility(this.entity)).thenReturn(this.visibility);
        when(this.visibility.getDefaultAccessLevel()).thenReturn(VIEW_ACCESS);
        Assert.assertSame(EDIT_ACCESS, this.entityAccess.getAccessLevel(COLLABORATOR));
    }

    /**
     * {@link EntityAccess#getAccessLevel(EntityReference)} returns owner access for an administrator.
     */
    @Test
    public void getAccessLevelForAdministratorAsCollaborator() throws ComponentLookupException
    {
        when(this.accessManager.isAdministrator(this.entity, OTHER_USER)).thenReturn(true);
        when(this.permissionsHelper.getCurrentUser()).thenReturn(COLLABORATOR);
        when(this.accessManager.getAccessLevel(this.entity, OTHER_USER)).thenReturn(EDIT_ACCESS);
        when(this.visibilityManager.getVisibility(this.entity)).thenReturn(this.visibility);
        when(this.accessManager.resolveAccessLevel(OWNER_LABEL)).thenReturn(OWNER_ACCESS);
        Assert.assertSame(OWNER_ACCESS, this.entityAccess.getAccessLevel(OTHER_USER));
    }

    /** {@link EntityAccess#getAccessLevel()} returns the default visibility access for non-collaborators. */
    @Test
    public void getAccessLevelWithOtherUser()
    {
        when(this.accessManager.isAdministrator(this.entity, OTHER_USER)).thenReturn(false);
        when(this.permissionsHelper.getCurrentUser()).thenReturn(OTHER_USER);
        when(this.accessManager.getAccessLevel(this.entity, OTHER_USER)).thenReturn(NO_ACCESS);
        when(this.visibilityManager.getVisibility(this.entity)).thenReturn(this.visibility);
        when(this.visibility.getDefaultAccessLevel()).thenReturn(VIEW_ACCESS);
        Assert.assertSame(VIEW_ACCESS, this.entityAccess.getAccessLevel());
    }

    /**
     * {@link EntityAccess#hasAccessLevel(AccessLevel)} returns {@code true} for lower or same access levels than the
     * current user's access, {@code false} for higher levels.
     */
    @Test
    public void hasAccessLevel()
    {
        when(this.permissionsHelper.getCurrentUser()).thenReturn(COLLABORATOR);
        when(this.accessManager.getAccessLevel(this.entity, COLLABORATOR)).thenReturn(EDIT_ACCESS);
        when(this.visibilityManager.getVisibility(this.entity)).thenReturn(this.visibility);
        when(this.visibility.getDefaultAccessLevel()).thenReturn(VIEW_ACCESS);
        Assert.assertTrue(this.entityAccess.hasAccessLevel(VIEW_ACCESS));
        Assert.assertTrue(this.entityAccess.hasAccessLevel(EDIT_ACCESS));
        Assert.assertFalse(this.entityAccess.hasAccessLevel(MANAGE_ACCESS));
        Assert.assertFalse(this.entityAccess.hasAccessLevel(OWNER_ACCESS));
    }

    /**
     * {@link EntityAccess#hasAccessLevel(AccessLevel)} returns {@code true} for lower or same access levels than the
     * specified user's access, {@code false} for higher levels.
     */
    @Test
    public void hasAccessLevelForOtherUser()
    {
        when(this.accessManager.getAccessLevel(this.entity, COLLABORATOR)).thenReturn(EDIT_ACCESS);
        when(this.visibilityManager.getVisibility(this.entity)).thenReturn(this.visibility);
        when(this.visibility.getDefaultAccessLevel()).thenReturn(VIEW_ACCESS);
        Assert.assertTrue(this.entityAccess.hasAccessLevel(COLLABORATOR, VIEW_ACCESS));
        Assert.assertTrue(this.entityAccess.hasAccessLevel(COLLABORATOR, EDIT_ACCESS));
        Assert.assertFalse(this.entityAccess.hasAccessLevel(COLLABORATOR, new ManageAccessLevel()));
        Assert.assertFalse(this.entityAccess.hasAccessLevel(COLLABORATOR, new OwnerAccessLevel()));
    }

    @Test
    public void hasAccessLevelForGuestUsers()
    {
        when(this.permissionsHelper.getCurrentUser()).thenReturn(null);
        when(this.accessManager.getAccessLevel(this.entity, COLLABORATOR)).thenReturn(EDIT_ACCESS);
        when(this.visibilityManager.getVisibility(this.entity)).thenReturn(this.visibility);
        when(this.visibility.getDefaultAccessLevel()).thenReturn(VIEW_ACCESS);
        when(this.accessManager.resolveAccessLevel(NONE_LABEL)).thenReturn(NO_ACCESS);
        Assert.assertFalse(this.entityAccess.hasAccessLevel(VIEW_ACCESS));
        Assert.assertFalse(this.entityAccess.hasAccessLevel(EDIT_ACCESS));
        Assert.assertFalse(this.entityAccess.hasAccessLevel(MANAGE_ACCESS));
        Assert.assertFalse(this.entityAccess.hasAccessLevel(OWNER_ACCESS));
    }

    @Test
    public void hasAccessLevelForGuestUsersAsOwners()
    {
        when(this.accessManager.getOwner(this.entity)).thenReturn(GUEST_OWNER_OBJECT);
        when(this.permissionsHelper.getCurrentUser()).thenReturn(null);
        when(this.visibilityManager.getVisibility(this.entity)).thenReturn(this.visibility);
        when(this.visibility.getDefaultAccessLevel()).thenReturn(VIEW_ACCESS);
        when(this.accessManager.resolveAccessLevel(NONE_LABEL)).thenReturn(NO_ACCESS);
        when(this.accessManager.resolveAccessLevel(OWNER_LABEL)).thenReturn(OWNER_ACCESS);
        Assert.assertTrue(this.entityAccess.hasAccessLevel(VIEW_ACCESS));
        Assert.assertTrue(this.entityAccess.hasAccessLevel(EDIT_ACCESS));
        Assert.assertTrue(this.entityAccess.hasAccessLevel(new ManageAccessLevel()));
        Assert.assertTrue(this.entityAccess.hasAccessLevel(new OwnerAccessLevel()));
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
        EntityAccess pa = new DefaultEntityAccess(null, this.permissionsHelper, this.accessManager,
            this.visibilityManager);
        Assert.assertEquals("Access rules for <unknown entity>", pa.toString());
    }
}
