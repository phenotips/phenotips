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
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.data.permissions.Owner;
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.data.permissions.internal.access.AccessHelper;
import org.phenotips.data.permissions.internal.access.EditAccessLevel;
import org.phenotips.data.permissions.internal.access.ManageAccessLevel;
import org.phenotips.data.permissions.internal.access.NoAccessLevel;
import org.phenotips.data.permissions.internal.access.OwnerAccessLevel;
import org.phenotips.data.permissions.internal.access.ViewAccessLevel;
import org.phenotips.data.permissions.internal.visibility.VisibilityHelper;

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
 * Tests for the default {@link PatientAccess} implementation, {@link DefaultPatientAccess}.
 *
 * @version $Id$
 */
public class DefaultPatientAccessTest
{
    /** The user used as the owner of the patient. */
    private static final DocumentReference OWNER = new DocumentReference("xwiki", "XWiki", "padams");

    /** The user used as the owner of the patient. */
    private static final Owner OWNER_OBJECT = new DefaultOwner(OWNER, mock(PermissionsHelper.class));

    /** The user used as a collaborator. */
    private static final DocumentReference COLLABORATOR = new DocumentReference("xwiki", "XWiki", "hmccoy");

    /** The user used as a non-collaborator. */
    private static final DocumentReference OTHER_USER = new DocumentReference("xwiki", "XWiki", "cxavier");

    @Mock
    private Visibility visibility;

    @Mock
    private Patient patient;

    @Mock
    private PermissionsHelper permissionsHelper;

    @Mock
    private AccessHelper accessHelper;

    @Mock
    private VisibilityHelper visibilityHelper;

