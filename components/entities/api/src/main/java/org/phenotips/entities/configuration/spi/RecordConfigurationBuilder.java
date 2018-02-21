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

import org.phenotips.entities.configuration.RecordConfiguration;

import org.xwiki.stability.Unstable;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.builder.Builder;

/**
 * Builder for a {@link RecordConfiguration} implementation.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable
public abstract class RecordConfigurationBuilder implements Builder<RecordConfiguration>
{
    /**
     * The list of available sections, enabled or disabled, that can be displayed in this type of record.
     *
     * @return a list of sections, or an empty list if none are defined
     */
    @Nonnull
    public abstract List<RecordSectionBuilder> getAllSections();

    /**
     * Updates the list of available section. All changes are done in-memory for this object only, the configuration
     * will remain unchanged.
     *
     * @param sections a list of sections, may be empty
     * @see #getAllSections()
     */
    public abstract void setSections(@Nullable List<RecordSectionBuilder> sections);

    /**
     * Build a read-only {@link RecordConfiguration configuration} from the current data set in this builder.
     *
     * @return a read-only configuration
     */
    @Override
    @Nonnull
    public abstract RecordConfiguration build();
}
