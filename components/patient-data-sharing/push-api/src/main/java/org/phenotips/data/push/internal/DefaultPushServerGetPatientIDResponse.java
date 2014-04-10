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
package org.phenotips.data.push.internal;

import org.phenotips.data.push.PushServerGetPatientIDResponse;
import org.phenotips.data.shareprotocol.ShareProtocol;

import net.sf.json.JSONObject;

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
