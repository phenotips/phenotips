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
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.data.permissions.rest.DomainObjectFactory;
import org.phenotips.data.permissions.rest.PermissionsResource;
import org.phenotips.data.permissions.rest.VisibilityResource;
import org.phenotips.data.permissions.rest.internal.utils.LinkBuilder;
import org.phenotips.data.permissions.rest.internal.utils.PatientAccessContext;
import org.phenotips.data.permissions.rest.internal.utils.RESTActionResolver;
import org.phenotips.data.permissions.rest.internal.utils.SecureContextFactory;
import org.phenotips.data.permissions.rest.model.Link;
import org.phenotips.data.permissions.rest.model.VisibilityRepresentation;
import org.phenotips.data.rest.PatientResource;
import org.phenotips.data.rest.Relations;

import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.rest.XWikiResource;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import net.sf.json.JSONObject;

/**
 * Default implementation for {@link VisibilityResource} using XWiki's support for REST resources.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Component
@Named("org.phenotips.data.permissions.rest.internal.DefaultVisibilityResourceImpl")
@Singleton
public class DefaultVisibilityResourceImpl extends XWikiResource implements VisibilityResource
{
    @Inject
    private Logger logger;

    @Inject
    private SecureContextFactory secureContextFactory;

    @Inject
    private DomainObjectFactory factory;

    @Inject
    private PermissionsManager manager;

    @Inject
    private Container container;

    @Inject
    private RESTActionResolver restActionResolver;

    @Override
    public VisibilityRepresentation getVisibility(String patientId)
    {
        this.logger.debug("Retrieving patient record's visibility [{}] via REST", patientId);
        // besides getting the patient, checks that the user has view access
        PatientAccessContext patientAccessContext = this.secureContextFactory.getContext(patientId, "view");

        VisibilityRepresentation result =
            this.factory.createVisibilityRepresentation(patientAccessContext.getPatient());

        AccessLevel accessLevel = patientAccessContext.getPatientAccess().getAccessLevel();
        LinkBuilder linkBuilder = new LinkBuilder()
            .withActionResolver(this.restActionResolver)
            .withAccessLevel(accessLevel)
            .withUriInfo(this.uriInfo)
            .withRootInterface(this.getClass().getInterfaces()[0])
            .withTargetPatient(patientId)
            .withActionableResources(PermissionsResource.class);
        try {
            Collection<Link> links = linkBuilder.build();
            links.add(new Link().withRel(Relations.PATIENT_RECORD)
                .withHref(this.uriInfo.getBaseUriBuilder().path(PatientResource.class).build(patientId).toString()));
            result.withLinks(links);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public Response putVisibilityWithJson(String json, String patientId)
    {
        try {
            String visibility = JSONObject.fromObject(json).getString("level");
            return putVisibility(visibility, patientId);
        } catch (Exception ex) {
            this.logger.error("The json was not properly formatted", ex.getMessage());
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public Response putVisibilityWithForm(String patientId)
    {
        Object visibilityInRequest = this.container.getRequest().getProperty("visibility");
        if (visibilityInRequest instanceof String) {
            String visibility = visibilityInRequest.toString();
            if (StringUtils.isNotBlank(visibility)) {
                return putVisibility(visibility, patientId);
            }
        }
        this.logger.error("The visibility level was not provided or is invalid");
        throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    private Response putVisibility(String visibilityNameRaw, String patientId)
    {
        if (StringUtils.isBlank(visibilityNameRaw)) {
            this.logger.error("The visibility level was not provided");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        String visibilityName = visibilityNameRaw.trim();
        // checking that the visibility level is valid
        Visibility visibility = null;
        for (Visibility visibilityOption : this.manager.listVisibilityOptions()) {
            if (StringUtils.equalsIgnoreCase(visibilityOption.getName(), visibilityName)) {
                visibility = visibilityOption;
                break;
            }
        }
        if (visibility == null) {
            this.logger.error("The visibility level does not match any available levels");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        this.logger.debug(
            "Setting the visibility of the patient record [{}] to [{}] via REST", patientId, visibilityName);
        // besides getting the patient, checks that the user has manage access
        PatientAccessContext patientAccessContext = this.secureContextFactory.getContext(patientId, "manage");

        PatientAccess patientAccess = patientAccessContext.getPatientAccess();
        if (!patientAccess.setVisibility(visibility)) {
            // todo. should this status be an internal server error, or a bad request?
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }

        return Response.ok().build();
    }
}
