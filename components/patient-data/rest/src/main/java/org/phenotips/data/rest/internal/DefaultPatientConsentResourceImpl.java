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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.slf4j.Logger;

/**
 * The only implementation of the {@link PatientConsentResource} endpoints.
 *
 * @version $Id$
 * @since 1.2RC2
 */
@Component
@Named("org.phenotips.data.rest.internal.DefaultPatientConsentResourceImpl")
@Singleton
public class DefaultPatientConsentResourceImpl extends XWikiResource implements PatientConsentResource
{
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
        Security security = this.securityCheck(patientId);
        if (security.isAllowed()) {
            List<Consent> consents = consentManager.loadConsentsFromPatient(security.getPatient());
            JSONArray json = consentManager.toJson(consents);
            return Response.ok(json, MediaType.APPLICATION_JSON_TYPE).build();
        } else {
            return security.getFailResponse();
        }
    }

    @Override
    public Response grantConsent(String patientId, String id)
    {
        Security security = this.securityCheck(patientId);
        if (security.isAllowed()) {
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
        Security security = this.securityCheck(patientId);
        if (security.isAllowed()) {
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

    private Security securityCheck(String patientId)
    {
        Patient patient = this.repository.getPatientById(patientId);
        if (patient == null) {
            this.logger.debug("No such patient record: [{}]", patientId);
            Response response = Response.status(Response.Status.NOT_FOUND).build();
            return new Security(patient, response, false);
        }
        User currentUser = this.users.getCurrentUser();
        if (!this.access.hasAccess(Right.VIEW, currentUser == null ? null : currentUser.getProfileDocument(),
            patient.getDocument())) {
            this.logger.debug("View access denied to user [{}] on patient record [{}]", currentUser, patientId);
            Response response = Response.status(Response.Status.FORBIDDEN).build();
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
