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
package org.phenotips.data.push.script;

import org.phenotips.data.push.PatientPushHistory;
import org.phenotips.data.push.PushPatientService;
import org.phenotips.data.push.PushServerConfigurationResponse;
import org.phenotips.data.push.PushServerGetPatientIDResponse;
import org.phenotips.data.push.PushServerInfo;
import org.phenotips.data.push.PushServerPatientStateResponse;
import org.phenotips.data.push.PushServerSendPatientResponse;
import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.json.JSONObject;

/**
 * API that allows pushing patient data to a remote PhenoTips instance (plus helper methods useful for displaying push
 * patient UI).
 *
 * @version $Id$
 * @since 1.0M11
 */
@Unstable
@Component
@Named("pushPatient")
@Singleton
public class PushPatientScriptService implements ScriptService
{
    /** Wrapped trusted API, doing the actual work. */
    @Inject
    private PushPatientService internalService;

    public Set<PushServerInfo> getAvailablePushTargets()
    {
        return this.internalService.getAvailablePushTargets();
    }

    public Map<PushServerInfo, PatientPushHistory> getPushTargetsWithHistory(String localPatientID)
    {
        return this.internalService.getPushTargetsWithHistory(localPatientID);
    }

    public PatientPushHistory getPatientPushHistory(String localPatientID, String remoteServerIdentifier)
    {
        return this.internalService.getPatientPushHistory(localPatientID, remoteServerIdentifier);
    }

    public JSONObject getLocalPatientJSON(String patientID, String exportFieldListJSON)
    {
        return this.internalService.getLocalPatientJSON(patientID, exportFieldListJSON);
    }

    public String getRemoteUsername(String remoteServerIdentifier)
    {
        return this.internalService.getRemoteUsername(remoteServerIdentifier);
    }

    public void removeStoredLoginTokens(String remoteServerIdentifier)
    {
        this.internalService.removeStoredLoginTokens(remoteServerIdentifier);
    }

    public PushServerConfigurationResponse getRemoteConfiguration(String remoteServerIdentifier)
    {
        return this.internalService.getRemoteConfiguration(remoteServerIdentifier);
    }

    public PushServerConfigurationResponse getRemoteConfiguration(String remoteServerIdentifier,
        String remoteUserName, String password, boolean saveUserToken)
    {
        return this.internalService.getRemoteConfiguration(remoteServerIdentifier, remoteUserName, password,
            saveUserToken);
    }

    public PushServerPatientStateResponse getRemotePatientState(String remoteServerIdentifier, String remoteGuid)
    {
        return this.internalService.getRemotePatientState(remoteServerIdentifier, remoteGuid);
    }

    public PushServerPatientStateResponse getRemotePatientState(String remoteServerIdentifier, String remoteGuid,
        String remoteUserName, String password)
    {
        return this.internalService.getRemotePatientState(remoteServerIdentifier, remoteGuid, remoteUserName, password);
    }

    public PushServerSendPatientResponse sendPatient(String patientID, String exportFieldListJSON, String patientState,
        String groupName, String remoteGUID, String remoteServerIdentifier)
    {
        return this.internalService.sendPatient(patientID, exportFieldListJSON, patientState, groupName,
            remoteGUID, remoteServerIdentifier);
    }

    public PushServerSendPatientResponse sendPatient(String patientID, String exportFieldListJSON, String patientState,
        String groupName, String remoteGUID, String remoteServerIdentifier, String remoteUserName, String password)
    {
        return this.internalService.sendPatient(patientID, exportFieldListJSON, patientState, groupName,
            remoteGUID, remoteServerIdentifier, remoteUserName, password);
    }

    public PushServerGetPatientIDResponse getPatientURL(String remoteServerIdentifier, String remotePatientGUID)
    {
        return this.internalService.getPatientURL(remoteServerIdentifier, remotePatientGUID);
    }

    public PushServerGetPatientIDResponse getPatientURL(String remoteServerIdentifier, String remotePatientGUID,
        String remoteUserName, String password)
    {
        return this.internalService.getPatientURL(remoteServerIdentifier, remotePatientGUID, remoteUserName, password);
    }
}
