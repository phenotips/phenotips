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
import org.phenotips.data.permissions.rest.VisibilityResource;
import org.phenotips.data.permissions.rest.internal.utils.PatientAccessContext;
import org.phenotips.data.permissions.rest.internal.utils.SecureContextFactory;
import org.phenotips.data.permissions.rest.model.VisibilityRepresentation;
import org.phenotips.data.rest.PatientResource;
import org.phenotips.rest.Autolinker;

import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.rest.XWikiResource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

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
    private Provider<Autolinker> autolinker;

    @Override
    public VisibilityRepresentation getVisibility(String patientId)
    {
        this.logger.debug("Retrieving patient record's visibility [{}] via REST", patientId);
        // besides getting the patient, checks that the user has view access
        PatientAccessContext patientAccessContext = this.secureContextFactory.getReadContext(patientId);

        VisibilityRepresentation result =
            this.factory.createVisibilityRepresentation(patientAccessContext.getPatient());

        AccessLevel accessLevel = patientAccessContext.getPatientAccess().getAccessLevel();
        result.withLinks(this.autolinker.get().forResource(getClass(), this.uriInfo)
            .withActionableResources(PatientResource.class)
            .build());
        return result;
    }

    @Override
    public Response setVisibility(VisibilityRepresentation visibility, String patientId)
    {
        try {
            String level = visibility.getLevel();
            return setVisibility(level, patientId);
        } catch (Exception ex) {
            this.logger.error("The json was not properly formatted", ex.getMessage());
            return Response.status(Response.Status.BAD_REQUEST.getStatusCode())
                .type(MediaType.TEXT_PLAIN_TYPE)
                .entity("Unknown visibility level: " + visibility.getLevel()).build();
        }
    }

    @Override
    public Response setVisibility(String patientId)
    {
        String visibility = (String) this.container.getRequest().getProperty("visibility");
        if (StringUtils.isNotBlank(visibility)) {
            return setVisibility(visibility, patientId);
        }
        this.logger.error("The visibility level was not provided or is invalid");
        throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    private Response setVisibility(String visibilityNameRaw, String patientId)
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
        PatientAccessContext patientAccessContext = this.secureContextFactory.getWriteContext(patientId);

        PatientAccess patientAccess = patientAccessContext.getPatientAccess();
        if (!patientAccess.setVisibility(visibility)) {
            // todo. should this status be an internal server error, or a bad request?
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }

        return Response.ok().build();
    }
}
