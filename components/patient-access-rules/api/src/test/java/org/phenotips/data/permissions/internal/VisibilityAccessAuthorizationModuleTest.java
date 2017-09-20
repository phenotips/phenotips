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
import org.phenotips.data.PatientRepository;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.security.authorization.AuthorizationModule;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;
import org.xwiki.users.internal.InvalidUser;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

/**
 * Tests for the {@link VisibilityAccessAuthorizationModule visibility granted access} {@link AuthorizationModule}
 * component.
 *
 * @version $Id$
 */
public class VisibilityAccessAuthorizationModuleTest
{
    @Rule
    public final MockitoComponentMockingRule<AuthorizationModule> mocker =
        new MockitoComponentMockingRule<>(VisibilityAccessAuthorizationModule.class);

    @Mock
    private User user;

    @Mock
    private Patient patient;

    @Mock
    private AccessLevel noneAccessLevel;

    @Mock
    private AccessLevel editAccessLevel;

    @Mock
    private AccessLevel viewAccessLevel;

    @Mock
    private Visibility openVisibility;

    @Mock
    private Visibility publicVisibility;

    @Mock
    private Visibility privateVisibility;

    private PatientAccessHelper helper;

    private PatientRepository repo;

    private DocumentReference doc = new DocumentReference("xwiki", "data", "P01");

    @Before
    public void setupMocks() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);

        this.helper = this.mocker.getInstance(PatientAccessHelper.class);
        this.repo = this.mocker.getInstance(PatientRepository.class);
        when(this.repo.get("xwiki:data.P01")).thenReturn(this.patient);

        when(this.openVisibility.getName()).thenReturn("open");
        when(this.openVisibility.getDefaultAccessLevel()).thenReturn(this.editAccessLevel);
        when(this.editAccessLevel.getGrantedRight()).thenReturn(Right.EDIT);

        when(this.publicVisibility.getName()).thenReturn("public");
        when(this.publicVisibility.getDefaultAccessLevel()).thenReturn(this.viewAccessLevel);
        when(this.viewAccessLevel.getGrantedRight()).thenReturn(Right.VIEW);

        when(this.privateVisibility.getName()).thenReturn("private");
        when(this.privateVisibility.getDefaultAccessLevel()).thenReturn(this.noneAccessLevel);
        when(this.noneAccessLevel.getGrantedRight()).thenReturn(Right.ILLEGAL);

        when(this.user.getProfileDocument()).thenReturn(new DocumentReference("xwiki", "Users", "padams"));
    }

    @Test
    public void openVisibilityAllowsView() throws ComponentLookupException
    {
        when(this.helper.getVisibility(this.patient)).thenReturn(this.openVisibility);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.VIEW, this.doc));
    }

    @Test
    public void openVisibilityAllowsEdit() throws ComponentLookupException
    {
        when(this.helper.getVisibility(this.patient)).thenReturn(this.openVisibility);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.EDIT, this.doc));
    }

    @Test
    public void openVisibilityDoesntAllowComment() throws ComponentLookupException
    {
        when(this.helper.getVisibility(this.patient)).thenReturn(this.openVisibility);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.COMMENT, this.doc));
    }

    @Test
    public void openVisibilityDoesntAllowDelete() throws ComponentLookupException
    {
        when(this.helper.getVisibility(this.patient)).thenReturn(this.openVisibility);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.DELETE, this.doc));
    }

    @Test
    public void openVisibilityDoesntAllowGuestAccess() throws ComponentLookupException
    {
        when(this.helper.getVisibility(this.patient)).thenReturn(this.openVisibility);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(null, Right.VIEW, this.doc));
    }

    @Test
    public void publicVisibilityAllowsView() throws ComponentLookupException
    {
        when(this.helper.getVisibility(this.patient)).thenReturn(this.publicVisibility);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.VIEW, this.doc));
    }

    @Test
    public void publicVisibilityDoesntAllowEdit() throws ComponentLookupException
    {
        when(this.helper.getVisibility(this.patient)).thenReturn(this.publicVisibility);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.EDIT, this.doc));
    }

    @Test
    public void publicVisibilityDoesntAllowComment() throws ComponentLookupException
    {
        when(this.helper.getVisibility(this.patient)).thenReturn(this.publicVisibility);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.COMMENT, this.doc));
    }

    @Test
    public void publicVisibilityDoesntAllowDelete() throws ComponentLookupException
    {
        when(this.helper.getVisibility(this.patient)).thenReturn(this.publicVisibility);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.DELETE, this.doc));
    }

    @Test
    public void publicVisibilityDoesntAllowGuestAccess() throws ComponentLookupException
    {
        when(this.helper.getVisibility(this.patient)).thenReturn(this.publicVisibility);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(null, Right.VIEW, this.doc));
    }

    @Test
    public void privateVisibilityDoesntAllowView() throws ComponentLookupException
    {
        when(this.helper.getVisibility(this.patient)).thenReturn(this.privateVisibility);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.VIEW, this.doc));
    }

    @Test
    public void privateVisibilityDoesntAllowEdit() throws ComponentLookupException
    {
        when(this.helper.getVisibility(this.patient)).thenReturn(this.privateVisibility);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.EDIT, this.doc));
    }

    @Test
    public void privateVisibilityDoesntAllowComment() throws ComponentLookupException
    {
        when(this.helper.getVisibility(this.patient)).thenReturn(this.privateVisibility);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.COMMENT, this.doc));
    }

    @Test
    public void privateVisibilityDoesntAllowDelete() throws ComponentLookupException
    {
        when(this.helper.getVisibility(this.patient)).thenReturn(this.privateVisibility);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.DELETE, this.doc));
    }

    @Test
    public void privateVisibilityDoesntAllowGuestAccess() throws ComponentLookupException
    {
        when(this.helper.getVisibility(this.patient)).thenReturn(this.privateVisibility);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(null, Right.VIEW, this.doc));
    }

    @Test
    public void noActionWithNonDocumentRight() throws ComponentLookupException
    {
        when(this.helper.getVisibility(this.patient)).thenReturn(this.openVisibility);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.REGISTER, this.doc));
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.PROGRAM, this.doc));
    }

    @Test
    public void noActionForNullVisibility() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.VIEW, this.doc));
    }

    @Test
    public void noActionWithNonPatient() throws ComponentLookupException
    {
        when(this.repo.get("xwiki:data.P01")).thenReturn(null);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.VIEW, this.doc));
    }

    @Test
    public void noActionForGuestUser() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(null, Right.VIEW, this.doc));
        Assert.assertNull(
            this.mocker.getComponentUnderTest().hasAccess(new InvalidUser(null, null), Right.VIEW, this.doc));
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
