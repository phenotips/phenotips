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
import org.phenotips.data.rest.PatientByLabeledExternalIdentifierResource;
import org.phenotips.data.rest.PatientResource;
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

import java.util.Collections;
import java.util.List;
import java.util.MissingResourceException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * Default implementation for {@link PatientByLabeledExternalIdentifierResource} using XWiki's support for REST
 * resources.
 *
 * @version $Id$
 */
@Component
@Named("org.phenotips.data.rest.internal.DefaultPatientByLabeledExternalIdentifierResourceImpl")
@Singleton
public class DefaultPatientByLabeledExternalIdentifierResourceImpl extends XWikiResource implements
    PatientByLabeledExternalIdentifierResource
{
    private static final String KEY_LABELED_EIDS = "labeled_eids";

    private static final String KEY_LABEL = "label";

    private static final String KEY_VALUE = "value";

    @Inject
    private Logger logger;

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

    @Inject
    private Provider<Autolinker> autolinker;

    /** Fills in missing reference fields with those from the current context document to create a full reference. */
    @Inject
    @Named("current")
    private EntityReferenceResolver<EntityReference> currentResolver;

    @Override
    public Response getPatient(String label, String id)
    {
        this.logger.debug("Retrieving patient record with label [{}] and corresponding external ID [{}] via REST",
            label, id);
        Patient patient;
        List<String> patients = getPatientInternalIdentifiersByLabelAndEid(label, id);
        if (patients.size() == 1) {
            patient = this.repository.get(patients.get(0));
        } else {
            return returnIfEmptyOrMultipleExistResponse(patients, label, id);
        }

        User currentUser = this.users.getCurrentUser();
        Right grantedRight;
        if (!this.access.hasAccess(currentUser, Right.VIEW, patient.getDocumentReference())) {
            this.logger.debug("View access denied to user [{}] on patient record [{}]", currentUser, patient.getId());
            throw new WebApplicationException(Response.Status.FORBIDDEN);
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
    public Response updatePatient(final String json, final String label, final String id,
        final String policy, final boolean create)
    {
        final PatientWritePolicy policyType = PatientWritePolicy.fromString(policy);
        return updatePatient(json, label, id, policyType, create);
    }

    private Response updatePatient(final String json, final String label, final String id,
        final PatientWritePolicy policy, final boolean create)
    {
        this.logger.debug("Updating patient record with label [{}] and corresponding external ID [{}] via REST"
                          + " with JSON: {}", label, id, json);
        if (policy == null) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        if (json == null) {
            // json == null does not create an exception when initializing a JSONObject
            // need to handle it separately to give explicit BAD_REQUEST to the user
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        JSONObject jsonInput;
        try {
            jsonInput = new JSONObject(json);
        } catch (final JSONException ex) {
            this.logger.error("Provided patient json: {} is invalid.", json, ex);
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        Patient patient;
        List<String> patients = getPatientInternalIdentifiersByLabelAndEid(label, id);
        if (patients.size() == 1) {
            patient = this.repository.get(patients.get(0));
        } else {
            return returnIfEmptyOrMultipleExistResponse(patients, label, id, jsonInput, create);
        }

        User currentUser = this.users.getCurrentUser();
        if (!this.access.hasAccess(currentUser, Right.EDIT, patient.getDocumentReference())) {
            this.logger.debug("Edit access denied to user [{}] on patient record [{}]", currentUser, patient.getId());
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        if (hasInternalIdConflict(jsonInput, patient)) {
            throw new WebApplicationException(Response.Status.CONFLICT);
        }
        return returnUpdatePatientResponse(patient, label, id, jsonInput, false, policy);
    }

    @Override
    public Response patchPatient(String json, String label, String id, boolean create)
    {
        return updatePatient(json, label, id, PatientWritePolicy.MERGE, create);
    }

    @Override
    public Response deletePatient(String label, String id)
    {
        this.logger.debug("Deleting patient record with label [{}] and corresponding external ID [{}] via REST",
            label, id);
        Patient patient;
        List<String> patients = getPatientInternalIdentifiersByLabelAndEid(label, id);
        if (patients.size() == 1) {
            patient = this.repository.get(patients.get(0));
        } else {
            return returnIfEmptyOrMultipleExistResponse(patients, label, id);
        }

        User currentUser = this.users.getCurrentUser();
        if (!this.access.hasAccess(currentUser, Right.DELETE, patient.getDocumentReference())) {
            this.logger.debug("Delete access denied to user [{}] on patient record [{}]", currentUser, patient.getId());
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        try {
            this.repository.delete(patient);
        } catch (Exception ex) {
            this.logger.warn("Failed to delete patient record with label [{}] and corresponding external id [{}]: {}",
                label, id, ex.getMessage(), ex);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        this.logger.debug("Deleted patient record with label [{}] corresponding external id [{}]", label, id);
        return Response.noContent().build();
    }

    private Response returnIfEmptyOrMultipleExistResponse(List<String> patients, String label, String id)
    {
        return returnIfEmptyOrMultipleExistResponse(patients, label, id, null, false);
    }

    /**
     * Checks for whether none or multiple patients exist and returns the appropriate response.
     * If {@code shouldCreate} is false and no existing patients exist for the provided {@code label} and {@code id}
     * (patients list is empty), then returns an error. If {@code shouldCreate} is true and no existing patients
     * exist for the provided {@code label} and {@code id}, then creates a patient and returns a successful response.
     * If there are multiple records that exist, then returns an array of links to the patient records.
     *
     * @param patients list of patient internal ID's for records that have the matching {@code label} and {@code id}
     * @param label the name of the label
     * @param id the id value for the label
     * @param jsonInput the incoming json from the request, used to update the created patient record
     * @param shouldCreate config for whether a patient record should be created
     * @return a {@link Response} with no content if successful, an error code otherwise
     */
    private Response returnIfEmptyOrMultipleExistResponse(List<String> patients, String label, String id,
        JSONObject jsonInput, boolean shouldCreate)
    {
        if (patients.isEmpty()) {
            this.logger.debug("No patient record with label [{}] and corresponding external ID [{}] exists yet",
                label, id);
            if (shouldCreate) {
                return returnCreatePatientResponse(label, id, jsonInput);
            } else {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
        } else {
            this.logger.debug("Multiple patient records ({}) with label [{}] and corresponding external ID [{}]: {}",
                patients.size(), label, id, patients);
            return Response.status(300).entity(this.factory.createAlternatives(patients, this.uriInfo)).build();
        }
    }

    /**
     * Tries to create a patient with provided {@code label} and {@code id} and {@code json data}. Returns a
     * {@link Response} with no content if successful, an error code otherwise.
     *
     * @param label the name of the label
     * @param id the id value for the label; will be overwritten if a label/id pair is specified in {@code jsonInput}
     * @param jsonInput the incoming json from the request, used to update the created patient record
     * @return a {@link Response} with no content if successful, an error code otherwise
     */
    private Response returnCreatePatientResponse(final String label, final String id, final JSONObject jsonInput)
    {
        this.logger.debug("Creating patient record with label [{}] and corresponding external ID [{}]", label, id);
        try {
            final User currentUser = this.users.getCurrentUser();
            if (!this.access.hasAccess(currentUser, Right.EDIT,
                this.currentResolver.resolve(Patient.DEFAULT_DATA_SPACE, EntityType.SPACE))) {
                this.logger.error("Edit access denied to user [{}].", currentUser);
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }
            final Patient patient = this.repository.create();
            return returnUpdatePatientResponse(patient, label, id, jsonInput, true, PatientWritePolicy.UPDATE);
        } catch (final WebApplicationException ex) {
            this.logger.error("Failed to create patient with label [{}] and corresponding external ID: [{}] from "
                              + "JSON: {}.", label, id, jsonInput, ex);
            throw new WebApplicationException(ex.getResponse().getStatus());
        }
    }

    /**
     * Updates the {@code patient} with provided {@code jsonInput JSON data}. If a patient was created
     * {@code wasCreated} and labeled identifiers {@code KEY_LABELED_EIDS} is not specified in the {@code jsonInput},
     * then the {@code label} and {@code id} provided in the request path parameters are used to set the labeled
     * identifiers in the patient.
     *
     * @param patient the {@link Patient} to update
     * @param label the name of the label
     * @param id the id value for the label
     * @param jsonInput the patient data used to update, as {@link JSONObject}
     * @param wasCreated whether a patient was created for the update
     * @param policy the policy according to which patient data should be written
     * @return a {@link Response} with no content if successful, an error code otherwise
     */
    private Response returnUpdatePatientResponse(final Patient patient, final String label, final String id,
        final JSONObject jsonInput, boolean wasCreated, PatientWritePolicy policy)
    {
        if (hasInternalIdConflict(jsonInput, patient)) {
            throw new WebApplicationException(Response.Status.CONFLICT);
        }
        if (wasCreated && !jsonInput.has(KEY_LABELED_EIDS)) {
            JSONArray labeledEids = new JSONArray();
            labeledEids.put(new JSONObject().accumulate(KEY_LABEL, label).accumulate(KEY_VALUE, id));
            jsonInput.put(KEY_LABELED_EIDS, labeledEids);
        }
        try {
            patient.updateFromJSON(jsonInput, policy);
        } catch (Exception ex) {
            this.logger.warn("Failed to update patient [{}] from JSON: {}. Source JSON was: {}", patient.getId(),
                ex.getMessage(), jsonInput, ex);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        return Response.noContent().build();
    }

    /**
     * Gets a list of patient internal IDs.
     *
     * @param label the name of the label
     * @param id the id value for the label
     * @return null if the label is invalid, an empty list if no patients have the label identifier and its
     *         corresponding id value, else a list of patient internal IDs
     */
    private List<String> getPatientInternalIdentifiersByLabelAndEid(String label, String id)
    {
        try {
            // Check global configs first before querying all labeled identifier objects on all patients, much cheaper
            if (isLabelConfiguredByAdmin(label) || allowOtherEids()) {
                return queryPatientsByLabelAndEid(label, id);
            } else {
                return Collections.emptyList();
            }
        } catch (QueryException ex) {
            this.logger.warn("Failed to query patient with label [{}] and corresponding external ID [{}]: {}",
                label, id, ex.getMessage(), ex);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } catch (MissingResourceException ex) {
            this.logger.warn("Failed to retrieve patient with label [{}] and corresponding external ID [{}]: {}",
                label, id, ex.getMessage(), ex);
            throw new WebApplicationException(Response.Status.CONFLICT);
        }
    }

    /**
     * For a given label and its corresponding id value (fields on {@code LabeledIdentifierClass} objects), query
     * patient documents.
     *
     * @param label the name of the label
     * @param id the id value for the label
     * @return an empty list if no patients have the label identifier and its corresponding id value, else a list of
     *         patient internal identifiers
     * @throws QueryException if there is any error during querying
     */
    private List<String> queryPatientsByLabelAndEid(String label, String id) throws QueryException
    {
        Query q = null;
        try {
            q = this.qm.createQuery(
                "select doc.name from Document doc, doc.object(PhenoTips.LabeledIdentifierClass) obj "
                + "where obj.label = :label and obj.value = :value", Query.XWQL);
            q.bindValue(KEY_LABEL, label);
            q.bindValue(KEY_VALUE, id);
            return q.execute();
        } catch (QueryException ex) {
            this.logger.warn("Failed to query patient documents with label [{}] and corresponding external ID [{}]: {}",
                label, id, ex.getMessage(), ex);
            throw new QueryException(ex.getMessage(), q, ex);
        }
    }

    /**
     * Checks if the label in question is configured at the administrative level, which means that the label is
     * available globally for all patients. If it is, then it will be the {@code label} value of a
     * {@code LabeledIdentifierSettings} object.
     *
     * @param label the name of the label
     * @return true if the label exists, else false
     * @throws QueryException if there is any error during querying
     */
    private boolean isLabelConfiguredByAdmin(String label) throws QueryException
    {
        Query q = null;
        try {
            q = this.qm.createQuery(
                "select obj.label from Document doc, doc.object(PhenoTips.LabeledIdentifierSettings) obj "
                + "where obj.label = :label", Query.XWQL);
            q.bindValue(KEY_LABEL, label);
            List<String> results = q.execute();
            return !results.isEmpty();
        } catch (QueryException ex) {
            this.logger.warn("Failed to query LabeledIdentifierSettings object: {}", ex.getMessage(), ex);
            throw new QueryException(ex.getMessage(), q, ex);
        }
    }

    /**
     * Checks whether the global configuration allows for other eids
     * {@code LabeledIdentifierGlobalSettings.allowOtherEids} to be set arbitrarily by the user for each patient.
     *
     * @return true if arbitrary eids are allowed, else false.
     * @throws QueryException if an instance of the {@code LabeledIdentifierGlobalSettings} object cannot be found,
     *         or if there is any other error during querying.
     */
    private boolean allowOtherEids() throws QueryException
    {
        Query q = null;
        try {
            q = this.qm.createQuery("select obj.allowOtherEids from Document doc, doc.object("
                                          + "PhenoTips.LabeledIdentifierGlobalSettings) obj", Query.XWQL);
            List<Integer> results = q.execute();
            if (results == null || results.isEmpty()) {
                this.logger.debug("There should be one LabeledIdentifierGlobalSettings object. None were found.");
                throw new MissingResourceException("Configuration object missing.",
                    "PhenoTips.LabeledIdentifierGlobalSettings", "allowOtherEids");
            }
            return results.get(0) != 0;
        } catch (QueryException ex) {
            this.logger.warn("Failed to query LabeledIdentifierGlobalSettings object: {}", ex.getMessage(), ex);
            throw new QueryException(ex.getMessage(), q, ex);
        }
    }

    /**
     * Returns true iff the internal ID is specified in {@code jsonInput} and does not correspond with the patient
     * internal ID, false otherwise.
     *
     * @param jsonInput the patient data being imported
     * @param patient the {@link Patient} object
     *
     * @return true iff the internal ID is specified in {@code jsonInput} and conflicts with patient internal ID, false
     *         otherwise
     */
    private boolean hasInternalIdConflict(final JSONObject jsonInput, final Patient patient)
    {
        final String idFromJson = jsonInput.optString("id");
        return StringUtils.isNotBlank(idFromJson) && !patient.getId().equals(idFromJson);
    }
}
