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

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang3.StringUtils;
import org.phenotips.Constants;
import org.phenotips.data.shareprotocol.ShareProtocol;
import org.phenotips.data.Patient;
import org.phenotips.data.push.PushPatientData;
import org.phenotips.data.push.PushServerConfigurationResponse;
import org.phenotips.data.push.PushServerGetPatientIDResponse;
import org.phenotips.data.push.PushServerSendPatientResponse;
import org.phenotips.data.internal.controller.VersionsController;
import org.phenotips.data.internal.PhenoTipsPatient;
import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;

import groovy.lang.Singleton;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation for the {@link PushPatientData} component.
 *
 * @version $Id$
 * @since 1.0M11
 */
@Component
@Singleton
@Named("pushPatientsEvent")  // TODO: remove with EvenListener
public class DefaultPushPatientData implements PushPatientData, EventListener
{
    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Provides access to the current request context. */
    @Inject
    private Execution execution;

    /** HTTP client used for communicating with the remote server. */
    private final HttpClient client = new HttpClient(new MultiThreadedHttpConnectionManager());

    /** Destination page. */
    private static final String PATIENT_DATA_SHARING_PAGE = "/bin/receivePatientData";

    /** Key and value which should be added to POST/GET requests to force XWiki to display raw output without any additional HTML
     *  E.g. http://localhost:8080/bin/receivePatientData?xpage=plain&...
     * */
    private static final String XWIKI_RAW_OUTPUT_KEY   = "xpage";
    private static final String XWIKI_RAW_OUTPUT_VALUE = "plain";

    /** Server configuration ID property name within the PushPatientServer class. */
    public static final String PUSH_SERVER_CONFIG_ID_PROPERTY_NAME  = "name";

    /** Server configuration URL property name within the PushPatientServer class. */
    public static final String PUSH_SERVER_CONFIG_URL_PROPERTY_NAME = "url";

    /** Server configuration URL property name within the PushPatientServer class. */
    public static final String PUSH_SERVER_CONFIG_TOKEN_PROPERTY_NAME = "token";


    //===========================================================================================
    @Override
    public String getName()
    {
        return "sharePatients";
    }

