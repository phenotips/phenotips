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
package org.phenotips.entities.configuration;

import org.phenotips.entities.PrimaryEntity;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import javax.annotation.Nonnull;

/**
 * Provides access to {@link PrimaryEntityConfiguration primary entity configurations}.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable
@Role
public interface PrimaryEntityConfigurationManager
{
    /**
     * Retrieves the {@link PrimaryEntityConfiguration} active for the current user on a specific record type.
     *
     * @param entityType an identifier for the type of record whose configuration is requested, such as {@code patient}
     *            or {@code family}
     * @return a valid configuration
     */
    @Nonnull
    PrimaryEntityConfiguration getConfiguration(@Nonnull String entityType);

    /**
     * Retrieves the {@link PrimaryEntityConfiguration} active for the current user on a specific primary entity record.
     *
     * @param entity the primary entity whose configuration is requested
     * @return a valid configuration
     */
    @Nonnull
    PrimaryEntityConfiguration getConfiguration(@Nonnull PrimaryEntity entity);
}
