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

import org.phenotips.entities.configuration.PrimaryEntityConfiguration;
import org.phenotips.entities.configuration.RecordConfiguration;

import javax.annotation.Nonnull;

/**
 * Static implementation of {@link PrimaryEntityConfiguration}, where all the data is passed into
 * {@link #StaticPrimaryEntityConfiguration the constructor}.
 *
 * @version $Id$
 * @since 1.4
 */
public class StaticPrimaryEntityConfiguration implements PrimaryEntityConfiguration
{
    /** @see #getRecordConfiguration() */
    private final RecordConfiguration recordConfiguration;

    /** @see #getIdFormat() */
    private final String idFormat;

    /** @see #getNameFormat() */
    private final String nameFormat;

    /**
     * Simple constructor passing all the needed data.
     *
     * @param recordConfiguration the {@link #getRecordConfiguration() record configuration} in use
     * @param idFormat the {@link #getIdFormat() identifier format} in use
     * @param nameFormat the {@link #getNameFormat() name format} in use
     */
    public StaticPrimaryEntityConfiguration(@Nonnull final RecordConfiguration recordConfiguration,
        @Nonnull final String idFormat, @Nonnull final String nameFormat)
    {
        this.recordConfiguration = recordConfiguration;
        this.idFormat = idFormat;
        this.nameFormat = nameFormat;
    }

    @Override
    public RecordConfiguration getRecordConfiguration()
    {
        return this.recordConfiguration;
    }

    @Override
    public String getIdFormat()
    {
        return this.idFormat;
    }

    @Override
    public String getNameFormat()
    {
        return this.nameFormat;
    }

    @Override
    public String toString()
    {
        return this.recordConfiguration.toString();
    }
}
