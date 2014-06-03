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

import org.phenotips.Constants;
import org.phenotips.data.Patient;
import org.phenotips.data.internal.controller.VersionsController;
import org.phenotips.data.push.PushPatientData;
import org.phenotips.data.push.PushServerConfigurationResponse;
import org.phenotips.data.push.PushServerGetPatientIDResponse;
import org.phenotips.data.push.PushServerSendPatientResponse;
import org.phenotips.data.shareprotocol.ShareProtocol;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;

import java.net.URLEncoder;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

/**
 * Default implementation for the {@link PushPatientData} component.
 *
 * @version $Id$
 * @since 1.0M11
 */
@Component
@Singleton
public class DefaultPushPatientData implements PushPatientData
{
    /** Server configuration ID property name within the PushPatientServer class. */
    public static final String PUSH_SERVER_CONFIG_ID_PROPERTY_NAME = "name";

    /** Server configuration URL property name within the PushPatientServer class. */
    public static final String PUSH_SERVER_CONFIG_URL_PROPERTY_NAME = "url";

    /** Server configuration Description property name within the PushPatientServer class. */
    public static final String PUSH_SERVER_CONFIG_DESC_PROPERTY_NAME = "description";

    /** Server configuration URL property name within the PushPatientServer class. */
    public static final String PUSH_SERVER_CONFIG_TOKEN_PROPERTY_NAME = "token";

    /** Destination page. */
    private static final String PATIENT_DATA_SHARING_PAGE = "/bin/receivePatientData";

    /**
     * Key and value which should be added to POST/GET requests to force XWiki to display raw output without any
     * additional HTML E.g. http://localhost:8080/bin/receivePatientData?xpage=plain&...
     */
    private static final String XWIKI_RAW_OUTPUT_KEY = "xpage";

    private static final String XWIKI_RAW_OUTPUT_VALUE = "plain";

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Provides access to the current request context. */
    @Inject
    private Execution execution;

    /** HTTP client used for communicating with the remote server. */
    private final HttpClient client = new HttpClient(new MultiThreadedHttpConnectionManager());

