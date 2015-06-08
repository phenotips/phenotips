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

import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DefaultPushServerResponseTest {

    private DefaultPushServerResponse defaultPushServerResponse;
    private JSONObject response;

    @Before
    public void setUp() {
        response = new JSONObject();
        defaultPushServerResponse = new DefaultPushServerConfigurationResponse(response);
    }

    @Test
    public void generateIncorrectCredentialsJSONWorksCorrectly() {
        JSONObject response = defaultPushServerResponse.generateIncorrectCredentialsJSON();
        Assert.assertFalse(response.getBoolean("success"));
        Assert.assertTrue(response.getBoolean("login_failed"));
        Assert.assertTrue(response.getBoolean("incorrect_credentials"));
    }

    @Test
    public void generateActionFailedJSONWorksCorrectly() {
        JSONObject response = defaultPushServerResponse.generateActionFailedJSON();
        Assert.assertFalse(response.getBoolean("success"));
        Assert.assertTrue(response.getBoolean("action_failed"));
    }

    @Test
    public void hasKeySetToTrueWorksCorrectly()
    {
        response.accumulate("success", false);
        response.accumulate("action_failed", true);
        Assert.assertFalse(defaultPushServerResponse.hasKeySetToTrue("success"));
        Assert.assertTrue(defaultPushServerResponse.hasKeySetToTrue("action_failed"));
    }

    @Test
    public void valueOrNullReturnsValue()
    {
        response.accumulate("the key", "value of the key");
        Assert.assertEquals("value of the key", defaultPushServerResponse.valueOrNull("the key"));

    }

    @Test
    public void valueOrNullReturnsNull()
    {
        Assert.assertNull(defaultPushServerResponse.valueOrNull("not key"));
    }

    @Test
    public void isSuccessfulWorksCorrectly()
    {
        Assert.assertFalse(defaultPushServerResponse.isSuccessful());
        response.accumulate("success", true);
        Assert.assertTrue(defaultPushServerResponse.isSuccessful());
    }

    @Test
    public void isIncorrectProtolcolVersionWorksCorrectly()
    {
        Assert.assertTrue(defaultPushServerResponse.isIncorrectProtocolVersion());
        response.accumulate("response_protocol_version", true);
        Assert.assertFalse(defaultPushServerResponse.isIncorrectProtocolVersion());
        response.accumulate("unsupported_post_protocol_version", true);
        Assert.assertTrue(defaultPushServerResponse.isIncorrectProtocolVersion());
    }

    @Test
    public void isLoginFailedWorksCorrectly()
    {
        response.accumulate("login_failed", true);
        Assert.assertTrue(defaultPushServerResponse.isLoginFailed());
    }

    @Test
    public void isActionFailedTestWorksCorrectly()
    {
        response.accumulate("action_failed", true);
        Assert.assertTrue(defaultPushServerResponse.isActionFailed());
    }
}
