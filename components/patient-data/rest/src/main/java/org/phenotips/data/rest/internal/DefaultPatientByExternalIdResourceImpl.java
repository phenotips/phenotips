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
import org.phenotips.data.PatientWritePolicy;
import org.phenotips.data.rest.DomainObjectFactory;
import org.phenotips.data.rest.PatientByExternalIdResource;
import org.phenotips.data.rest.PatientResource;
import org.phenotips.data.rest.model.Alternatives;
import org.phenotips.rest.Autolinker;
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.rest.XWikiResource;
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
    private PatientRepository repository;

    @Inject
    private DomainObjectFactory factory;

    @Inject
    private QueryManager qm;

    @Inject
    private AuthorizationService access;

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
        this.slf4Jlogger.debug("Retrieving patient record with external ID [{}] via REST", eid);
        Patient patient = this.repository.getByName(eid);
        if (patient == null) {
            return checkForMultipleRecords(patient, eid);
        }
        User currentUser = this.users.getCurrentUser();
        Right grantedRight;
        if (!this.access.hasAccess(currentUser, Right.VIEW, patient.getDocumentReference())) {
            this.slf4Jlogger.debug("View access denied to user [{}] on patient record [{}]", currentUser,
                patient.getId());
            return Response.status(Status.FORBIDDEN).build();
        } else {
            grantedRight = Right.VIEW;
        }
        if (this.access.hasAccess(currentUser, Right.EDIT, patient.getDocumentReference())) {
            grantedRight = Right.EDIT;
        }

        JSONObject json = patient.toJSON();
        json.put("links", this.autolinker.get().forResource(PatientResource.class, this.uriInfo)
            .withExtraParameters("entity-id", patient.getId())
            .withExtraParameters("entity-type", "patients")
            .withGrantedRight(grantedRight)
            .build());
        return Response.ok(json, MediaType.APPLICATION_JSON_TYPE).build();
    }

    @Override
    public Response updatePatient(final String json, final String eid, final String policy)
    {
        final PatientWritePolicy policyType = PatientWritePolicy.fromString(policy);
        return updatePatient(json, eid, policyType);
    }

    @Override
    public Response patchPatient(final String json, final String eid)
    {
        return updatePatient(json, eid, PatientWritePolicy.MERGE);
    }

    /**
     * Update a patient record, identified by its given {@code eid "external" identifier}, from its {@code json JSON
     * representation}, and according to the provided {@code policy}. If the user sending the request doesn't have the
     * right to edit the target patient record, no change is performed and an error is returned. If the indicated
     * patient record doesn't exist, and a valid JSON is provided, a new patient record is created with the provided
     * data. If multiple records exist with the same given identifier, no change is performed, and a list of links to
     * each such record is returned. If a field is set in the patient record, but missing in the JSON, then that field
     * is not changed, unless {@link PatientWritePolicy#REPLACE} is selected.
     *
     * @param json the JSON representation of the new patient to add
     * @param eid the patient's given "external" identifier, see {@link org.phenotips.data.Patient#getExternalId()}
     * @param policy the {@link PatientWritePolicy} according to which the patient should be updated
     * @return a status message
     */
    private Response updatePatient(final String json, final String eid, final PatientWritePolicy policy)
    {
        this.slf4Jlogger.debug("Updating patient record with external ID [{}] via REST with JSON: {}", eid, json);
        if (policy == null) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        if (json == null) {
            // json == null does not create an exception when initializing a JSONObject
            // need to handle it separately to give explicit BAD_REQUEST to the user
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        Patient patient = this.repository.getByName(eid);
        if (patient == null) {
            return checkRecords(eid, json);
        }
        User currentUser = this.users.getCurrentUser();
        if (!this.access.hasAccess(currentUser, Right.EDIT, patient.getDocumentReference())) {
            this.slf4Jlogger.debug("Edit access denied to user [{}] on patient record [{}]", currentUser,
                patient.getId());
            throw new WebApplicationException(Status.FORBIDDEN);
        }
        JSONObject jsonInput;
        try {
            jsonInput = new JSONObject(json);
        } catch (final JSONException ex) {
            this.slf4Jlogger.error("Provided patient json: {} is invalid.", json);
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        if (hasInternalIdConflict(jsonInput, patient)) {
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
    public Response deletePatient(String eid)
    {
        this.slf4Jlogger.debug("Deleting patient record with external ID [{}] via REST", eid);
        Patient patient = this.repository.getByName(eid);
        if (patient == null) {
            return checkForMultipleRecords(patient, eid);
        }
        User currentUser = this.users.getCurrentUser();
        if (!this.access.hasAccess(currentUser, Right.DELETE, patient.getDocumentReference())) {
            this.slf4Jlogger.debug("Delete access denied to user [{}] on patient record [{}]", currentUser,
                patient.getId());
            return Response.status(Status.FORBIDDEN).build();
        }
        try {
            this.repository.delete(patient);
        } catch (Exception ex) {
            this.slf4Jlogger.warn("Failed to delete patient record with external id [{}]: {}", eid, ex.getMessage());
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
        this.slf4Jlogger.debug("Deleted patient record with external id [{}]", eid);
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
                this.slf4Jlogger.debug("Multiple patient records ({}) with external ID [{}]: {}", results.size(), eid,
                    results);
                Alternatives response = this.factory.createAlternatives(results, this.uriInfo);
                return Response.status(300).entity(response).build();
            }
        } catch (QueryException ex) {
            this.slf4Jlogger.warn("Failed to retrieve patient with external id [{}]: {}", eid, ex.getMessage());
        }
        if (patient == null) {
            this.slf4Jlogger.debug("No patient record with external ID [{}] exists yet", eid);
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
        this.slf4Jlogger.debug("Creating patient record with external ID [{}]", eid);
        try {
            if (StringUtils.isBlank(json)) {
                this.slf4Jlogger.error("Provided patient json: {} is invalid.", json);
                return Response.status(Status.BAD_REQUEST).build();
            }
            final JSONObject patientJson = new JSONObject(json);
            final User currentUser = this.users.getCurrentUser();
            if (!this.access.hasAccess(currentUser, Right.EDIT,
                this.currentResolver.resolve(Patient.DEFAULT_DATA_SPACE, EntityType.SPACE))) {
                this.slf4Jlogger.error("Edit access denied to user [{}].", currentUser);
                return Response.status(Status.FORBIDDEN).build();
            }
            final Patient patient = this.repository.create();
            return updatePatientWithJsonData(patient, eid, patientJson);
        } catch (final JSONException ex) {
            this.slf4Jlogger.error("Provided patient json: {} is invalid.", json);
            return Response.status(Status.BAD_REQUEST).build();
        } catch (final Exception ex) {
            this.slf4Jlogger.error("Failed to create patient with external ID: [{}] from JSON: {}.", eid, json);
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
