/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.data.internal.controller;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.SimpleNamedData;
import org.phenotips.data.permissions.Owner;
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.groups.Group;
import org.phenotips.groups.GroupManager;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Iterator;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the contact data controller implementation, {@link ContactInformationController}.
 * 
 * @version $Id$
 */
public class ContactInformationControllerTest
{
    private Patient patient = mock(Patient.class);

    /** The user used as the owner of the patient. */
    private static final DocumentReference USER = new DocumentReference("xwiki", "XWiki", "padams");

    private static final String USER_STR = "xwiki:XWiki.padams";

    private static final String USER_EMAIL = "padams@hospital.org";

    private static final String USER_NAME = "Patch Adams";

    private static final String USER_INSTITUTION = "Hospital";

    /** Group used as collaborator. */
    private static final DocumentReference GROUP = new DocumentReference("xwiki", "XWiki", "collaborators");

    private static final String GROUP_EMAIL = "contact@hospital.org";

    @Rule
    public final MockitoComponentMockingRule<PatientDataController<String>> mocker =
        new MockitoComponentMockingRule<PatientDataController<String>>(
            ContactInformationController.class);

    /** {@link ContactInformationController#load(Patient)} returns the user information when the owner is a user. */
    @Test
    public void loadWithUserOwner() throws ComponentLookupException
    {
        PermissionsManager permissions = this.mocker.getInstance(PermissionsManager.class);
        PatientAccess access = mock(PatientAccess.class);
        when(permissions.getPatientAccess(this.patient)).thenReturn(access);
        Owner owner = mock(Owner.class);
        when(access.getOwner()).thenReturn(owner);
        when(owner.isGroup()).thenReturn(false);
        when(owner.getUser()).thenReturn(USER);
        UserManager users = this.mocker.getInstance(UserManager.class);
        User user = mock(User.class);
        when(users.getUser(USER_STR)).thenReturn(user);
        when(user.getName()).thenReturn(USER_NAME);
        when(user.getUsername()).thenReturn(USER.getName());
        when(user.getAttribute("email")).thenReturn(USER_EMAIL);
        when(user.getAttribute("company")).thenReturn(USER_INSTITUTION);

        SimpleNamedData<String> result =
            (SimpleNamedData<String>) this.mocker.getComponentUnderTest().load(this.patient);
        Assert.assertNotNull(result);
        Assert.assertEquals("contact", result.getName());
        Assert.assertEquals(USER_NAME, result.get("name"));
        Assert.assertEquals("padams", result.get("user_id"));
        Assert.assertEquals(USER_EMAIL, result.get("email"));
        Assert.assertEquals(USER_INSTITUTION, result.get("institution"));
    }

    /** {@link ContactInformationController#load(Patient)} doesn't return empty fields. */
    @Test
    public void loadWithUserOwnerAndMissingFields() throws ComponentLookupException
    {
        PermissionsManager permissions = this.mocker.getInstance(PermissionsManager.class);
        PatientAccess access = mock(PatientAccess.class);
        when(permissions.getPatientAccess(this.patient)).thenReturn(access);
        Owner owner = mock(Owner.class);
        when(access.getOwner()).thenReturn(owner);
        when(owner.isGroup()).thenReturn(false);
        when(owner.getUser()).thenReturn(USER);
        UserManager users = this.mocker.getInstance(UserManager.class);
        User user = mock(User.class);
        when(users.getUser(USER_STR)).thenReturn(user);
        when(user.getName()).thenReturn(null);
        when(user.getUsername()).thenReturn(USER.getName());
        when(user.getAttribute("email")).thenReturn("");
        when(user.getAttribute("company")).thenReturn(" ");

        SimpleNamedData<String> result =
            (SimpleNamedData<String>) this.mocker.getComponentUnderTest().load(this.patient);
        Assert.assertNotNull(result);
        Assert.assertEquals("contact", result.getName());
        Assert.assertNull(result.get("name"));
        Assert.assertEquals("padams", result.get("user_id"));
        Assert.assertNull(result.get("email"));
        Assert.assertNull(result.get("institution"));

        //Recoding Assert.assertEquals(1, result.size())
        Iterator resultIterator = result.iterator();
        Assert.assertTrue(resultIterator.hasNext());
        resultIterator.next();
        Assert.assertTrue(!resultIterator.hasNext());
    }

