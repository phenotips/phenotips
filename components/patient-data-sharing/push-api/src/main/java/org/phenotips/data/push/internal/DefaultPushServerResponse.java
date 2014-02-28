package org.phenotips.data.push.internal;

import net.sf.json.JSONObject;

import org.phenotips.data.shareprotocol.ShareProtocol;
import org.phenotips.data.push.PushServerResponse;

public class DefaultPushServerResponse implements PushServerResponse
{
    protected final JSONObject response;

    DefaultPushServerResponse(JSONObject serverResponse)
    {
        this.response = serverResponse;
    }

    // TODO: come up with a better way to generate server responses of any kind locally
    public static JSONObject generateIncorrectCredentialsJSON()
    {
        JSONObject response = new JSONObject();
        response.element(ShareProtocol.SERVER_JSON_KEY_NAME_SUCCESS, false);
        response.element(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_LOGINFAILED, true);
        response.element(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_WRONGCREDENTIALS, true);
        return response;
    }

    public static JSONObject generateActionFailedJSON()
    {
        JSONObject response = new JSONObject();
        response.element(ShareProtocol.SERVER_JSON_KEY_NAME_SUCCESS, false);
        response.element(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_ACTIONFAILED, true);
        return response;
    }

    protected boolean hasKeySetToTrue(String key)
    {
        return response.containsKey(key) && response.getBoolean(key);
    }

    protected String valueOrNull(String key)
    {
        if (!response.containsKey(key))
            return null;

        return response.getString(key);
    }

    @Override
    public boolean isSuccessful()
    {
        return hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_SUCCESS);
    }

    @Override
    public boolean isIncorrectProtocolVersion() {
        return hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_PROTOCOLFAILED);
    }

    @Override
    public boolean isLoginFailed() {
        return hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_LOGINFAILED);
    }

    @Override
    public boolean isActionFailed() {
        return hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_ACTIONFAILED);
    }

    @Override
    public boolean isLoginFailed_knownReason() {
        return (isLoginFailed_UnauthorizedServer() || isLoginFailed_IncorrectCredentials() ||
                isLoginFailed_TokensNotSuported()  || isLoginFailed_UserTokenExpired());
    }

    @Override
    public boolean isLoginFailed_UnauthorizedServer() {
        return isLoginFailed() && hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_UNTRUSTEDSERVER);
    }

    @Override
    public boolean isLoginFailed_IncorrectCredentials() {
        return isLoginFailed() && hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_WRONGCREDENTIALS);
    }

    @Override
    public boolean isLoginFailed_UserTokenExpired() {
        return isLoginFailed() && hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_EXPIREDUSERTOKEN);
    }

    @Override
    public boolean isLoginFailed_TokensNotSuported() {
        return isLoginFailed() && hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_NOUSERTOKENS);
    }

    @Override
    public boolean isActionFailed_knownReason()
    {
        return isActionFailed_isUnknownAction();
    }

    @Override
    public boolean isActionFailed_isUnknownAction() {
        return isActionFailed() && hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_UNSUPPORTEDOP);
    }
}
