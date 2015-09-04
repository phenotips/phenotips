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

package org.phenotips.data.push;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import java.util.Map;
import java.util.Set;

import net.sf.json.JSONObject;

/**
 * A wrapper around PushPatientData which deals with saving and retrieval of remote user names and tokens and provides
 * methods for reading the list of push targets.
 * <p>
 * Note: unlike {@code PushPatientData} this service checks that the currently logged in user has permissions to push
 * the patient or view the patient representation in JSON.
 *
 * @version $Id$
 * @since 1.0M11
 */
@Unstable
@Role
public interface PushPatientService
{
    /**
     * @return The list of configured phenotips servers which can be used as patient push targets.
     */
    Set<PushServerInfo> getAvailablePushTargets();

    /**
     * @return For each configured phenotips server which can be used as patient push target information about the last
     *         push of the same patient to the server.
     */
    Map<PushServerInfo, PatientPushHistory> getPushTargetsWithHistory(String localPatientID);

    /**
     * @return Data about the last push attempt of the given local patient to the given remote server;
     *         that includes remote GUID and ID, if the push was successful, and the time of the last push.
     *         Null otherwise.
     */
    PatientPushHistory getPatientPushHistory(String localPatientID, String remoteServerIdentifier);

    /**
     * Returns the (specified subset of) patient data in JSON format. When exportFieldListJSON is {@code null} all
     * available data is returned.
     *
     * @param patientID PhenoTips Patient ID
     * @param exportFieldListJSON a string in the JSON array format with the list of fields which should be included in
     *            the output. When not {@code null} only patient data fields listed will be serialized.
     * @return
     */
    JSONObject getLocalPatientJSON(String patientID, String exportFieldListJSON);

    /**
     * Get the previously stored remote username associated with the current user and the given remote server
     *
     * @return Remote user name, or {@code null} if none is stored
     */
    String getRemoteUsername(String remoteServerIdentifier);

    /**
     * Retrieves the list of patient fields accepted by the remote server as well as the list of groups the given remote
     * user is a member of on the remote server.
     *
     * @param remoteServerIdentifier server name as configured in TODO
     * @param userName user name on the remote server
     * @param password user password on the remote server
     * @param saveUserToken save userLoignToken received from the remote server or not. The user may not wish to
     *            compromise his account on the remote server if someone gets access to the account on the local server
     * @return server response which, upon successful login, contains the list of remote phenotips groups the user is a
     *         part of as well as the list of accepted patient fields.
     *         <p>
     *         Returns {@code null} if no response was received from the server (e.g. a wrong server IP, a network
     *         problem, etc.)
     */
    PushServerConfigurationResponse getRemoteConfiguration(String remoteServerIdentifier,
        String remoteUserName, String password, boolean saveUserToken);

    /**
     * Same as above, but uses the previously stored remote user name and login token to authenticate on the remote
     * server. The retrieved user name and token are based on the current local user and the remote server.
     *
     * @return If there is no user or token stored for the given remote server and the current local user, returns a
     *         {@code PushServerConfigurationResponse} equivalent to the "incorrect password" response. Otherwise see
     *         the docs for the other version.
     */
    PushServerConfigurationResponse getRemoteConfiguration(String remoteServerIdentifier);

    PushServerPatientStateResponse getRemotePatientState(String remoteServerIdentifier, String remoteGUID,
        String remoteUserName, String password);

    PushServerPatientStateResponse getRemotePatientState(String remoteServerIdentifier, String remoteGUID);

    /**
     * Removes stored remote login token, if any - for security purposes
     *
     * @param remoteServerIdentifier
     */
    void removeStoredLoginTokens(String remoteServerIdentifier);

    /**
     * Submits the specified subset of patient data to the specified remote server. The new patient created on the
     * remote server will be created and authored by the given user, and owned by the given group (if provided) or the
     * user otherwise.
     * <p>
     * A new remote patient will be created with each submission, unless remoteGUID is provided, and a patient with the
     * given GUID exists on the remote server and owned by the given group and is authored by the given user - in which
     * case remote patient will be updated (only the submitted fields)
     *
     * @param patient local patient to be pushed to the remove server
     * @param exportFieldListJSON patient fields to be pushed, as a string representing a JSON array. When not
     *            {@code null} only patient data fields listed will be pushed. When {@code null}, all available data
     *            fields will be pushed.
     * @param patientState a JSON encoded as a {@link String}, containing (meta) information about the state of the
     *            patient's record.
     * @param groupName group name (optional, can be {@code null})
     * @param remoteGUID if a remote patient with the same GUID exists and is owned by the given group and is authored
     *            by the given user patient data will be updated instead of creating a new patient (optional, can be
     *            {@code null})
     * @param remoteServerIdentifier server name as configured in TODO
     * @param userName user name on the remote server
     * @param password user password on the remote server. Ignored if user_token is not null.
     * @param user_token passwordless-login token provided by the remote server on the last successful login (optional,
     *            can be {@code null})
     * @return Server response, see {@code PushServerSendPatientResponse}.
     *         <p>
     *         Returns {@code null} if no response was received from the server (e.g. a wrong server IP, a network
     *         problem, etc.)
     */
    PushServerSendPatientResponse sendPatient(String patientID, String exportFieldListJSON, String patientState,
        String groupName, String remoteGUID, String remoteServerIdentifier, String remoteUserName, String password);

    /**
     * Same as above, but uses the previously stored remote user name and login token to authenticate on the remote
     * server. The retrieved user name and token are based on the current local user and the remote server.
     *
     * @return If there is no user or token stored for the given remote server and the current local user, returns a
     *         {@code PushServerSendPatientResponse} equivalent to the "incorrect password" response. Otherwise see the
     *         docs for the other version.
     */
    PushServerSendPatientResponse sendPatient(String patientID, String exportFieldListJSON, String patientState,
        String groupName, String remoteGUID, String remoteServerIdentifier);

    /**
     * @param remoteServerIdentifier
     * @param remotePatientGUID
     * @return
     */
    PushServerGetPatientIDResponse getPatientURL(String remoteServerIdentifier, String remotePatientGUID);

    PushServerGetPatientIDResponse getPatientURL(String remoteServerIdentifier, String remotePatientGUID,
        String remoteUserName, String password);
}
