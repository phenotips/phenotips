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
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

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
            patient.getDocument())) {
            this.logger.debug("View access denied to user [{}] on patient record [{}]", currentUser, patient.getId());
            return Response.status(Status.FORBIDDEN).build();
        } else {
            grantedRight = Right.VIEW;
        }
        if (this.access.hasAccess(Right.EDIT, currentUser == null ? null : currentUser.getProfileDocument(),
            patient.getDocument())) {
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
            return checkForMultipleRecords(patient, eid);
        }
        User currentUser = this.users.getCurrentUser();
        if (!this.access.hasAccess(Right.EDIT, currentUser == null ? null : currentUser.getProfileDocument(),
            patient.getDocument())) {
            this.logger.debug("Edit access denied to user [{}] on patient record [{}]", currentUser, patient.getId());
            throw new WebApplicationException(Status.FORBIDDEN);
        }
        JSONObject jsonInput = new JSONObject(json);
        String idFromJson = jsonInput.optString("id");
        if (StringUtils.isNotBlank(idFromJson) && !patient.getId().equals(idFromJson)) {
            // JSON for a different patient, bail out
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
            patient.getDocument())) {
            this.logger.debug("Delete access denied to user [{}] on patient record [{}]", currentUser, patient.getId());
            return Response.status(Status.FORBIDDEN).build();
        }
        XWikiContext context = this.getXWikiContext();
        XWiki xwiki = context.getWiki();
        try {
            xwiki.deleteDocument(xwiki.getDocument(patient.getDocument(), context), context);
        } catch (XWikiException ex) {
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
}
