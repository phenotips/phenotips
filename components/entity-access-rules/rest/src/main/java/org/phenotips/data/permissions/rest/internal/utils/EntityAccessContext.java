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
package org.phenotips.data.permissions.rest.internal.utils;

import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.EntityAccess;
import org.phenotips.data.permissions.EntityPermissionsManager;
import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.PrimaryEntityManager;
import org.phenotips.entities.PrimaryEntityResolver;

import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default context that securely provides the current user, entity instance, and entity access. In case that the
 * current user does not have the minimum required rights, the context will fail to initialize.
 *
 * @version $Id$
 * @since 1.3M2
 * @since 1.4; under a new name
 */
public class EntityAccessContext
{
    private Logger logger = LoggerFactory.getLogger(EntityAccessContext.class);

    private PrimaryEntity entity;

    private User currentUser;

    private EntityAccess entityAccess;

    private EntityPermissionsManager manager;

    private DocumentReferenceResolver<String> userOrGroupResolver;

    /**
     * Initializes the context, making sure that the entity exists, and that the current user has sufficient rights. If
     * any of these conditions are not met, initialization fails.
     *
     * @param entityId by which to find an entity record
     * @param entityType the type of entity
     * @param minimumAccessLevel that the current must have or exceed
     * @param resolver used to find the entity record
     * @param users used to get the current user
     * @param manager used to initialize instance with access API
     * @param userOrGroupResolver document reference resolver that can resolve an identifier to either a user or a group
     * @throws WebApplicationException if the entity could not be found, or the current user has insufficient rights
     */
    public EntityAccessContext(String entityId, String entityType, AccessLevel minimumAccessLevel,
        PrimaryEntityResolver resolver, UserManager users, EntityPermissionsManager manager,
        DocumentReferenceResolver<String> userOrGroupResolver)
        throws WebApplicationException
    {
        this.manager = manager;
        final PrimaryEntityManager primaryEntityManager = resolver.getEntityManager(entityType);
        if (primaryEntityManager == null) {
            this.logger.debug("No such entity type: [{}]", entityType);
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        this.entity = primaryEntityManager.get(entityId);
        if (this.entity == null) {
            this.logger.debug("No such entity record: [{}]", entityId);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        this.userOrGroupResolver = userOrGroupResolver;
        this.entityAccess = this.manager.getEntityAccess(this.entity);
        this.initializeUser(minimumAccessLevel, users, this.logger);
    }

    private void initializeUser(AccessLevel minimumAccessLevel, UserManager users, Logger logger)
    {
        this.currentUser = users.getCurrentUser();
        if (!this.entityAccess.hasAccessLevel(this.currentUser == null ? null : this.currentUser.getProfileDocument(),
            minimumAccessLevel)) {
            logger.debug("{} access denied to user [{}] on entity record [{}]",
                minimumAccessLevel.getName(), this.currentUser, this.entity.getId());
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
    }

    /**
     * The main use of the context is this method.
     *
     * @return an entity that was either found by internal id, or was passed in during initialization
     */
    public PrimaryEntity getEntity()
    {
        return this.entity;
    }

    /**
     * This method is not used.
     *
     * @return the current user
     */
    public User getCurrentUser()
    {
        return this.currentUser;
    }

    /**
     * Allows for reuse of {@link EntityAccess} instance.
     *
     * @return an initialized instance of {@link EntityAccess}
     */
    public EntityAccess getEntityAccess()
    {
        return this.entityAccess;
    }

    /**
     * Check if the data provided for a collaborator is valid: does the collaborator exist and is a valid principal, and
     * is the specified access level valid?
     *
     * @param collaboratorId internal id of a principal, ideally fully qualified, (ex. {@code xwiki:XWiki.JohnDoe})
     * @param levelName identifier (level name) of an access level
     * @throws WebApplicationException if the data is invalid
     */
    public void checkCollaboratorInfo(String collaboratorId, String levelName) throws WebApplicationException
    {
        if (StringUtils.isBlank(collaboratorId)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                .entity("The collaborator id was not provided").build());
        }
        if (this.userOrGroupResolver.resolve(collaboratorId) == null) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                .entity("Unknown collaborator: " + collaboratorId).build());
        }
        if (StringUtils.isBlank(levelName)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                .entity("The collaborator's access level was not provided").build());
        }
        if (this.manager.resolveAccessLevel(levelName) == null) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                .entity("Invalid access level requested: " + levelName).build());
        }
    }
}