    /**
     * Helper method for obtaining a valid xcontext from the execution context.
     *
     * @return the current request context
     */
    private XWikiContext getXContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    }

    /**
     * Return the the URL of the specified remote PhenoTips instance.
     *
     * @param serverConfiguration the XObject holding the remote server configuration
     * @return the configured URL, in the format {@code http://remote.host.name/bin/}, or {@code null} if the
     *         configuration isn't valid
     */
    private String getBaseURL(BaseObject serverConfiguration)
    {
        if (serverConfiguration != null) {
            String result = serverConfiguration.getStringValue(PUSH_SERVER_CONFIG_URL_PROPERTY_NAME);
            if (StringUtils.isBlank(result)) {
                return null;
            }
            if (!result.startsWith("http")) {
                result = "http://" + result;
            }
            return StringUtils.stripEnd(result, "/") + PATIENT_DATA_SHARING_PAGE;
        }
        return null;
    }

    /**
     * Return the token given for the specified remote PhenoTips instance.
     *
     * @param serverConfiguration the XObject holding the remote server configuration
     * @return the token, as a free-form string of characters
     */
    private String getServerToken(BaseObject serverConfiguration)
    {
        if (serverConfiguration != null) {
            return serverConfiguration.getStringValue(PUSH_SERVER_CONFIG_TOKEN_PROPERTY_NAME);
        }
        return null;
    }

    private PostMethod generatePostMethod(String remoteServerIdentifier, String actionName,
        String userName, String password, String userToken)
    {
        BaseObject serverConfiguration = this.getPushServerConfiguration(remoteServerIdentifier);

        String submitURL = getBaseURL(serverConfiguration);
        if (submitURL == null) {
            return null;
        }

        this.logger.trace("POST URL: {}", submitURL);

        PostMethod method = new PostMethod(submitURL);

        method.addParameter(XWIKI_RAW_OUTPUT_KEY, XWIKI_RAW_OUTPUT_VALUE);
        method.addParameter(ShareProtocol.CLIENT_POST_KEY_NAME_PROTOCOLVER, ShareProtocol.POST_PROTOCOL_VERSION);
        method.addParameter(ShareProtocol.CLIENT_POST_KEY_NAME_SERVER_TOKEN, getServerToken(serverConfiguration));
        method.addParameter(ShareProtocol.CLIENT_POST_KEY_NAME_ACTION, actionName);

        method.addParameter(ShareProtocol.CLIENT_POST_KEY_NAME_USERNAME, userName);
        if (userToken != null) {
            method.addParameter(ShareProtocol.CLIENT_POST_KEY_NAME_USER_TOKEN, userToken);
        } else {
            method.addParameter(ShareProtocol.CLIENT_POST_KEY_NAME_PASSWORD, password);
        }

        return method;
    }

    /**
     * Get the push server configuration given its name.
     *
     * @param serverName name of the server
     * @return {@link BaseObject XObjects} with server configuration, may be {@code null}
     */
    private BaseObject getPushServerConfiguration(String serverName)
    {
        try {
            XWikiContext context = getXContext();
            XWiki xwiki = context.getWiki();
            XWikiDocument prefsDoc =
                xwiki.getDocument(new DocumentReference(context.getDatabase(), "XWiki", "XWikiPreferences"), context);
            return prefsDoc.getXObject(new DocumentReference(context.getDatabase(), Constants.CODE_SPACE,
                "PushPatientServer"), PUSH_SERVER_CONFIG_ID_PROPERTY_NAME, serverName);
        } catch (XWikiException ex) {
            this.logger.warn("Failed to get server info: {}", ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    public PushServerConfigurationResponse getRemoteConfiguration(String remoteServerIdentifier, String userName,
        String password, String userToken)
    {
        this.logger.debug("===> Getting server configuration for: [{}]", remoteServerIdentifier);

        PostMethod method = null;

        try {
            method = generatePostMethod(remoteServerIdentifier, ShareProtocol.CLIENT_POST_ACTIONKEY_VALUE_INFO,
                userName, password, userToken);
            if (method == null) {
                return null;
            }

            int returnCode = this.client.executeMethod(method);

            this.logger.trace("GetConfig HTTP return code: {}", returnCode);

            String response = method.getResponseBodyAsString();

            // can't be valid JSOn with less than 2 characters: most likely empty response from an un-accepting server
            if (response.length() < 2) {
                return null;
            }

            JSONObject responseJSON = (JSONObject) JSONSerializer.toJSON(response);

            return new DefaultPushServerConfigurationResponse(responseJSON);
        } catch (Exception ex) {
            this.logger.error("Failed to login: {}", ex.getMessage(), ex);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
        return null;
    }

    @Override
    public PushServerSendPatientResponse sendPatient(Patient patient, Set<String> exportFields, String groupName,
        String remoteGUID, String remoteServerIdentifier, String userName, String password, String userToken)
    {
        this.logger.debug("===> Sending to server: [{}]", remoteServerIdentifier);

        PostMethod method = null;

        try {
            method = generatePostMethod(remoteServerIdentifier, ShareProtocol.CLIENT_POST_ACTIONKEY_VALUE_PUSH,
                userName, password, userToken);
            if (method == null) {
                return null;
            }

            if (exportFields != null) {
                // Version information is required in the JSON; when exportFields is null everything is included anyway
                exportFields.add(VersionsController.getEnablingFieldName());
            }

            String patientJSON = patient.toJSON(exportFields).toString();

            method.addParameter(ShareProtocol.CLIENT_POST_KEY_NAME_PATIENTJSON,
                URLEncoder.encode(patientJSON, XWiki.DEFAULT_ENCODING));

            if (groupName != null) {
                method.addParameter(ShareProtocol.CLIENT_POST_KEY_NAME_GROUPNAME, groupName);
            }
            if (remoteGUID != null) {
                method.addParameter(ShareProtocol.CLIENT_POST_KEY_NAME_GUID, remoteGUID);
            }

            int returnCode = this.client.executeMethod(method);

            this.logger.trace("Push HTTP return code: {}", returnCode);

            String response = method.getResponseBodyAsString();

            JSONObject responseJSON = (JSONObject) JSONSerializer.toJSON(response);

            return new DefaultPushServerSendPatientResponse(responseJSON);

        } catch (Exception ex) {
            this.logger.error("Failed to push patient: {}", ex.getMessage(), ex);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
        return null;
    }

    @Override
    public PushServerGetPatientIDResponse getPatientURL(String remoteServerIdentifier, String remoteGUID,
        String userName, String password, String user_token)
    {
        this.logger.debug("===> Contacting server: [{}]", remoteServerIdentifier);

        PostMethod method = null;

        try {
            method = generatePostMethod(remoteServerIdentifier, ShareProtocol.CLIENT_POST_ACTIONKEY_VALUE_GETID,
                userName, password, user_token);
            if (method == null) {
                return null;
            }

            method.addParameter(ShareProtocol.CLIENT_POST_KEY_NAME_GUID, remoteGUID);

            int returnCode = this.client.executeMethod(method);
            this.logger.trace("Push HTTP return code: {}", returnCode);

            String response = method.getResponseBodyAsString();
            this.logger.trace("RESPONSE FROM SERVER: {}", response);

            JSONObject responseJSON = (JSONObject) JSONSerializer.toJSON(response);

            return new DefaultPushServerGetPatientIDResponse(responseJSON);
        } catch (Exception ex) {
            this.logger.error("Failed to get patient URL: {}", ex.getMessage(), ex);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
        return null;
    }
}
