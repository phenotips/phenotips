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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.TreeMap;

import javax.inject.Inject;
import javax.inject.Named;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.phenotips.Constants;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.push.PushPatientData;
import org.phenotips.data.push.PushPatientService;
import org.phenotips.data.push.PushServerConfigurationResponse;
import org.phenotips.data.push.PushServerGetPatientIDResponse;
import org.phenotips.data.push.PushServerSendPatientResponse;
import org.phenotips.data.push.internal.DefaultPushServerConfigurationResponse;
import org.phenotips.data.push.internal.DefaultPushServerGetPatientIDResponse;
import org.phenotips.data.push.internal.DefaultPushServerSendPatientResponse;
import org.phenotips.data.securestorage.SecureStorageManager;
import org.phenotips.data.securestorage.RemoteLoginData;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;

import groovy.lang.Singleton;

import org.xwiki.model.reference.DocumentReference;

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

    /** Used for login token storage in a way unavailable to web pages */
    @Inject
    private SecureStorageManager storageManager;

    @Inject
    private PatientRepository patientRepository;


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

    private String getLocalUserName()
    {
        XWikiContext context = getXContext();

        if (context.getUserReference() == null)
            return null;

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
    public Set<String> getAvailablePushTargets()
    {
        try {
            XWikiContext context = getXContext();

            XWiki xwiki = context.getWiki();
            XWikiDocument prefsDoc =
                xwiki.getDocument(new DocumentReference(xwiki.getDatabase(), "XWiki", "XWikiPreferences"), context);
            List<BaseObject> servers = prefsDoc.getXObjects(new DocumentReference(xwiki.getDatabase(), Constants.CODE_SPACE, "PushPatientServer"));

            Set<String> response = new TreeSet<String>();
            for (BaseObject serverConfiguration : servers) {
                this.logger.warn("   ...available: [{}]", serverConfiguration.getStringValue(DefaultPushPatientData.PUSH_SERVER_CONFIG_ID_PROPERTY_NAME));
                response.add(serverConfiguration.getStringValue(DefaultPushPatientData.PUSH_SERVER_CONFIG_ID_PROPERTY_NAME));
            }
            return response;
        } catch (Exception ex) {
            this.logger.error("Failed to get server list: [{}] {}", ex.getMessage(), ex);
            return Collections.emptySet();
        }
    }

    @Override
    public Map<String, Long> getAvailablePushTargets(String patientID)
    {
        Set<String> servers = getAvailablePushTargets();

        Map<String, Long> response = new TreeMap<String, Long>();

        for (String server: servers) {
            long ageInDays = this.storageManager.getLastPushAgeInDays(patientID, server);

            response.put(server, ageInDays);
        }
        return response;
    }

    private Patient getPatientByID(String patientID)
    {
        Patient patient = this.patientRepository.getPatientById(patientID);
        return patient;
    }

    @Override
    public JSONObject getLocalPatientJSON(String patientID, String exportFieldListJSON)
    {
        Patient patient = getPatientByID(patientID);
        if (patient == null) {
            return null;
        }

        Set<String> fieldSet = null;
        if (exportFieldListJSON != null) {
            JSONArray fields = JSONArray.fromObject(exportFieldListJSON);
            if (fields != null) {
                fieldSet = new TreeSet<String>();
                for (Object field: fields) {
                    fieldSet.add(field.toString());
                }
            }
        }

        return patient.toJSON(fieldSet);
    }

    @Override
    public PushServerConfigurationResponse getRemoteConfiguration(String remoteServerIdentifier, String remoteUserName, String password)
    {
        PushServerConfigurationResponse response = this.internalService.getRemoteConfiguration(remoteServerIdentifier, remoteUserName, password, null);

        storeUserData(remoteServerIdentifier, remoteUserName, response.getRemoteUserToken());   // note: even if newly received token is null

        return response;
    }

    @Override
    public PushServerConfigurationResponse getRemoteConfiguration(String remoteServerIdentifier)
    {
        RemoteLoginData storedData = getStoredData(remoteServerIdentifier);
        if (storedData == null || storedData.getRemoteUserName() == null || storedData.getLoginToken() == null)
            return new DefaultPushServerConfigurationResponse(DefaultPushServerResponse.generateIncorrectCredentialsJSON());

        PushServerConfigurationResponse response = this.internalService.getRemoteConfiguration(remoteServerIdentifier,
                                                      storedData.getRemoteUserName(), null, storedData.getLoginToken());

        if (response.getRemoteUserToken() != null && response.getRemoteUserToken() != storedData.getLoginToken()) {
            storeUserData(remoteServerIdentifier, storedData.getRemoteUserName(), response.getRemoteUserToken());  // update token to newly received one
        }

        return response;
    }

    @Override
    public PushServerSendPatientResponse sendPatient(String patientID, Set<String> exportFields, String groupName,
                                                     String remoteGUID, String remoteServerIdentifier)
    {
        Patient patient = getPatientByID(patientID);
        if (patient == null) {
            return new DefaultPushServerSendPatientResponse(DefaultPushServerResponse.generateActionFailedJSON());
        }
        RemoteLoginData storedData = getStoredData(remoteServerIdentifier);
        if (storedData == null || storedData.getRemoteUserName() == null || storedData.getLoginToken() == null)
            return new DefaultPushServerSendPatientResponse(DefaultPushServerResponse.generateIncorrectCredentialsJSON());

        PushServerSendPatientResponse response =  this.internalService.sendPatient(patient, exportFields, groupName, remoteGUID, remoteServerIdentifier,
                                                                                   storedData.getRemoteUserName(), null, storedData.getLoginToken());

        if (response.isSuccessful()) {
            this.storageManager.storePatientPushInfo(patient.getDocument().getName(), remoteServerIdentifier);
        }
        return response;
    }

    @Override
    public PushServerSendPatientResponse sendPatient(String patientID, Set<String> exportFields, String groupName,
                                                     String remoteGUID, String remoteServerIdentifier,
                                                     String remoteUserName, String password)
    {
        Patient patient = getPatientByID(patientID);
        if (patient == null) {
            return new DefaultPushServerSendPatientResponse(DefaultPushServerResponse.generateActionFailedJSON());
        }

        PushServerSendPatientResponse response = this.internalService.sendPatient(patient, exportFields, groupName, remoteGUID, remoteServerIdentifier,
                                                                                  remoteUserName, password, null);

        if (response.isSuccessful()) {
            this.storageManager.storePatientPushInfo(patient.getDocument().getName(), remoteServerIdentifier);
        }
        return response;
    }

    @Override
    public String getRemoteUsername(String remoteServerIdentifier)
    {
        RemoteLoginData storedData = getStoredData(remoteServerIdentifier);
        if (storedData == null) return null;
        return storedData.getRemoteUserName();
    }

    @Override
    public PushServerGetPatientIDResponse getPatientURL(String remoteServerIdentifier, String remotePatientGUID)
    {
        RemoteLoginData storedData = getStoredData(remoteServerIdentifier);
        if (storedData == null || storedData.getRemoteUserName() == null || storedData.getLoginToken() == null)
            return new DefaultPushServerGetPatientIDResponse(DefaultPushServerResponse.generateIncorrectCredentialsJSON());

        return this.internalService.getPatientURL(remoteServerIdentifier, remotePatientGUID,
                                                  storedData.getRemoteUserName(), null, storedData.getLoginToken());
    }

    @Override
    public PushServerGetPatientIDResponse getPatientURL(String remoteServerIdentifier, String remotePatientGUID, String remoteUserName, String password)
    {
        return this.internalService.getPatientURL(remoteServerIdentifier, remotePatientGUID, remoteUserName, password, null);

    }
}
