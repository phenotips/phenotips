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

import org.phenotips.data.ContactInfo;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientContactProvider;
import org.phenotips.data.permissions.EntityPermissionsManager;
import org.phenotips.data.permissions.Owner;
import org.phenotips.data.permissions.EntityAccess;
import org.phenotips.groups.Group;
import org.phenotips.groups.GroupManager;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the owner {@link PatientContactProvider} implementation, {@link OwnerContactProvider}.
 *
 * @version $Id$
 */
public class OwnerContactProviderTest
{
    /** The user used as the owner of the patient. */
    private static final DocumentReference USER = new DocumentReference("xwiki", "XWiki", "padams");

    private static final String USER_STR = "xwiki:XWiki.padams";

    private static final String USER_EMAIL = "padams@hospital.org";

    private static final String USER_NAME = "Patch Adams";

    private static final String USER_INSTITUTION = "Hospital";

    /** Group used as collaborator. */
    private static final DocumentReference GROUP = new DocumentReference("xwiki", "XWiki", "collaborators");

    private static final String GROUP_EMAIL = "contact@hospital.org";

    private static final String GROUP_TITLE = "Main Hospital Group";

    @Rule
    public final MockitoComponentMockingRule<PatientContactProvider> mocker =
        new MockitoComponentMockingRule<PatientContactProvider>(OwnerContactProvider.class);

    @Mock
    private Patient patient;