    @Mock
    private DefaultPermissionsManager manager;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        when(this.accessHelper.getOwner(this.patient)).thenReturn(OWNER_OBJECT);
    }

    /** Basic tests for {@link PatientAccess#getPatient()}. */
    @Test
    public void getPatient() throws ComponentLookupException
    {
        PatientAccess pa = new DefaultPatientAccess(this.patient, this.permissionsHelper, this.accessHelper,
            this.visibilityHelper);
        Assert.assertSame(this.patient, pa.getPatient());
    }

    /** Basic tests for {@link PatientAccess#getOwner()}. */
    @Test
    public void getOwner() throws ComponentLookupException
    {
        PatientAccess pa = new DefaultPatientAccess(this.patient, this.permissionsHelper, this.accessHelper,
            this.visibilityHelper);
        Assert.assertSame(OWNER_OBJECT, pa.getOwner());
        Assert.assertSame(OWNER, pa.getOwner().getUser());
    }

    /** Basic tests for {@link PatientAccess#isOwner()}. */
    @Test
    public void isOwner() throws ComponentLookupException
    {
        when(this.permissionsHelper.getCurrentUser()).thenReturn(OWNER);
        PatientAccess pa = new DefaultPatientAccess(this.patient, this.permissionsHelper, this.accessHelper,
            this.visibilityHelper);
        Assert.assertTrue(pa.isOwner());

        when(this.permissionsHelper.getCurrentUser()).thenReturn(OTHER_USER);
        Assert.assertFalse(pa.isOwner());
    }

    /** {@link PatientAccess#isOwner()} with guest as the current user always returns false. */
    @Test
    public void isOwnerForGuests() throws ComponentLookupException
    {
        when(this.permissionsHelper.getCurrentUser()).thenReturn(null);
        PatientAccess pa = new DefaultPatientAccess(this.patient, this.permissionsHelper, this.accessHelper,
            this.visibilityHelper);
        Assert.assertFalse(pa.isOwner());

        // False even if the owner is guest as well
        when(this.accessHelper.getOwner(this.patient)).thenReturn(null);
        Assert.assertFalse(pa.isOwner());
    }

    /** Basic tests for {@link PatientAccess#isOwner(org.xwiki.model.reference.EntityReference)}. */
    @Test
    public void isOwnerWithSpecificUser() throws ComponentLookupException
    {
        PatientAccess pa = new DefaultPatientAccess(this.patient, this.permissionsHelper, this.accessHelper,
            this.visibilityHelper);
        Assert.assertTrue(pa.isOwner(OWNER));
        Assert.assertFalse(pa.isOwner(OTHER_USER));
        Assert.assertFalse(pa.isOwner(null));
    }

    /** Basic tests for {@link PatientAccess#setOwner(org.xwiki.model.reference.EntityReference)}. */
    @Test
    public void setOwner() throws ComponentLookupException
    {
        PatientAccess pa = new DefaultPatientAccess(this.patient, this.permissionsHelper, this.accessHelper,
            this.visibilityHelper);
        when(this.accessHelper.setOwner(this.patient, OTHER_USER)).thenReturn(true);
        Assert.assertTrue(pa.setOwner(OTHER_USER));
    }

    /** Basic tests for {@link PatientAccess#getVisibility()}. */
    @Test
    public void getVisibility() throws ComponentLookupException
    {
        when(this.visibilityHelper.getVisibility(this.patient)).thenReturn(this.visibility);
        PatientAccess pa = new DefaultPatientAccess(this.patient, this.permissionsHelper, this.accessHelper,
            this.visibilityHelper);
        Assert.assertSame(this.visibility, pa.getVisibility());
    }

    /** Basic tests for {@link PatientAccess#setVisibility(Visibility)}. */
    @Test
    public void setVisibility() throws ComponentLookupException
    {
        PatientAccess pa = new DefaultPatientAccess(this.patient, this.permissionsHelper, this.accessHelper,
            this.visibilityHelper);
        Visibility v = mock(Visibility.class);
        when(this.visibilityHelper.setVisibility(this.patient, v)).thenReturn(true);
        Assert.assertTrue(pa.setVisibility(v));
    }

    /** Basic tests for {@link PatientAccess#getCollaborators()}. */
    @Test
    public void getCollaborators() throws ComponentLookupException
    {
        PatientAccess pa = new DefaultPatientAccess(this.patient, this.permissionsHelper, this.accessHelper,
            this.visibilityHelper);
        Collection<Collaborator> collaborators = new HashSet<>();
        when(this.accessHelper.getCollaborators(this.patient)).thenReturn(collaborators);
        Assert.assertSame(collaborators, pa.getCollaborators());
    }

    /** Basic tests for {@link PatientAccess#updateCollaborators(Collection)}. */
    @Test
    public void updateCollaborators() throws ComponentLookupException
    {
        PatientAccess pa = new DefaultPatientAccess(this.patient, this.permissionsHelper, this.accessHelper,
            this.visibilityHelper);
        Collection<Collaborator> collaborators = new HashSet<>();
        when(this.accessHelper.setCollaborators(this.patient, collaborators)).thenReturn(true);
        Assert.assertTrue(pa.updateCollaborators(collaborators));
    }

    /** Basic tests for {@link PatientAccess#addCollaborator(org.xwiki.model.reference.EntityReference, AccessLevel)}. */
    @Test
    public void addCollaborator() throws ComponentLookupException
    {
        PatientAccess pa = new DefaultPatientAccess(this.patient, this.permissionsHelper, this.accessHelper,
            this.visibilityHelper);
        when(this.accessHelper.addCollaborator(Matchers.same(this.patient), Matchers.any(Collaborator.class)))
            .thenReturn(true);
        Assert.assertTrue(pa.addCollaborator(COLLABORATOR, mock(AccessLevel.class)));
    }

    /** Basic tests for {@link PatientAccess#removeCollaborator(Collaborator)}. */
    @Test
    public void removeCollaborator() throws ComponentLookupException
    {
        PatientAccess pa = new DefaultPatientAccess(this.patient, this.permissionsHelper, this.accessHelper,
            this.visibilityHelper);

        when(this.accessHelper.removeCollaborator(Matchers.same(this.patient), Matchers.any(Collaborator.class)))
            .thenReturn(true);
        Assert.assertTrue(pa.removeCollaborator(COLLABORATOR));

        Collaborator collaborator = mock(Collaborator.class);
        when(this.accessHelper.removeCollaborator(this.patient, collaborator)).thenReturn(true);
        Assert.assertTrue(pa.removeCollaborator(collaborator));
    }

    /** {@link PatientAccess#getAccessLevel()} returns the default visibility access for guest users. */
    @Test
    public void getAccessLevelWithGuestUser() throws ComponentLookupException
    {
        PatientAccess pa = new DefaultPatientAccess(this.patient, this.permissionsHelper, this.accessHelper,
            this.visibilityHelper);
        when(this.accessHelper.getOwner(this.patient)).thenReturn(null);
        when(this.permissionsHelper.getCurrentUser()).thenReturn(null);
        Visibility publicV = mock(Visibility.class);
        when(this.visibilityHelper.getVisibility(this.patient)).thenReturn(publicV);
        AccessLevel view = new ViewAccessLevel();
        when(publicV.getDefaultAccessLevel()).thenReturn(view);
        Assert.assertSame(view, pa.getAccessLevel());
    }

    /** {@link PatientAccess#getAccessLevel()} returns the default visibility access for guest users. */
    @Test
    public void getAccessLevelWithGuestUserAndPrivateVisibility() throws ComponentLookupException
    {
        PatientAccess pa = new DefaultPatientAccess(this.patient, this.permissionsHelper, this.accessHelper,
            this.visibilityHelper);
        when(this.accessHelper.getOwner(this.patient)).thenReturn(null);
        when(this.permissionsHelper.getCurrentUser()).thenReturn(null);
        Visibility privateV = mock(Visibility.class);
        when(this.visibilityHelper.getVisibility(this.patient)).thenReturn(privateV);
        AccessLevel none = new NoAccessLevel();
        when(privateV.getDefaultAccessLevel()).thenReturn(none);
        Assert.assertSame(none, pa.getAccessLevel());
    }

    /** {@link PatientAccess#getAccessLevel()} returns Owner access for the owner. */
    @Test
    public void getAccessLevelWithOwner() throws ComponentLookupException
    {
        PatientAccess pa = new DefaultPatientAccess(this.patient, this.permissionsHelper, this.accessHelper,
            this.visibilityHelper);
        when(this.accessHelper.getOwner(this.patient)).thenReturn(OWNER_OBJECT);
        when(this.permissionsHelper.getCurrentUser()).thenReturn(OWNER);
        AccessLevel owner = new OwnerAccessLevel();
        when(this.accessHelper.resolveAccessLevel("owner")).thenReturn(owner);
        Assert.assertSame(owner, pa.getAccessLevel());
    }

    /** {@link PatientAccess#getAccessLevel()} returns Owner access for site administrators. */
    @Test
    public void getAccessLevelWithAdministrator() throws ComponentLookupException
    {
        PatientAccess pa = new DefaultPatientAccess(this.patient, this.permissionsHelper, this.accessHelper,
            this.visibilityHelper);
        when(this.accessHelper.getOwner(this.patient)).thenReturn(OWNER_OBJECT);
        when(this.permissionsHelper.getCurrentUser()).thenReturn(OTHER_USER);
        when(this.accessHelper.isAdministrator(this.patient, OTHER_USER)).thenReturn(true);
        AccessLevel owner = new OwnerAccessLevel();
        when(this.accessHelper.resolveAccessLevel("owner")).thenReturn(owner);
        Assert.assertSame(owner, pa.getAccessLevel());
    }

    /** {@link PatientAccess#getAccessLevel()} returns the specified collaborator access for a collaborator. */
    @Test
    public void getAccessLevelWithCollaborator() throws ComponentLookupException
    {
        PatientAccess pa = new DefaultPatientAccess(this.patient, this.permissionsHelper, this.accessHelper,
            this.visibilityHelper);
        when(this.accessHelper.getOwner(this.patient)).thenReturn(OWNER_OBJECT);
        AccessLevel edit = new EditAccessLevel();
        when(this.accessHelper.isAdministrator(this.patient, COLLABORATOR)).thenReturn(false);
        when(this.permissionsHelper.getCurrentUser()).thenReturn(COLLABORATOR);
        when(this.accessHelper.getAccessLevel(this.patient, COLLABORATOR)).thenReturn(edit);
        Visibility publicV = mock(Visibility.class);
        when(this.visibilityHelper.getVisibility(this.patient)).thenReturn(publicV);
        AccessLevel view = new ViewAccessLevel();
        when(publicV.getDefaultAccessLevel()).thenReturn(view);
        Assert.assertSame(edit, pa.getAccessLevel());
    }

    /**
     * {@link PatientAccess#getAccessLevel(EntityReference)} returns the specified collaborator access for a
     * collaborator.
     */
    @Test
    public void getAccessLevelForCollaboratorAsAdministrator() throws ComponentLookupException
    {
        PatientAccess pa = new DefaultPatientAccess(this.patient, this.permissionsHelper, this.accessHelper,
            this.visibilityHelper);
        when(this.accessHelper.getOwner(this.patient)).thenReturn(OWNER_OBJECT);
        AccessLevel edit = new EditAccessLevel();
        when(this.accessHelper.isAdministrator(this.patient, COLLABORATOR)).thenReturn(false);
        when(this.permissionsHelper.getCurrentUser()).thenReturn(OTHER_USER);
        when(this.accessHelper.getAccessLevel(this.patient, COLLABORATOR)).thenReturn(edit);
        Visibility publicV = mock(Visibility.class);
        when(this.visibilityHelper.getVisibility(this.patient)).thenReturn(publicV);
        AccessLevel view = new ViewAccessLevel();
        when(publicV.getDefaultAccessLevel()).thenReturn(view);
        Assert.assertSame(edit, pa.getAccessLevel(COLLABORATOR));
    }

    /**
     * {@link PatientAccess#getAccessLevel(EntityReference)} returns owner access for an administrator.
     */
    @Test
    public void getAccessLevelForAdministratorAsCollaborator() throws ComponentLookupException
    {
        PatientAccess pa = new DefaultPatientAccess(this.patient, this.permissionsHelper, this.accessHelper,
            this.visibilityHelper);
        when(this.accessHelper.getOwner(this.patient)).thenReturn(OWNER_OBJECT);
        AccessLevel edit = new EditAccessLevel();
        when(this.accessHelper.isAdministrator(this.patient, OTHER_USER)).thenReturn(true);
        when(this.permissionsHelper.getCurrentUser()).thenReturn(COLLABORATOR);
        when(this.accessHelper.getAccessLevel(this.patient, OTHER_USER)).thenReturn(edit);
        Visibility publicV = mock(Visibility.class);
        when(this.visibilityHelper.getVisibility(this.patient)).thenReturn(publicV);
        AccessLevel owner = new OwnerAccessLevel();
        when(accessHelper.resolveAccessLevel("owner")).thenReturn(owner);
        Assert.assertSame(owner, pa.getAccessLevel(OTHER_USER));
    }

    /** {@link PatientAccess#getAccessLevel()} returns the default visibility access for non-collaborators. */
    @Test
    public void getAccessLevelWithOtherUser() throws ComponentLookupException
    {
        PatientAccess pa = new DefaultPatientAccess(this.patient, this.permissionsHelper, this.accessHelper,
            this.visibilityHelper);
        when(this.accessHelper.getOwner(this.patient)).thenReturn(OWNER_OBJECT);
        AccessLevel none = new NoAccessLevel();
        when(this.accessHelper.isAdministrator(this.patient, OTHER_USER)).thenReturn(false);
        when(this.permissionsHelper.getCurrentUser()).thenReturn(OTHER_USER);
        when(this.accessHelper.getAccessLevel(this.patient, OTHER_USER)).thenReturn(none);
        Visibility publicV = mock(Visibility.class);
        when(this.visibilityHelper.getVisibility(this.patient)).thenReturn(publicV);
        AccessLevel view = new ViewAccessLevel();
        when(publicV.getDefaultAccessLevel()).thenReturn(view);
        Assert.assertSame(view, pa.getAccessLevel());
    }

    /**
     * {@link PatientAccess#hasAccessLevel(AccessLevel)} returns {@code true} for lower or same access levels than the
     * current user's access, {@code false} for higher levels.
     */
    @Test
    public void hasAccessLevel()
    {
        PatientAccess pa = new DefaultPatientAccess(this.patient, this.permissionsHelper, this.accessHelper,
            this.visibilityHelper);
        when(this.accessHelper.getOwner(this.patient)).thenReturn(OWNER_OBJECT);
        when(permissionsHelper.getCurrentUser()).thenReturn(COLLABORATOR);
        AccessLevel edit = new EditAccessLevel();
        when(this.accessHelper.getAccessLevel(this.patient, COLLABORATOR)).thenReturn(edit);
        Visibility publicV = mock(Visibility.class);
        when(this.visibilityHelper.getVisibility(this.patient)).thenReturn(publicV);
        AccessLevel view = new ViewAccessLevel();
        when(publicV.getDefaultAccessLevel()).thenReturn(view);
        Assert.assertTrue(pa.hasAccessLevel(view));
        Assert.assertTrue(pa.hasAccessLevel(edit));
        Assert.assertFalse(pa.hasAccessLevel(new ManageAccessLevel()));
        Assert.assertFalse(pa.hasAccessLevel(new OwnerAccessLevel()));
    }

    /**
     * {@link PatientAccess#hasAccessLevel(AccessLevel)} returns {@code true} for lower or same access levels than the
     * specified user's access, {@code false} for higher levels.
     */
    @Test
    public void hasAccessLevelForOtherUser()
    {
        PatientAccess pa = new DefaultPatientAccess(this.patient, this.permissionsHelper, this.accessHelper,
            this.visibilityHelper);
        when(this.accessHelper.getOwner(this.patient)).thenReturn(OWNER_OBJECT);
        AccessLevel edit = new EditAccessLevel();
        when(this.accessHelper.getAccessLevel(this.patient, COLLABORATOR)).thenReturn(edit);
        Visibility publicV = mock(Visibility.class);
        when(this.visibilityHelper.getVisibility(this.patient)).thenReturn(publicV);
        AccessLevel view = new ViewAccessLevel();
        when(publicV.getDefaultAccessLevel()).thenReturn(view);
        Assert.assertTrue(pa.hasAccessLevel(COLLABORATOR, view));
        Assert.assertTrue(pa.hasAccessLevel(COLLABORATOR, edit));
        Assert.assertFalse(pa.hasAccessLevel(COLLABORATOR, new ManageAccessLevel()));
        Assert.assertFalse(pa.hasAccessLevel(COLLABORATOR, new OwnerAccessLevel()));
    }

    /** {@link PatientAccess#toString()} is customized. */
    @Test
    public void toStringTest()
    {
        PatientAccess pa = new DefaultPatientAccess(this.patient, null, null, null);
        when(this.patient.getDocumentReference()).thenReturn(new DocumentReference("xwiki", "data", "P123"));
        Assert.assertEquals("Access rules for xwiki:data.P123", pa.toString());
    }

    /** {@link PatientAccess#toString()} is customized. */
    @Test
    public void toStringWithNullPatient()
    {
        PatientAccess pa = new DefaultPatientAccess(null, null, null, null);
        Assert.assertEquals("Access rules for <unknown patient>", pa.toString());
    }
}
