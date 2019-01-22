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
package org.phenotips.data.receive.internal;

import org.phenotips.Constants;
import org.phenotips.configuration.RecordConfigurationManager;
import org.phenotips.configuration.internal.consent.ConsentAuthorizer;
import org.phenotips.data.ConsentManager;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.internal.PhenoTipsPatient;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.data.receive.ReceivePatientData;
import org.phenotips.data.securestorage.LocalLoginToken;
import org.phenotips.data.securestorage.SecureStorageManager;
import org.phenotips.data.shareprotocol.ShareProtocol;
import org.phenotips.groups.Group;
import org.phenotips.groups.GroupManager;
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.security.Principal;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.XWikiRequest;

/**
 * Default implementation for the {@link ReceivePatientData} component.
 *
 * @version $Id$
 * @since 1.0M11
 */
@Component
@Singleton
public class DefaultReceivePatientData implements ReceivePatientData
{
    private final static int DEFAULT_USER_TOKEN_LIFETIME = 7;

    private final static boolean DEFAULT_USER_TOKENS_ENABLED = true;

    private final static String MAIN_CONFIG_ALLOW_ANY_SOURCE_PROPERTY_NAME = "AllowPushesFromNonListedServers";

    private final static String MAIN_CONFIG_ALLOW_NO_CONSENTS_FROM_OLD_CLIENTS = "AllowNoConsentsFromOldClients";

    private final static String SERVER_CONFIG_IP_PROPERTY_NAME = "ip";

    private final static String SERVER_CONFIG_SERVER_NAME_PROPERTY_NAME = "name";

    private final static String SERVER_CONFIG_USER_TOKEN_EXPIRE_PROPERTY_NAME = "user_token_life_in_days";

    /** used for secure user login token generation */
    private SecureRandom secureRandomGenerator = new SecureRandom();

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Provides access to the current request context. */
    @Inject
    private Execution execution;

    @Inject
    private PatientRepository patientRepository;

    /** Used for getting the configured mapping. */
    @Inject
    private RecordConfigurationManager configurationManager;

    /** Used for getting the list of groups a user belongs to. */
    @Inject
    private GroupManager groupManager;

    /** Used for vlaidating the user and gettin guser groups. */
    @Inject
    private UserManager userManager;

    /** Used for searching for groups. */
    @Inject
    private QueryManager queryManager;

    /** Parses string representations of document references into proper references. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> stringResolver;

    /** Provides access to the XWiki data. */
    @Inject
    private DocumentAccessBridge bridge;

    /** Used for login token storage in a way unavailable to web pages */
    @Inject
    private SecureStorageManager storageManager;

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    @Inject
    private PermissionsManager permissionManager;

    @Inject
    private AuthorizationService authService;

    @Inject
    private ConsentManager consentManager;

    @Inject
    private ConsentAuthorizer consentAuthorizer;

    @Inject
    @Named("user/current")
    private DocumentReferenceResolver<String> userResolver;  // used to convert usernames from XWiki.user to plain user

    @Override
    public boolean isServerTrusted()
    {
        // server is trusted if either "allow pushes from non-listed servers" is checked,
        // or the server is listed and thus have a configuration
        XWikiContext context = getXContext();

        BaseObject mainConfig = getMainConfiguration(context);
        if (mainConfig == null) {
            this.logger.error("Receive configuration not found");
            return false;
        }

        if (mainConfig.getIntValue(MAIN_CONFIG_ALLOW_ANY_SOURCE_PROPERTY_NAME) == 1) {
            return true;
        }

        BaseObject serverConfig = getSourceServerConfiguration(context.getRequest().getRemoteAddr(), context);
        if (serverConfig == null) {
            this.logger.error("Connection from an untrusted server {}", context.getRequest().getRemoteAddr());
            return false;
        }
        return true;
    }

    @Override
    public JSONObject untrustedServerResponse()
    {
        return generateFailedLoginResponse(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_UNTRUSTEDSERVER);
    }

    @Override
    public JSONObject unsupportedeActionResponse()
    {
        return generateFailedActionResponse(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_UNSUPPORTEDOP);
    }

    protected JSONObject generateFailedLoginResponse()
    {
        return generateFailedLoginResponse(null);
    }

