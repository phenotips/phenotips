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

import org.phenotips.entities.configuration.RecordConfiguration;
import org.phenotips.entities.configuration.RecordSection;
import org.phenotips.entities.configuration.spi.RecordConfigurationBuilder;
import org.phenotips.entities.configuration.spi.RecordSectionBuilder;
import org.phenotips.entities.configuration.spi.StaticRecordConfiguration;

import org.xwiki.stability.Unstable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Exposes the configuration for displaying records.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable
public class DefaultRecordConfigurationBuilder extends RecordConfigurationBuilder
{
    private List<RecordSectionBuilder> sections = new LinkedList<>();

    @Override
    public List<RecordSectionBuilder> getAllSections()
    {
        return this.sections;
    }

    @Override
    public void setSections(List<RecordSectionBuilder> sections)
    {
        this.sections =
            sections == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(sections));
    }

    @Override
    public RecordConfiguration build()
    {
        final List<RecordSection> builtSections =
            this.sections.stream().map(RecordSectionBuilder::build).collect(Collectors.toList());
        return new StaticRecordConfiguration(builtSections);
    }
}
