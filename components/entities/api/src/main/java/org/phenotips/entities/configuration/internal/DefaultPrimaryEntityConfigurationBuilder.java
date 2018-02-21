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
package org.phenotips.entities.configuration.internal;

import org.phenotips.entities.PrimaryEntity;
import org.phenotips.entities.PrimaryEntityManager;
import org.phenotips.entities.configuration.PrimaryEntityConfiguration;
import org.phenotips.entities.configuration.spi.PrimaryEntityConfigurationBuilder;
import org.phenotips.entities.configuration.spi.RecordConfigurationBuilder;
import org.phenotips.entities.configuration.spi.StaticPrimaryEntityConfiguration;

import org.xwiki.stability.Unstable;

import org.codehaus.plexus.util.StringUtils;

/**
 * Exposes the configuration for displaying records.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable
public final class DefaultPrimaryEntityConfigurationBuilder extends PrimaryEntityConfigurationBuilder
{
    private final PrimaryEntityManager<? extends PrimaryEntity> entityManager;

    private RecordConfigurationBuilder record = new DefaultRecordConfigurationBuilder();

    private String idFormat = "%s%07d";

    private String nameFormat = "$!{entity.identifier}";

    private DefaultPrimaryEntityConfigurationBuilder(final PrimaryEntityManager<? extends PrimaryEntity> entityManager)
    {
        if (entityManager == null) {
            throw new IllegalArgumentException("An entity manager is required");
        }
        this.entityManager = entityManager;
    }

    /**
     * Creates a new {@link PrimaryEntityConfigurationBuilder} for the given entity manager. object.
     *
     * @param entityManager the entity manager for which the configuration is built
     * @return the new entity configuration builder
     */
    public static DefaultPrimaryEntityConfigurationBuilder with(
        final PrimaryEntityManager<? extends PrimaryEntity> entityManager)
    {
        return new DefaultPrimaryEntityConfigurationBuilder(entityManager);
    }

    @Override
    public PrimaryEntityManager<? extends PrimaryEntity> getEntityManager()
    {
        return this.entityManager;
    }

    @Override
    public RecordConfigurationBuilder getRecordConfiguration()
    {
        return this.record;
    }

    @Override
    public void setRecordConfiguration(RecordConfigurationBuilder recordConfiguration)
    {
        if (recordConfiguration == null) {
            throw new IllegalArgumentException("Record Configuration Builder cannot be null");
        }
        this.record = recordConfiguration;
    }

    @Override
    public String getIdFormat()
    {
        return this.idFormat;
    }

    @Override
    public void setIdFormat(String format)
    {
        if (StringUtils.isEmpty(format)) {
            throw new IllegalArgumentException("ID Format cannot be null");
        }
        this.idFormat = format;
    }

    @Override
    public String getNameFormat()
    {
        return this.nameFormat;
    }

    @Override
    public void setNameFormat(String format)
    {
        if (StringUtils.isEmpty(format)) {
            throw new IllegalArgumentException("Name Format cannot be null");
        }
        this.nameFormat = format;
    }

    @Override
    public PrimaryEntityConfiguration build()
    {
        return new StaticPrimaryEntityConfiguration(this.record.build(), this.idFormat, this.nameFormat);
    }
}