    protected JSONObject generateFailedLoginResponse(String jsonKeyToSet)
    {
        JSONObject response = generateFailureResponse();
        response.put(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_LOGINFAILED, true);
        if (jsonKeyToSet != null) {
            response.put(jsonKeyToSet, true);
        }
        return response;
    }

    protected JSONObject generateFailedCredentialsResponse()
    {
        return generateFailedCredentialsResponse(null);
    }

    protected JSONObject generateFailedCredentialsResponse(String jsonKeyToSet)
    {
        JSONObject response = generateFailedLoginResponse(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_WRONGCREDENTIALS);
        if (jsonKeyToSet != null) {
            response.put(jsonKeyToSet, true);
        }
        return response;
    }

    protected JSONObject generateNotCanonicalUsernameResponse(String expectedName, String providedName)
    {
        JSONObject response = generateFailedLoginResponse();
        response.put(ShareProtocol.SERVER_JSON_KEY_NAME_USERNAME_NOT_ACCEPTED, providedName);
        response.put(ShareProtocol.SERVER_JSON_KEY_NAME_USERNAME_EXPECTED, expectedName);
        return response;
    }

    protected JSONObject generateIncompatibleVersionResponse()
    {
        JSONObject response = generateFailureResponse();
        response.put(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_PROTOCOLFAILED, true);
        return response;
    }

    protected JSONObject generateFailedActionResponse()
    {
        return generateFailedActionResponse(null);
    }

    protected JSONObject generateFailedActionResponse(String jsonKeyToSet)
    {
        JSONObject response = generateFailureResponse();
        response.put(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_ACTIONFAILED, true);
        if (jsonKeyToSet != null) {
            response.put(jsonKeyToSet, true);
        }
        return response;
    }

    protected XWikiDocument getPatientDocument(Patient patient) throws Exception
    {
        return (XWikiDocument) this.bridge.getDocument(patient.getDocument());
    }

    protected String getPatientGUID(Patient patient)
    {
        try {
            XWikiDocument doc = getPatientDocument(patient);
            String guid = doc.getXObject(Patient.CLASS_REFERENCE).getGuid();
            return guid;
        } catch (Exception ex) {
            this.logger.error("Failed to get patient GUID: [{}] {}", ex.getMessage(), ex);
            return null;
        }
    }

    protected String getPatientURL(Patient patient, XWikiContext context)
    {
        try {
            XWikiDocument doc = getPatientDocument(patient);
            String url = doc.getURL("view", context);
            return url;
        } catch (Exception ex) {
            this.logger.error("Failed to get patient URL: [{}] {}", ex.getMessage(), ex);
            return null;
        }
    }

    protected JSONObject generateSuccessfulResponseWithPatientIDs(Patient patient, XWikiContext context)
    {
        try {
            String guid = getPatientGUID(patient);
            String url = getPatientURL(patient, context);
            String id = patient.getDocument().getName();

            JSONObject response = generateSuccessfulResponse();
            response.put(ShareProtocol.SERVER_JSON_PUSH_KEY_NAME_PATIENTGUID, guid);
            response.put(ShareProtocol.SERVER_JSON_PUSH_KEY_NAME_PATIENTID, id);
            response.put(ShareProtocol.SERVER_JSON_PUSH_KEY_NAME_PATIENTURL, url);
            return response;
        } catch (Exception ex) {
            this.logger.error("Failed to get patient GUID/ID/URL: [{}] {}", ex.getMessage(), ex);
            return generateFailedActionResponse();
        }
    }

    protected JSONObject generateSuccessfulResponse()
    {
        JSONObject response = generateEmptyResponse();
        response.put(ShareProtocol.SERVER_JSON_KEY_NAME_SUCCESS, true);
        return response;
    }

    protected JSONObject generateFailureResponse()
    {
        JSONObject response = generateEmptyResponse();
        response.put(ShareProtocol.SERVER_JSON_KEY_NAME_SUCCESS, false);
        return response;
    }

    protected JSONObject generateEmptyResponse()
    {
        JSONObject response = new JSONObject();
        response.put(ShareProtocol.SERVER_JSON_KEY_NAME_PROTOCOLVER, ShareProtocol.CURRENT_PUSH_PROTOCOL_VERSION);
        return response;
    }

