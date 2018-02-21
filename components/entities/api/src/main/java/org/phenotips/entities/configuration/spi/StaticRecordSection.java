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

import org.phenotips.entities.configuration.RecordElement;
import org.phenotips.entities.configuration.RecordSection;
import org.phenotips.entities.configuration.RecordSectionOption;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

/**
 * Static implementation of {@link RecordSection}, where all the data is passed into {@link #StaticRecordSection the
 * constructor}.
 *
 * @version $Id$
 * @since 1.4
 */
public class StaticRecordSection implements RecordSection
{
    private final String name;

    /** @see #isEnabled() */
    private final boolean enabled;

    /** @see #getAllElements() */
    private final List<RecordElement> elements;

    /** @see #getEnabledElements() */
    private final List<RecordElement> enabledElements;

    /** @see #getOptions() */
    private final EnumSet<RecordSectionOption> options;

    /** @see #getParameters() */
    private final Map<String, String> parameters;

    /**
     * Simple constructor passing all the needed data.
     *
     * @param name the {@link #getName() name} of this section
     * @param enabled whether this section is {@link #isEnabled() enabled} or not
     * @param options the {@link #getOptions() configuration options} for this section
     * @param parameters the {@link #getParameters() optional parameters} for this element
     * @param elements the list of {@link #getAllElements() contained elements}
     */
    public StaticRecordSection(
        @Nonnull final String name,
        final boolean enabled,
        @Nullable final EnumSet<RecordSectionOption> options,
        @Nullable final Map<String, String> parameters,
        @Nullable final List<RecordElement> elements)
    {
        this.name = name;
        this.enabled = enabled;
        this.elements =
            elements == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(elements));
        this.enabledElements = Collections.unmodifiableList(
            this.elements.stream().filter(RecordElement::isEnabled).collect(Collectors.toList()));
        this.options = options == null ? EnumSet.noneOf(RecordSectionOption.class) : EnumSet.copyOf(options);
        this.parameters =
            parameters == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(parameters));
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public boolean isEnabled()
    {
        return this.enabled;
    }

    @Override
    public List<RecordElement> getAllElements()
    {
        return this.elements;
    }

    @Override
    public List<RecordElement> getEnabledElements()
    {
        return this.enabledElements;
    }

    @Override
    public String toString()
    {
        StringBuilder result = new StringBuilder(getName());
        result.append(" [");
        result.append(StringUtils.join(getEnabledElements(), ", "));
        result.append(']');
        return result.toString();
    }

    @Override
    public EnumSet<RecordSectionOption> getOptions()
    {
        return EnumSet.copyOf(this.options);
    }

    @Override
    public Map<String, String> getParameters()
    {
        return this.parameters;
    }
}
