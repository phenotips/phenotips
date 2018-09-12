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

import org.phenotips.data.permissions.EntityAccess;
import org.phenotips.data.permissions.EntityPermissionsManager;
import org.phenotips.data.permissions.events.EntityRightsUpdatedEvent.RightsUpdateEventType;
import org.phenotips.data.permissions.rest.DomainObjectFactory;
import org.phenotips.data.permissions.rest.OwnerResource;
import org.phenotips.data.permissions.rest.internal.utils.EntityAccessContext;
import org.phenotips.data.permissions.rest.internal.utils.SecureContextFactory;
import org.phenotips.data.permissions.rest.model.OwnerRepresentation;
import org.phenotips.rest.Autolinker;

import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rest.XWikiResource;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

/**
 * Default implementation for {@link OwnerResource} using XWiki's support for REST resources.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Component
@Named("org.phenotips.data.permissions.rest.internal.DefaultOwnerResourceImpl")
@Singleton
public class DefaultOwnerResourceImpl extends XWikiResource implements OwnerResource
{
    @Inject
    private Logger logger;

    @Inject
    private SecureContextFactory secureContextFactory;

    @Inject
    @Named("userOrGroup")
    private DocumentReferenceResolver<String> userOrGroupResolver;

    @Inject
    private DomainObjectFactory factory;

    @Inject
    private Container container;

    @Inject
    private Provider<Autolinker> autolinker;

    @Inject
    private EntityPermissionsManager manager;

    @Override
    public OwnerRepresentation getOwner(String entityId, String entityType)
    {
        this.logger.debug("Retrieving entity record's owner [{}] via REST", entityId);
        // besides getting the entity, checks that the user has view access
        EntityAccessContext entityAccessContext = this.secureContextFactory.getReadContext(entityId, entityType);

        OwnerRepresentation result = this.factory.createOwnerRepresentation(entityAccessContext.getEntity());

        // adding links relative to this context
        result.withLinks(this.autolinker.get().forResource(getClass(), this.uriInfo)
            .withGrantedRight(entityAccessContext.getEntityAccess().getAccessLevel().getGrantedRight())
            .build());

        return result;
    }

    @Override
    public Response setOwner(OwnerRepresentation owner, String entityId, String entityType)
    {
        try {
            return putOwner(owner.getId(), entityId, entityType);
        } catch (Exception ex) {
            this.logger.error("The json was not properly formatted", ex.getMessage());
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
    }

    @Override
    public Response setOwner(String entityId, String entityType)
    {
        String ownerId = (String) this.container.getRequest().getProperty("owner");
        if (StringUtils.isNotBlank(ownerId)) {
            return putOwner(ownerId, entityId, entityType);
        }
        this.logger.error("The owner id was not provided or is invalid");
        throw new WebApplicationException(Status.BAD_REQUEST);
    }

    private Response putOwner(String ownerId, String entityId, String entityType)
    {
        if (StringUtils.isBlank(ownerId)) {
            this.logger.error("The owner id was not provided");
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        this.logger.debug("Setting owner of the entity record [{}] to [{}] via REST", entityId, ownerId);
        // besides getting the entity, checks that the current user has manage access
        EntityAccessContext entityAccessContext = this.secureContextFactory.getWriteContext(entityId, entityType);

        EntityReference ownerReference = this.userOrGroupResolver.resolve(ownerId);
        if (ownerReference == null) {
            // what would be a better status to indicate that the user/group id is not valid?
            // ideally, the status page should show some sort of a message indicating that the id was not found
            throw new WebApplicationException(
                new IllegalArgumentException("Specified user/group was not found"), Status.NOT_FOUND);
        }
        // todo. ask Sergiu as to what the right thing to do is
        // the code in DefaultPatientAccessHelper needs to be changed
        // this is just a hack
        // the helper in EntityAccess needs to use this.entitySerializer.serialize
        DocumentReference ownerDocRef = new DocumentReference(ownerReference);

        EntityAccess entityAccess = entityAccessContext.getEntityAccess();
        if (!entityAccess.setOwner(ownerDocRef)) {
            // todo. should this status be an internal server error, or a bad request?
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }

        this.manager.fireRightsUpdateEvent(Arrays.asList(RightsUpdateEventType.ENTITY_OWNER_UPDATED), entityId);
        return Response.ok().build();
    }
}
