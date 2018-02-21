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
package org.phenotips.entities.configuration.spi;

import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.PrimaryEntityManager;
import org.phenotips.entities.configuration.PrimaryEntityConfiguration;

import org.xwiki.stability.Unstable;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.builder.Builder;

/**
 * Exposes the configuration for displaying records.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable
public abstract class PrimaryEntityConfigurationBuilder implements Builder<PrimaryEntityConfiguration>
{
    /**
     * The entity manager to configure.
     *
     * @return an entity manager
     */
    @Nonnull
    public abstract PrimaryEntityManager<? extends PrimaryEntity> getEntityManager();

    /**
     * The record configuration builder to use for displaying this type of record.
     *
     * @return a record configuration builder
     */
    @Nonnull
    public abstract RecordConfigurationBuilder getRecordConfiguration();

    /**
     * Updates the record configuration builder to use. All changes are done in-memory for this object only, the
     * configuration will remain unchanged.
     *
     * @param recordConfiguration a valid record configuration
     * @see #getRecordConfiguration()
     */
    public abstract void setRecordConfiguration(@Nonnull RecordConfigurationBuilder recordConfiguration);

    /**
     * The format of the internal identifier (document name) to use for new records.
     *
     * @return a string in the format {@code PREFIX%07d} where {@code %07d} will format a sequence number; more or less
     *         digits can be used, and a suffix may be added if desired
     */
    @Nonnull
    public abstract String getIdFormat();

    /**
     * Updates the format of the internal identifier (document name) to use for new records. All changes are done
     * in-memory for this object only, the configuration will remain unchanged.
     *
     * @param format a string in the format {@code PREFIX%07d} where {@code %07d} will format a sequence number; more or
     *            less digits can be used, and a suffix may be added if desired
     * @see #getIdFormat()
     */
    public abstract void setIdFormat(@Nonnull String format);

    /**
     * The format of the {@link PrimaryEntity#getName() entity name}, displayed on top of the entity record.
     *
     * @return a Velocity script that will produce a name, given {@code $entity} as a variable
     */
    @Nonnull
    public abstract String getNameFormat();

    /**
     * Updates the format of the {@link PrimaryEntity#getName() entity name}, displayed on top of the entity record. All
     * changes are done in-memory for this object only, the configuration will remain unchanged.
     *
     * @param format a Velocity script that will produce a name, given {@code $entity} as a variable
     * @see #getNameFormat()
     */
    public abstract void setNameFormat(@Nonnull String format);

    /**
     * Build a read-only {@link PrimaryEntityConfiguration configuration} from the current data set in this builder.
     *
     * @return a read-only configuration
     */
    @Override
    @Nonnull
    public abstract PrimaryEntityConfiguration build();
}
