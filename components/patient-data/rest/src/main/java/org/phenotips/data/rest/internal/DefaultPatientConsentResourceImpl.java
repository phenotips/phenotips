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
package org.phenotips.data.rest.internal;

import org.phenotips.data.Consent;
import org.phenotips.data.ConsentManager;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.rest.PatientConsentResource;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.slf4j.Logger;

/**
 * The only implementation of the {@link PatientConsentResource} endpoints.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Component
@Named("org.phenotips.data.rest.internal.DefaultPatientConsentResourceImpl")
@Singleton
public class DefaultPatientConsentResourceImpl extends XWikiResource implements PatientConsentResource
{
    private static final Response.Status INVALID_CONSENT_ID_CODE = Response.Status.BAD_REQUEST;
    private static final Response.Status PATIENT_NOT_FOUND = Response.Status.NOT_FOUND;
    private static final Response.Status ACCESS_DENIED = Response.Status.FORBIDDEN;

    @Inject
    private Logger logger;

    @Inject
    private PatientRepository repository;

    @Inject
    private AuthorizationManager access;

    @Inject
    private UserManager users;

    @Inject
    private ConsentManager consentManager;

    @Override
    public Response getConsents(String patientId)
    {
        this.logger.debug("Retrieving consents from patient record [{}] via REST", patientId);
        Security security = this.securityCheck(patientId, Right.VIEW);
        if (security.isAllowed()) {
            Set<Consent> consents = consentManager.getAllConsentsForPatient(security.getPatient());
            JSONArray json = consentManager.toJSON(consents);
            return Response.ok(json, MediaType.APPLICATION_JSON_TYPE).build();
        } else {
            return security.getFailResponse();
        }
    }

    @Override
    public Response grantConsent(String patientId, String id)
    {
        Security security = this.securityCheck(patientId, Right.EDIT);
        if (security.isAllowed()) {
            if (!this.consentManager.isValidConsentId(id)) {
                return Response.status(INVALID_CONSENT_ID_CODE).build();
            }
            boolean status = this.consentManager.grantConsent(security.getPatient(), id);
            if (status) {
                return Response.ok().build();
            } else {
                return Response.serverError().build();
            }
        } else {
            return security.getFailResponse();
        }
    }

    @Override
    public Response revokeConsent(String patientId, String id)
    {
        Security security = this.securityCheck(patientId, Right.EDIT);
        if (security.isAllowed()) {
            if (!this.consentManager.isValidConsentId(id)) {
                return Response.status(INVALID_CONSENT_ID_CODE).build();
            }
            boolean status = this.consentManager.revokeConsent(security.getPatient(), id);
            if (status) {
                return Response.ok().build();
            } else {
                return Response.serverError().build();
            }
        } else {
            return security.getFailResponse();
        }
    }

    @Override
    public Response assignConsents(@PathParam("patient_id") String patientId, String json)
    {
        try {
            Security security = this.securityCheck(patientId, Right.EDIT);
            if (security.isAllowed()) {
                JSONArray consentsJSON = json == null ? null : new JSONArray(json);
                Set<String> consentIds = new HashSet<String>();
                for (int i = 0; i < consentsJSON.length(); i++) {
                    String consentId = consentsJSON.optString(i);
                    if (consentId != null) {
                        if (!this.consentManager.isValidConsentId(consentId)) {
                            return Response.status(INVALID_CONSENT_ID_CODE).build();
                        }
                        consentIds.add(consentId);
                    }
                }
                boolean status = this.consentManager.setPatientConsents(security.getPatient(), consentIds);
                if (status) {
                    return Response.ok().build();
                } else {
                    return Response.serverError().build();
                }
            } else {
                return security.getFailResponse();
            }
        } catch (Exception ex) {
            this.logger.error("Could not process assign consents request [{}]: {}", json, ex);
            return Response.serverError().build();
        }
    }

    private Security securityCheck(String patientId, Right right)
    {
        Patient patient = this.repository.get(patientId);
        if (patient == null) {
            this.logger.debug("No such patient record: [{}]", patientId);
            Response response = Response.status(PATIENT_NOT_FOUND).build();
            return new Security(patient, response, false);
        }
        User currentUser = this.users.getCurrentUser();
        if (!this.access.hasAccess(right, currentUser == null ? null : currentUser.getProfileDocument(),
            patient.getDocument())) {
            this.logger.debug("View access denied to user [{}] on patient record [{}]", currentUser, patientId);
            Response response = Response.status(ACCESS_DENIED).build();
            return new Security(patient, response, false);
        }
        return new Security(patient, null, true);
    }

    private class Security
    {
        private Patient patient;

        private Response response;

        private boolean isAllowed;

        Security(Patient patient, Response failResponse, boolean isAllowed)
        {
            this.patient = patient;
            this.response = failResponse;
            this.isAllowed = isAllowed;
        }

        public Patient getPatient()
        {
            return this.patient;
        }

        public Response getFailResponse()
        {
            return this.response;
        }

        public boolean isAllowed()
        {
            return this.isAllowed;
        }
    }
}
