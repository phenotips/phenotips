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
import org.phenotips.data.rest.PatientsFetchResource;
import org.phenotips.entities.PrimaryEntity;

import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.rest.XWikiResource;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.ImmutableSet;

/**
 * Default implementation for {@link PatientsFetchResource} using XWiki's support for REST resources.
 *
 * @version $Id$
 * @since 1.3M5
 */
@Component
@Named("org.phenotips.data.rest.internal.DefaultPatientsFetchResourceImpl")
@Singleton
public class DefaultPatientsFetchResourceImpl extends XWikiResource implements PatientsFetchResource
{
    /** Jackson object mapper to facilitate array serialization. */
    private static final ObjectMapper OBJECT_MAPPER = getCustomObjectMapper();

    private static final String ORG_JSON_LABEL = "org.json";

    /** Logging helper object. */
    @Inject
    private Logger logger;

    @Inject
    private QueryManager qm;

    @Inject
    private PatientRepository repository;

    @Inject
    private AuthorizationManager access;

    @Inject
    private UserManager users;

    @Inject
    private Container container;

    @Override
    public Response fetchPatients()
    {
        final Request request = this.container.getRequest();
        // Get the internal and external IDs, if provided.
        final List<Object> eids = request.getProperties("eid");
        final List<Object> ids = request.getProperties("id");

        this.logger.debug("Retrieving patient records with external IDs [{}] and patient IDs [{}] via REST", eids, ids);

        // Build a set of patients from the provided external and/or internal ID data.
        final ImmutableSet.Builder<PrimaryEntity> patientsBuilder = ImmutableSet.builder();

        addEids(patientsBuilder, eids);
        addIds(patientsBuilder, ids);

        try {
            // Generate JSON for all retrieved patients.
            final String json = OBJECT_MAPPER.writeValueAsString(patientsBuilder.build());
            return Response.ok(json, MediaType.APPLICATION_JSON_TYPE).build();
        } catch (final JsonProcessingException ex) {
            logger.warn("Failed to serialize patients [{}] to JSON: {}", eids, ex.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Retrieves patient entities given a list of external patient IDs.
     *
     * @param patientsBuilder a patient entity set builder
     * @param eids a list of external patient IDs, as strings
     */
    private void addEids(@Nonnull final ImmutableSet.Builder<PrimaryEntity> patientsBuilder,
        @Nonnull final List<Object> eids)
    {
        for (final Object eid : eids) {
            if (StringUtils.isNotBlank((String) eid)) {
                collectPatientsFromEid(patientsBuilder, eid);
            }
        }
    }

    /**
     * Retrieves and collects patient entities that correspond to the provided external ID.
     *
     * @param patientsBuilder a patient entity set builder
     * @param eid an external patient ID, as string
     */
    private void collectPatientsFromEid(@Nonnull final ImmutableSet.Builder<PrimaryEntity> patientsBuilder,
        @Nonnull final Object eid)
    {
        try {
            final Query q = qm.createQuery("where doc.object(PhenoTips.PatientClass).external_id = :eid", Query.XWQL);
            q.bindValue("eid", eid);
            final List<Object> patientIds = q.execute();
            addIds(patientsBuilder, patientIds);
        } catch (final QueryException ex) {
            logger.warn("Failed to retrieve patient with external id [{}]: {}", eid, ex.getMessage());
        }
    }

    /**
     * Retrieves patient entities given a list of internal patient IDs.
     *
     * @param patientsBuilder a patient entity set builder
     * @param ids a list of patient ids, as strings
     */
    private void addIds(@Nonnull final ImmutableSet.Builder<PrimaryEntity> patientsBuilder,
        @Nonnull final List<Object> ids)
    {
        for (final Object id : ids) {
            if (StringUtils.isNotBlank((String) id)) {
                addPatientFromId(patientsBuilder, id);
            }
        }
    }

    /**
     * Given the patient's internal ID, retrieves the patient entity, if it exists and if the user has view rights, and
     * adds it to the set of patient entities.
     *
     * @param patientsBuilder a patient entity set builder
     * @param id an internal patient ID
     */
    private void addPatientFromId(@Nonnull final ImmutableSet.Builder<PrimaryEntity> patientsBuilder,
        @Nonnull final Object id)
    {
        try {
            // Try to get the patient entity.
            final PrimaryEntity patient = this.repository.get((String) id);
            // Get the current user.
            final User currentUser = this.users.getCurrentUser();
            // If user has view rights and patient with the provided ID exists, add patient to patient set.
            if (patient != null && this.access.hasAccess(Right.VIEW, currentUser == null ? null
                : currentUser.getProfileDocument(), patient.getDocument())) {
                patientsBuilder.add(patient);
            }
        } catch (final IllegalArgumentException ex) {
            logger.warn("Failed to retrieve patient with ID [{}]: {}", id, ex.getMessage());
        }
    }

    /**
     * A custom object mapper to facilitate serializing a list of {@link Patient} objects.
     *
     * @return an object mapper that can serialize {@link Patient} objects
     */
    private static ObjectMapper getCustomObjectMapper()
    {
        final ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule m = new SimpleModule(ORG_JSON_LABEL, new Version(1, 0, 0, "", ORG_JSON_LABEL, "json"));
        m.addSerializer(PrimaryEntity.class, new PrimaryEntitySerializer());
        objectMapper.registerModule(m);
        return objectMapper;
    }

    /**
     * A custom serializer for primary entities.
     */
    private static final class PrimaryEntitySerializer extends JsonSerializer<PrimaryEntity>
    {
        @Override
        public void serialize(final PrimaryEntity primaryEntity, final JsonGenerator jgen,
            final SerializerProvider provider) throws IOException
        {
            jgen.writeRawValue(primaryEntity.toJSON().toString());
        }
    }
}
