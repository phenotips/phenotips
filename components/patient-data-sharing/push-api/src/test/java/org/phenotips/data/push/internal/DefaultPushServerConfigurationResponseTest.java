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
package org.phenotips.data.push.internal;

import org.xwiki.component.manager.ComponentLookupException;

import java.util.Set;
import java.util.TreeSet;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DefaultPushServerConfigurationResponseTest
{
    private DefaultPushServerConfigurationResponse defaultPushServerConfigurationResponse;

    private JSONObject serverResponse;

    private Set<String> expected;

    @Before
    public void setUp()
    {
        this.serverResponse = new JSONObject();
        this.defaultPushServerConfigurationResponse = new DefaultPushServerConfigurationResponse(this.serverResponse);
        this.expected = new TreeSet<>();
    }

    @Test
    public void getRemoteUserGroupsTest() throws ComponentLookupException
    {
        // populate a JSONObject
        for (int i = 0; i < 10; i++) {
            this.serverResponse.accumulate("user_groups", i);
            this.expected.add(String.valueOf(i));
        }

        Assert.assertEquals(this.expected, this.defaultPushServerConfigurationResponse.getRemoteUserGroups());
    }

    @Test
    public void getRemoteUserGroupsNullTest()
    {
        Assert.assertNull(this.defaultPushServerConfigurationResponse.getRemoteUserGroups());
    }

    @Test
    public void getRemoteAcceptedPatientFieldsTest()
    {
        // populate a JSONObject
        for (int i = 0; i < 10; i++) {
            this.serverResponse.accumulate("accepted_fields", i);
            this.expected.add(String.valueOf(i));
        }

        Assert.assertEquals(this.expected,
            this.defaultPushServerConfigurationResponse.getRemoteAcceptedPatientFields());
        Assert.assertEquals(this.expected,
            this.defaultPushServerConfigurationResponse.getRemoteAcceptedPatientFields("string"));
    }

    @Test
    public void getRemoteAcceptedPatientFieldsNullTest()
    {
        Assert.assertNull(this.defaultPushServerConfigurationResponse.getRemoteAcceptedPatientFields());
        Assert.assertNull(this.defaultPushServerConfigurationResponse.getRemoteAcceptedPatientFields("string"));
    }

    @Test
    public void getPushableFieldsTest()
    {
        // populate a JSONObject
        for (int i = 0; i < 10; i++) {
            this.serverResponse.accumulate("accepted_fields", i);
            this.expected.add(String.valueOf(i));
        }

        Assert.assertTrue(this.defaultPushServerConfigurationResponse.getPushableFields().isEmpty());
        Assert.assertTrue(this.defaultPushServerConfigurationResponse.getPushableFields("string").isEmpty());
    }

    @Test
    public void getPushableFieldsNullTest()
    {
        Assert.assertTrue(this.defaultPushServerConfigurationResponse.getPushableFields().isEmpty());
        Assert.assertTrue(this.defaultPushServerConfigurationResponse.getPushableFields("string").isEmpty());
    }

    @Test
    public void getPushableFieldsExceptionTest()
    {
        this.serverResponse = null;
        Assert.assertTrue(this.defaultPushServerConfigurationResponse.getPushableFields().isEmpty());
    }

    @Test
    public void remoteUpdatesAvailableTest()
    {
        this.serverResponse.accumulate("updates_enabled", true);
        Assert.assertTrue(this.defaultPushServerConfigurationResponse.remoteUpdatesEnabled());
    }

    @Test
    public void remoteUpdatesNotAvailableTest()
    {
        // check before anything has been set
        Assert.assertFalse(this.defaultPushServerConfigurationResponse.remoteUpdatesEnabled());
        this.serverResponse.accumulate("updates_enabled", false);
        // check after it has been set false
        Assert.assertFalse(this.defaultPushServerConfigurationResponse.remoteUpdatesEnabled());
    }

    @Test
    public void getRemoteUserTokenTest()
    {
        Assert.assertNull(this.defaultPushServerConfigurationResponse.getRemoteUserToken());
        this.serverResponse.accumulate("user_login_token", "yes");
        Assert.assertEquals("yes", this.defaultPushServerConfigurationResponse.getRemoteUserToken());
    }
}
