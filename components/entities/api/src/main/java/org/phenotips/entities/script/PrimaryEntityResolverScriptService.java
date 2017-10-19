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
package org.phenotips.entities.script;

import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.PrimaryEntityResolver;
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.users.UserManager;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

/**
 * Allows users to resolve an entity based solely on its identifier.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("entityResolver")
@Singleton
public class PrimaryEntityResolverScriptService implements ScriptService
{
    /** The resolver that will do the actual work. */
    @Inject
    private PrimaryEntityResolver resolver;

    /** Used for obtaining the current user. */
    @Inject
    private UserManager userManager;

    /** Used for checking access rights. */
    @Inject
    private AuthorizationService access;

    /**
     * Retrieve an entity based on its identifier. For this to work correctly, the {@code entityId} must contain a
     * known entity prefix (e.g. FAM0000007).
     *
     * @param  entityId the entity identifier, in the format {@code <entity prefix><entity id>},
     *         for example {@code P0000007}
     * @return the requested entity, or {@code null} if the entity cannot be retrieved, or the user does not have the
     *         required access rights
     */
    @Nullable
    public PrimaryEntity resolve(@Nullable final String entityId)
    {
        if (StringUtils.isBlank(entityId)) {
            return null;
        }

        return null;
    }
}