    @Override
    public List<Event> getEvents()
    {
        this.logger.warn("[GET EVENTS] DefaultPushPatientDat");
        return Arrays
            .<Event> asList(new DocumentCreatedEvent(), new DocumentUpdatedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        this.logger.warn("[ON EVENT] DefaultPushPatientData");

        XWikiDocument doc = (XWikiDocument) source;

        if (!isPatient(doc)) {
            this.logger.warn("[HANDLE] not a patient");
            return;
        }

        this.logger.warn("Pushing updated document [{}]", doc.getDocumentReference());

        Patient patient = new PhenoTipsPatient(doc);

        XWikiContext context = getXContext();
        List<BaseObject> servers = getRegisteredServers(context);
        if (servers != null && !servers.isEmpty()) {
            for (BaseObject serverConfiguration : servers) {
                this.logger.warn("   ...pushing to: {} [{}]", serverConfiguration.getStringValue(PUSH_SERVER_CONFIG_ID_PROPERTY_NAME), serverConfiguration.getStringValue("url"));
                fakeSendPatient(patient, serverConfiguration.getStringValue(PUSH_SERVER_CONFIG_ID_PROPERTY_NAME));
            }
        }
    }

    /**
     * Check if the modified document is a patient record.
     *
     * @param doc the modified document
     * @return {@code true} if the document contains a PatientClass object and a non-empty external identifier,
     *         {@code false} otherwise
     */
    private boolean isPatient(XWikiDocument doc)
    {
        BaseObject o = doc.getXObject(Patient.CLASS_REFERENCE);
        return (o != null && !StringUtils.equals("PatientTemplate", doc.getDocumentReference().getName()));
    }

    /**
     * Get all the trusted remote instances where data should be sent that are configured in the current instance.
     *
     * @param context the current request object
     * @return a list of {@link BaseObject XObjects} with LIMS server configurations, may be {@code null}
     */
    private List<BaseObject> getRegisteredServers(XWikiContext context)
    {
        try {
            XWiki xwiki = context.getWiki();
            XWikiDocument prefsDoc =
                xwiki.getDocument(new DocumentReference(xwiki.getDatabase(), "XWiki", "XWikiPreferences"), context);
            return prefsDoc
                .getXObjects(new DocumentReference(xwiki.getDatabase(), Constants.CODE_SPACE, "PushPatientServer"));
        } catch (XWikiException ex) {
            this.logger.error("Failed to get server info: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    private void fakeSendPatient(Patient patient, String remoteServerIdentifier)
    {
        this.logger.warn("===> FAKE UI: Sending to server: {}", remoteServerIdentifier);

        String userName   = "Zzz2";    // these should come from UI
        String password   = "zzz123";
        String user_token = null;
        String groupName  = null;
        String remoteGUID = null;

        PushServerConfigurationResponse loginResponse = getRemoteConfiguration(remoteServerIdentifier, userName, password, user_token);

        if (loginResponse == null || !loginResponse.isSuccessful()) return;

        Set<String> exportFields = loginResponse.getPushableFields(groupName);

        sendPatient(patient, exportFields, groupName, remoteGUID, remoteServerIdentifier, userName, password, user_token);
    }
    //===========================================================================================

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
                                          String userName, String password, String user_token)
    {
        BaseObject serverConfiguration = this.getPushServerConfiguration(remoteServerIdentifier);

        String submitURL = getBaseURL(serverConfiguration);
        if (submitURL == null) return null;

        this.logger.warn("POST URL: {}", submitURL);

        PostMethod method = new PostMethod(submitURL);

        method.addParameter(XWIKI_RAW_OUTPUT_KEY, XWIKI_RAW_OUTPUT_VALUE);
        method.addParameter(ShareProtocol.CLIENT_POST_KEY_NAME_PROTOCOLVER,  ShareProtocol.POST_PROTOCOL_VERSION);
        method.addParameter(ShareProtocol.CLIENT_POST_KEY_NAME_SERVER_TOKEN, getServerToken(serverConfiguration));
        method.addParameter(ShareProtocol.CLIENT_POST_KEY_NAME_ACTION,       actionName);

        method.addParameter(ShareProtocol.CLIENT_POST_KEY_NAME_USERNAME, userName);
        if (user_token != null) {
            method.addParameter(ShareProtocol.CLIENT_POST_KEY_NAME_USER_TOKEN, user_token);
        }
        else {
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
            XWikiDocument prefsDoc = xwiki.getDocument(new DocumentReference(xwiki.getDatabase(), "XWiki", "XWikiPreferences"), context);
            return prefsDoc.getXObject(new DocumentReference(xwiki.getDatabase(), Constants.CODE_SPACE, "PushPatientServer"), PUSH_SERVER_CONFIG_ID_PROPERTY_NAME, serverName);
        } catch (XWikiException ex) {
            this.logger.warn("Failed to get server info: [{}] {}", ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    public PushServerConfigurationResponse getRemoteConfiguration(String remoteServerIdentifier, String userName, String password, String user_token)
    {
        this.logger.warn("===> Getting server configuration: [{}]", remoteServerIdentifier);

        PostMethod method = null;

        try {
            method = generatePostMethod(remoteServerIdentifier,
                                        ShareProtocol.CLIENT_POST_ACTIONKEY_VALUE_INFO,
                                        userName, password, user_token);
            if (method == null) return null;

            int returnCode = this.client.executeMethod(method);

            this.logger.warn("GetConfig HTTP return code: {}", returnCode);

            String response = method.getResponseBodyAsString();

            this.logger.warn("RESPONSE FROM SERVER: {}", response);

            JSONObject responseJSON = (JSONObject)JSONSerializer.toJSON(response);

            return new DefaultPushServerConfigurationResponse(responseJSON);
        } catch (Exception ex) {
            this.logger.error("Failed to login - [{}] {}", ex.getMessage(), ex);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
        return null;
    }

    @Override
    public PushServerSendPatientResponse sendPatient(Patient patient, Set<String> exportFields, String groupName, String remoteGUID,
                                                     String remoteServerIdentifier, String userName, String password, String user_token)
    {
        this.logger.warn("===> Sending to server: [{}]", remoteServerIdentifier);

        PostMethod method = null;

        try {
            method = generatePostMethod(remoteServerIdentifier,
                                        ShareProtocol.CLIENT_POST_ACTIONKEY_VALUE_PUSH,
                                        userName, password, user_token);
            if (method == null) return null;

            exportFields.add(VersionsController.getEnablingFieldName());   // require version information in JSON output

            String patientJSON = patient.toJSON(exportFields).toString();

            this.logger.warn("===> Patient\\Document {} as JSON: {}", patient.getDocument().getName(), patientJSON);

            method.addParameter(ShareProtocol.CLIENT_POST_KEY_NAME_PATIENTJSON,
                                URLEncoder.encode(patientJSON, XWiki.DEFAULT_ENCODING));

            if (groupName != null) {
                method.addParameter(ShareProtocol.CLIENT_POST_KEY_NAME_GROUPNAME, groupName);
            }
            if (remoteGUID != null) {
                method.addParameter(ShareProtocol.CLIENT_POST_KEY_NAME_GUID, remoteGUID);
            }

            int returnCode = this.client.executeMethod(method);

            this.logger.warn("Push HTTP return code: {}", returnCode);

            String response = method.getResponseBodyAsString();

            this.logger.warn("RESPONSE FROM SERVER: {}", response);

            JSONObject responseJSON = (JSONObject)JSONSerializer.toJSON(response);

            return new DefaultPushServerSendPatientResponse(responseJSON);

        } catch (Exception ex) {
            this.logger.error("Failed to push patient: [{}] {}", ex.getMessage(), ex);
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
        this.logger.warn("===> Sending to server: [{}]", remoteServerIdentifier);

        PostMethod method = null;

        try {
            method = generatePostMethod(remoteServerIdentifier,
                                        ShareProtocol.CLIENT_POST_ACTIONKEY_VALUE_GETID,
                                        userName, password, user_token);
            if (method == null) return null;

            method.addParameter(ShareProtocol.CLIENT_POST_KEY_NAME_GUID, remoteGUID);

            int returnCode = this.client.executeMethod(method);

            this.logger.warn("Push HTTP return code: {}", returnCode);

            String response = method.getResponseBodyAsString();

            this.logger.warn("RESPONSE FROM SERVER: {}", response);

            JSONObject responseJSON = (JSONObject)JSONSerializer.toJSON(response);

            return new DefaultPushServerGetPatientIDResponse(responseJSON);

        } catch (Exception ex) {
            this.logger.error("Failed to get patient URL: [{}] {}", ex.getMessage(), ex);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
        return null;
    }
}