    /** {@link ContactInformationController#load(Patient)} returns null when the owner is a user that doesn't exist. */
    @Test
    public void loadWithInexistingUserOwner() throws ComponentLookupException
    {
        PermissionsManager permissions = this.mocker.getInstance(PermissionsManager.class);
        PatientAccess access = mock(PatientAccess.class);
        when(permissions.getPatientAccess(this.patient)).thenReturn(access);
        Owner owner = mock(Owner.class);
        when(access.getOwner()).thenReturn(owner);
        when(owner.isGroup()).thenReturn(false);
        when(owner.getUser()).thenReturn(USER);
        UserManager users = this.mocker.getInstance(UserManager.class);
        when(users.getUser(USER_STR)).thenReturn(null);

        SimpleNamedData<String> result =
            (SimpleNamedData<String>) this.mocker.getComponentUnderTest().load(this.patient);
        Assert.assertNull(result);
    }

    /** {@link ContactInformationController#load(Patient)} returns basic group information when the owner is a group. */
    @Test
    public void loadWithGroupOwner() throws Exception
    {
        PermissionsManager permissions = this.mocker.getInstance(PermissionsManager.class);
        PatientAccess access = mock(PatientAccess.class);
        when(permissions.getPatientAccess(this.patient)).thenReturn(access);
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

        SimpleNamedData<String> result =
            (SimpleNamedData<String>) this.mocker.getComponentUnderTest().load(this.patient);
        Assert.assertNotNull(result);
        Assert.assertEquals("contact", result.getName());
        Assert.assertEquals(GROUP.getName(), result.get("name"));
        Assert.assertNull(result.get("user_id"));
        Assert.assertEquals(GROUP_EMAIL, result.get("email"));
        Assert.assertNull(result.get("institution"));
    }

    /** {@link ContactInformationController#load(Patient)} returns null when the owner is a group that doesn't exist. */
    @Test
    public void loadWithInexistingGroupOwner() throws Exception
    {
        PermissionsManager permissions = this.mocker.getInstance(PermissionsManager.class);
        PatientAccess access = mock(PatientAccess.class);
        when(permissions.getPatientAccess(this.patient)).thenReturn(access);
        Owner owner = mock(Owner.class);
        when(access.getOwner()).thenReturn(owner);
        when(owner.isGroup()).thenReturn(true);
        when(owner.getUser()).thenReturn(GROUP);
        GroupManager groups = this.mocker.getInstance(GroupManager.class);
        when(groups.getGroup(GROUP)).thenReturn(null);

        SimpleNamedData<String> result =
            (SimpleNamedData<String>) this.mocker.getComponentUnderTest().load(this.patient);
        Assert.assertNull(result);
    }

    /** {@link ContactInformationController#load(Patient)} returns null when no owner is defined. */
    @Test
    public void loadWithNullOwner() throws Exception
    {
        PermissionsManager permissions = this.mocker.getInstance(PermissionsManager.class);
        PatientAccess access = mock(PatientAccess.class);
        when(permissions.getPatientAccess(this.patient)).thenReturn(access);
        when(access.getOwner()).thenReturn(null);

        SimpleNamedData<String> result =
            (SimpleNamedData<String>) this.mocker.getComponentUnderTest().load(this.patient);
        Assert.assertNull(result);
    }

    /** {@link ContactInformationController#getName()} returns "contact". */
    @Test
    public void getName() throws Exception
    {
        Assert.assertEquals("contact", this.mocker.getComponentUnderTest().getName());
    }
}
