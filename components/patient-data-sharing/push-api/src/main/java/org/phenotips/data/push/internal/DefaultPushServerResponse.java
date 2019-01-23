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

import org.phenotips.data.push.PushServerResponse;
import org.phenotips.data.shareprotocol.ShareProtocol;

import org.json.JSONObject;

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
        response.put(ShareProtocol.SERVER_JSON_KEY_NAME_SUCCESS, false);
        response.put(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_LOGINFAILED, true);
        response.put(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_WRONGCREDENTIALS, true);
        return response;
    }

    public static JSONObject generateActionFailedJSON()
    {
        JSONObject response = new JSONObject();
        response.put(ShareProtocol.SERVER_JSON_KEY_NAME_SUCCESS, false);
        response.put(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_ACTIONFAILED, true);
        return response;
    }

    protected boolean hasKeySetToTrue(String key)
    {
        return this.response.has(key) && this.response.getBoolean(key);
    }

    protected String valueOrNull(String key)
    {
        if (!this.response.has(key)) {
            return null;
        }

        return this.response.getString(key);
    }

    @Override
    public String getServerProtocolVersion()
    {
        return this.response.optString(ShareProtocol.SERVER_JSON_KEY_NAME_PROTOCOLVER, null);
    }

    @Override
    public boolean isSuccessful()
    {
        return hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_SUCCESS);
    }

    @Override
    public boolean isServerDoesNotAcceptClientProtocolVersion()
    {
        return hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_PROTOCOLFAILED) ||
            !this.response.has(ShareProtocol.SERVER_JSON_KEY_NAME_PROTOCOLVER);
    }

    @Override
    public boolean isClientDoesNotAcceptServerProtocolVersion()
    {
        return false;
    }

    @Override
    public boolean isLoginFailed()
    {
        return hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_LOGINFAILED);
    }

    @Override
    public boolean isActionFailed()
    {
        return hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_ACTIONFAILED);
    }

    @Override
    public boolean isLoginFailed_knownReason()
    {
        return (isLoginFailed_UnauthorizedServer() ||
            isLoginFailed_IncorrectCredentials() ||
            isLoginFailed_TokensNotSuported() ||
            isLoginFailed_UserTokenExpired());
    }

    @Override
    public boolean isLoginFailed_UnauthorizedServer()
    {
        return isLoginFailed() && hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_UNTRUSTEDSERVER);
    }

    @Override
    public boolean isLoginFailed_IncorrectCredentials()
    {
        return isLoginFailed() && hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_WRONGCREDENTIALS);
    }

    @Override
    public boolean isLoginFailed_UsernameNotCanonical()
    {
        return isLoginFailed()
                && this.response.has(ShareProtocol.SERVER_JSON_KEY_NAME_USERNAME_NOT_ACCEPTED);
    }

    @Override
    public boolean isLoginFailed_UserTokenExpired()
    {
        return isLoginFailed() && hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_EXPIREDUSERTOKEN);
    }

    @Override
    public boolean isLoginFailed_TokensNotSuported()
    {
        return isLoginFailed() && hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_NOUSERTOKENS);
    }

    @Override
    public boolean isActionFailed_knownReason()
    {
        return isActionFailed_isUnknownAction();
    }

    @Override
    public boolean isActionFailed_isUnknownAction()
    {
        return isActionFailed() && hasKeySetToTrue(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_UNSUPPORTEDOP);
    }
}
