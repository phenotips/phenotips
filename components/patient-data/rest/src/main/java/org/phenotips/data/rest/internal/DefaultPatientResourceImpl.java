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
import org.phenotips.data.rest.Relations;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.rest.XWikiResource;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

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
    @Inject
    private Logger logger;

    @Inject
    private PatientRepository repository;

    @Inject
    private AuthorizationManager access;

    @Inject
    private UserManager users;

    /** Fills in missing reference fields with those from the current context document to create a full reference. */
    @Inject
    @Named("current")
    private EntityReferenceResolver<EntityReference> currentResolver;

    @Override
    public Response getPatient(String id)
    {
        this.logger.debug("Retrieving patient record [{}] via REST", id);
        Patient patient = this.repository.getPatientById(id);
        if (patient == null) {
            this.logger.debug("No such patient record: [{}]", id);
            return Response.status(Status.NOT_FOUND).build();
        }
        User currentUser = this.users.getCurrentUser();
        if (!this.access.hasAccess(Right.VIEW, currentUser == null ? null : currentUser.getProfileDocument(),
            patient.getDocument())) {
            this.logger.debug("View access denied to user [{}] on patient record [{}]", currentUser, id);
            return Response.status(Status.FORBIDDEN).build();
        }
        JSONObject json = patient.toJSON();
        JSONObject link = new JSONObject().accumulate("rel", Relations.SELF).accumulate("href",
            this.uriInfo.getRequestUri().toString());
        json.accumulate("links", link);
        return Response.ok(json, MediaType.APPLICATION_JSON_TYPE).build();
    }

    @Override
    public Response updatePatient(String json, String id)
    {
        this.logger.debug("Updating patient record [{}] via REST with JSON: {}", id, json);
        Patient patient = this.repository.getPatientById(id);
        if (patient == null) {
            this.logger.debug(
                "Patient record [{}] doesn't exist yet. It can be created by POST-ing the JSON to /rest/patients", id);
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        User currentUser = this.users.getCurrentUser();
        if (!this.access.hasAccess(Right.EDIT, currentUser == null ? null : currentUser.getProfileDocument(),
            patient.getDocument())) {
            this.logger.debug("Edit access denied to user [{}] on patient record [{}]", currentUser, id);
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
    public Response deletePatient(String id)
    {
        this.logger.debug("Deleting patient record [{}] via REST", id);
        Patient patient = this.repository.getPatientById(id);
        if (patient == null) {
            this.logger.debug("Patient record [{}] didn't exist", id);
            return Response.status(Status.NOT_FOUND).build();
        }
        User currentUser = this.users.getCurrentUser();
        if (!this.access.hasAccess(Right.DELETE, currentUser == null ? null : currentUser.getProfileDocument(),
            patient.getDocument())) {
            this.logger.debug("Delete access denied to user [{}] on patient record [{}]", currentUser, id);
            return Response.status(Status.FORBIDDEN).build();
        }
        XWikiContext context = this.getXWikiContext();
        XWiki xwiki = context.getWiki();
        try {
            xwiki.deleteDocument(xwiki.getDocument(patient.getDocument(), context), context);
        } catch (XWikiException ex) {
            this.logger.warn("Failed to delete patient record [{}]: {}", id, ex.getMessage());
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
        this.logger.debug("Deleted patient record [{}]", id);
        return Response.noContent().build();
    }
}
