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
import org.phenotips.data.PatientRepository;
import org.phenotips.data.rest.DomainObjectFactory;
import org.phenotips.data.rest.PatientByExternalIdResource;
import org.phenotips.data.rest.PatientResource;
import org.phenotips.data.rest.model.Alternatives;
import org.phenotips.rest.Autolinker;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.rest.XWikiResource;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * Default implementation for {@link PatientByExternalIdResource} using XWiki's support for REST resources.
 *
 * @version $Id$
 * @since 1.2M5
 */
@Component
@Named("org.phenotips.data.rest.internal.DefaultPatientByExternalIdResourceImpl")
@Singleton
public class DefaultPatientByExternalIdResourceImpl extends XWikiResource implements PatientByExternalIdResource
{
    private static final String EID_LABEL = "external_id";

    @Inject
    private Logger logger;

    @Inject
    private PatientRepository repository;

    @Inject
    private DomainObjectFactory factory;

    @Inject
    private QueryManager qm;

    @Inject
    private AuthorizationManager access;

    @Inject
    private UserManager users;

    /** Fills in missing reference fields with those from the current context document to create a full reference. */
    @Inject
    @Named("current")
    private EntityReferenceResolver<EntityReference> currentResolver;

    @Inject
    private Provider<Autolinker> autolinker;

    @Override
    public Response getPatient(String eid)
    {
        this.logger.debug("Retrieving patient record with external ID [{}] via REST", eid);
        Patient patient = this.repository.getByName(eid);
        if (patient == null) {
            return checkForMultipleRecords(patient, eid);
        }
        User currentUser = this.users.getCurrentUser();
        Right grantedRight;
        if (!this.access.hasAccess(Right.VIEW, currentUser == null ? null : currentUser.getProfileDocument(),
            patient.getDocumentReference())) {
            this.logger.debug("View access denied to user [{}] on patient record [{}]", currentUser, patient.getId());
            return Response.status(Status.FORBIDDEN).build();
        } else {
            grantedRight = Right.VIEW;
        }
        if (this.access.hasAccess(Right.EDIT, currentUser == null ? null : currentUser.getProfileDocument(),
            patient.getDocumentReference())) {
            grantedRight = Right.EDIT;
        }

        JSONObject json = patient.toJSON();
        json.put("links", this.autolinker.get().forResource(PatientResource.class, this.uriInfo)
            .withExtraParameters("patient-id", patient.getId())
            .withGrantedRight(grantedRight)
            .build());
        return Response.ok(json, MediaType.APPLICATION_JSON_TYPE).build();
    }

