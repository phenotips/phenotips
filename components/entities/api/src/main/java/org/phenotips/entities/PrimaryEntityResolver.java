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
package org.phenotips.entities;

import org.xwiki.component.annotation.Role;

import javax.annotation.Nullable;

/**
 * Provides access to the available {@link PrimaryEntityManager} implementations and their entities.
 *
 * @version $Id$
 * @since 1.4
 */
@Role
public interface PrimaryEntityResolver
{
    /**
     * Retrieve an entity from its {@link PrimaryEntityManager}. For this to work correctly, the {@code entityId} must
     * contain an known entity prefix (e.g. FAM0000007).
     *
     * @param  entityId the entity identifier, in the format {@code <entity prefix><entity id>},
     *         for example {@code P0000007}
     * @return the requested entity, or {@code null} if the {@link PrimaryEntityManager} cannot retrieve the entity or
     *         no matching {@link PrimaryEntityManager} is available
     */
    @Nullable
    PrimaryEntity resolveEntity(@Nullable String entityId);

    /**
     * Retrieves the {@link PrimaryEntityManager} that is associated with the provided {@code entityType}.
     *
     * @param entityType the entity type as string, for example {@code patients}
     * @return the {@link PrimaryEntityManager} associated with {@code entityType}, or {@code null} if no matching
     *         {@link PrimaryEntityManager} is available
     */
    @Nullable
    PrimaryEntityManager getEntityManager(@Nullable String entityType);

    /**
     * Returns true iff a {@link PrimaryEntityManager} exists for the provided {@code entityType}.
     *
     * @param entityType the entity type as string, for example {@code patients}
     * @return true iff a {@link PrimaryEntityManager} exists for the provided {@code entityType}
     */
    boolean hasEntityManager(@Nullable String entityType);
}
