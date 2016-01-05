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

import org.phenotips.data.push.PushServerGetPatientIDResponse;
import org.phenotips.data.shareprotocol.ShareProtocol;

import org.json.JSONObject;

public class DefaultPushServerGetPatientIDResponse extends DefaultPushServerResponse implements
    PushServerGetPatientIDResponse
{
    DefaultPushServerGetPatientIDResponse(JSONObject serverResponse)
    {
        super(serverResponse);
    }

    @Override
    public String getRemotePatientGUID()
    {
        return valueOrNull(ShareProtocol.SERVER_JSON_PUSH_KEY_NAME_PATIENTGUID);
    }

    @Override
    public String getRemotePatientURL()
    {
        return valueOrNull(ShareProtocol.SERVER_JSON_PUSH_KEY_NAME_PATIENTURL);
    }

    @Override
    public String getRemotePatientID()
    {
        return valueOrNull(ShareProtocol.SERVER_JSON_PUSH_KEY_NAME_PATIENTID);
    }
}
