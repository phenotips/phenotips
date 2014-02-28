package org.phenotips.data.push.internal;

import net.sf.json.JSONObject;

import org.phenotips.data.shareprotocol.ShareProtocol;
import org.phenotips.data.push.PushServerSendPatientResponse;

public class DefaultPushServerSendPatientResponse extends DefaultPushServerGetPatientIDResponse implements PushServerSendPatientResponse
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
                isActionFailed_UpdatesDisabled()   || isActionFailed_IncorrectGUID() ||
                isActionFailed_GUIDAccessDenied());
    }
}
