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

    @Override
    public boolean isActionFailed_knownReason()
    {
        return (super.isActionFailed_knownReason() || isActionFailed_incorrectGroup() ||
            isActionFailed_UpdatesDisabled() || isActionFailed_IncorrectGUID() || isActionFailed_GUIDAccessDenied());
    }
}
