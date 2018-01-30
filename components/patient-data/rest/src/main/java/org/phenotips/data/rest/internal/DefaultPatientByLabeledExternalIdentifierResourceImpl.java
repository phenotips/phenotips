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
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.rest.XWikiResource;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
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

    @Override
    public Response getPatient(String label, String id)
    {
        this.logger.debug("Retrieving patient record with label [{}] and corresponding external ID [{}] via REST",
            label, id);
        Patient patient;
        List<String> patients = getPatientDocumentReferencesByLabelAndEid(label, id);
        if (patients.size() == 1) {
            patient = this.repository.get(patients.get(0));
        } else {
            return returnIfEmptyOrMultipleExistsResponse(patients, label, id);
        }

        User currentUser = this.users.getCurrentUser();
        Right grantedRight;
        if (!this.access.hasAccess(currentUser, Right.VIEW, patient.getDocumentReference())) {
            this.logger.debug("View access denied to user [{}] on patient record [{}]", currentUser, patient.getId());
            return Response.status(Response.Status.FORBIDDEN).build();
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
    public Response updatePatient(final String json, final String label, final String id, final String policy)
    {
        final PatientWritePolicy policyType = PatientWritePolicy.fromString(policy);
        return updatePatient(json, label, id, policyType);
    }

    private Response updatePatient(final String json, final String label, final String id,
        final PatientWritePolicy policy)
    {
        this.logger.debug("Updating patient record with label [{}] and corresponding external ID [{}] via REST: {}",
            label, id, json);
        if (policy == null) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        if (json == null) {
            // json == null does not create an exception when initializing a JSONObject
            // need to handle it separately to give explicit BAD_REQUEST to the user
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        Patient patient;
        List<String> patients = getPatientDocumentReferencesByLabelAndEid(label, id);
        if (patients.size() == 1) {
            patient = this.repository.get(patients.get(0));
        } else {
            return returnIfEmptyOrMultipleExistsResponse(patients, label, id);
        }

        User currentUser = this.users.getCurrentUser();
        if (!this.access.hasAccess(currentUser, Right.EDIT, patient.getDocumentReference())) {
            this.logger.debug("Edit access denied to user [{}] on patient record [{}]", currentUser, patient.getId());
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        JSONObject jsonInput;
        try {
            jsonInput = new JSONObject(json);
        } catch (final JSONException ex) {
            this.logger.error("Provided patient json: {} is invalid.", json, ex);
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        if (hasInternalIdConflict(jsonInput, patient)) {
            throw new WebApplicationException(Response.Status.CONFLICT);
        }

        try {
            patient.updateFromJSON(jsonInput, policy);
        } catch (Exception ex) {
            this.logger.warn("Failed to update patient [{}] from JSON: {}. Source JSON was: {}", patient.getId(),
                ex.getMessage(), json, ex);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        return Response.noContent().build();
    }

    @Override
    public Response patchPatient(String json, String label, String id)
    {
        return updatePatient(json, label, id, PatientWritePolicy.MERGE);
    }

    @Override
    public Response deletePatient(String label, String id)
    {
        this.logger.debug("Deleting patient record with label [{}] and corresponding external ID [{}] via REST",
            label, id);
        Patient patient;
        List<String> patients = getPatientDocumentReferencesByLabelAndEid(label, id);
        if (patients.size() == 1) {
            patient = this.repository.get(patients.get(0));
        } else {
            return returnIfEmptyOrMultipleExistsResponse(patients, label, id);
        }

        User currentUser = this.users.getCurrentUser();
        if (!this.access.hasAccess(currentUser, Right.DELETE, patient.getDocumentReference())) {
            this.logger.debug("Delete access denied to user [{}] on patient record [{}]", currentUser, patient.getId());
            return Response.status(Response.Status.FORBIDDEN).build();
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

    private Response returnIfEmptyOrMultipleExistsResponse(List<String> patients, String label, String id)
    {
        if (patients.isEmpty()) {
            this.logger.debug("No patient record with label [{}] and corresponding external ID [{}] exists yet",
                label, id);
            return Response.status(Response.Status.NOT_FOUND).build();

        } else {
            this.logger.debug("Multiple patient records ({}) with label [{}] and corresponding external ID [{}]: {}",
                patients.size(), label, id, patients);
            return Response.status(300).entity(this.factory.createAlternatives(patients, this.uriInfo)).build();
        }
    }

    /**
     * Gets a list of patient document references.
     *
     * @param label the label for the eid
     * @param id the value of the eid
     * @return null if the label is invalid, an empty list if no patients have the label identifier and its
     *         corresponding id value, else a list of patient document references.
     */
    private List<String> getPatientDocumentReferencesByLabelAndEid(String label, String id)
    {
        try {
            // Check global configs first before querying all labeled identifier objects on all patients, much cheaper
            if (isLabelConfiguredByAdmin(label) || allowOtherEids()) {
                return queryPatientsByLabelAndEid(label, id);
            } else {
                return Collections.emptyList();
            }
        } catch (Exception ex) {
            this.logger.error("Failed to retrieve patient with label [{}] and corresponding external ID [{}]: {}",
                label, id, ex.getMessage(), ex);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * For a given label and its corresponding id value (fields on {@code LabeledIdentifierClass} objects), query
     * patient documents.
     *
     * @param label the label for the eid
     * @param id the value of the eid
     * @return an empty list if no patients have the label identifier and its corresponding id value, else a list of
     *         patient document references.
     * @throws Exception if there is any error during querying.
     */
    private List<String> queryPatientsByLabelAndEid(String label, String id) throws Exception
    {
        Query q = null;
        try {
            q = this.qm.createQuery(
                "select doc.name from Document doc, doc.object(PhenoTips.LabeledIdentifierClass) obj "
                + "where obj.label = :label and obj.value = :value", Query.XWQL);
            q.bindValue("label", label);
            q.bindValue("value", id);
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
     * @param label the label to check
     * @return true if the label exists, else false.
     * @throws Exception if there is any error during querying.
     */
    private boolean isLabelConfiguredByAdmin(String label) throws Exception
    {
        Query q = null;
        try {
            q = this.qm.createQuery(
                "select obj.label from Document doc, doc.object(PhenoTips.LabeledIdentifierSettings) obj "
                + "where obj.label = :label", Query.XWQL);
            q.bindValue("label", label);
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
     * @throws Exception if an instance of the {@code LabeledIdentifierGlobalSettings} object cannot be found,
     *         or if there is any other error during querying.
     */
    private boolean allowOtherEids() throws Exception
    {
        Query q = null;
        try {
            q = this.qm.createQuery("select obj.allowOtherEids from Document doc, doc.object("
                                          + "PhenoTips.LabeledIdentifierGlobalSettings) obj", Query.XWQL);
            List<Integer> results = q.execute();
            if (results == null || results.isEmpty()) {
                this.logger.debug("There should be one LabeledIdentifierGlobalSettings object. None were found.");
                throw new Exception("LabeledIdentifierGlobalSettings object not found.");
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
