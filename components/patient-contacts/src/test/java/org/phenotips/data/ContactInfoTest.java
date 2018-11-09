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
package org.phenotips.data;

import org.phenotips.data.ContactInfo.Builder;

import java.util.Arrays;
import java.util.Collections;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the {@link ContactInfo} {@link ContactInfo.Builder builder} and implementation produced by it.
 *
 * @version $Id$
 */
public class ContactInfoTest
{
    private static final String USER_ID = "xwiki:XWiki.padams";

    private static final String USER_EMAIL = "padams@hospital.org";

    private static final String USER_EMAILS = "padams@hospital.org,rodens@hospital.org";

    private static final String USER_NAME = "Patch Adams";

    private static final String USER_INSTITUTION = "Hospital";

    private static final String USER_URL = "http://prestigious.clinic/padams";

    @Test
    public void builderOutcomeReturnsCorrectContactInfo()
    {
        Builder builder = new Builder();
        builder.withUserId(USER_ID);
        builder.withName(USER_NAME);
        builder.withInstitution(USER_INSTITUTION);
        builder.withEmails(Collections.singletonList(USER_EMAIL));
        builder.withUrl(USER_URL);

        ContactInfo contact = builder.build();
        Assert.assertNotNull(contact);
        Assert.assertEquals(USER_ID, contact.getUserId());
        Assert.assertEquals(USER_NAME, contact.getName());
        Assert.assertEquals(USER_INSTITUTION, contact.getInstitution());
        Assert.assertEquals(Collections.singletonList(USER_EMAIL), contact.getEmails());
        Assert.assertEquals(USER_URL, contact.getUrl());

        Assert.assertEquals("Patch Adams <padams@hospital.org>", contact.toString());

        JSONObject json = new JSONObject();
        json.put("id", USER_ID);
        json.put("name", USER_NAME);
        json.put("institution", USER_INSTITUTION);
        json.put("email", USER_EMAIL);
        json.put("url", USER_URL);
        Assert.assertTrue(json.similar(contact.toJSON()));
    }

    @Test
    public void emptyStringsAreConvertedToNull()
    {
        Builder builder = new Builder();
        builder.withUserId("");
        builder.withName("");
        builder.withInstitution("");
        builder.withEmail("");
        builder.withUrl("");

        ContactInfo contact = builder.build();
        Assert.assertNotNull(contact);
        Assert.assertNull(contact.getUserId());
        Assert.assertNull(contact.getName());
        Assert.assertNull(contact.getInstitution());
        Assert.assertTrue(contact.getEmails().isEmpty());
        Assert.assertNull(contact.getUrl());

        Assert.assertEquals("[empty contact]", contact.toString());

        Assert.assertEquals(0, contact.toJSON().length());
    }

    @Test
    public void rebuildingReturnsSameContactInfo()
    {
        Builder builder = new Builder();
        builder.withUserId(USER_ID);
        builder.withName(USER_NAME);
        builder.withInstitution(USER_INSTITUTION);
        builder.withEmail(USER_EMAIL);
        builder.withUrl(USER_URL);

        ContactInfo contact = builder.build();
        Assert.assertNotNull(contact);
        Assert.assertEquals(USER_ID, contact.getUserId());
        Assert.assertEquals(USER_NAME, contact.getName());
        Assert.assertEquals(USER_INSTITUTION, contact.getInstitution());
        Assert.assertEquals(Collections.singletonList(USER_EMAIL), contact.getEmails());
        Assert.assertEquals(USER_URL, contact.getUrl());

        Assert.assertEquals("Patch Adams <padams@hospital.org>", contact.toString());

        JSONObject json = new JSONObject();
        json.put("id", USER_ID);
        json.put("name", USER_NAME);
        json.put("institution", USER_INSTITUTION);
        json.put("email", USER_EMAIL);
        json.put("url", USER_URL);
        Assert.assertTrue(json.similar(contact.toJSON()));

        // Check again
        contact = builder.build();
        Assert.assertNotNull(contact);
        Assert.assertEquals(USER_ID, contact.getUserId());
        Assert.assertEquals(USER_NAME, contact.getName());
        Assert.assertEquals(USER_INSTITUTION, contact.getInstitution());
        Assert.assertEquals(Collections.singletonList(USER_EMAIL), contact.getEmails());
        Assert.assertEquals(USER_URL, contact.getUrl());
        Assert.assertEquals("Patch Adams <padams@hospital.org>", contact.toString());
        Assert.assertTrue(json.similar(contact.toJSON()));
    }

    @Test
    public void lastCallWins()
    {
        Builder builder = new Builder();
        // Set invalid values
        builder.withUserId("invalid");
        builder.withName("invalid");
        builder.withInstitution("invalid");
        builder.withEmail("invalid");
        builder.withUrl("invalid");

        // Set correct values
        builder.withUserId(USER_ID);
        builder.withName(USER_NAME);
        builder.withInstitution(USER_INSTITUTION);
        builder.withEmail(USER_EMAIL);
        builder.withUrl(USER_URL);

        ContactInfo contact = builder.build();
        Assert.assertNotNull(contact);
        Assert.assertEquals(USER_ID, contact.getUserId());
        Assert.assertEquals(USER_NAME, contact.getName());
        Assert.assertEquals(USER_INSTITUTION, contact.getInstitution());
        Assert.assertEquals(Collections.singletonList(USER_EMAIL), contact.getEmails());
        Assert.assertEquals(USER_URL, contact.getUrl());

        Assert.assertEquals("Patch Adams <padams@hospital.org>", contact.toString());

        JSONObject json = new JSONObject();
        json.put("id", USER_ID);
        json.put("name", USER_NAME);
        json.put("institution", USER_INSTITUTION);
        json.put("email", USER_EMAIL);
        json.put("url", USER_URL);
        Assert.assertTrue(json.similar(contact.toJSON()));
    }

    @Test
    public void builderOutputsEmptyContactWithoutAnySetup()
    {
        ContactInfo contact = new Builder().build();
        Assert.assertNull(contact.getUserId());
        Assert.assertNull(contact.getName());
        Assert.assertNull(contact.getInstitution());
        Assert.assertNull(contact.getUrl());
        Assert.assertTrue(contact.getEmails().isEmpty());

        Assert.assertEquals("[empty contact]", contact.toString());

        JSONObject json = new JSONObject();
        Assert.assertTrue(json.similar(contact.toJSON()));
    }

    @Test
    public void builderResultWithOnlyUserId()
    {
        ContactInfo contact = new Builder().withUserId(USER_ID).build();
        Assert.assertEquals(USER_ID, contact.getUserId());
        Assert.assertNull(contact.getName());
        Assert.assertNull(contact.getInstitution());
        Assert.assertNull(contact.getUrl());
        Assert.assertTrue(contact.getEmails().isEmpty());

        Assert.assertEquals(USER_ID, contact.toString());

        JSONObject json = new JSONObject("{\"id\":\"" + USER_ID + "\"}");
        Assert.assertTrue(json.similar(contact.toJSON()));
    }

    @Test
    public void builderResultCorrectEmails()
    {
        ContactInfo contact = new Builder().withEmail(USER_EMAILS).build();
        Assert.assertNull(contact.getUserId());
        Assert.assertNull(contact.getName());
        Assert.assertNull(contact.getInstitution());
        Assert.assertNull(contact.getUrl());
        Assert.assertEquals(Arrays.asList("padams@hospital.org", "rodens@hospital.org"), contact.getEmails());
    }
}
