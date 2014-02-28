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
package org.phenotips.data.push.script;

import net.sf.json.JSONObject;

import org.phenotips.data.Patient;
import org.phenotips.data.push.PushPatientService;
import org.phenotips.data.push.PushServerConfigurationResponse;
import org.phenotips.data.push.PushServerGetPatientIDResponse;
import org.phenotips.data.push.PushServerSendPatientResponse;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.Map;
import java.util.Set;

import groovy.lang.Singleton;

/**
 * API that allows pushing patient data to a remote PhenoTips instance.
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

    /** Logging helper object. */
    @Inject
    private Logger logger;

    public Set<String> getAvailablePushTargets()
    {
        this.logger.warn("[SCRIPTSERVICE0] Get available");
        return this.internalService.getAvailablePushTargets();
    }

    public Map<String, Long> getAvailablePushTargets(String patientID)
    {
        this.logger.warn("[SCRIPTSERVICE0] Get available (patientID)");
        return this.internalService.getAvailablePushTargets(patientID);
    }

    public JSONObject getLocalPatientJSON(String patientID, Set<String> exportFields)
    {
        this.logger.warn("[SCRIPTSERVICE0] PATIENTJSON");
        return this.internalService.getLocalPatientJSON(patientID, exportFields);
    }

    public String getRemoteUsername(String remoteServerIdentifier)
    {
        this.logger.warn("[SCRIPTSERVICE0] Get remote user");
        return this.internalService.getRemoteUsername(remoteServerIdentifier);
    }

    public PushServerConfigurationResponse getRemoteConfiguration(String remoteServerIdentifier)
    {
        this.logger.warn("[SCRIPTSERVICE0] Get config");
        return this.internalService.getRemoteConfiguration(remoteServerIdentifier);
    }

    public PushServerConfigurationResponse getRemoteConfiguration(String remoteServerIdentifier,
                                                                  String remoteUserName, String password)
    {
        this.logger.warn("[SCRIPTSERVICE0] Get config (user/pasword)");
        return this.internalService.getRemoteConfiguration(remoteServerIdentifier, remoteUserName, password);
    }

    public PushServerSendPatientResponse sendPatient(String patientID, Set<String> exportFields, String groupName,
                                                     String remoteGUID, String remoteServerIdentifier)
    {
        this.logger.warn("[SCRIPTSERVICE0] Send");
        return this.internalService.sendPatient(patientID, exportFields, groupName, remoteGUID, remoteServerIdentifier);
    }

    public PushServerSendPatientResponse sendPatient(String patientID, Set<String> exportFields, String groupName,
                                                     String remoteGUID, String remoteServerIdentifier,
                                                     String remoteUserName, String password)
    {
        this.logger.warn("[SCRIPTSERVICE0] Send (user/pasword)");
        return this.internalService.sendPatient(patientID, exportFields, groupName, remoteGUID, remoteServerIdentifier,
                                                remoteUserName, password);
    }

    public PushServerGetPatientIDResponse getPatientURL(String remoteServerIdentifier, String remotePatientGUID)
    {
        this.logger.warn("[SCRIPTSERVICE0] get patientID");
        return this.internalService.getPatientURL(remoteServerIdentifier, remotePatientGUID);
    }

    public PushServerGetPatientIDResponse getPatientURL(String remoteServerIdentifier, String remotePatientGUID,
                                                        String remoteUserName, String password)
    {
        this.logger.warn("[SCRIPTSERVICE0] get patientID (user/pasword)");
        return this.internalService.getPatientURL(remoteServerIdentifier, remotePatientGUID, remoteUserName, password);
    }
}
