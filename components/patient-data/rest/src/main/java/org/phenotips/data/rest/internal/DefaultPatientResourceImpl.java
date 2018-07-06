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

import org.phenotips.data.Patient;
import org.phenotips.data.PatientDataController;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.PatientWritePolicy;
import org.phenotips.data.rest.PatientResource;
import org.phenotips.rest.Autolinker;
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.rest.XWikiResource;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

/**
 * Default implementation for {@link PatientResource} using XWiki's support for REST resources.
 *
 * @version $Id$
 * @since 1.2M5
 */
@Component
@Named("org.phenotips.data.rest.internal.DefaultPatientResourceImpl")
@Singleton
public class DefaultPatientResourceImpl extends XWikiResource implements PatientResource
{
    /** Name of the URL parameter used for excluding a given controller when retrieving a patient. */
    public static final String EXCLUDE_FIELD_URL_PARAM = "excludeField";

    /** Name of the URL parameter used for including a given controller when retrieving a patient. */
    public static final String INCLUDE_FIELD_URL_PARAM = "includeField";

    @Inject
    private PatientRepository repository;

    @Inject
    private AuthorizationService access;

    @Inject
    private UserManager users;

    @Inject
    private Provider<Autolinker> autolinker;

    /** The list of all the initialized data controllers. */
    @Inject
    private List<PatientDataController> patientDataControllers;

    /**
     * Needed for getting access to the servlet request, which is used to determine which controllers are selected for
     * JSON export.
     */
    @Inject
    private Container container;

    @Override
    public Response getPatient(String id)
    {
        this.slf4Jlogger.debug("Retrieving patient record [{}] via REST", id);
        Patient patient = this.repository.get(id);
        if (patient == null) {
            this.slf4Jlogger.debug("No such patient record: [{}]", id);
            return Response.status(Status.NOT_FOUND).build();
        }
        User currentUser = this.users.getCurrentUser();
        Right grantedRight;
        if (!this.access.hasAccess(currentUser, Right.VIEW, patient.getDocumentReference())) {
            this.slf4Jlogger.debug("View access denied to user [{}] on patient record [{}]", currentUser, id);
            return Response.status(Status.FORBIDDEN).build();
        } else {
            grantedRight = Right.VIEW;
        }
        if (this.access.hasAccess(currentUser, Right.EDIT, patient.getDocumentReference())) {
            grantedRight = Right.EDIT;
        }
        Right manageRight = Right.toRight("manage");
        if (manageRight != Right.ILLEGAL
            && this.access.hasAccess(currentUser, manageRight, patient.getDocumentReference())) {
            grantedRight = manageRight;
        }
        JSONObject json = patient.toJSON(this.getSelectedControllerNames());
        json.put("links",
            this.autolinker.get().forResource(getClass(), this.uriInfo)
                .withExtraParameters("entity-type", "patients")
                .withGrantedRight(grantedRight).build());
        return Response.ok(json, MediaType.APPLICATION_JSON_TYPE).build();
    }

    @Override
    public Response updatePatient(String json, String id, String policy)
    {
        final PatientWritePolicy policyType = PatientWritePolicy.fromString(policy);
        return updatePatient(json, id, policyType);
    }

    @Override
    public Response patchPatient(final String json, final String id)
    {
        return updatePatient(json, id, PatientWritePolicy.MERGE);
    }

