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
package org.phenotips.data.internal.controller;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONObject;
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
 * Tests for the contact data controller implementation, {@link ContactInformationController}.
 *
 * @version $Id$
 */
public class ContactInformationControllerTest
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

    @Rule
    public final MockitoComponentMockingRule<PatientDataController<String>> mocker =
        new MockitoComponentMockingRule<PatientDataController<String>>(
            ContactInformationController.class);

    @Mock
    private Patient patient;

    @Mock
    private PatientData<String> data;

    @Before
    public void setupComponents()
    {
        MockitoAnnotations.initMocks(this);
        when(this.patient.<String>getData("contact")).thenReturn(this.data);
        when(this.data.isNamed()).thenReturn(true);
    }

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

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);
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

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);
        Assert.assertNotNull(result);
        Assert.assertEquals("contact", result.getName());
        Assert.assertNull(result.get("name"));
        Assert.assertEquals("padams", result.get("user_id"));
        Assert.assertNull(result.get("email"));
        Assert.assertNull(result.get("institution"));

        // Recoding Assert.assertEquals(1, result.size())
        Iterator<String> resultIterator = result.iterator();
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

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);
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

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);
        Assert.assertNotNull(result);
        Assert.assertEquals("contact", result.getName());
        Assert.assertEquals(GROUP.getName(), result.get("name"));
        Assert.assertEquals(GROUP.getName(), result.get("user_id"));
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

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);
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

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);
        Assert.assertNull(result);
    }

    /** {@link ContactInformationController#load(Patient)} returns null when the owner is guest. */
    @Test
    public void loadWithGuestOwner() throws Exception
    {
        PermissionsManager permissions = this.mocker.getInstance(PermissionsManager.class);
        PatientAccess access = mock(PatientAccess.class);
        when(permissions.getPatientAccess(this.patient)).thenReturn(access);
        Owner owner = mock(Owner.class);
        when(access.getOwner()).thenReturn(owner);
        when(owner.getUser()).thenReturn(null);

        PatientData<String> result = this.mocker.getComponentUnderTest().load(this.patient);
        Assert.assertNull(result);
    }

    @Test
    public void writeJSONWithNoContact() throws ComponentLookupException
    {
        when(this.patient.getData("contact")).thenReturn(null);
        JSONObject json = new JSONObject();
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);
        Assert.assertEquals(0, json.length());
    }

    @Test
    public void writeJSONWithSkippedField() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();
        Collection<String> enabledFields = Collections.singleton("features");
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, enabledFields);
        Assert.assertEquals(0, json.length());
    }

    @Test
    public void writeJSONWithOtherTypeOfData() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();
        when(this.data.isNamed()).thenReturn(false);
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);
        Assert.assertEquals(0, json.length());
    }

    @Test
    public void writeJSONWithEmptyData() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();
        when(this.data.dictionaryIterator()).thenReturn(Collections.<Entry<String, String>>emptyIterator());
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);
        Assert.assertEquals(0, json.length());
    }

    @Test
    public void writeJSONUpdatesExistingElement() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();
        JSONObject contact = new JSONObject();
        contact.put("workgroup", "Cardiology");
        contact.put("email", "cardio@hospital.org");
        json.put("contact", contact);

        Map<String, String> data = new HashMap<>();
        data.put("email", "jdoe@hospital.org");
        data.put("name", "John Doe");
        when(this.data.dictionaryIterator()).thenReturn(data.entrySet().iterator());
        Collection<String> enabledFields = Collections.singleton("contact");
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, enabledFields);
        Assert.assertEquals(1, json.length());
        contact = json.getJSONObject("contact");
        Assert.assertEquals(3, contact.length());
        Assert.assertEquals("jdoe@hospital.org", contact.getString("email"));
        Assert.assertEquals("Cardiology", contact.getString("workgroup"));
        Assert.assertEquals("John Doe", contact.getString("name"));
    }

    @Test
    public void writeJSONReplacesNullElement() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();

        Map<String, String> data = new HashMap<>();
        data.put("email", "jdoe@hospital.org");
        data.put("name", "John Doe");
        when(this.data.dictionaryIterator()).thenReturn(data.entrySet().iterator());
        Collection<String> enabledFields = Collections.singleton("contact");
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json, enabledFields);
        Assert.assertEquals(1, json.length());
        JSONObject contact = json.getJSONObject("contact");
        Assert.assertEquals(2, contact.length());
        Assert.assertEquals("jdoe@hospital.org", contact.getString("email"));
        Assert.assertEquals("John Doe", contact.getString("name"));
    }

    @Test
    public void writeJSONCreatesContactElement() throws ComponentLookupException
    {
        JSONObject json = new JSONObject();

        Map<String, String> data = new HashMap<>();
        data.put("email", "jdoe@hospital.org");
        data.put("name", "John Doe");
        when(this.data.dictionaryIterator()).thenReturn(data.entrySet().iterator());
        this.mocker.getComponentUnderTest().writeJSON(this.patient, json);
        Assert.assertEquals(1, json.length());
        JSONObject contact = json.getJSONObject("contact");
        Assert.assertEquals(2, contact.length());
        Assert.assertEquals("jdoe@hospital.org", contact.getString("email"));
        Assert.assertEquals("John Doe", contact.getString("name"));
    }

    /** {@link ContactInformationController#getName()} returns "contact". */
    @Test
    public void getName() throws Exception
    {
        Assert.assertEquals("contact", this.mocker.getComponentUnderTest().getName());
    }
}
