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
import org.phenotips.data.rest.PatientResource;
import org.phenotips.data.rest.PatientsFetchResource;
import org.phenotips.entities.PrimaryEntity;
import org.phenotips.rest.Autolinker;

import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.rest.XWikiResource;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
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
    private final ObjectMapper objectMapper = getCustomObjectMapper();

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** The query manager for patient retrieval. */
    @Inject
    private QueryManager qm;

    /** The secure patient repository. */
    @Inject
    @Named("secure")
    private PatientRepository repository;

    /** XWiki request container. */
    @Inject
    private Container container;

    @Inject
    private Provider<Autolinker> autolinker;

    @Override
    public Response fetchPatients()
    {
        final Request request = this.container.getRequest();
        // Get the internal and external IDs, if provided.
        final List<Object> eids = request.getProperties("eid");
        final List<Object> ids = request.getProperties("id");

        this.logger.debug("Retrieving patient records with external IDs [{}] and internal IDs [{}]", eids, ids);

        // Build a set of patients from the provided external and/or internal ID data.
        final ImmutableSet.Builder<PrimaryEntity> patientsBuilder = ImmutableSet.builder();

        try {
            addEids(patientsBuilder, eids);
            addIds(patientsBuilder, ids);
            // Generate JSON for all retrieved patients.
            final String json = objectMapper.writeValueAsString(patientsBuilder.build());
            return Response.ok(json, MediaType.APPLICATION_JSON_TYPE).build();
        } catch (final JsonProcessingException ex) {
            logger.error("Failed to serialize patients [{}] to JSON: {}", eids, ex.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } catch (final QueryException ex) {
            logger.error("Failed to retrieve patients with external ids [{}]: {}", eids, ex.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Retrieves patient entities given a list of external patient IDs.
     *
     * @param patientsBuilder a patient entity set builder
     * @param eids a list of external patient IDs, as strings
     * @throws QueryException if the query fails
     */
    private void addEids(@Nonnull final ImmutableSet.Builder<PrimaryEntity> patientsBuilder,
        @Nonnull final List<Object> eids) throws QueryException
    {
        if (CollectionUtils.isNotEmpty(eids)) {
            collectPatientsFromEids(patientsBuilder, eids);
        }
    }

    /**
     * Retrieves and collects patient entities that correspond to the provided external ID.
     *
     * @param patientsBuilder a patient entity set builder
     * @param eids external patient IDs, as a list
     * @throws QueryException if the query fails
     */
    private void collectPatientsFromEids(@Nonnull final ImmutableSet.Builder<PrimaryEntity> patientsBuilder,
        @Nonnull final List<Object> eids) throws QueryException
    {
        final Query q = qm.createQuery("from doc.object(PhenoTips.PatientClass) p where p.external_id in (:eids)",
            Query.XWQL);
        q.bindValue("eids", eids);
        final List<Object> patientIds = q.execute();
        addIds(patientsBuilder, patientIds);
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
            // If user has view rights and patient with the provided ID exists, add patient to patient set.
            if (patient != null) {
                patientsBuilder.add(patient);
            }
        } catch (final SecurityException ex) {
            logger.warn("Failed to retrieve patient with ID [{}]: {}", id, ex.getMessage());
        }
    }

    /**
     * A custom object mapper to facilitate serializing a list of {@link Patient} objects.
     *
     * @return an object mapper that can serialize {@link Patient} objects
     */
    private ObjectMapper getCustomObjectMapper()
    {
        final ObjectMapper mapper = new ObjectMapper();
        SimpleModule m = new SimpleModule("PrimaryEntitySerializer", new Version(1, 0, 0, "", "org.phenotips",
            "PatientReferenceSerializer"));
        m.addSerializer(PrimaryEntity.class, new PrimaryEntitySerializer());
        mapper.registerModule(m);
        return mapper;
    }

    /**
     * A custom serializer for primary entities.
     */
    private final class PrimaryEntitySerializer extends JsonSerializer<PrimaryEntity>
    {
        @Override
        public void serialize(final PrimaryEntity primaryEntity, final JsonGenerator jgen,
            final SerializerProvider provider) throws IOException
        {
            final JSONObject json = primaryEntity.toJSON();
            json.put("links", autolinker.get().forSecondaryResource(PatientResource.class, uriInfo)
                .withExtraParameters("patient-id", primaryEntity.getId()).build());
            jgen.writeRawValue(json.toString());
        }
    }
}
