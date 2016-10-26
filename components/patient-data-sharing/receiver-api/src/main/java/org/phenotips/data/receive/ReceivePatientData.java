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

package org.phenotips.data.receive;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import org.json.JSONObject;

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
     * valid, returns the list of PhenoTips patient fields accepted in push requests by the server and the list of local
     * PhenoTips groups that the given user is a member of.<br>
     * If enabled on the server, also generates a user_token which can be used to push patients without providing
     * password again. This token can only be used for pushing patients and its lifetime is configured per-server.
     * <p>
     * If the username or credentials are incorrect returns a {@code JSONObject} with {@code "success"} key set to
     * {@code false}, {@code "login_failed"} field set to {@code true} and possibly some other fields indicating the
     * exact failure reason. See description of the protocol in {@link org.phenotips.data.shareprotocol.ShareProtocol}.
     * <p>
     * In case of any failures after provided credentials were successfully validated returns a {@code JSONObject} with
     * {@code "success"} key set to {@code false} and {@code "action_failed"} field set to {@code true};
     * {@code "login_failed"} is guaranteed to be absent in the output.
     * <p>
     * On success the following fields are set:
     * <ul>
     * <li>{@code success}: {@code true}</li>
     * <li>{@code user_groups}: a JSON list of local PhenoTips group names the username given belongs to, e.g.
     * {@code ["Group1", "Group2"]}</li>
     * <li>{@code accepted_fields}: a JSON list of accepted PhenoTips patient fields, e.g.
     * {@code ["measurements", "features"]}</li>
     * <li>{@code user_login_token}: (optional, if enabled on the server) a {@code String} token which can be used for
     * pushing patient data without providing a password again. This value may be the same as the token provided in the
     * POST request, or different, depending on the server configuration.</li>
     * </ul>
     *
     * @return {@code JSONObject} with {@code "success"} set to {@code true} if the request was successful and the list
     *         of fields and groups was returned, and set to {@code false} in case of any failures (see above for
     *         details)
     */
    JSONObject getConfiguration();

    /**
     * Receives patient data and either updates an existing patient or creates a new patient.
     * <p>
     * Requires a valid username and credentials to be supplied in the request, which are validated the same way
     * {@code getConfiguration()} does, and returns the same {@code JSONObject} in case of any problems.
     * <p>
     * If credentials are valid, checks if "remote_guid" parameter is supplied in the request:
     * <ol>
     * <li>if it is supplied, but updating existing patients is disabled on the server, returns a {@code JSONObject}
     * with {@code success} key set to {@code false}, {@code action_failed} field set to {@code true} and
     * {@code updates_disabled} field set to {@code true}</li>
     * <li>if it is supplied, and updates are enabled, checks the type and ownership of the object with the given GUID.
     * If the object is not a PhenoTips Patient or it is not owned by the user or group provided in the request, returns
     * a "failure" JSON with {@code success} key set to {@code false}, {@code action_failed} field set to {@code true}
     * and either {@code incorrect_guid} or {@code guid_access_denied} field set to {@code true}. If everything is OK,
     * attempts to update the existing patient with the data provided. Only the fields provided which are accepted by
     * the server will be updated, all other data will be left intact.</li>
     * <li>if no GUID is supplied, attempts to create a new patient filled with data supplied in the request. The user
     * provided will be set as both the author and the creator of the patient and the group provided will be set as the
     * owner. If no group were provided the user will also be the owner. If the user is not a member of the group
     * provided or the group is not a PhenoTips group a failure JSON will be generated with the
     * {@code incorrect_user_group} key set to {@code true}.</li>
     * </ol>
     * In case of any failures after provided credentials were successfully validated returns a {@code JSONObject} with
     * {@code success} key set to {@code false} and {@code action_failed} field set to {@code true};
     * {@code login_failed} is guaranteed to be absent in the output.
     * <p>
     * On success the following fields are set:
     * <ul>
     * <li>{@code success}: {@code true}</li>
     * <li>{@code patient_id}: a {@code String}, ID of the newly created or updated patient (the same
     * {@link #getPatientURL()} would return)</li>
     * <li>{@code patient_url}: a {@code String}, the URL of the newly created or updated patient (the same
     * {@link #getPatientURL()} would return)</li>
     * <li>{@code patient_guid}: a {@code String}, the GUID of the newly created or updated patient (which can be used
     * to update the patient or to get the URL of the patient at a later time)</li>
     * </ul>
     *
     * @return {@code JSONObject} with {@code success} set to {@code true} if the request was successful, and set to
     *         {@code false} in case of any failures (see above for details)
     */
    JSONObject receivePatient();

    /**
     * Returns the URL of the patient object with the given GUID. Requires a valid username and credentials to be
     * supplied in the request, which are validated the same way {@code getConfiguration()} does, and returns the same
     * {@code JSONObject} in case of any problems.
     * <p>
     * In case of any failures after credentials were successfully validated returns a {@code JSONObject} with
     * {@code success} key set to {@code false} and {@code action_failed} field set to {@code true}. If the user
     * provided is not the owner or the author of the object with the given GUID, {@code access_denied} field will be
     * set to {@code true}. If the given GUID does not represent a PhenoTips patient object, {@code incorrect_guid}
     * field will be set to {@code true}.
     * <p>
     * On success the following fields are set:
     * <ul>
     * <li>{@code success}: {@code true}</li>
     * <li>{@code patient_id}: a {@code String}, ID of the PhenoTips patient object with the given GUID</li>
     * <li>{@code patient_url}: a {@code String}, the URL of the PhenoTips patient object with the given GUID</li>
     * </ul>
     *
     * @return {@code JSONObject} with {@code success} set to {@code true} if the request was successful, and set to
     *         {@code false} in case of any failures (see above for details)
     */
    JSONObject getPatientURL();

    JSONObject unsupportedeActionResponse();

    JSONObject untrustedServerResponse();
}
