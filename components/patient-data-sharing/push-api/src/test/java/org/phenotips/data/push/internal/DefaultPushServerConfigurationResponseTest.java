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
        serverResponse = new JSONObject();
        defaultPushServerConfigurationResponse = new
            DefaultPushServerConfigurationResponse(serverResponse);
        expected = new TreeSet<String>();
    }

    @Test
    public void getRemoteUserGroupsTest() throws ComponentLookupException
    {
        // populate a JSONObject
        for (int i = 0; i < 10; i++) {
            serverResponse.accumulate("user_groups", i);
            expected.add(String.valueOf(i));
        }

        Assert.assertEquals(expected, defaultPushServerConfigurationResponse.getRemoteUserGroups());
    }

    @Test
    public void getRemoteUserGroupsNullTest()
    {
        Assert.assertNull(defaultPushServerConfigurationResponse.getRemoteUserGroups());
    }

    @Test
    public void getRemoteAcceptedPatientFieldsTest()
    {
        // populate a JSONObject
        for (int i = 0; i < 10; i++) {
            serverResponse.accumulate("accepted_fields", i);
            expected.add(String.valueOf(i));
        }

        Assert.assertEquals(expected, defaultPushServerConfigurationResponse.getRemoteAcceptedPatientFields());
        Assert.assertEquals(expected, defaultPushServerConfigurationResponse.getRemoteAcceptedPatientFields("string"));
    }

    @Test
    public void getRemoteAcceptedPatientFieldsNullTest()
    {
        Assert.assertNull(defaultPushServerConfigurationResponse.getRemoteAcceptedPatientFields());
        Assert.assertNull(defaultPushServerConfigurationResponse.getRemoteAcceptedPatientFields("string"));
    }

    @Test
    public void getPushableFieldsTest()
    {
        // populate a JSONObject
        for (int i = 0; i < 10; i++) {
            serverResponse.accumulate("accepted_fields", i);
            expected.add(String.valueOf(i));
        }

        Assert.assertTrue(defaultPushServerConfigurationResponse.getPushableFields().isEmpty());
        Assert.assertTrue(defaultPushServerConfigurationResponse.getPushableFields("string").isEmpty());
    }

    @Test
    public void getPushableFieldsNullTest()
    {
        Assert.assertTrue(defaultPushServerConfigurationResponse.getPushableFields().isEmpty());
        Assert.assertTrue(defaultPushServerConfigurationResponse.getPushableFields("string").isEmpty());
    }

    @Test
    public void getPushableFieldsExceptionTest()
    {
        serverResponse = null;
        Assert.assertTrue(defaultPushServerConfigurationResponse.getPushableFields().isEmpty());
    }

    @Test
    public void remoteUpdatesAvailableTest()
    {
        serverResponse.accumulate("updates_enabled", true);
        Assert.assertTrue(defaultPushServerConfigurationResponse.remoteUpdatesEnabled());
    }

    @Test
    public void remoteUpdatesNotAvailableTest()
    {
        // check before anything has been set
        Assert.assertFalse(defaultPushServerConfigurationResponse.remoteUpdatesEnabled());
        serverResponse.accumulate("updates_enabled", false);
        // check after it has been set false
        Assert.assertFalse(defaultPushServerConfigurationResponse.remoteUpdatesEnabled());
    }

    @Test
    public void getRemoteUserTokenTest()
    {
        Assert.assertNull(defaultPushServerConfigurationResponse.getRemoteUserToken());
        serverResponse.accumulate("user_login_token", "yes");
        Assert.assertEquals("yes", defaultPushServerConfigurationResponse.getRemoteUserToken());
    }

}
