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

package org.phenotips.data.receive;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import net.sf.json.JSONObject;

/**
 * API that allows receiving patient data from a remote PhenoTips instance.
 *
 * @version $Id$
 * @since 1.0M11
 */
@Unstable
@Role
public interface ReceivePatientData
{
    /**
     * Check that the server pushing the patient is authorized to push to this server.
     *
     * @return {@code true} if source server is authorized.
     */
    boolean isServerTrusted();

    /**
     * Validates the username and credentials (password or user_token) given in the request, and iff credentials are
     * valid, returns the list of Phenotips patient fields accepted in push requests by the server and the list of local
     * Phenotips groups that the given user is a member of.<br>
     * If enabled on the server, also generates a user_token which can be used to push patients without providing
     * password again. This token can only be used for pushing patients and its lifetime is configured per-server.
     * <p>
     * If the username or credentilas are incorrect returns a {@code JSONObject} with {@code "success"} key set to
     * {@code false}, {@code "login_failed"} field set to {@code true} and possibly some other fields indicating the
     * exact failure reason. See description of the protocol in
     * {@code org.phenotips.data.shareprotocol.ShareProtocol.java}.
     * <p>
     * In case of any failures after provided credentials were successfully validated returns a {@code JSONObject} with
     * {@code "success"} key set to {@code false} and {@code "action_failed"} field set to {@code true};
     * {@code "login_failed"} is guaranteed to be absent in the output.
     *
     * @return {@code JSONObject} with {@code "success"} set to {@code true} if the request was successful and the list of fields
     * and groups was returned, and set to {@code false} in case of any failures (see above for details). On success the
     * following fields are set:
     *  "success": {@code true}
     *  "user_groups": a JSON list of local Phenotips group names the username given belongs to, e.g. {@code ["Group1", "Group2"]}
     *  ""accepted_fields": a JSON list of accepted Phenotips patient fields, e.g. {@code ["measurements", "features"]}
     *  "user_login_token": (optional, if enabled on the server) a {@code String} token which can be used for pushing patient
     *                      data without providing a password again. This value may be the same as the token provided in the
     *                      POST request, or different, depending on the server configuration.
     */
    JSONObject getConfiguration();

    /**
     * Receives patient data and either updates an existing patient or creates a new patient.
     * <p>
     * Requires a valid username and credentials to be supplied in the request, which are validated the same way
     * {@code getConfiguration()} does, and returns the same {@code JSONObject} in case of any problems.
     * <p>
     * If credentials are valid, checks if "remote_guid" parameter is supplied in the request:<br>
     *   1) if it is supplied, but updating existing patients is disabled on the server, returns a {@code JSONObject}
     *      with {@code "success"} key set to {@code false}, {@code "action_failed"} field set to {@code true} and
     *      {@code "updates_disabled"} field set to {@code true}<br>
     *   2) if it is supplied, and updates are enabled, checks the type and ownership of the object with the given GUID.
     *      If the object is not a Phenotips Patient or it is not owned by the user or group provided in the request
     *      returnes a "failure" JSON with {@code "success"} key set to {@code false}, {@code "action_failed"} field set
     *      to {@code true} and either {@code "incorrect_guid"} or {@code "guid_access_denied"} field set to {@code true}.
     *      If everything is OK attempts to update the existing patient with the data provided. Only the fields provided
     *      which are accepted by the server will be updated, all other data will be left intact.<br>
     *   3) if no GUID is supplied, attempts to create a new patient filled with data supplied in the request.
     *      The user provided will be set as both the author and the creator of the patient and the group provided
     *      will be set as the owner. If no group were provided the user will also be the owner. If the user is not
     *      a member of the group provided or the group is not a Phenotips group a failure JSON will be generated with the
     *      {@code "incorrect_user_group"} key set to {@code true}.<p>
     *
     * In case of any failures after provided credentials were successfully validated returns a {@code JSONObject} with
     * {@code "success"} key set to {@code false} and {@code "action_failed"} field set to {@code true};
     * {@code "login_failed"} is guaranteed to be absent in the output.
     *
     * @return {@code JSONObject} with {@code "success"} set to {@code true} if the request was successful,
     * and set to {@code false} in case of any failures (see above for details). On success the following fields are set:
     *  "success": {@code true}
     *  "patient_id": {@code String}, ID of the newly created or updated patient (the same {@code getPatientURL()} would return)
     *  "patient_url": {@code String}, the URL of the newly created or updated patient (the same {@code getPatientURL()} would return)
     *  "patient_guid": {@code String}, the GUID of the newly created or updated patient (which can be used to update the patient or to get the URL
     *  of the patient at a later time)
     */
    JSONObject receivePatient();

    /**
     * Returns the URL of the patient object with the given GUID.
     *
     * Requires a valid username and credentials to be supplied in the request, which are validated the same way
     * {@code getConfiguration()} does, and returns the same {@code JSONObject} in case of any problems.<p>
     *
     * In case of any failures after credentials were successfully validated returns a {@code JSONObject} with {@code "success"}
     * key set to {@code false} and {@code "action_failed"} field set to {@code true}. If the user provided is not the
     * owner or the author of the object with the given GUID, {@code "access_denied"} field will be set to {@code true}.
     * If the given GUID does not represent a Phenotips patient object, {@code "incorrect_guid"} field will be set to {@code true}.
     *
     * @return {@code JSONObject} with {@code "success"} set to {@code true} if the request was successful,
     * and set to {@code false} in case of any failures (see above for details). On success the following fields are set:
     *  "success": {@code true}
     *  "patient_id": {@code String}, ID of the Phenotips patient object with the given GUID
     *  "patient_url": {@code String}, the URL of the Phenotips patient object with the given GUID
     */
    JSONObject getPatientURL();

    JSONObject unsupportedeActionResponse();

    JSONObject untrustedServerResponse();
}
