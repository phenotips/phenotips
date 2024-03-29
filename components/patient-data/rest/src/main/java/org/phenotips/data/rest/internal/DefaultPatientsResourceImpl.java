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
import org.phenotips.data.rest.PatientResource;
import org.phenotips.data.rest.PatientsResource;
import org.phenotips.data.rest.model.PatientSummary;
import org.phenotips.data.rest.model.Patients;
import org.phenotips.rest.Autolinker;
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.query.Query;
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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Default implementation for {@link PatientsResource} using XWiki's support for REST resources.
 *
 * @version $Id$
 * @since 1.2M5
 */
@Component
@Named("org.phenotips.data.rest.internal.DefaultPatientsResourceImpl")
@Singleton
public class DefaultPatientsResourceImpl extends XWikiResource implements PatientsResource
{
    @Inject
    private PatientRepository repository;

    @Inject
    private QueryManager queries;

    @Inject
    private AuthorizationService access;

    @Inject
    private UserManager users;

    /** Fills in missing reference fields with those from the current context document to create a full reference. */
    @Inject
    @Named("current")
    private EntityReferenceResolver<EntityReference> currentResolver;

    @Inject
    private DomainObjectFactory factory;

    @Inject
    private Provider<Autolinker> autolinker;

    @Override
    public Response add(final String json)
    {
        this.slf4Jlogger.debug("Importing new patient from JSON via REST: {}", json);

        final User currentUser = this.users.getCurrentUser();
        if (!this.access.hasAccess(currentUser, Right.EDIT,
            this.currentResolver.resolve(Patient.DEFAULT_DATA_SPACE, EntityType.SPACE))) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }

        try {
            final Response response;
            if (json != null) {
                response = json.startsWith("[") ? addPatients(json) : addPatient(json);
            } else {
                response = buildCreatedResponse(this.repository.create());
            }
            return response;
        } catch (Exception ex) {
            this.slf4Jlogger.error("Could not process patient creation request: {}", ex.getMessage(), ex);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Import new patients from their representation as a JSON array.
     *
     * @param json the JSON representation of the new patients to be created
     * @return a response containing locations of the newly created patients in its body, if successful
     * @throws WebApplicationException if a {@link JSONArray} object cannot be created or one of the patient objects is
     *             null
     * @throws NullPointerException if the patient was not created
     */
    private Response addPatients(final String json)
    {
        final JSONArray patientsData;
        final JSONArray createdPatientUri = new JSONArray();
        try {
            patientsData = new JSONArray(json);
        } catch (JSONException ex) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        final int jsonArrayLength = patientsData.length();
        for (int i = 0; i < jsonArrayLength; i++) {
            JSONObject jsonObject = patientsData.optJSONObject(i);
            if (jsonObject == null) {
                this.slf4Jlogger.warn("One of the members of the patient JSONArray is null.");
                continue;
            }
            Patient patient = this.repository.create();
            patient.updateFromJSON(jsonObject);
            createdPatientUri.put(UriBuilder.fromUri(this.uriInfo.getBaseUri())
                .path(PatientResource.class)
                .build(patient.getId()));
        }
        final ResponseBuilder response = Response.created(null);
        response.entity(createdPatientUri.toString());
        return response.build();
    }

    /**
     * Import a new patient from its JSON representation.
     *
     * @param json the JSON representation of the new patient
     * @return the location of the newly created patient, if successful
     * @throws WebApplicationException if a {@link JSONObject} cannot be created
     * @throws NullPointerException if the patient was not created
     */
    private Response addPatient(final String json)
    {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(json);
        } catch (Exception ex) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        final Patient patient = this.repository.create();
        patient.updateFromJSON(jsonObject);
        return buildCreatedResponse(patient);
    }

    /**
     * Creates a response upon successful creation of a {@link Patient}.
     *
     * @param patient the successfully created patient
     * @return the response for a successfully created patient
     */
    private Response buildCreatedResponse(final Patient patient)
    {
        final ResponseBuilder response = Response.created(UriBuilder
            .fromUri(this.uriInfo.getBaseUri())
            .path(PatientResource.class)
            .build(patient.getId()));
        return response.build();
    }

    @Override
    public Patients listPatients(Integer start, Integer number, String orderField, String order)
    {
        Patients result = new Patients();
        try {
            String safeOrderField = "doc.name";
            if ("eid".equals(orderField)) {
                safeOrderField = "p.external_id";
            }
            String safeOrder = " asc";
            if ("desc".equals(order)) {
                safeOrder = " desc";
            }
            Query query = this.queries.createQuery(
                "select doc.fullName, p.external_id, doc.creator, doc.creationDate, doc.version, doc.author, doc.date"
                    + " from Document doc, doc.object(PhenoTips.PatientClass) p where doc.name <> :t order by "
                    + safeOrderField + safeOrder,
                "xwql");
            query.bindValue("t", "PatientTemplate");

            List<Object[]> records = query.execute();
            int skipped = 0;
            for (Object[] record : records) {
                PatientSummary summary = this.factory.createPatientSummary(record, this.uriInfo);
                // Since raw queries can't take into account access rights, we must do our own paging with rights checks
                if (summary != null) {
                    if (++skipped > start) {
                        result.getPatientSummaries().add(summary);
                    }
                    if (result.getPatientSummaries().size() >= number) {
                        break;
                    }
                }
            }
            result.withLinks(this.autolinker.get().forResource(getClass(), this.uriInfo)
                .withGrantedRight(getGrantedRight()).build());
        } catch (Exception ex) {
            this.slf4Jlogger.error("Failed to list patients: {}", ex.getMessage(), ex);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }

        return result;
    }

    private Right getGrantedRight()
    {
        User currentUser = this.users.getCurrentUser();
        EntityReference dataSpace = this.currentResolver.resolve(Patient.DEFAULT_DATA_SPACE, EntityType.SPACE);
        Right grantedRight = Right.ILLEGAL;
        if (this.access.hasAccess(currentUser, Right.EDIT, dataSpace)) {
            grantedRight = Right.EDIT;
        } else if (this.access.hasAccess(currentUser, Right.VIEW, dataSpace)) {
            grantedRight = Right.VIEW;
        }
        return grantedRight;
    }
}
