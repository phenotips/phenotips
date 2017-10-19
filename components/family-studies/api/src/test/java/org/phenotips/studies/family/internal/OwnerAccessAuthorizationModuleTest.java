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

import org.phenotips.data.permissions.Owner;
import org.phenotips.security.authorization.AuthorizationModule;
import org.phenotips.security.authorization.AuthorizationService;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.ManageRight;
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
 * Tests for the {@link OwnerAccessAuthorizationModule owner granted access} {@link AuthorizationModule} component.
 *
 * @version $Id$
 */
public class OwnerAccessAuthorizationModuleTest
{
    @Rule
    public final MockitoComponentMockingRule<AuthorizationModule> mocker =
        new MockitoComponentMockingRule<>(OwnerAccessAuthorizationModule.class);

    @Mock
    private User user;

    @Mock
    private User admin;

    @Mock
    private Family family;

    private FamilyRepository repo;

    private DocumentAccessBridge dab;

    private DocumentReference doc = new DocumentReference("xwiki", "Families", "FAM01");

    private DocumentReference xclass = new DocumentReference("xwiki", "PhenoTips", "OwnerClass");

    private DocumentReference userProfile = new DocumentReference("xwiki", "Users", "padams");

    private DocumentReference adminProfile = new DocumentReference("xwiki", "Users", "Admin");

    @Before
    public void setupMocks() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        this.repo = this.mocker.getInstance(FamilyRepository.class);
        when(this.repo.get("xwiki:Families.FAM01")).thenReturn(this.family);
        when(this.family.getDocumentReference()).thenReturn(this.doc);

        this.dab = this.mocker.getInstance(DocumentAccessBridge.class);
        when(this.dab.getProperty(this.doc, this.xclass, Owner.PROPERTY_NAME)).thenReturn("xwiki:Users.padams");

        when(this.user.getProfileDocument()).thenReturn(this.userProfile);
        when(this.admin.getProfileDocument()).thenReturn(this.adminProfile);
        DocumentReferenceResolver<EntityReference> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "current");
        when(resolver.resolve(Owner.CLASS_REFERENCE)).thenReturn(this.xclass);
        DocumentReferenceResolver<String> strResolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        when(strResolver.resolve("xwiki:Users.padams")).thenReturn(this.userProfile);
        when(strResolver.resolve("")).thenReturn(new DocumentReference("xwiki", "data", "WebHome"));
        when(strResolver.resolve(null)).thenReturn(new DocumentReference("xwiki", "data", "WebHome"));

        AuthorizationService auth = this.mocker.getInstance(AuthorizationService.class);
        when(auth.hasAccess(this.admin, Right.ADMIN, this.doc)).thenReturn(true);
    }

    @Test
    public void viewAccessGrantedForOwner() throws ComponentLookupException
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.VIEW, this.doc));
    }

    @Test
    public void editAccessGrantedForOwner() throws ComponentLookupException
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.EDIT, this.doc));
    }

    @Test
    public void commentAccessGrantedForOwner() throws ComponentLookupException
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.COMMENT, this.doc));
    }

    @Test
    public void deleteAccessGrantedForOwner() throws ComponentLookupException
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.DELETE, this.doc));
    }

    @Test
    public void manageAccessGrantedForOwner() throws ComponentLookupException
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.user, ManageRight.MANAGE, this.doc));
    }

    @Test
    public void viewAccessGrantedForAdministrator() throws ComponentLookupException
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.admin, Right.VIEW, this.doc));
    }

    @Test
    public void editAccessGrantedForAdministrator() throws ComponentLookupException
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.admin, Right.EDIT, this.doc));
    }

    @Test
    public void commentAccessGrantedForAdministrator() throws ComponentLookupException
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.admin, Right.COMMENT, this.doc));
    }

    @Test
    public void deleteAccessGrantedForAdministrator() throws ComponentLookupException
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.admin, Right.DELETE, this.doc));
    }

    @Test
    public void manageAccessGrantedForAdministrator() throws ComponentLookupException
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(this.admin, ManageRight.MANAGE, this.doc));
    }

    @Test
    public void viewAccessGrantedForGuestOwner() throws ComponentLookupException
    {
        when(this.dab.getProperty(this.doc, this.xclass, Owner.PROPERTY_NAME)).thenReturn("");
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(null, Right.VIEW, this.doc));
        Assert.assertTrue(
            this.mocker.getComponentUnderTest().hasAccess(new InvalidUser(null, null), Right.VIEW, this.doc));
    }

    @Test
    public void editAccessGrantedForGuestOwner() throws ComponentLookupException
    {
        when(this.dab.getProperty(this.doc, this.xclass, Owner.PROPERTY_NAME)).thenReturn(null);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(null, Right.EDIT, this.doc));
        Assert.assertTrue(
            this.mocker.getComponentUnderTest().hasAccess(new InvalidUser(null, null), Right.EDIT, this.doc));
    }

    @Test
    public void commentAccessGrantedForGuestOwner() throws ComponentLookupException
    {
        when(this.dab.getProperty(this.doc, this.xclass, Owner.PROPERTY_NAME)).thenReturn("");
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(null, Right.COMMENT, this.doc));
        Assert.assertTrue(
            this.mocker.getComponentUnderTest().hasAccess(new InvalidUser(null, null), Right.COMMENT, this.doc));

    }

    @Test
    public void deleteAccessGrantedForGuestOwner() throws ComponentLookupException
    {
        when(this.dab.getProperty(this.doc, this.xclass, Owner.PROPERTY_NAME)).thenReturn(null);
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(null, Right.DELETE, this.doc));
        Assert.assertTrue(
            this.mocker.getComponentUnderTest().hasAccess(new InvalidUser(null, null), Right.DELETE, this.doc));

    }

    @Test
    public void manageAccessGrantedForGuestOwner() throws ComponentLookupException
    {
        when(this.dab.getProperty(this.doc, this.xclass, Owner.PROPERTY_NAME)).thenReturn("");
        Assert.assertTrue(this.mocker.getComponentUnderTest().hasAccess(null, ManageRight.MANAGE, this.doc));
        Assert.assertTrue(
            this.mocker.getComponentUnderTest().hasAccess(new InvalidUser(null, null), ManageRight.MANAGE, this.doc));

    }

    @Test
    public void noAccessGrantedForGuestsUserWithRealOwner() throws ComponentLookupException
    {
        Assert.assertNull(
            this.mocker.getComponentUnderTest().hasAccess(new InvalidUser(null, null), Right.VIEW, this.doc));
    }

    @Test
    public void noAccessGrantedForRealUserWithGuestOwner() throws ComponentLookupException
    {
        when(this.dab.getProperty(this.doc, this.xclass, Owner.PROPERTY_NAME)).thenReturn("");
        Assert.assertNull(
            this.mocker.getComponentUnderTest().hasAccess(this.user, Right.VIEW, this.doc));
    }

    @Test
    public void noActionForNonOwner() throws ComponentLookupException
    {
        when(this.user.getProfileDocument()).thenReturn(new DocumentReference("xwiki", "Users", "hmccoy"));
        Assert.assertNull(this.mocker.getComponentUnderTest().hasAccess(this.user, Right.EDIT, this.doc));
    }

    @Test
    public void noActionWithNonPatient() throws ComponentLookupException
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
        Assert.assertEquals(400, this.mocker.getComponentUnderTest().getPriority());
    }
}