    @Override
    public Response updatePatient(String json, String eid)
    {
        this.logger.debug("Updating patient record with external ID [{}] via REST with JSON: {}", eid, json);
        Patient patient = this.repository.getByName(eid);
        if (patient == null) {
            return checkRecords(eid, json);
        }
        User currentUser = this.users.getCurrentUser();
        if (!this.access.hasAccess(Right.EDIT, currentUser == null ? null : currentUser.getProfileDocument(),
            patient.getDocumentReference())) {
            this.logger.debug("Edit access denied to user [{}] on patient record [{}]", currentUser, patient.getId());
            throw new WebApplicationException(Status.FORBIDDEN);
        }
        JSONObject jsonInput;
        try {
            jsonInput = new JSONObject(json);
        } catch (final JSONException ex) {
            this.logger.error("Provided patient json: {} is invalid.", json);
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        if (hasInternalIdConflict(jsonInput, patient)) {
            throw new WebApplicationException(Status.CONFLICT);
        }
        try {
            patient.updateFromJSON(jsonInput);
        } catch (Exception ex) {
            this.logger.warn("Failed to update patient [{}] from JSON: {}. Source JSON was: {}", patient.getId(),
                ex.getMessage(), json);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
        return Response.noContent().build();
    }

    @Override
    public Response deletePatient(String eid)
    {
        this.logger.debug("Deleting patient record with external ID [{}] via REST", eid);
        Patient patient = this.repository.getByName(eid);
        if (patient == null) {
            return checkForMultipleRecords(patient, eid);
        }
        User currentUser = this.users.getCurrentUser();
        if (!this.access.hasAccess(Right.DELETE, currentUser == null ? null : currentUser.getProfileDocument(),
            patient.getDocumentReference())) {
            this.logger.debug("Delete access denied to user [{}] on patient record [{}]", currentUser, patient.getId());
            return Response.status(Status.FORBIDDEN).build();
        }
        try {
            this.repository.delete(patient);
        } catch (Exception ex) {
            this.logger.warn("Failed to delete patient record with external id [{}]: {}", eid, ex.getMessage());
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
        this.logger.debug("Deleted patient record with external id [{}]", eid);
        return Response.noContent().build();
    }

    private Response checkForMultipleRecords(Patient patient, String eid)
    {
        try {
            Query q =
                this.qm.createQuery("where doc.object(PhenoTips.PatientClass).external_id = :eid", Query.XWQL);
            q.bindValue("eid", eid);
            List<String> results = q.execute();
            if (results.size() > 1) {
                this.logger.debug("Multiple patient records ({}) with external ID [{}]: {}", results.size(), eid,
                    results);
                Alternatives response = this.factory.createAlternatives(results, this.uriInfo);
                return Response.status(300).entity(response).build();
            }
        } catch (QueryException ex) {
            this.logger.warn("Failed to retrieve patient with external id [{}]: {}", eid, ex.getMessage());
        }
        if (patient == null) {
            this.logger.debug("No patient record with external ID [{}] exists yet", eid);
            return Response.status(Status.NOT_FOUND).build();
        }
        return null;
    }

    /**
     * Checks patient records for duplicates. Creates and updates a patient if no existing patients with {@code eid} are
     * found, otherwise lists duplicate records in the response.
     *
     * @param eid the external ID of the patient of interest
     * @param json the data to update the patient with
     * @return a {@link Response} with no content if a new patient is created, a list of patients if duplicate patients
     *         exist, or an error code in the event of error
     */
    private Response checkRecords(final String eid, final String json)
    {
        final Response response = checkForMultipleRecords(null, eid);
        return (response != null && response.getStatus() == Status.NOT_FOUND.getStatusCode())
            ? createPatient(eid, json)
            : response;
    }

    /**
     * Tries to create a patient with provided {@code eid} and {@code json data}. Returns a {@link Response} with no
     * content if successful, an error code otherwise.
     *
     * @param eid the external identifier for the patient; will be overwritten if an eid is specified in {@code json}
     * @param json patient data as json string
     * @return a {@link Response} with no content if successful, an error code otherwise
     */
    private Response createPatient(final String eid, final String json)
    {
        this.logger.debug("Creating patient record with external ID [{}]", eid);
        try {
            if (StringUtils.isBlank(json)) {
                this.logger.error("Provided patient json: {} is invalid.", json);
                return Response.status(Status.BAD_REQUEST).build();
            }
            final JSONObject patientJson = new JSONObject(json);
            final User currentUser = this.users.getCurrentUser();
            if (!this.access.hasAccess(Right.EDIT, currentUser == null ? null : currentUser.getProfileDocument(),
                this.currentResolver.resolve(Patient.DEFAULT_DATA_SPACE, EntityType.SPACE))) {
                this.logger.error("Edit access denied to user [{}].", currentUser);
                return Response.status(Status.FORBIDDEN).build();
            }
            final Patient patient = this.repository.create();
            return updatePatientWithJsonData(patient, eid, patientJson);
        } catch (final JSONException ex) {
            this.logger.error("Provided patient json: {} is invalid.", json);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final Exception ex) {
            this.logger.error("Failed to create patient with external ID: [{}] from JSON: {}.", eid, json);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Updates the {@code patient} with provided {@code patientJson JSON data}.
     *
     * @param patient the {@link Patient} to update
     * @param eid the external identifier for the patient
     * @param patientJson the patient data as {@link JSONObject}
     * @return a response with no content if patient update is successful, an error code otherwise
     */
    private Response updatePatientWithJsonData(final Patient patient, final String eid, final JSONObject patientJson)
    {
        // Check that internal ids are not conflicting.
        if (hasInternalIdConflict(patientJson, patient)) {
            return Response.status(Status.CONFLICT).build();
        }
        // Since we're creating a new patient, if eid is not provided in patientJson, use the eid that was passed in
        if (!patientJson.has(EID_LABEL)) {
            patientJson.put(EID_LABEL, eid);
        }
        patient.updateFromJSON(patientJson);
        return Response.noContent().build();
    }

    /**
     * Returns true iff the internal ID is specified in {@code jsonInput} and does not correspond with the patient
     * internal ID, false otherwise.
     *
     * @param jsonInput the patient data being imported
     * @param patient the {@link Patient} object
     * @return true iff the internal ID is specified in {@code jsonInput} and conflicts with patient internal ID, false
     *         otherwise
     */
    private boolean hasInternalIdConflict(final JSONObject jsonInput, final Patient patient)
    {
        final String idFromJson = jsonInput.optString("id");
        return StringUtils.isNotBlank(idFromJson) && !patient.getId().equals(idFromJson);
    }
}
