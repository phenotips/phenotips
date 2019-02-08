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

package org.phenotips.data.shareprotocol;

import org.xwiki.stability.Unstable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Push protocol constants defining client HTTP POST fields and server JSON response fields.
 *
 * @version $Id$
 * @since 1.0M11
 */
@Unstable
public class ShareProtocol
{
    /** version 1: first version */
    public static final String VERSION_1   = "1";
    /** version 1.1: added consents */
    public static final String VERSION_1_1 = "1.1";
    /** version 1.2: new date format in patient JSON + new genes + new prenatal features format */   // TODO: check what is the difference between 1.1 and 1.2
    public static final String VERSION_1_2 = "1.2";

    public static final String CURRENT_PUSH_PROTOCOL_VERSION = VERSION_1_2;

    // list of protocol versions that the current server can read data from
    public static final List<String> COMPATIBLE_CLIENT_PROTOCOL_VERSIONS =
            Arrays.asList(VERSION_1, VERSION_1_1, VERSION_1_2);

    // list of versions which can push even if requred consents have not been checked
    public static final List<String> ALLOW_NO_CONSENTS_PROTOCOL_VERSIONS = Arrays.asList(VERSION_1);

    // list of known incompatibilities, a.k.a. enabling field names which are seriallized differently
    // in old versions of push protocol/patient JSON, together with a "compatibility" field (or null
    // if data can not be serialized in an old way for some reason)
    public static final Incompatibility BIRTH_DATE_INCOMPAT = new Incompatibility("date_of_birth", "date_of_birth_v1");
    public static final Incompatibility DEATH_DATE_INCOMPAT = new Incompatibility("date_of_death", "date_of_death_v1");
    public static final Incompatibility EXAM_DATE_INCOMPAT  = new Incompatibility("exam_date", "exam_date_v1");

    // a list of known (fixable) incompatibilities in the supported past versions of push protocol
    public static final Map<String, List<Incompatibility> > INCOMPATIBILITIES_IN_OLD_PROTOCOL_VERSIONS =
            new HashMap<String, List<Incompatibility> >();
    static {
        INCOMPATIBILITIES_IN_OLD_PROTOCOL_VERSIONS.put(VERSION_1,
                Arrays.asList( BIRTH_DATE_INCOMPAT, DEATH_DATE_INCOMPAT, EXAM_DATE_INCOMPAT ));
        INCOMPATIBILITIES_IN_OLD_PROTOCOL_VERSIONS.put(VERSION_1_1,
                Arrays.asList( BIRTH_DATE_INCOMPAT, DEATH_DATE_INCOMPAT, EXAM_DATE_INCOMPAT ));
    }

    // list of old push protocol versions which are explicitly not supported. The idea is that clients are
    // allowed to push to servers running unknown versions of push protocol (e.g. future not-yet-known versions).
    // But we may explicitly disallow pushing to a known old version which is known to be incompatible
    public static final List<String> OLD_INCOMPATIBLE_VERSIONS = Arrays.asList();

    // list of protocol versions that the current client can regress to.
    // Those are listed explicitly because there is no way to tell if a version is old or new
    // unless it is explicitly listed
    public static final List<String> COMPATIBLE_OLD_SERVER_PROTOCOL_VERSIONS =
            Arrays.asList(VERSION_1, VERSION_1_1);

    //=========================================================================

    // Every POST request should include the following parameters:
    public static final String CLIENT_POST_KEY_NAME_PROTOCOLVER  = "push_protocol_version";
    public static final String CLIENT_POST_KEY_NAME_ACTION       = "action";
    public static final String CLIENT_POST_KEY_NAME_USERNAME     = "username";
    // ...and either PASSWORD or USER_TOKEN:
    public static final String CLIENT_POST_KEY_NAME_PASSWORD     = "password";
    public static final String CLIENT_POST_KEY_NAME_USER_TOKEN   = "user_login_token";  // if user_token is provided password is ignored

    // possible ACTION key values are:
    public static final String CLIENT_POST_ACTIONKEY_VALUE_INFO  = "get_server_info";  // get patient data fields supported by the server and remote user groups
    public static final String CLIENT_POST_ACTIONKEY_VALUE_STATE = "get_patient_state";  // get the state of a patient record
    public static final String CLIENT_POST_ACTIONKEY_VALUE_PUSH  = "push";             // push the patient
    public static final String CLIENT_POST_ACTIONKEY_VALUE_GETID = "get_patient_id";   // get remote ID and remote URL of the patient object with the given GUID

    // for the PUSH action the following fields must be set:
    public static final String CLIENT_POST_KEY_NAME_PATIENTJSON  = "patient_json";
    // ...and the following optional fields may be set:
    public static final String CLIENT_POST_KEY_NAME_GROUPNAME    = "groupname";    // if provided, the ownership of the newly created patient object will be transferred to this group
    public static final String CLIENT_POST_KEY_NAME_GUID         = "patient_guid"; // if provided, the existing remote patient object with the given GUID will be updated.
                                                                                   //   an error will be returned if updating is disabled or GUID is incorrect or the patient object
                                                                                   //   referenced does not belong to the given user and/or group (if provided)
    public static final String CLIENT_POST_KEY_NAME_PATIENTSTATE = "patient_state";
    public static final String CLIENT_POST_KEY_NAME_PATIENTSTATE_CONSENTS = "consents"; // key name which can exist within the patient state JSON
    // for the GETURL action the String CLIENT_POST_KEY_NAME_GUID key must be set

