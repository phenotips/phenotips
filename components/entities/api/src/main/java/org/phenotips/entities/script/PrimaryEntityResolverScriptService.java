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
import org.phenotips.entities.PrimaryEntityManager;
import org.phenotips.entities.PrimaryEntityResolver;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

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
    @Named("secure")
    private PrimaryEntityResolver resolver;

    @Inject
    private Logger logger;

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
        try {
            // Blank checks performed by resolver.
            return this.resolver.resolveEntity(entityId);
        } catch (final SecurityException e) {
            this.logger.error("Unauthorized access for [{}]", entityId);
            return null;
        }
    }

    /**
     * Retrieves the first {@link PrimaryEntityManager} that is associated with the provided {@code entityType}.
     *
     * @param entityType the entity type as string, for example {@code patients}
     * @return the {@link PrimaryEntityManager} associated with {@code entityType}, or {@code null} if no matching
     *         {@link PrimaryEntityManager} is available
     */
    @Nullable
    public PrimaryEntityManager getEntityManager(@Nullable final String entityType)
    {
        return this.resolver.getEntityManager(entityType);
    }

    /**
     * Returns true iff a {@link PrimaryEntityManager} exists for the provided {@code entityType}.
     *
     * @param entityType the entity type as string, for example {@code patients}
     * @return true iff a {@link PrimaryEntityManager} exists for the provided {@code entityType}
     */
    public boolean hasEntityManager(@Nullable final String entityType)
    {
        return this.resolver.hasEntityManager(entityType);
    }
}
