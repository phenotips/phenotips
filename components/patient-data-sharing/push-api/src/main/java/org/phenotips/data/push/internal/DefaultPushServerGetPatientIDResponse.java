package org.phenotips.data.push.internal;

import net.sf.json.JSONObject;

import org.phenotips.data.shareprotocol.ShareProtocol;
import org.phenotips.data.push.PushServerGetPatientIDResponse;

public class DefaultPushServerGetPatientIDResponse extends DefaultPushServerResponse implements PushServerGetPatientIDResponse
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