    //=========================================================================

    // every server response JSON will include the following fields:
    public static final String SERVER_JSON_KEY_NAME_PROTOCOLVER = "response_protocol_version";
    public static final String SERVER_JSON_KEY_NAME_SUCCESS     = "success";

    // in case of any failures (SERVER_JSON_KEY_NAME_SUCCESS is set to `false`) one of the
    // following keys is guaranteed to be included in response JSON with the `true` value
    // (and it is guaranteed only one of these is included):
    public static final String SERVER_JSON_KEY_NAME_ERROR_LOGINFAILED    = "login_failed";
    public static final String SERVER_JSON_KEY_NAME_ERROR_ACTIONFAILED   = "action_failed";
    public static final String SERVER_JSON_KEY_NAME_ERROR_PROTOCOLFAILED = "unsupported_post_protocol_version";

    public static final String SERVER_JSON_KEY_NAME_USERNAME_NOT_ACCEPTED = "username_not_accepted";
    public static final String SERVER_JSON_KEY_NAME_USERNAME_EXPECTED     = "username_expected";

    // one of the following may be set in case SERVER_JSON_KEY_NAME_ERROR_LOGINFAILED is set:
    public static final String SERVER_JSON_KEY_NAME_ERROR_WRONGCREDENTIALS = "incorrect_credentials";     // incorrect username or password or token
    public static final String SERVER_JSON_KEY_NAME_ERROR_UNTRUSTEDSERVER  = "unauthorized_server";       // unapproved source server
    public static final String SERVER_JSON_KEY_NAME_ERROR_EXPIREDUSERTOKEN = "user_token_expired";
    public static final String SERVER_JSON_KEY_NAME_ERROR_NOUSERTOKENS     = "user_tokens_not_supported"; // user_tokens are disabled on the server (in case a token is provided)

    // one of the following may be set in case SERVER_JSON_KEY_NAME_ERROR_ACTIONFAILED is set:
    public static final String SERVER_JSON_KEY_NAME_ERROR_UNSUPPORTEDOP    = "unsupported_action";
    public static final String SERVER_JSON_KEY_NAME_ERROR_INCORRECTGROUP   = "incorrect_user_group";   // user group supplied either does not exist or user is not in the group
    public static final String SERVER_JSON_KEY_NAME_ERROR_UPDATESDISABLED  = "updates_disabled";       // in case GUID is provided in the request and updating is disabled on the server
    public static final String SERVER_JSON_KEY_NAME_ERROR_INCORRECTGUID    = "incorrect_guid";         // GUID provided in the request does not represents a patient document
    public static final String SERVER_JSON_KEY_NAME_ERROR_GUIDACCESSDENIED = "guid_access_denied";     // GUID provided in the request represents a document which is not
                                                                                                       //  authored or owned by the user provided
    public static final String SERVER_JSON_KEY_NAME_ERROR_MISSINGCONSENT   = "missing_consent";        // if any of the required consents are missing

    // response to a GETINFO action request will include the following fields (iff successful):
    public static final String SERVER_JSON_GETINFO_KEY_NAME_USERGROUPS     = "user_groups";
    public static final String SERVER_JSON_GETINFO_KEY_NAME_ACCEPTEDFIELDS = "accepted_fields";
    public static final String SERVER_JSON_GETINFO_KEY_NAME_UPDATESENABLED = "updates_enabled";
    public static final String SERVER_JSON_GETINFO_KEY_NAME_CONSENTS       = "consents";
    // (optional) ...and optionally this as well, if enabled on the server:
    public static final String SERVER_JSON_GETINFO_KEY_NAME_USERTOKEN      = "user_login_token";

    // response to a PUSH and GETID action requests will include the following fields (iff successful):
    public static final String SERVER_JSON_PUSH_KEY_NAME_PATIENTID   = "patient_id";      // ID of the patient (either updated or newly created)
    public static final String SERVER_JSON_PUSH_KEY_NAME_PATIENTURL  = "patient_url";     // URL of the patient (either updated or newly created)
    public static final String SERVER_JSON_PUSH_KEY_NAME_PATIENTGUID = "patient_guid";    // GUID of the patient object on the remote server which can be used to link to the
                                                                                          //  patient from the remote server and/or to update the patient later

    /**
     * Helper class describing an incompatibility between serializers in two different PhenoTips versions.
     *
     * An incommpatibility is described in terms of a "controlling field name" which triggers
     * (part of) a serializer when serializing a patient to JSON, i.e. one incompatibility is limited to
     * one field name.
     *
     * An alternative "deprecated field name" may be specified, which is supposed to trigger an
     * certain old serializer.
     */
    public static class Incompatibility
    {
        private String currentName;
        private String deprecatedName;

        public Incompatibility(String currentControllingFieldName, String deprecatedControllingFieldName) {
            this.currentName = currentControllingFieldName;
            this.deprecatedName = deprecatedControllingFieldName;
        }

        public String getCurrentFieldName() {
            return this.currentName;
        }
        public String getDeprecatedFieldName() {
            return this.deprecatedName;
        }
    }
}
