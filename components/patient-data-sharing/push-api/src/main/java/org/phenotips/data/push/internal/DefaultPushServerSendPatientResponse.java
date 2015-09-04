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

import org.phenotips.data.push.PushServerSendPatientResponse;
import org.phenotips.data.shareprotocol.ShareProtocol;

import net.sf.json.JSONObject;

public class DefaultPushServerSendPatientResponse extends DefaultPushServerGetPatientIDResponse implements
    PushServerSendPatientResponse
{
    DefaultPushServerSendPatientResponse(JSONObject serverResponse)
    {
        super(serverResponse);
    }

    @Override
    public boolean isActionFailed_incorrectGroup()
    {
        return hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_INCORRECTGROUP);
    }

    @Override
    public boolean isActionFailed_UpdatesDisabled()
    {
        return hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_UPDATESDISABLED);
    }

    @Override
    public boolean isActionFailed_IncorrectGUID()
    {
        return hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_INCORRECTGUID);
    }

    @Override
    public boolean isActionFailed_GUIDAccessDenied()
    {
        return hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_GUIDACCESSDENIED);
    }

    @Override public boolean isActionFailed_MissingConsent()
    {
        return hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_MISSINGCONSENT);
    }

    @Override
    public boolean isActionFailed_knownReason()
    {
        return (super.isActionFailed_knownReason() || isActionFailed_incorrectGroup() ||
            isActionFailed_UpdatesDisabled() || isActionFailed_IncorrectGUID() || isActionFailed_GUIDAccessDenied());
    }
}