    /**
     * Update a patient record, identified by its {@code id internal PhenoTips identifier}, from its {@code json JSON
     * representation}, and according to the provided {@code policy}. If the indicated patient record doesn't exist, or
     * if the user sending the request doesn't have the right to edit the target patient record, no change is performed
     * and an error is returned. If a field is set in the patient record, but missing in the JSON, then that field is
     * not changed, unless {@link PatientWritePolicy#REPLACE} is selected.
     *
     * @param json the JSON representation of the new patient to add
     * @param id the patient's internal identifier, see {@link org.phenotips.data.Patient#getId()}
     * @param policy the {@link PatientWritePolicy} according to which the patient should be updated
     * @return a status message
     */
    private Response updatePatient(final String json, final String id, final PatientWritePolicy policy)
    {
        this.slf4Jlogger.debug("Updating patient record [{}] via REST with JSON: {}", id, json);
        if (policy == null || json == null) {
            // json == null does not create an exception when initializing a JSONObject
            // need to handle it separately to give explicit BAD_REQUEST to the user
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        Patient patient = this.repository.get(id);
        if (patient == null) {
            this.slf4Jlogger.debug(
                "Patient record [{}] doesn't exist yet. It can be created by POST-ing the JSON to /rest/patients", id);
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        User currentUser = this.users.getCurrentUser();
        if (!this.access.hasAccess(currentUser, Right.EDIT, patient.getDocumentReference())) {
            this.slf4Jlogger.debug("Edit access denied to user [{}] on patient record [{}]", currentUser, id);
            throw new WebApplicationException(Status.FORBIDDEN);
        }
        JSONObject jsonInput;
        try {
            jsonInput = new JSONObject(json);
        } catch (Exception ex) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        String idFromJson = jsonInput.optString("id");
        if (StringUtils.isNotBlank(idFromJson) && !patient.getId().equals(idFromJson)) {
            // JSON for a different patient, bail out
            throw new WebApplicationException(Status.CONFLICT);
        }
        try {
            patient.updateFromJSON(jsonInput, policy);
        } catch (Exception ex) {
            this.slf4Jlogger.warn("Failed to update patient [{}] from JSON: {}. Source JSON was: {}", patient.getId(),
                ex.getMessage(), json);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
        return Response.noContent().build();
    }

    @Override
    public Response deletePatient(String id)
    {
        this.slf4Jlogger.debug("Deleting patient record [{}] via REST", id);
        Patient patient = this.repository.get(id);
        if (patient == null) {
            this.slf4Jlogger.debug("Patient record [{}] didn't exist", id);
            return Response.status(Status.NOT_FOUND).build();
        }
        User currentUser = this.users.getCurrentUser();
        if (!this.access.hasAccess(currentUser, Right.DELETE, patient.getDocumentReference())) {
            this.slf4Jlogger.debug("Delete access denied to user [{}] on patient record [{}]", currentUser, id);
            return Response.status(Status.FORBIDDEN).build();
        }
        try {
            this.repository.delete(patient);
        } catch (Exception ex) {
            this.slf4Jlogger.warn("Failed to delete patient record [{}]: {}", id, ex.getMessage());
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
        this.slf4Jlogger.debug("Deleted patient record [{}]", id);
        return Response.noContent().build();
    }

    /**
     * Returns the set of selected patient data controller names. Looks at "include field" and "exclude field" HTTP
     * request URL parameters; if at least one "include field" is specified, operates in whitelist mode using the set of
     * "include fields", otherwise operates in blacklist mode using the set of "exclude fields" (with all controllers
     * being included by default). Operates using the set of patient data controller instances injected by the XWiki
     * component manager.
     * <p>
     * Multiple include or exclude fields may be specified in typical servlet fashion by repeating the URL parameter
     * with different argument values, e.g. includeField=field1&includeField=field2&includeField=field3
     *
     * @return A set of selected patient data controller names, as specified by the controllers' respective
     *         {@code getName()} methods.
     *         An empty set, in case all controllers are excluded (or no controllers have been injected)
     */
    private Collection<String> getSelectedControllerNames()
    {
        HttpServletRequest httpServletRequest = ((ServletRequest) this.container.getRequest()).getHttpServletRequest();

        String[] excludedFieldsArr = httpServletRequest.getParameterValues(EXCLUDE_FIELD_URL_PARAM);
        String[] includedFieldsArr = httpServletRequest.getParameterValues(INCLUDE_FIELD_URL_PARAM);
        List<String> excludedFields = Arrays.asList(excludedFieldsArr != null ? excludedFieldsArr : new String[0]);
        List<String> includedFields = Arrays.asList(includedFieldsArr != null ? includedFieldsArr : new String[0]);

        Collection<String> selectedControllerNames = new HashSet<>();
        for (PatientDataController<?> controller : this.patientDataControllers) {
            String name = controller.getName();

            if (includedFields.contains(name) || (includedFields.isEmpty() && !excludedFields.contains(name))) {
                selectedControllerNames.add(name);
            }
        }

        return selectedControllerNames;
    }
}
