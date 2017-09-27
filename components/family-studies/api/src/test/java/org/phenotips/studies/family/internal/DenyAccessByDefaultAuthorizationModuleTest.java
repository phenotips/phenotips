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
import org.phenotips.security.authorization.AuthorizationModule;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.ManageRight;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;
import org.xwiki.users.internal.InvalidUser;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

/**
 * Tests for the {@link DenyAccessByDefaultAuthorizationModule denied access} {@link AuthorizationModule} component.
 *
 * @version $Id$
 */
public class DenyAccessByDefaultAuthorizationModuleTest
{
    @Rule
    public final MockitoComponentMockingRule<AuthorizationModule> mocker =
        new MockitoComponentMockingRule<>(DenyAccessByDefaultAuthorizationModule.class);

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
        when(this.access1.getGrantedRight()).thenReturn(ManageRight.MANAGE);
        when(this.access2.getGrantedRight()).thenReturn(ManageRight.MANAGE);

        when(this.user.getProfileDocument()).thenReturn(this.userProfile);
    }

    @Test
    public void documentAccessIsDeniedOnFamilies() throws ComponentLookupException
    {
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.VIEW, this.doc));
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.COMMENT, this.doc));
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.EDIT, this.doc));
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.DELETE, this.doc));
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasAccess(this.user, ManageRight.MANAGE, this.doc));
    }

    @Test
    public void documentAccessIsDeniedForGuestsOnFamilies() throws ComponentLookupException
    {
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasAccess(null, Right.VIEW, this.doc));
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasAccess(null, Right.COMMENT, this.doc));
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasAccess(null, Right.EDIT, this.doc));
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasAccess(null, Right.DELETE, this.doc));
        Assert.assertFalse(this.mocker.getComponentUnderTest().hasAccess(null, ManageRight.MANAGE, this.doc));

        Assert.assertFalse(
            this.mocker.getComponentUnderTest().hasAccess(new InvalidUser(null, null), Right.VIEW, this.doc));
        Assert.assertFalse(
            this.mocker.getComponentUnderTest().hasAccess(new InvalidUser(null, null), Right.COMMENT, this.doc));
        Assert.assertFalse(
            this.mocker.getComponentUnderTest().hasAccess(new InvalidUser(null, null), Right.EDIT, this.doc));
        Assert.assertFalse(
            this.mocker.getComponentUnderTest().hasAccess(new InvalidUser(null, null), Right.DELETE, this.doc));
        Assert.assertFalse(
            this.mocker.getComponentUnderTest().hasAccess(new InvalidUser(null, null), ManageRight.MANAGE, this.doc));
    }

    @Test
    public void noActionWithNonDocumentRight() throws ComponentLookupException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.REGISTER, this.doc));
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.PROGRAM, this.doc));
    }

    @Test
    public void noActionWithNonFamily() throws ComponentLookupException
    {
        when(this.repo.get("xwiki:Families.FAM01")).thenReturn(null);
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.VIEW, this.doc));
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
        Assert.assertEquals(110, this.mocker.getComponentUnderTest().getPriority());
    }
}
