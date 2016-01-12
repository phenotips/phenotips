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

import org.phenotips.data.Patient;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import java.util.Set;

import net.sf.json.JSON;

/**
 * API that allows pushing patient data to a remote PhenoTips instance. Note: this API does not check any permissions
 * and assumes the caller has the right to push the patient.
 *
 * @version $Id$
 * @since 1.0M11
 */
@Unstable
@Role
public interface PushPatientData
{
    /**
     * Retrieves the list of patient fields accepted by the remote server as well as the list of groups the given remote
     * user is a member of on the remote server.
     * <p>
     * Uses the provided credentials to authorize on the remote server. In case optional user_token is not null the
     * token is used and the password is ignored.
     *
     * @param remoteServerIdentifier server name as configured in TODO
     * @param userName user name on the remote server
     * @param password user password on the remote server. Ignored if user_token is not null.
     * @param user_token passwordless-login token provided by the remote server on the last successful login (optional)
     * @return server response which, upon successful login, contains the list of remote phenotips groups the user is a
     *         part of as well as the list of accepted patient fields. If enabled on the the remote server will also
     *         contain a token for future passwordless logins, which may or may not be the same as the token provided.
     *         See {@code PushServerConfigurationResponse}.
     *         <p>
     *         Returns {@code null} if no response was received from the server (e.g. a wrong server IP, a network
     *         problem, etc.)
     */
    PushServerConfigurationResponse getRemoteConfiguration(String remoteServerIdentifier, String userName,
        String password, String user_token);

    /**
     * A patient record can have a state, which is a form of metadata. For example, a patient record can have consents
     * granted by the patient relating to which data is allowed to be part of the record. This function retrieves the
     * patient state from a remote server. In case that this query is made with no specific patient, the server should
     * return the patient state of a newly created record.
     *
     * @param remoteServerIdentifier server name as configured in TODO
     * @param remoteGUID if a remote patient with the same GUID exists and is owned by the given group and is authored
     * by the given user, patient state will be read from that patient, otherwise patient state will be that of a newly
     * created patient (optional, can be {@code null})
     * @param userName user name on the remote server
     * @param password user password on the remote server. Ignored if user_token is not null.
     * @param userToken passwordless-login token provided by the remote server on the last successful login (optional)
     */
    PushServerPatientStateResponse getRemotePatientState(String remoteServerIdentifier, String remoteGUID,
        String userName, String password, String userToken);

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
     * @param exportFields patient fields to be pushed. All other fields will be omitted.
     * @param patientState JSON containing different categories of patient state, such as granted consents
     * @param groupName group name (optional, can be {@code null})
     * @param remoteGUID if a remote patient with the same GUID exists and is owned by the given group and is authored
     *            by the given user patient data will be updated instead of creating a new patient (optional, can be
     *            {@code null})
     * @param remoteServerIdentifier server name as configured in TODO
     * @param userName user name on the remote server
     * @param password user password on the remote server. Ignored if user_token is not null.
     * @param user_token passwordless-login token provided by the remote server on the last successful login (optional,
     *            can be {@code null})
     * @return Server response, indicating whether the submit was successful, and the remote ID and GUID of the remote
     *         patient object (for linking to and future updates of the created remote patient). See
     *         {@code PushServerSendPatientResponse} for details.
     *         <p>
     *         Returns {@code null} if no response was received from the server (e.g. a wrong server IP, a network
     *         problem, etc.)
     */
    PushServerSendPatientResponse sendPatient(Patient patient, Set<String> exportFields, JSON patientState,
        String groupName, String remoteGUID, String remoteServerIdentifier, String userName, String password,
        String user_token);

    /**
     * Gets the remote patient ID and URL for viewing the remote patient.
     * <p>
     * Uses the provided credentials to authorize on the remote server. In case the optional {@code user_token} is not
     * null, the token is used and the password is ignored.
     *
     * @param remoteServerIdentifier server name as configured in TODO
     * @param remotePatientGUID GUID of the remote patient object, as received from the remote server when the patient
     *            was created
     * @param remoteServerIdentifier server name as configured in TODO
     * @param userName user name on the remote server
     * @param password user password on the remote server. Ignored if user_token is not null.
     * @param user_token passwordless-login token provided by the remote server on the last successful login (optional,
     *            can be {@code null})
     * @return server response which, upon successful login, contains the remote patient URL and ID. See
     *         {@code PushServerGetPatientIDResponse}.
     *         <p>
     *         Returns {@code null} if no response was received from the server (e.g. a wrong server IP, a network
     *         problem, etc.)
     */
    PushServerGetPatientIDResponse getPatientURL(String remoteServerIdentifier, String remoteGUID,
        String userName, String password, String user_token);
}
