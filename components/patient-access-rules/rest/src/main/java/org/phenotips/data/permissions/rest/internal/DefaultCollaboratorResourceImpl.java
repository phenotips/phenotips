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
package org.phenotips.data.permissions.rest.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.data.permissions.rest.CollaboratorResource;
import org.phenotips.data.permissions.rest.CollaboratorsResource;
import org.phenotips.data.permissions.rest.DomainObjectFactory;
import org.phenotips.data.permissions.rest.PermissionsResource;
import org.phenotips.data.permissions.rest.Relations;
import org.phenotips.data.permissions.rest.internal.utils.PatientUserContext;
import org.phenotips.data.permissions.rest.internal.utils.SecureContextFactory;
import org.phenotips.data.permissions.script.SecurePatientAccess;
import org.phenotips.data.rest.PatientResource;
import org.phenotips.data.rest.model.Link;
import org.phenotips.data.rest.model.PhenotipsUser;

import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.rest.XWikiResource;
import org.xwiki.security.authorization.Right;
import org.xwiki.text.StringUtils;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;

import net.sf.json.JSONObject;

/**
 * Default implementation for {@link CollaboratorResource} using XWiki's support for REST resources.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Component
@Named("org.phenotips.data.permissions.rest.internal.DefaultCollaboratorResourceImpl")
@Singleton
public class DefaultCollaboratorResourceImpl extends XWikiResource implements CollaboratorResource
{
    private static final String LEVEL = "level";

    private static final EntityReference XWIKI_SPACE = new EntityReference("XWiki", EntityType.SPACE);

    @Inject
    private Logger logger;

    @Inject
    private SecureContextFactory secureContextFactory;

    /** Fills in missing reference fields with those from the current context document to create a full reference. */
    @Inject
    @Named("current")
    private EntityReferenceResolver<String> currentResolver;

    @Inject
    private DomainObjectFactory factory;

    @Inject
    private PermissionsManager manager;

    @Inject
    private Container container;

    @Override
    public PhenotipsUser getCollaborator(String patientId, String collaboratorId)
    {
        this.logger.debug(
            "Retrieving collaborator with id [{}] of patient record [{}] via REST", collaboratorId, patientId);
        // besides getting the patient, checks that the user has view access
        PatientUserContext patientUserContext = this.secureContextFactory.getContext(patientId, Right.VIEW);

        try {
            PhenotipsUser result = this.createCollaborator(patientUserContext.getPatient(), collaboratorId.trim());

            // adding links relative to this context
            result.getLinks().add(new Link().withRel(Relations.SELF).withHref(this.uriInfo.getRequestUri().toString()));
            result.getLinks().add(new Link().withRel(Relations.PATIENT_RECORD)
                .withHref(this.uriInfo.getBaseUriBuilder().path(PatientResource.class).build(patientId).toString()));
            result.getLinks().add(new Link().withRel(Relations.COLLABORATORS).withHref(
                this.uriInfo.getBaseUriBuilder().path(CollaboratorsResource.class).build(patientId).toString()));
            result.getLinks().add(new Link().withRel(Relations.PERMISSIONS).withHref(
                this.uriInfo.getBaseUriBuilder().path(PermissionsResource.class).build(patientId).toString()));

            return result;
        } catch (Exception ex) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Response putLevelWithJson(String json, String patientId, String collaboratorId)
    {
        String level;
        try {
            JSONObject jsonObject = JSONObject.fromObject(json);
            level = jsonObject.getString(LEVEL);
        } catch (Exception ex) {
            this.logger.debug("Changing collaborator's access level failed: the JSON was not properly formatted");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        return putLevel(collaboratorId.trim(), level, patientId);
    }

    @Override
    public Response putLevelWithForm(String patientId, String collaboratorId)
    {
        Object levelInRequest = container.getRequest().getProperty(LEVEL);
        if (levelInRequest instanceof String) {
            String level = levelInRequest.toString().trim();
            if (StringUtils.isNotBlank(level)) {
                return putLevel(collaboratorId, level, patientId);
            }
        }
        this.logger.error("The id, permissions level, or both were not provided or are invalid");
        throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    @Override
    public Response deleteCollaborator(String patientId, String collaboratorId)
    {
        this.logger.debug(
            "Removing collaborator with id [{}] from patient record [{}] via REST", collaboratorId, patientId);
        // besides getting the patient, checks that the user has edit access
        PatientUserContext patientUserContext = this.secureContextFactory.getContext(patientId, Right.EDIT);

        PatientAccess patientAccess =
            new SecurePatientAccess(this.manager.getPatientAccess(patientUserContext.getPatient()), this.manager);
        EntityReference collaboratorReference =
            this.currentResolver.resolve(collaboratorId, EntityType.DOCUMENT, XWIKI_SPACE);

        if (!patientAccess.removeCollaborator(collaboratorReference)) {
            this.logger.error("Could not remove collaborator [{}] from patient record [{}]", collaboratorId, patientId);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }

        return Response.noContent().build();
    }

    private PhenotipsUser createCollaborator(Patient patient, String id) throws Exception
    {
        String collaboratorId = id.trim();
        // check if the space reference is used more than once in this class
        EntityReference collaboratorReference =
            this.currentResolver.resolve(collaboratorId, EntityType.DOCUMENT, XWIKI_SPACE);
        PatientAccess patientAccess = new SecurePatientAccess(this.manager.getPatientAccess(patient), this.manager);
        for (Collaborator collaborator : patientAccess.getCollaborators()) {
            if (collaboratorReference.equals(collaborator.getUser())) {
                return this.factory.createCollaborator(patient, collaborator);
            }
        }
        throw new Exception(String.format(
            "Collaborator of patient record [%s] with id [%s] was not found", patient.getId(), collaboratorId));
    }

    private Response putLevel(String collaboratorId, String accessLevelName, String patientId)
    {
        throw new NotImplementedException();
    }
}
