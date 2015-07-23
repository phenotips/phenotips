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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
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
    private final CloseableHttpClient client = HttpClients.createSystem();

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

    private HttpPost generateRequest(String remoteServerIdentifier, List<NameValuePair> data)
    {
        BaseObject serverConfiguration = this.getPushServerConfiguration(remoteServerIdentifier);

        String submitURL = getBaseURL(serverConfiguration);
        if (submitURL == null) {
            return null;
        }

        this.logger.trace("POST URL: {}", submitURL);

        HttpPost method = new HttpPost(submitURL);

        method.setEntity(new UrlEncodedFormEntity(data, Consts.UTF_8));

        return method;
    }

    private List<NameValuePair> generateRequestData(String actionName, String userName, String password,
        String userToken)
    {
        List<NameValuePair> result = new LinkedList<>();
        result.add(new BasicNameValuePair(XWIKI_RAW_OUTPUT_KEY, XWIKI_RAW_OUTPUT_VALUE));
        result.add(new BasicNameValuePair(ShareProtocol.CLIENT_POST_KEY_NAME_PROTOCOLVER,
            ShareProtocol.POST_PROTOCOL_VERSION));
        result.add(new BasicNameValuePair(ShareProtocol.CLIENT_POST_KEY_NAME_ACTION, actionName));
        result.add(new BasicNameValuePair(ShareProtocol.CLIENT_POST_KEY_NAME_USERNAME, userName));
        if (StringUtils.isNotBlank(userToken)) {
            result.add(new BasicNameValuePair(ShareProtocol.CLIENT_POST_KEY_NAME_USER_TOKEN, userToken));
        } else {
            result.add(new BasicNameValuePair(ShareProtocol.CLIENT_POST_KEY_NAME_PASSWORD, password));
        }
        return result;
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
                xwiki.getDocument(new DocumentReference(context.getWikiId(), "XWiki", "XWikiPreferences"), context);
            return prefsDoc.getXObject(new DocumentReference(context.getWikiId(), Constants.CODE_SPACE,
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

        HttpPost method = null;

        try {
            method = generateRequest(remoteServerIdentifier,
                generateRequestData(ShareProtocol.CLIENT_POST_ACTIONKEY_VALUE_INFO, userName, password, userToken));
            if (method == null) {
                return null;
            }

            try (CloseableHttpResponse httpResponse = this.client.execute(method)) {
                int returnCode = httpResponse.getStatusLine().getStatusCode();
                this.logger.trace("GetConfig HTTP return code: {}", returnCode);

                String response = IOUtils.toString(httpResponse.getEntity().getContent(), Consts.UTF_8);

                // Can't be valid JSON with less than 2 characters: most likely empty response from an un-accepting
                // server
                if (response.length() < 2) {
                    return null;
                }

                JSONObject responseJSON = (JSONObject) JSONSerializer.toJSON(response);

                return new DefaultPushServerConfigurationResponse(responseJSON);
            }
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

        HttpPost method = null;

        try {
            List<NameValuePair> data =
                generateRequestData(ShareProtocol.CLIENT_POST_ACTIONKEY_VALUE_PUSH, userName, password, userToken);
            if (exportFields != null) {
                // Version information is required in the JSON; when exportFields is null everything is included anyway
                exportFields.add(VersionsController.getEnablingFieldName());
            }

            String patientJSON = patient.toJSON(exportFields).toString();

            data.add(new BasicNameValuePair(ShareProtocol.CLIENT_POST_KEY_NAME_PATIENTJSON,
                URLEncoder.encode(patientJSON, XWiki.DEFAULT_ENCODING)));

            if (groupName != null) {
                data.add(new BasicNameValuePair(ShareProtocol.CLIENT_POST_KEY_NAME_GROUPNAME, groupName));
            }
            if (remoteGUID != null) {
                data.add(new BasicNameValuePair(ShareProtocol.CLIENT_POST_KEY_NAME_GUID, remoteGUID));
            }

            method = generateRequest(remoteServerIdentifier, data);
            if (method == null) {
                return null;
            }
            try (CloseableHttpResponse httpResponse = this.client.execute(method)) {
                int returnCode = httpResponse.getStatusLine().getStatusCode();
                this.logger.trace("Push HTTP return code: {}", returnCode);

                String response = IOUtils.toString(httpResponse.getEntity().getContent(), Consts.UTF_8);
                this.logger.trace("RESPONSE FROM SERVER: {}", response);
                JSONObject responseJSON = (JSONObject) JSONSerializer.toJSON(response);

                return new DefaultPushServerSendPatientResponse(responseJSON);
            }
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
        String userName, String password, String userToken)
    {
        this.logger.debug("===> Contacting server: [{}]", remoteServerIdentifier);

        HttpPost method = null;

        try {
            List<NameValuePair> data =
                generateRequestData(ShareProtocol.CLIENT_POST_ACTIONKEY_VALUE_GETID, userName, password, userToken);
            data.add(new BasicNameValuePair(ShareProtocol.CLIENT_POST_KEY_NAME_GUID, remoteGUID));

            method = generateRequest(remoteServerIdentifier, data);
            if (method == null) {
                return null;
            }

            try (CloseableHttpResponse httpResponse = this.client.execute(method)) {
                int returnCode = httpResponse.getStatusLine().getStatusCode();
                this.logger.trace("Push HTTP return code: {}", returnCode);

                String response = IOUtils.toString(httpResponse.getEntity().getContent(), Consts.UTF_8);
                this.logger.trace("RESPONSE FROM SERVER: {}", response);
                JSONObject responseJSON = (JSONObject) JSONSerializer.toJSON(response);

                return new DefaultPushServerGetPatientIDResponse(responseJSON);
            }
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
