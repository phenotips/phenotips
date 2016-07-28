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
import org.phenotips.data.PatientRepository;
import org.phenotips.data.push.PatientPushHistory;
import org.phenotips.data.push.PushPatientData;
import org.phenotips.data.push.PushPatientService;
import org.phenotips.data.push.PushServerConfigurationResponse;
import org.phenotips.data.push.PushServerGetPatientIDResponse;
import org.phenotips.data.push.PushServerInfo;
import org.phenotips.data.push.PushServerSendPatientResponse;
import org.phenotips.data.securestorage.PatientPushedToInfo;
import org.phenotips.data.securestorage.RemoteLoginData;
import org.phenotips.data.securestorage.SecureStorageManager;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
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
public class DefaultPushPatientService implements PushPatientService
{
    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Provides access to the current request context. */
    @Inject
    private Execution execution;

    /** Wrapped API, doing the actual client-server communication. */
    @Inject
    private PushPatientData internalService;

    /** Used for storing login token so they're unaccessible from public scripts. */
    @Inject
    private SecureStorageManager storageManager;

    @Inject
    private PatientRepository patientRepository;

    /** Used for checking access rights. */
    @Inject
    private AuthorizationManager access;

    /** Used for obtaining the current user. */
    @Inject
    private DocumentAccessBridge bridge;

    protected RemoteLoginData getStoredData(String remoteServerIdentifier)
    {
        String localUserName = getLocalUserName();
        return this.storageManager.getRemoteLoginData(localUserName, remoteServerIdentifier);
    }

    protected void storeUserData(String remoteServerIdentifier, String remoteUser, String loginToken)
    {
        String localUserName = getLocalUserName();
        this.storageManager.storeRemoteLoginData(localUserName, remoteServerIdentifier, remoteUser, loginToken);
    }

    protected void removeUserData(String remoteServerIdentifier)
    {
        String localUserName = getLocalUserName();
        this.storageManager.removeRemoteLoginData(localUserName, remoteServerIdentifier);
    }

    private String getLocalUserName()
    {
        XWikiContext context = getXContext();

        if (context.getUserReference() == null) {
            return null;
        }

        return context.getUserReference().getName();
    }