    @Before
    public void setupComponents() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        EntityReferenceSerializer<String> serializer = this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
        when(serializer.serialize(GROUP)).thenReturn(GROUP.toString());
    }

    /** {@link OwnerContactProvider#getContacts(Patient)} returns the user information when the owner is a user. */
    @Test
    public void loadWithUserOwner() throws ComponentLookupException
    {
        EntityPermissionsManager permissions = this.mocker.getInstance(EntityPermissionsManager.class);
        EntityAccess access = mock(EntityAccess.class);
        when(permissions.getEntityAccess(this.patient)).thenReturn(access);
        Owner owner = mock(Owner.class);
        when(access.getOwner()).thenReturn(owner);
        when(owner.isGroup()).thenReturn(false);
        when(owner.getUser()).thenReturn(USER);
        UserManager users = this.mocker.getInstance(UserManager.class);
        User user = mock(User.class);
        when(users.getUser(USER_STR)).thenReturn(user);
        when(user.getId()).thenReturn(USER_STR);
        when(user.getName()).thenReturn(USER_NAME);
        when(user.getUsername()).thenReturn(USER.getName());
        when(user.getAttribute("email")).thenReturn(USER_EMAIL);
        when(user.getAttribute("company")).thenReturn(USER_INSTITUTION);

        List<ContactInfo> result = this.mocker.getComponentUnderTest().getContacts(this.patient);
        Assert.assertEquals(1, result.size());
        ContactInfo contact = result.get(0);
        Assert.assertEquals(USER_NAME, contact.getName());
        Assert.assertEquals(USER_STR, contact.getUserId());
        Assert.assertEquals(Collections.singletonList(USER_EMAIL), contact.getEmails());
        Assert.assertEquals(USER_INSTITUTION, contact.getInstitution());
    }

    /** {@link OwnerContactProvider#getContacts(Patient)} doesn't return empty fields. */
    @Test
    public void loadWithUserOwnerAndMissingFields() throws ComponentLookupException
    {
        EntityPermissionsManager permissions = this.mocker.getInstance(EntityPermissionsManager.class);
        EntityAccess access = mock(EntityAccess.class);
        when(permissions.getEntityAccess(this.patient)).thenReturn(access);
        Owner owner = mock(Owner.class);
        when(access.getOwner()).thenReturn(owner);
        when(owner.isGroup()).thenReturn(false);
        when(owner.getUser()).thenReturn(USER);
        UserManager users = this.mocker.getInstance(UserManager.class);
        User user = mock(User.class);
        when(users.getUser(USER_STR)).thenReturn(user);
        when(user.getId()).thenReturn(USER_STR);
        when(user.getName()).thenReturn(null);
        when(user.getUsername()).thenReturn(USER.getName());
        when(user.getAttribute("email")).thenReturn("");
        when(user.getAttribute("company")).thenReturn(" ");

        List<ContactInfo> result = this.mocker.getComponentUnderTest().getContacts(this.patient);
        Assert.assertEquals(1, result.size());
        ContactInfo contact = result.get(0);
        Assert.assertNull(contact.getName());
        Assert.assertEquals(USER_STR, contact.getUserId());
        Assert.assertTrue(contact.getEmails().isEmpty());
        Assert.assertNull(contact.getInstitution());
    }

    /** {@link OwnerContactProvider#getContacts(Patient)} returns null when the owner is a user that doesn't exist. */
    @Test
    public void loadWithInexistingUserOwner() throws ComponentLookupException
    {
        EntityPermissionsManager permissions = this.mocker.getInstance(EntityPermissionsManager.class);
        EntityAccess access = mock(EntityAccess.class);
        when(permissions.getEntityAccess(this.patient)).thenReturn(access);
        Owner owner = mock(Owner.class);
        when(access.getOwner()).thenReturn(owner);
        when(owner.isGroup()).thenReturn(false);
        when(owner.getUser()).thenReturn(USER);
        UserManager users = this.mocker.getInstance(UserManager.class);
        when(users.getUser(USER_STR)).thenReturn(null);

        List<ContactInfo> result = this.mocker.getComponentUnderTest().getContacts(this.patient);
        Assert.assertNull(result);
    }

    /** {@link OwnerContactProvider#getContacts(Patient)} returns basic group information when the owner is a group. */
    @Test
    public void loadWithGroupOwner() throws Exception
    {
        EntityPermissionsManager permissions = this.mocker.getInstance(EntityPermissionsManager.class);
        EntityAccess access = mock(EntityAccess.class);
        when(permissions.getEntityAccess(this.patient)).thenReturn(access);
        Owner owner = mock(Owner.class);
        when(access.getOwner()).thenReturn(owner);
        when(owner.isGroup()).thenReturn(true);
        when(owner.getUser()).thenReturn(GROUP);
        GroupManager groups = this.mocker.getInstance(GroupManager.class);
        Group group = mock(Group.class);
        when(groups.getGroup(GROUP)).thenReturn(group);
        DocumentAccessBridge dab = this.mocker.getInstance(DocumentAccessBridge.class);
        when(group.getReference()).thenReturn(GROUP);
        XWikiDocument doc = mock(XWikiDocument.class);
        when(dab.getDocument(GROUP)).thenReturn(doc);
        BaseObject obj = mock(BaseObject.class);
        when(doc.getXObject(Group.CLASS_REFERENCE)).thenReturn(obj);
        when(obj.getStringValue("contact")).thenReturn(GROUP_EMAIL);
        when(doc.getTitle()).thenReturn(GROUP_TITLE);

        List<ContactInfo> result = this.mocker.getComponentUnderTest().getContacts(this.patient);
        Assert.assertEquals(1, result.size());
        ContactInfo contact = result.get(0);
        Assert.assertEquals(GROUP_TITLE, contact.getName());
        Assert.assertEquals(GROUP.toString(), contact.getUserId());
        Assert.assertEquals(Collections.singletonList(GROUP_EMAIL), contact.getEmails());
        Assert.assertNull(contact.getInstitution());
    }

    @Test
    public void loadWithGroupOwnerAndMissingFields() throws Exception
    {
        EntityPermissionsManager permissions = this.mocker.getInstance(EntityPermissionsManager.class);
        EntityAccess access = mock(EntityAccess.class);
        when(permissions.getEntityAccess(this.patient)).thenReturn(access);
        Owner owner = mock(Owner.class);
        when(access.getOwner()).thenReturn(owner);
        when(owner.isGroup()).thenReturn(true);
        when(owner.getUser()).thenReturn(GROUP);
        GroupManager groups = this.mocker.getInstance(GroupManager.class);
        Group group = mock(Group.class);
        when(groups.getGroup(GROUP)).thenReturn(group);
        DocumentAccessBridge dab = this.mocker.getInstance(DocumentAccessBridge.class);
        when(group.getReference()).thenReturn(GROUP);
        XWikiDocument doc = mock(XWikiDocument.class);
        when(dab.getDocument(GROUP)).thenReturn(doc);
        BaseObject obj = mock(BaseObject.class);
        when(doc.getXObject(Group.CLASS_REFERENCE)).thenReturn(obj);
        when(obj.getStringValue("contact")).thenReturn("");

        List<ContactInfo> result = this.mocker.getComponentUnderTest().getContacts(this.patient);
        Assert.assertEquals(1, result.size());
        ContactInfo contact = result.get(0);
        Assert.assertNull(contact.getName());
        Assert.assertEquals(GROUP.toString(), contact.getUserId());
        Assert.assertTrue(contact.getEmails().isEmpty());
        Assert.assertNull(contact.getInstitution());
    }

    /** {@link OwnerContactProvider#getContacts(Patient)} returns null when the owner is a group that doesn't exist. */
    @Test
    public void loadWithInexistingGroupOwner() throws Exception
    {
        EntityPermissionsManager permissions = this.mocker.getInstance(EntityPermissionsManager.class);
        EntityAccess access = mock(EntityAccess.class);
        when(permissions.getEntityAccess(this.patient)).thenReturn(access);
        Owner owner = mock(Owner.class);
        when(access.getOwner()).thenReturn(owner);
        when(owner.isGroup()).thenReturn(true);
        when(owner.getUser()).thenReturn(GROUP);
        GroupManager groups = this.mocker.getInstance(GroupManager.class);
        when(groups.getGroup(GROUP)).thenReturn(null);

        List<ContactInfo> result = this.mocker.getComponentUnderTest().getContacts(this.patient);
        Assert.assertNull(result);
    }

    /** {@link OwnerContactProvider#getContacts(Patient)} returns null when no owner is defined. */
    @Test
    public void loadWithNullOwner() throws Exception
    {
        EntityPermissionsManager permissions = this.mocker.getInstance(EntityPermissionsManager.class);
        EntityAccess access = mock(EntityAccess.class);
        when(permissions.getEntityAccess(this.patient)).thenReturn(access);
        when(access.getOwner()).thenReturn(null);

        List<ContactInfo> result = this.mocker.getComponentUnderTest().getContacts(this.patient);
        Assert.assertNull(result);
    }

    /** {@link OwnerContactProvider#getContacts(Patient)} returns null when the owner is guest. */
    @Test
    public void loadWithGuestOwner() throws Exception
    {
        EntityPermissionsManager permissions = this.mocker.getInstance(EntityPermissionsManager.class);
        EntityAccess access = mock(EntityAccess.class);
        when(permissions.getEntityAccess(this.patient)).thenReturn(access);
        Owner owner = mock(Owner.class);
        when(access.getOwner()).thenReturn(owner);
        when(owner.getUser()).thenReturn(null);

        List<ContactInfo> result = this.mocker.getComponentUnderTest().getContacts(this.patient);
        Assert.assertNull(result);
    }

    @Test
    public void priorityIs100() throws ComponentLookupException
    {
        Assert.assertEquals(100, this.mocker.getComponentUnderTest().getPriority());
    }
}
