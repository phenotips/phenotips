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

public class DefaultPushServerGetPatientIDResponseTest
{

    private DefaultPushServerGetPatientIDResponse pushServerResponse;

    @Before
    public void setUp()
    {
        JSONObject response = new JSONObject();
        this.pushServerResponse = new DefaultPushServerGetPatientIDResponse(response);
    }

    @Test
    public void getRemotePatientGUIDReturnsCorrectValue()
    {
        this.pushServerResponse.response.accumulate("patient_guid", "a");
        Assert.assertEquals("a", this.pushServerResponse.getRemotePatientGUID());
    }

    @Test
    public void getRemotePatientURLReturnsCorrectValue()
    {
        this.pushServerResponse.response.accumulate("patient_url", "b");
        Assert.assertEquals("b", this.pushServerResponse.getRemotePatientURL());
    }

    @Test
    public void getRemotePatientIdReturnsCorrectValue()
    {
        this.pushServerResponse.response.accumulate("patient_id", "c");
        Assert.assertEquals("c", this.pushServerResponse.getRemotePatientID());
    }
}
