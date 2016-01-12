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

import org.phenotips.data.Consent;
import org.phenotips.data.ConsentManager;
import org.phenotips.data.push.PushServerPatientStateResponse;
import org.phenotips.data.shareprotocol.ShareProtocol;

import java.util.List;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class DefaultPushServerPatientStateResponse extends DefaultPushServerResponse implements
    PushServerPatientStateResponse
{
    private ConsentManager consentManager;

    DefaultPushServerPatientStateResponse(JSONObject serverResponse, ConsentManager consentManager)
    {
        super(serverResponse);
        this.consentManager = consentManager;
    }

    /** To be used when the server response is an error. */
    DefaultPushServerPatientStateResponse(JSONObject serverResponse)
    {
        super(serverResponse);
    }


    @Override public List<Consent> getConsents()
    {
        if (consentManager == null) {
            return null;
        }
        /* todo. the underlying function is not implemented */
        return consentManager.fromJson(this.getConsentsAsJson());
    }

    @Override public JSON getConsentsAsJson()
    {
        JSONArray consents = this.response.optJSONArray(ShareProtocol.SERVER_JSON_GETPATIENTSTATE_KEY_NAME_CONSENTS);
        if (consents == null) {
            return null;
        }
        return consents;
    }
}
