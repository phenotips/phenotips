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
package org.phenotips.studies.family.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.internal.EntityAccessHelper;
import org.phenotips.security.authorization.AuthorizationModule;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.ManageRight;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

/**
 * Tests for the {@link AccessThroughFamilyMemberAuthorisationModule member granted access} {@link AuthorizationModule} component.
 *
 * @version $Id$
 */
public class AccessThroughFamilyMemberAuthorisationModuleTest
{
    @Rule
    public final MockitoComponentMockingRule<AuthorizationModule> mocker =
        new MockitoComponentMockingRule<>(AccessThroughFamilyMemberAuthorisationModule.class);

    @Mock
    private User user;

    @Mock
    private Family family;

    @Mock
    private Patient patient1;

    @Mock
    private AccessLevel access1;

    @Mock
    private Patient patient2;

    @Mock
    private AccessLevel access2;

    private EntityAccessHelper helper;

    private FamilyRepository repo;

    private DocumentReference doc = new DocumentReference("xwiki", "Families", "FAM01");

    @Mock
    private DocumentReference userProfile;

    @Before
    public void setupMocks() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.repo = this.mocker.getInstance(FamilyRepository.class);
        when(this.repo.get("xwiki:Families.FAM01")).thenReturn(this.family);
        when(this.family.getDocumentReference()).thenReturn(this.doc);
        when(this.family.getMembers()).thenReturn(Arrays.asList(this.patient1, this.patient2));

        this.helper = this.mocker.getInstance(EntityAccessHelper.class);

        when(this.user.getProfileDocument()).thenReturn(this.userProfile);

