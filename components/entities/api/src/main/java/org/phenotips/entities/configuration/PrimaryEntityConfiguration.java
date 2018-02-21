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

import org.phenotips.Constants;
import org.phenotips.entities.PrimaryEntity;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

import javax.annotation.Nonnull;

/**
 * Exposes the configuration for primary entity records.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable
public interface PrimaryEntityConfiguration
{
    /** The XClass used for storing the default configuration. */
    EntityReference CONFIGURATION_CLASS =
        new EntityReference("PrimaryEntityConfigurationClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    /**
     * The sheet configuration.
     *
     * @return a valid record configuration
     */
    @Nonnull
    RecordConfiguration getRecordConfiguration();

    /**
     * The format of the internal identifier (document name) to use for new records.
     *
     * @return a string in the format {@code PREFIX%07d} where {@code %07d} will format a sequence number; more or less
     *         digits can be used, and a suffix may be added if desired
     */
    @Nonnull
    String getIdFormat();

    /**
     * The format of the {@link PrimaryEntity#getName() entity name}, displayed on top of the entity record.
     *
     * @return a Velocity script that will produce a name, given {@code $entity} as a variable
     */
    @Nonnull
    String getNameFormat();
}