    protected boolean isValidUserGroup(String userName, String groupName)
    {
        Set<Group> userGroups = this.groupManager.getGroupsForUser(this.userManager.getUser(userName));

        // note that Group and default Group implementation do not overwrite equals(), thus
        // using Set.contains() with groupManager.getGroup(groupName) is not possible

        for (Group group : userGroups) {
            if (group.getReference().getName().equals(groupName)) {
                return true;
            }
        }
        return false;
    }

    protected String generateNewToken(String userName)
    {
        this.logger.warn("generating new token for user {}", userName);
        // alternatively may use smth like org.apache.commons.lang.RandomStringUtils.randomAlphanumeric(128);
        return new BigInteger(256, this.secureRandomGenerator).toString(32);
    }

    protected static enum TokenStatus
    {
        VALID,
        EXPIRED,
        INVALID
    };

    protected TokenStatus checkUserToken(String userName, String serverName, String token, long tokenLifeTimeInDays)
    {
        if (token == null) {
            return TokenStatus.INVALID;
        }

        LocalLoginToken storedToken = this.storageManager.getLocalLoginToken(userName, serverName);

        if (storedToken == null) {
            return TokenStatus.INVALID;
        }

        // this.logger.debug("Expected token for user [{}]: [{}] aged [{}] out of [{}]",
        // userName, storedToken.getLoginToken(), storedToken.getTokenAgeInDays(), tokenLifeTimeInDays);

        if (!token.equals(storedToken.getLoginToken())) {
            this.logger.warn("Stored token does not match provided token");
            return TokenStatus.INVALID;
        }

        if (tokenLifeTimeInDays != 0 && (storedToken.getTokenAgeInDays() > tokenLifeTimeInDays)) {
            this.logger.warn("Stored token has expired");
            return TokenStatus.EXPIRED;
        }

        return TokenStatus.VALID;
    }

    protected String getRemoteServerName(BaseObject serverConfig, XWikiRequest request)
    {
        if (serverConfig == null) {
            return request.getRemoteAddr(); // default for non-configured servers
        }
        return serverConfig.getStringValue(SERVER_CONFIG_SERVER_NAME_PROPERTY_NAME);
    }

    protected long getUserTokenLifetime(BaseObject serverConfig)
    {
        if (serverConfig == null) {
            return DEFAULT_USER_TOKEN_LIFETIME; // default for non-configured servers
        }
        return serverConfig.getLongValue(SERVER_CONFIG_USER_TOKEN_EXPIRE_PROPERTY_NAME);
    }

    protected boolean userTokensEnabled(BaseObject serverConfig)
    {
        return DEFAULT_USER_TOKENS_ENABLED;
    }

    // internal class which contains details about a login attempt
    protected class LoginResult
    {
        private String authorizedUsername;
        private String userToken;

        private JSONObject failedLoginDetails;

        public LoginResult(String authorizedUsername, String userToken) {
            this.authorizedUsername = authorizedUsername;
            this.userToken = userToken;
        }

        public LoginResult(JSONObject failedLoginDetails) {
            this.failedLoginDetails = failedLoginDetails == null
                    ? generateFailedLoginResponse()
                    : failedLoginDetails;
        }

        public boolean isSuccessful() {
            return this.authorizedUsername != null;
        }

        public boolean isFailed() {
            return !this.isSuccessful();
        }

        public String getAuthorizedUsername() {
            return this.authorizedUsername;
        }

        public String getUserToken() {
            return this.userToken;
        }

        public JSONObject getFailedLoginDetails() {
            return this.failedLoginDetails;
        }
    }