    /**
     * Helper method for obtaining a valid xcontext from the execution context.
     *
     * @return the current request context
     */
    private XWikiContext getXContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    }

    @Override
    public Set<PushServerInfo> getAvailablePushTargets()
    {
        try {
            XWikiContext context = getXContext();

            XWiki xwiki = context.getWiki();
            XWikiDocument prefsDoc =
                xwiki.getDocument(new DocumentReference(context.getWikiId(), "XWiki", "XWikiPreferences"), context);
            List<BaseObject> servers = prefsDoc.getXObjects(new DocumentReference(context.getWikiId(),
                Constants.CODE_SPACE, "PushPatientServer"));

            Set<PushServerInfo> response = new TreeSet<PushServerInfo>();
            if (servers == null || servers.isEmpty()) {
                return response;
            }

            for (BaseObject serverConfiguration : servers) {
                if (serverConfiguration == null) {
                    continue;
                }
                this.logger.debug("   ...available: [{}]",
                    serverConfiguration.getStringValue(DefaultPushPatientData.PUSH_SERVER_CONFIG_ID_PROPERTY_NAME));
                PushServerInfo info = new DefaultPushServerInfo(
                    serverConfiguration.getStringValue(DefaultPushPatientData.PUSH_SERVER_CONFIG_ID_PROPERTY_NAME),
                    serverConfiguration.getStringValue(DefaultPushPatientData.PUSH_SERVER_CONFIG_URL_PROPERTY_NAME),
                    serverConfiguration.getStringValue(DefaultPushPatientData.PUSH_SERVER_CONFIG_DESC_PROPERTY_NAME));
                response.add(info);
            }
            return response;
        } catch (Exception ex) {
            this.logger.error("Failed to get server list: {}", ex.getMessage(), ex);
            return Collections.emptySet();
        }
    }

    @Override
    public Map<PushServerInfo, PatientPushHistory> getPushTargetsWithHistory(String localPatientID)
    {
        Set<PushServerInfo> servers = getAvailablePushTargets();

        Map<PushServerInfo, PatientPushHistory> response = new TreeMap<PushServerInfo, PatientPushHistory>();

        for (PushServerInfo server : servers) {
            PatientPushHistory history = getPatientPushHistory(localPatientID, server.getServerID());
            response.put(server, history);
        }
        return response;
    }

    @Override
    public PatientPushHistory getPatientPushHistory(String localPatientID, String remoteServerIdentifier)
    {
        PatientPushedToInfo pushInfo = this.storageManager.getPatientPushInfo(localPatientID, remoteServerIdentifier);
        PatientPushHistory history = (pushInfo == null) ? null : new DefaultPatientPushHistory(pushInfo);
        return history;
    }

    private Patient getPatientByID(String patientID, String accessLevelName)
    {
        Patient patient = this.patientRepository.get(patientID);
        if (patient == null) {
            return null;
        }

        if (accessLevelName.equals("push")) {
            accessLevelName = "view";
        }

        // FIXME: Access rights should be checked in the script service, not here
        if (!this.access.hasAccess(Right.toRight(accessLevelName), this.bridge.getCurrentUserReference(),
            patient.getDocument())) {
            this.logger.warn("Can't access patient [{}] at level [{}]: access level violation", patientID,
                accessLevelName);
            return null;
        }

        return patient;
    }

    private Set<String> parseJSONArrayIntoSet(String listOfStrings)
    {
        Set<String> fieldSet = null;
        try {
            if (listOfStrings != null) {
                JSONArray fields = new JSONArray(listOfStrings);
                if (fields != null) {
                    fieldSet = new TreeSet<String>();
                    for (Object field : fields) {
                        fieldSet.add(field.toString());
                    }
                }
            }
        } catch (Exception ex) {
            // ignore
        }
        return fieldSet;
    }

    @Override
    public JSONObject getLocalPatientJSON(String patientID, String exportFieldListJSON)
    {
        Patient patient = getPatientByID(patientID, "view");
        if (patient == null) {
            return null;
        }

        return patient.toJSON(parseJSONArrayIntoSet(exportFieldListJSON));
    }

    @Override
    public PushServerConfigurationResponse getRemoteConfiguration(String remoteServerIdentifier, String remoteUserName,
        String password, boolean saveUserToken)
    {
        PushServerConfigurationResponse response =
            this.internalService.getRemoteConfiguration(remoteServerIdentifier, remoteUserName, password, null);

        if (response != null && response.isSuccessful()) {
            if (saveUserToken) {
                // note: even if newly received token is null
                storeUserData(remoteServerIdentifier, remoteUserName, response.getRemoteUserToken());
            } else {
                removeUserData(remoteServerIdentifier);
            }

        }

        return response;
    }

    @Override
    public void removeStoredLoginTokens(String remoteServerIdentifier)
    {
        removeUserData(remoteServerIdentifier);
    }

    @Override
    public PushServerConfigurationResponse getRemoteConfiguration(String remoteServerIdentifier)
    {
        RemoteLoginData storedData = getStoredData(remoteServerIdentifier);
        if (storedData == null || storedData.getRemoteUserName() == null || storedData.getLoginToken() == null) {
            return new DefaultPushServerConfigurationResponse(
                DefaultPushServerResponse.generateIncorrectCredentialsJSON());
        }

        PushServerConfigurationResponse response = this.internalService.getRemoteConfiguration(remoteServerIdentifier,
            storedData.getRemoteUserName(), null, storedData.getLoginToken());

        if (response != null && response.getRemoteUserToken() != null
            && response.getRemoteUserToken() != storedData.getLoginToken()) {
            // update token to newly received one
            storeUserData(remoteServerIdentifier, storedData.getRemoteUserName(), response.getRemoteUserToken());
        }

        return response;
    }

    @Override
    public PushServerSendPatientResponse sendPatient(String patientID, String exportFieldListJSON, String patientState,
        String groupName, String remoteGUID, String remoteServerIdentifier)
    {
        Patient patient = getPatientByID(patientID, "push");
        if (patient == null) {
            return new DefaultPushServerSendPatientResponse(DefaultPushServerResponse.generateActionFailedJSON());
        }
        RemoteLoginData storedData = getStoredData(remoteServerIdentifier);
        if (storedData == null || storedData.getRemoteUserName() == null || storedData.getLoginToken() == null) {
            return new DefaultPushServerSendPatientResponse(
                DefaultPushServerResponse.generateIncorrectCredentialsJSON());
        }

        Set<String> exportFields = parseJSONArrayIntoSet(exportFieldListJSON);
        JSONObject patientStateJSON = this.parsePatientStateToJSON(patientState);

        PushServerSendPatientResponse response = this.internalService.sendPatient(patient, exportFields,
            patientStateJSON, groupName, remoteGUID, remoteServerIdentifier, storedData.getRemoteUserName(), null,
            storedData.getLoginToken());

        if (response != null && response.isSuccessful()) {
            this.storageManager.storePatientPushInfo(patient.getDocument().getName(), remoteServerIdentifier,
                response.getRemotePatientGUID(), response.getRemotePatientID(), response.getRemotePatientURL());
        }
        return response;
    }

    @Override
    public PushServerSendPatientResponse sendPatient(String patientID, String exportFieldListJSON, String patientState,
        String groupName, String remoteGUID, String remoteServerIdentifier, String remoteUserName, String password)
    {
        Patient patient = getPatientByID(patientID, "push");
        if (patient == null) {
            return new DefaultPushServerSendPatientResponse(DefaultPushServerResponse.generateActionFailedJSON());
        }

        Set<String> exportFields = parseJSONArrayIntoSet(exportFieldListJSON);
        JSONObject patientStateJSON = this.parsePatientStateToJSON(patientState);

        PushServerSendPatientResponse response = this.internalService.sendPatient(patient, exportFields,
            patientStateJSON, groupName, remoteGUID, remoteServerIdentifier, remoteUserName, password, null);

        if (response != null && response.isSuccessful()) {
            this.storageManager.storePatientPushInfo(patient.getDocument().getName(), remoteServerIdentifier,
                response.getRemotePatientGUID(), response.getRemotePatientID(), response.getRemotePatientURL());
        }

        return response;
    }

    @Override
    public String getRemoteUsername(String remoteServerIdentifier)
    {
        RemoteLoginData storedData = getStoredData(remoteServerIdentifier);
        if (storedData == null) {
            return null;
        }
        return storedData.getRemoteUserName();
    }

    @Override
    public PushServerGetPatientIDResponse getPatientURL(String remoteServerIdentifier, String remotePatientGUID)
    {
        RemoteLoginData storedData = getStoredData(remoteServerIdentifier);
        if (storedData == null || storedData.getRemoteUserName() == null || storedData.getLoginToken() == null) {
            return new DefaultPushServerGetPatientIDResponse(
                DefaultPushServerResponse.generateIncorrectCredentialsJSON());
        }

        return this.internalService.getPatientURL(remoteServerIdentifier, remotePatientGUID,
            storedData.getRemoteUserName(), null, storedData.getLoginToken());
    }

    @Override
    public PushServerGetPatientIDResponse getPatientURL(String remoteServerIdentifier, String remotePatientGUID,
        String remoteUserName, String password)
    {
        return this.internalService.getPatientURL(remoteServerIdentifier, remotePatientGUID, remoteUserName, password,
            null);
    }

    private JSONObject parsePatientStateToJSON(String patientStateString)
    {
        // since the state comes directly from the user side, taking some basic security precautions
        try {
            JSONObject patientState = new JSONObject(patientStateString);
            // server will have to validate received JSON anyway, so only makin gsure we do send a valid JSON
            return patientState;
        } catch (Exception ex) {
            return new JSONObject();
        }
    }
}
