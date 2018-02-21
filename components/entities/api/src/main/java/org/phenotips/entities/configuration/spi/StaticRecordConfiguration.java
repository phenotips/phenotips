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
import org.phenotips.entities.configuration.RecordSection;

import org.xwiki.model.reference.ClassPropertyReference;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

/**
 * Static implementation of {@link RecordConfiguration}, where all the data is passed into
 * {@link #StaticRecordConfiguration the constructor}.
 *
 * @version $Id$
 * @since 1.4
 */
public class StaticRecordConfiguration implements RecordConfiguration
{
    /** @see #getAllSections() */
    private final List<RecordSection> sections;

    /** @see #getEnabledSections() */
    private final List<RecordSection> enabledSections;

    /**
     * Simple constructor passing all the needed data.
     *
     * @param sections all the {@link #getAllSections() contained sections}.
     */
    public StaticRecordConfiguration(final List<RecordSection> sections)
    {
        this.sections = sections == null ? Collections.emptyList() : Collections.unmodifiableList(sections);
        this.enabledSections = Collections.unmodifiableList(
            this.sections.stream().filter(RecordSection::isEnabled).collect(Collectors.toList()));
    }

    @Override
    public List<RecordSection> getAllSections()
    {
        return this.sections;
    }

    @Override
    public List<RecordSection> getEnabledSections()
    {
        return this.enabledSections;
    }

    @Override
    public List<ClassPropertyReference> getEnabledFields()
    {
        return Collections.unmodifiableList(
            this.getEnabledSections().stream()
                .flatMap(section -> section.getEnabledElements().stream())
                .flatMap(element -> element.getDisplayedFields().stream())
                .collect(Collectors.toList()));
    }

    @Override
    public List<ClassPropertyReference> getAllFields()
    {
        return Collections.unmodifiableList(
            this.getAllSections().stream()
                .flatMap(section -> section.getAllElements().stream())
                .flatMap(element -> element.getDisplayedFields().stream())
                .collect(Collectors.toList()));
    }

    @Override
    public String toString()
    {
        return StringUtils.join(getEnabledSections(), ", ");
    }
}