    /**
     * Check that username and credentials (either password or user_token) are received in the request, that
     * provided username is a valid user name on this server and that credentials are valid for the username given.
     *
     * If user_token are enabled on the server and user_token is present in the request then only the token is
     * validated and password is ignored.
     *
     * @return {@code LoginResult} a LoginResult object which either contains the login failure reason (in JSON format,
     *                             which can be returned directly to the requestor), or authorized user name (which may
     *                             be different form the name provided because of the peculiarities of XWiki framework
     *                             (e.g. current version strips all spaces from the provided username before attempting
     *                             to authorize)
     */
    protected LoginResult validateLogin(XWikiRequest request, XWikiContext context)
    {
        try {
            String clientVersion = request.getParameter(ShareProtocol.CLIENT_POST_KEY_NAME_PROTOCOLVER);
            if (!isCompatibleVersion(clientVersion)) {
                this.logger.error("Rejecting push request by {} - incompatible push protocol version",
                    request.getRemoteAddr());
                return new LoginResult(generateIncompatibleVersionResponse());
            }

            String userName = request.getParameter(ShareProtocol.CLIENT_POST_KEY_NAME_USERNAME);
            String token = request.getParameter(ShareProtocol.CLIENT_POST_KEY_NAME_USER_TOKEN);

            if (userName == null) {
                return new LoginResult(generateFailedCredentialsResponse());
            }

            if (token == null) {
                String password = request.getParameter(ShareProtocol.CLIENT_POST_KEY_NAME_PASSWORD);

                Principal authenticatedUser =
                        context.getWiki().getAuthService().authenticate(userName, password, context);

                if (authenticatedUser == null) {
                    return new LoginResult(generateFailedCredentialsResponse());
                }

                // authenticate() may accept usernames which are not in "canonical" format, e.g. with
                // extra whitespace added or explicit XWiki: and/or XWiki. prefixes. In such cases the problem
                // arises when we try to authenticate the same username using a token and our own authentication
                // system, which does not know about all possible accepted username variations.
                //
                // One way is to reject all usernames in non-canonical form, however we do not want to reject
                // usernames with prefixes added, as that is useful for debugging PT installs on a farm.
                // So the solution is to try to instantiate a user object using the name, which is what
                // we ultimately need from a username once authenticated. If it works - accept, else - reject.
                User user = this.userManager.getUser(userName);
                if (user == null || user.getProfileDocument() == null) {
                    DocumentReference userReference = this.userResolver.resolve(authenticatedUser.getName());
                    if (userReference == null) {
                        return new LoginResult(generateFailedCredentialsResponse());
                    }
                    String plainUserNameAsAuthenticated = userReference.getName();
                    return new LoginResult(generateNotCanonicalUsernameResponse(plainUserNameAsAuthenticated, userName));
                }

                return new LoginResult(userName, null);  // successful login, no token provided
            } else {
                BaseObject serverConfig = getSourceServerConfiguration(request.getRemoteAddr(), context);

                if (!userTokensEnabled(serverConfig)) {
                    this.logger.warn("user token provided by [{}] but tokens are disabled", userName);
                    return new LoginResult(
                            generateFailedCredentialsResponse(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_NOUSERTOKENS));
                }

                String serverName = getRemoteServerName(serverConfig, request);
                long tokenLifeTime = getUserTokenLifetime(serverConfig);

                TokenStatus tokenStatus = checkUserToken(userName, serverName, token, tokenLifeTime);

                if (tokenStatus == TokenStatus.INVALID) {
                    return new LoginResult(generateFailedCredentialsResponse());
                } else if (tokenStatus == TokenStatus.EXPIRED) {
                    return new LoginResult(
                            generateFailedCredentialsResponse(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_EXPIREDUSERTOKEN));
                }

                return new LoginResult(userName, token);    // successful login using the given token
            }
        } catch (Exception ex) {
            this.logger.error("Error during remote login [{}] {}", ex.getMessage(), ex);
            return new LoginResult(generateFailedLoginResponse());
        }
    }

    protected boolean isCompatibleVersion(String clientVersion)
    {
        if (ShareProtocol.COMPATIBLE_CLIENT_PROTOCOL_VERSIONS.contains(clientVersion)) {
            return true;
        }
        return false;
    }

