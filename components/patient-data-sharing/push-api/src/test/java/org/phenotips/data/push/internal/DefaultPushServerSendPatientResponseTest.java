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

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DefaultPushServerSendPatientResponseTest
{

    private DefaultPushServerSendPatientResponse pushResponse;

    @Before
    public void setUp()
    {
        JSONObject jsonObject = new JSONObject();
        this.pushResponse = new DefaultPushServerSendPatientResponse(jsonObject);
    }

    @Test
    public void isActionFailed_incorrectGroupVerifiesTrueKey()
    {
        this.pushResponse.response.put("incorrect_user_group", true);
        Assert.assertTrue(this.pushResponse.isActionFailed_incorrectGroup());
        this.pushResponse.response.put("incorrect_user_group", false);
        Assert.assertFalse(this.pushResponse.isActionFailed_incorrectGroup());
    }

    @Test
    public void isActionFailed_UpdateDisabledVerifiesTrueKey()
    {
        this.pushResponse.response.put("updates_disabled", true);
        Assert.assertTrue(this.pushResponse.isActionFailed_UpdatesDisabled());
        this.pushResponse.response.put("updates_disabled", false);
        Assert.assertFalse(this.pushResponse.isActionFailed_IncorrectGUID());
    }

    @Test
    public void isActionFailed_IncorrectGUIDVerifiesTrueKey()
    {
        this.pushResponse.response.put("incorrect_guid", true);
        Assert.assertTrue(this.pushResponse.isActionFailed_IncorrectGUID());
        this.pushResponse.response.put("incorrect_guid", false);
        Assert.assertFalse(this.pushResponse.isActionFailed_IncorrectGUID());
    }

    @Test
    public void isActionFailed_GUIDAccessDeniedVerifiesTrueKey()
    {
        this.pushResponse.response.put("guid_access_denied", true);
        Assert.assertTrue(this.pushResponse.isActionFailed_GUIDAccessDenied());
        this.pushResponse.response.put("guid_access_denied", false);
        Assert.assertFalse(this.pushResponse.isActionFailed_GUIDAccessDenied());
    }

    @Test
    public void isActionFailed_knownReasonVerifiesTrueKey()
    {
        this.pushResponse.response.put("guid_access_denied", true);
        Assert.assertTrue(this.pushResponse.isActionFailed_knownReason());
        this.pushResponse.response.put("guid_access_denied", false);
        Assert.assertFalse(this.pushResponse.isActionFailed_knownReason());
    }
}
