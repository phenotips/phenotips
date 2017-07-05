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
package org.phenotips.data.internal;

import org.phenotips.data.ContactInfo;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientContactProvider;

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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the reporter {@link PatientContactProvider contact provider} implementation,
 * {@link ReporterContactProvider}.
 *
 * @version $Id$
 */
public class ReporterContactProviderTest
{
    /** The user used as the reporter of the patient. */
    private static final DocumentReference USER = new DocumentReference("xwiki", "XWiki", "padams");

    private static final String USER_STR = "xwiki:XWiki.padams";

    private static final String USER_EMAIL = "padams@hospital.org";

    private static final String USER_NAME = "Patch Adams";

    private static final String USER_INSTITUTION = "Hospital";

    @Rule
    public final MockitoComponentMockingRule<PatientContactProvider> mocker =
        new MockitoComponentMockingRule<PatientContactProvider>(ReporterContactProvider.class);

    @Mock
    private Patient patient;

    @Before
    public void setupComponents() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        EntityReferenceSerializer<String> serializer = this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
        when(serializer.serialize(USER)).thenReturn(USER_STR);
    }

    /** {@link ReporterContactProvider} returns the user information when the reporter is a user. */
    @Test
    public void patientWithValidReporterReturnsCorrectContactInfo() throws ComponentLookupException
    {
        UserManager users = this.mocker.getInstance(UserManager.class);
        User user = mock(User.class);
        when(users.getUser(USER_STR)).thenReturn(user);
        when(user.getName()).thenReturn(USER_NAME);
        when(user.getUsername()).thenReturn(USER.getName());
        when(user.getAttribute("email")).thenReturn(USER_EMAIL);
        when(user.getAttribute("company")).thenReturn(USER_INSTITUTION);

        when(this.patient.getReporter()).thenReturn(USER);

        List<ContactInfo> result = this.mocker.getComponentUnderTest().getContacts(this.patient);

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        ContactInfo contact = result.get(0);
        Assert.assertEquals(USER_STR, contact.getUserId());
        Assert.assertEquals(USER_NAME, contact.getName());
        Assert.assertEquals(Collections.singletonList(USER_EMAIL), contact.getEmails());
        Assert.assertEquals(USER_INSTITUTION, contact.getInstitution());
    }

    /** {@link ReporterContactProvider} returns simple user information when the reporter is an invalid user. */
    @Test
    public void patientWithInvalidReporterReturnsJustUserName() throws ComponentLookupException
    {
        UserManager users = this.mocker.getInstance(UserManager.class);
        when(users.getUser(USER_STR)).thenReturn(null);

        when(this.patient.getReporter()).thenReturn(USER);

        List<ContactInfo> result = this.mocker.getComponentUnderTest().getContacts(this.patient);

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        ContactInfo contact = result.get(0);
        Assert.assertEquals(USER_STR, contact.getUserId());
        Assert.assertNull(contact.getName());
        Assert.assertTrue(contact.getEmails().isEmpty());
        Assert.assertNull(contact.getInstitution());
        Assert.assertNull(contact.getUrl());
    }

    /** {@link ReporterContactProvider} returns an empty contact list when the reporter is not known / guest. */
    @Test
    public void patientWithUnknownReporterReturnsEmptyContactList() throws ComponentLookupException
    {
        when(this.patient.getReporter()).thenReturn(null);

        List<ContactInfo> result = this.mocker.getComponentUnderTest().getContacts(this.patient);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void priorityIs900() throws ComponentLookupException
    {
        Assert.assertEquals(900, this.mocker.getComponentUnderTest().getPriority());
    }
}