    @Override
    public JSONObject receivePatient()
    {
        try {
            XWikiContext context = getXContext();
            XWikiRequest request = context.getRequest();

            this.logger.warn("Push patient request from remote [{}]", request.getRemoteAddr());

            LoginResult loginResult = validateLogin(request, context);
            if (loginResult.isFailed()) {
                return loginResult.getFailedLoginDetails();
            }

            String userName = loginResult.getAuthorizedUsername();
            String groupName = request.getParameter(ShareProtocol.CLIENT_POST_KEY_NAME_GROUPNAME);
            if (groupName != null && !isValidUserGroup(userName, groupName)) {
                this.logger.warn("Incorrect group name provided by {}", request.getRemoteAddr());
                return generateFailedActionResponse(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_INCORRECTGROUP);
            }

            String patientJSONRaw = context.getRequest().getParameter(ShareProtocol.CLIENT_POST_KEY_NAME_PATIENTJSON);
            if (patientJSONRaw == null) {
                this.logger.error("No patient data provided by {})", request.getRemoteAddr());
                return generateFailedActionResponse();
            }

            Set<String> consentIds = null;
            String patientStateRaw = request.getParameter(ShareProtocol.CLIENT_POST_KEY_NAME_PATIENTSTATE);
            if (patientStateRaw != null) {
                consentIds = extractConsents(patientStateRaw);
                // there should not be any consent updates if consents are not enabled
                if (!consentIds.isEmpty() && !this.consentAuthorizer.consentsGloballyEnabled()) {
                    // reject, as a non-malicious user would never arrive to this execution point
                    return this.generateFailedActionResponse();
                }
            }

            boolean requireConsents =
                areConsentsRequired(request.getParameter(ShareProtocol.CLIENT_POST_KEY_NAME_PROTOCOLVER));
            if (requireConsents) {
                boolean consentAuthorized = this.consentAuthorizer.authorizeInteraction(consentIds);
                if (!consentAuthorized) {
                    this.logger.error("Rejecting patient data from {} - not all required consents have been given",
                        request.getRemoteAddr());
                    return this.generateFailedActionResponse(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_MISSINGCONSENT);
                }
            }

            String patientJSON = URLDecoder.decode(patientJSONRaw, XWiki.DEFAULT_ENCODING);
            this.logger.debug("Received patient JSON: [{}]", patientJSON);

            Patient affectedPatient;

            // if GUID is present in the request attempt to update an existing patient
            // (or fail if GUID is invalid or the patient is not created/authored by the user)
            String guid = request.getParameter(ShareProtocol.CLIENT_POST_KEY_NAME_GUID);
            User user = this.userManager.getUser(userName);
            context.setUserReference(user.getProfileDocument());

            if (guid != null) {
                affectedPatient = getPatientByGUID(guid);
                if (affectedPatient == null) {
                    return generateFailedActionResponse(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_INCORRECTGUID);
                }
                if (!userCanAccessPatient(userName, affectedPatient)) {
                    return generateFailedActionResponse(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_GUIDACCESSDENIED);
                }
                this.logger.warn("Loaded existing patient [{}] successfully", affectedPatient.getDocument().getName());
            } else {

                affectedPatient = this.patientRepository.create(user.getProfileDocument());

                XWikiDocument doc = getPatientDocument(affectedPatient);
                doc.setAuthorReference(user.getProfileDocument());

                // assign ownership to group (if provided) or to the user, and set access rights
                if (groupName != null) {
                    Group group = this.groupManager.getGroup(groupName);
                    this.permissionManager.getPatientAccess(affectedPatient).setOwner(group.getReference());
                    this.permissionManager.getPatientAccess(affectedPatient).addCollaborator(user.getProfileDocument(),
                        this.permissionManager.resolveAccessLevel("manage"));
                } else {
                    this.permissionManager.getPatientAccess(affectedPatient).setOwner(user.getProfileDocument());
                }

                if (affectedPatient == null) {
                    this.logger.error("Can not create new patient");
                    return generateFailedActionResponse();
                }

                this.logger.warn("Created new patient successfully");
            }

            // get updated saved instance of patient document
            affectedPatient = this.patientRepository.get(affectedPatient.getDocument());
            JSONObject patientData = new JSONObject(patientJSON);
            affectedPatient.updateFromJSON(patientData);

            if (consentIds != null) {
                this.consentManager.setPatientConsents(affectedPatient, consentIds);
            }

            this.logger.warn("Updated patient successfully");

            // store separately from the patient object
            BaseObject serverConfig = getSourceServerConfiguration(request.getRemoteAddr(), context);
            String sourceServerName = getRemoteServerName(serverConfig, request);
            String patientGUID = getPatientGUID(affectedPatient);
            this.storageManager.storePatientSourceServerInfo(patientGUID, sourceServerName);

            return generateSuccessfulResponseWithPatientIDs(affectedPatient, context);
        } catch (Exception ex) {
            this.logger.error("Error importing patient [{}] {}", ex.getMessage(), ex);
            return this.generateFailedActionResponse();
        }
    }

