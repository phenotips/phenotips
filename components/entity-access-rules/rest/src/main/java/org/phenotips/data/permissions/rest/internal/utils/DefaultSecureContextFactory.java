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
import org.phenotips.data.permissions.EntityPermissionsManager;
import org.phenotips.entities.PrimaryEntityResolver;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.users.UserManager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;

/**
 * Default implementation of {@link SecureContextFactory}. The purpose is to reduce the number of common injections
 * between default implementations of the REST resources.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Component
@Singleton
public class DefaultSecureContextFactory implements SecureContextFactory
{
    @Inject
    private PrimaryEntityResolver resolver;

    @Inject
    private UserManager users;

    @Inject
    @Named("secure")
    private EntityPermissionsManager entityPermissionsManager;

    @Inject
    @Named("userOrGroup")
    private DocumentReferenceResolver<String> userOrGroupResolver;

    @Override
    public EntityAccessContext getContext(String entityId, String entityType, String minimumAccessLevel)
        throws WebApplicationException
    {
        AccessLevel level = this.entityPermissionsManager.resolveAccessLevel(minimumAccessLevel);
        return new EntityAccessContext(entityId, level, this.resolver, this.users, this.entityPermissionsManager,
            this.userOrGroupResolver);
    }

    @Override
    public EntityAccessContext getReadContext(String entityId, String entityType) throws WebApplicationException
    {
        return getContext(entityId, entityType, "view");
    }

    @Override
    public EntityAccessContext getWriteContext(String entityId, String entityType) throws WebApplicationException
    {
        return getContext(entityId, entityType, "manage");
    }
}
