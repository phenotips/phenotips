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

import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.rest.CollaboratorsResource;
import org.phenotips.data.permissions.rest.DomainObjectFactory;
import org.phenotips.data.permissions.rest.OwnerResource;
import org.phenotips.data.permissions.rest.PermissionsResource;
import org.phenotips.data.permissions.rest.VisibilityResource;
import org.phenotips.data.permissions.rest.internal.utils.LinkBuilder;
import org.phenotips.data.permissions.rest.internal.utils.PatientAccessContext;
import org.phenotips.data.permissions.rest.internal.utils.RESTActionResolver;
import org.phenotips.data.permissions.rest.internal.utils.SecureContextFactory;
import org.phenotips.data.permissions.rest.model.CollaboratorsRepresentation;
import org.phenotips.data.permissions.rest.model.Link;
import org.phenotips.data.permissions.rest.model.OwnerRepresentation;
import org.phenotips.data.permissions.rest.model.PermissionsRepresentation;
import org.phenotips.data.permissions.rest.model.VisibilityRepresentation;
import org.phenotips.data.rest.PatientResource;
import org.phenotips.data.rest.Relations;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * @version $Id$
 * @since 1.3M2
 */
@Component
@Named("org.phenotips.data.permissions.rest.internal.DefaultPermissionsResourceImpl")
@Singleton
public class DefaultPermissionsResourceImpl extends XWikiResource implements PermissionsResource
{
    @Inject
    private Logger logger;

    @Inject
    private SecureContextFactory secureContextFactory;

    @Inject
    private DomainObjectFactory factory;

    @Inject
    private RESTActionResolver restActionResolver;

    @Inject
    @Named("org.phenotips.data.permissions.rest.internal.DefaultOwnerResourceImpl")
    private OwnerResource ownerResource;

    @Inject
    @Named("org.phenotips.data.permissions.rest.internal.DefaultVisibilityResourceImpl")
    private VisibilityResource visibilityResource;

    @Inject
    @Named("org.phenotips.data.permissions.rest.internal.DefaultCollaboratorsResourceImpl")
    private CollaboratorsResource collaboratorsResource;

    @Override
    public PermissionsRepresentation getPermissions(String patientId)
    {
        this.logger.debug("Retrieving patient record [{}] via REST", patientId);
        // besides getting the patient, checks that the user has view access
        PatientAccessContext patientAccessContext = this.secureContextFactory.getContext(patientId, "view");

        PermissionsRepresentation result = new PermissionsRepresentation();
        OwnerRepresentation owner = this.factory.createOwnerRepresentation(patientAccessContext.getPatient());
        VisibilityRepresentation visibility =
            this.factory.createVisibilityRepresentation(patientAccessContext.getPatient());
        CollaboratorsRepresentation collaborators =
            this.factory.createCollaboratorsRepresentation(patientAccessContext.getPatient(), this.uriInfo);

        AccessLevel accessLevel = patientAccessContext.getPatientAccess().getAccessLevel();
        // adding links into sub-parts
        owner.withLinks(new LinkBuilder(this.uriInfo, this.restActionResolver)
            .withAccessLevel(accessLevel)
            .withActionableResources(OwnerResource.class)
            .withTargetPatient(patientId)
            .build());
        visibility.withLinks(new LinkBuilder(this.uriInfo, this.restActionResolver)
            .withAccessLevel(accessLevel)
            .withActionableResources(VisibilityResource.class)
            .withTargetPatient(patientId)
            .build());
        collaborators.withLinks(new LinkBuilder(this.uriInfo, this.restActionResolver)
            .withAccessLevel(accessLevel)
            .withActionableResources(CollaboratorsResource.class)
            .withTargetPatient(patientId)
            .build());

        result.withOwner(owner);
        result.withVisibility(visibility);
        result.withCollaborators(collaborators);

        // adding links relative to this context
        result.withLinks(new LinkBuilder(this.uriInfo, this.restActionResolver)
            .withAccessLevel(accessLevel)
            .withRootInterface(PermissionsResource.class)
            .withTargetPatient(patientId)
            .build());
        result.getLinks().add(new Link().withRel(Relations.PATIENT_RECORD)
            .withHref(this.uriInfo.getBaseUriBuilder().path(PatientResource.class).build(patientId).toString()));

        return result;
    }

    @Override
    public Response putPermissions(String json, String patientId)
    {
        this.logger.debug("Setting permissions of patient record [{}] via REST", patientId);
        // no permissions checks here, since this method is just a recombination of existing endpoints

        String ownerJsonStr;
        String visibilityJsonStr;
        String collaboratorsJsonStr;
        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONObject owner = jsonObject.getJSONObject("owner");
            JSONObject visibility = jsonObject.getJSONObject("visibility");
            JSONArray collaborators = jsonObject.getJSONArray("collaborators");

            ownerJsonStr = owner.toString();
            visibilityJsonStr = visibility.toString();
            collaboratorsJsonStr = collaborators.toString();
        } catch (Exception ex) {
            this.logger.error("JSON was not properly formatted");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        this.ownerResource.putOwnerWithJson(ownerJsonStr, patientId);
        this.visibilityResource.putVisibilityWithJson(visibilityJsonStr, patientId);
        this.collaboratorsResource.putCollaborators(collaboratorsJsonStr, patientId);

        return Response.ok().build();
    }
}