    private boolean areConsentsRequired(String clientProtocolVersion)
    {
        if (!ShareProtocol.ALLOW_NO_CONSENTS_PROTOCOL_VERSIONS.contains(clientProtocolVersion)) {
            return true;
        }
        // protocol is one of those which may be allowed to skip consents; now need to check if
        // not providing consents is allowed/configured in receive patient settings
        BaseObject mainConfig = getMainConfiguration(getXContext());
        if (mainConfig != null && mainConfig.getIntValue(MAIN_CONFIG_ALLOW_NO_CONSENTS_FROM_OLD_CLIENTS) == 1) {
            return false;
        }
        return true;
    }

    /**
     * Exctacts the list of granted consents from a request
     *
     * @param rawPatientState patient state JSON string directly from the {@link Request} object
     */
    private Set<String> extractConsents(String rawPatientState)
    {
        Set<String> consents = new HashSet<>();
        JSONObject patientState = this.patientStateToJson(rawPatientState);
        if (patientState != null) {
            try {
                JSONArray consentsJson =
                    patientState.optJSONArray(ShareProtocol.CLIENT_POST_KEY_NAME_PATIENTSTATE_CONSENTS);
                if (consentsJson != null) {
                    for (Object consent : consentsJson) {
                        consents.add(consent.toString());
                    }
                }
            } catch (Exception ex) {
                this.logger.error("Misformatted patient consent string: {}", ex.getMessage());
            }
        }
        return consents;
    }

    /**
     * Converts raw patient state string from the request to {@link JSONObject}, or {@link null} if the function fails.
     */
    private JSONObject patientStateToJson(String rawPatientState)
    {
        if (rawPatientState != null) {
            try {
                return new JSONObject(URLDecoder.decode(rawPatientState, XWiki.DEFAULT_ENCODING));
            } catch (JSONException ex) {
                this.logger.error("Patient state JSON has errors: {}", ex.getMessage());
            } catch (Exception ex) {
                this.logger.error("Error processing patient state: {}", ex.getMessage());
            }
        }
        return null;
    }

    @Override
    public JSONObject getConfiguration()
    {
        try {
            XWikiContext context = getXContext();
            XWikiRequest request = context.getRequest();

            this.logger.warn("Get config request from remote [{}]", request.getRemoteAddr());

            LoginResult loginResult = validateLogin(request, context);
            if (loginResult.isFailed()) {
                return loginResult.getFailedLoginDetails();
            }

            String userName = loginResult.getAuthorizedUsername();
            Set<Group> userGroups = this.groupManager.getGroupsForUser(this.userManager.getUser(userName));
            JSONArray groupList = new JSONArray();
            for (Group g : userGroups) {
                groupList.put(g.getReference().getName());
            }

            List<String> acceptedFields =
                this.configurationManager.getActiveConfiguration().getEnabledFieldNames();

            JSONObject response = generateSuccessfulResponse();
            response.put(ShareProtocol.SERVER_JSON_GETINFO_KEY_NAME_USERGROUPS, groupList);
            response.put(ShareProtocol.SERVER_JSON_GETINFO_KEY_NAME_ACCEPTEDFIELDS, acceptedFields);
            response.put(ShareProtocol.SERVER_JSON_GETINFO_KEY_NAME_UPDATESENABLED, true);
            response.put(ShareProtocol.SERVER_JSON_GETINFO_KEY_NAME_CONSENTS,
                this.consentManager.toJSON(this.consentManager.getSystemConsents()));

            BaseObject serverConfig = getSourceServerConfiguration(request.getRemoteAddr(), context); // TODO: make nice
            if (this.userTokensEnabled(serverConfig)) {

                // a valid token should be returned for every configuration request. If a valid token already exists,
                // that token will be returned back. Otherwise a new one is generated and returned
                String token = loginResult.getUserToken() == null
                        ? generateNewToken(userName)
                        : loginResult.getUserToken();

                String serverName = getRemoteServerName(serverConfig, request);
                this.logger.debug("Remote server name: [{}]", serverName);

                this.storageManager.storeLocalLoginToken(userName, serverName, token);

                response.put(ShareProtocol.SERVER_JSON_GETINFO_KEY_NAME_USERTOKEN, token);
            }
            return response;

        } catch (Exception ex) {
            this.logger.error("Unable to perform getConfig [{}] {}", ex.getMessage(), ex);
            return generateFailedActionResponse();
        }
    }