        when(this.helper.getAccessLevel(this.patient1, this.userProfile)).thenReturn(this.access1);
        when(this.helper.getAccessLevel(this.patient2, this.userProfile)).thenReturn(this.access2);
    }

    @Test
    public void viewAccessGrantedForMemberOwner() throws ComponentLookupException
    {
        when(this.access1.getGrantedRight()).thenReturn(Right.ILLEGAL);
        when(this.access2.getGrantedRight()).thenReturn(ManageRight.MANAGE);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.VIEW, this.doc));
    }

    @Test
    public void editAccessGrantedForMemberOwner() throws ComponentLookupException
    {
        when(this.access1.getGrantedRight()).thenReturn(ManageRight.MANAGE);
        when(this.access2.getGrantedRight()).thenReturn(Right.ILLEGAL);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.EDIT, this.doc));
    }

    @Test
    public void commentAccessNotGrantedForMemberOwner() throws ComponentLookupException
    {
        when(this.access1.getGrantedRight()).thenReturn(ManageRight.MANAGE);
        when(this.access2.getGrantedRight()).thenReturn(Right.ILLEGAL);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.COMMENT, this.doc));
    }

    @Test
    public void deleteAccessNotGrantedForMemberOwner() throws ComponentLookupException
    {
        when(this.access1.getGrantedRight()).thenReturn(Right.ILLEGAL);
        when(this.access2.getGrantedRight()).thenReturn(ManageRight.MANAGE);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.DELETE, this.doc));
    }

    @Test
    public void manageAccessNotGrantedForMemberOwner() throws ComponentLookupException
    {
        when(this.access1.getGrantedRight()).thenReturn(Right.ILLEGAL);
        when(this.access2.getGrantedRight()).thenReturn(ManageRight.MANAGE);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, ManageRight.MANAGE, this.doc));
    }

    @Test
    public void viewAccessGrantedForEditCollaborator() throws ComponentLookupException
    {
        when(this.access1.getGrantedRight()).thenReturn(Right.ILLEGAL);
        when(this.access2.getGrantedRight()).thenReturn(Right.EDIT);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.VIEW, this.doc));
    }

    @Test
    public void editAccessGrantedForEditCollaborator() throws ComponentLookupException
    {
        when(this.access1.getGrantedRight()).thenReturn(Right.EDIT);
        when(this.access2.getGrantedRight()).thenReturn(Right.ILLEGAL);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.EDIT, this.doc));
    }

    @Test
    public void commentAccessNotGrantedForEditCollaborator() throws ComponentLookupException
    {
        when(this.access1.getGrantedRight()).thenReturn(Right.EDIT);
        when(this.access2.getGrantedRight()).thenReturn(Right.ILLEGAL);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.COMMENT, this.doc));
    }

    @Test
    public void deleteAccessNotGrantedForEditCollaborator() throws ComponentLookupException
    {
        when(this.access1.getGrantedRight()).thenReturn(Right.ILLEGAL);
        when(this.access2.getGrantedRight()).thenReturn(Right.EDIT);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.DELETE, this.doc));
    }

    @Test
    public void manageAccessNotGrantedForEditCollaborator() throws ComponentLookupException
    {
        when(this.access1.getGrantedRight()).thenReturn(Right.VIEW);
        when(this.access2.getGrantedRight()).thenReturn(Right.EDIT);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, ManageRight.MANAGE, this.doc));
    }

    @Test
    public void viewAccessGrantedForViewCollaborator() throws ComponentLookupException
    {
        when(this.access1.getGrantedRight()).thenReturn(Right.ILLEGAL);
        when(this.access2.getGrantedRight()).thenReturn(Right.VIEW);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.VIEW, this.doc));
    }

    @Test
    public void editAccessNotGrantedForViewCollaborator() throws ComponentLookupException
    {
        when(this.access1.getGrantedRight()).thenReturn(Right.VIEW);
        when(this.access2.getGrantedRight()).thenReturn(Right.VIEW);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.EDIT, this.doc));
    }

    @Test
    public void commentAccessNotGrantedForViewCollaborator() throws ComponentLookupException
    {
        when(this.access1.getGrantedRight()).thenReturn(Right.VIEW);
        when(this.access2.getGrantedRight()).thenReturn(Right.ILLEGAL);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.COMMENT, this.doc));
    }

    @Test
    public void deleteAccessNotGrantedForViewCollaborator() throws ComponentLookupException
    {
        when(this.access1.getGrantedRight()).thenReturn(Right.ILLEGAL);
        when(this.access2.getGrantedRight()).thenReturn(Right.VIEW);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.DELETE, this.doc));
    }

    @Test
    public void manageAccessNotGrantedForViewCollaborator() throws ComponentLookupException
    {
        when(this.access1.getGrantedRight()).thenReturn(null);
        when(this.access2.getGrantedRight()).thenReturn(Right.VIEW);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, ManageRight.MANAGE, this.doc));
    }

    @Test
    public void viewAccessNotGrantedForNonCollaborator() throws ComponentLookupException
    {
        when(this.access1.getGrantedRight()).thenReturn(Right.ILLEGAL);
        when(this.access2.getGrantedRight()).thenReturn(Right.ILLEGAL);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.VIEW, this.doc));
    }

    @Test
    public void editAccessNotGrantedForNonCollaborator() throws ComponentLookupException
    {
        when(this.access1.getGrantedRight()).thenReturn(Right.ILLEGAL);
        when(this.access2.getGrantedRight()).thenReturn(null);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.EDIT, this.doc));
    }

    @Test
    public void commentAccessNotGrantedForNonCollaborator() throws ComponentLookupException
    {
        when(this.access1.getGrantedRight()).thenReturn(null);
        when(this.access2.getGrantedRight()).thenReturn(null);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.COMMENT, this.doc));
    }

    @Test
    public void deleteAccessNotGrantedForNonCollaborator() throws ComponentLookupException
    {
        when(this.access1.getGrantedRight()).thenReturn(Right.ILLEGAL);
        when(this.access2.getGrantedRight()).thenReturn(Right.ILLEGAL);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.DELETE, this.doc));
    }

    @Test
    public void manageAccessNotGrantedForNonCollaborator() throws ComponentLookupException
    {
        when(this.access1.getGrantedRight()).thenReturn(null);
        when(this.access2.getGrantedRight()).thenReturn(Right.ILLEGAL);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, ManageRight.MANAGE, this.doc));
    }

    @Test
    public void noActionWithNonFamily() throws ComponentLookupException
    {
        when(this.repo.get("xwiki:Families.FAM01")).thenReturn(null);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.VIEW, this.doc));
    }

    @Test
    public void noActionWithNonDocumentRight() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.REGISTER, this.doc));
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.PROGRAM, this.doc));
    }

    @Test
    public void noActionWithNullRight() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, null, this.doc));
    }

    @Test
    public void noActionWithNullDocument() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.VIEW, null));
    }

    @Test
    public void expectedPriority() throws ComponentLookupException
    {
        Assert.assertEquals(200, this.mocker.getComponentUnderTest().getPriority());
    }
}