    @Override
    public JSONObject getPatientURL()
    {
        try {
            XWikiContext context = getXContext();
            XWikiRequest request = context.getRequest();

            this.logger.warn("Get patient URL request from remote [{}]", request.getRemoteAddr());

            LoginResult loginResult = validateLogin(request, context);
            if (loginResult.isFailed()) {
                return loginResult.getFailedLoginDetails();
            }

            String userName = loginResult.getAuthorizedUsername();
            String guid = request.getParameter(ShareProtocol.CLIENT_POST_KEY_NAME_GUID);

            if (userName == null || guid == null) {
                return generateFailedActionResponse();
            }

            Patient patient = getPatientByGUID(guid);

            if (patient == null) {
                return generateFailedActionResponse(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_INCORRECTGUID);
            }
            if (!userCanAccessPatient(userName, patient)) {
                return generateFailedActionResponse(ShareProtocol.SERVER_JSON_KEY_NAME_ERROR_GUIDACCESSDENIED);
            }

            return generateSuccessfulResponseWithPatientIDs(patient, context);
        } catch (Exception ex) {
            this.logger.error("Unable to process URL request [{}] {}", ex.getMessage(), ex);
            return generateFailedActionResponse();
        }
    }

    protected Patient getPatientByGUID(String guid)
    {
        try {
            Query q =
                this.queryManager.createQuery("from doc.object(PhenoTips.PatientClass) as o where o.guid = :guid",
                    Query.XWQL).bindValue("guid", guid);

            List<String> results = q.<String>execute();

            if (results.size() == 1) {

                DocumentReference reference =
                    this.stringResolver.resolve(results.get(0), Patient.DEFAULT_DATA_SPACE);

                return new PhenoTipsPatient((XWikiDocument) this.bridge.getDocument(reference));
            }
        } catch (Exception ex) {
            this.logger.warn("Failed to get patient with GUID [{}]: [{}] {}", guid, ex.getMessage(), ex);
        }

        return null;
    }

    private boolean userCanAccessPatient(String userName, Patient patient)
    {
        try {
            String owner = this.permissionManager.getPatientAccess(patient).getOwner().getUsername();
            if (owner.equals(userName)) {
                return true;
            }

            boolean hasEditRights =
                this.authService.hasAccess(this.userManager.getUser(userName), Right.EDIT, patient.getDocument());
            if (hasEditRights) {
                return true;
            }
        } catch (Exception ex) {
        }
        return false;
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

    /**
     * Get the source push server configuration given its IP.
     *
     * @param serverIP IP of the server
     * @param context the current request object
     * @return {@link BaseObject XObjects} with server configuration, may be {@code null}
     */
    private BaseObject getSourceServerConfiguration(String serverIP, XWikiContext context)
    {
        try {
            XWiki xwiki = context.getWiki();
            XWikiDocument prefsDoc =
                xwiki.getDocument(new DocumentReference(context.getWikiId(), "XWiki", "XWikiPreferences"), context);
            BaseObject result = prefsDoc.getXObject(new DocumentReference(context.getWikiId(), Constants.CODE_SPACE,
                "ReceivePatientServer"), SERVER_CONFIG_IP_PROPERTY_NAME, serverIP);

            if (result != null) {
                return result;
            }

            // failed to find by IP - look up by hostname
            String domainName = InetAddress.getByName(serverIP).getHostName();
            return prefsDoc.getXObject(new DocumentReference(context.getWikiId(), Constants.CODE_SPACE,
                "ReceivePatientServer"), SERVER_CONFIG_IP_PROPERTY_NAME, domainName);
        } catch (Exception ex) {
            this.logger.warn("Failed to get server info: [{}] {}", ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Get the non-server specific part of the "Receive Patient Configuration".
     *
     * @param context the current request object
     * @return {@link BaseObject XObjects} receive configuration
     */
    private BaseObject getMainConfiguration(XWikiContext context)
    {
        try {
            XWiki xwiki = context.getWiki();
            XWikiDocument prefsDoc =
                xwiki.getDocument(new DocumentReference(context.getWikiId(), "XWiki", "XWikiPreferences"), context);
            BaseObject result = prefsDoc.getXObject(new DocumentReference(context.getWikiId(), Constants.CODE_SPACE,
                "ReceivePatientSettings"));

            if (result != null) {
                return result;
            }

            return null;
        } catch (Exception ex) {
            this.logger.warn("Failed to get server info: [{}] {}", ex.getMessage(), ex);
            return null;
        }
    }
}
