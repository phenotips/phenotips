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
import org.phenotips.entities.configuration.RecordElementOption;

import org.xwiki.model.reference.ClassPropertyReference;
import org.xwiki.uiextension.UIExtension;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Static implementation of {@link RecordElement}, where all the data is passed into {@link #StaticRecordElement the
 * constructor}.
 *
 * @version $Id$
 * @since 1.4
 */
public class StaticRecordElement implements RecordElement
{
    /** @see #getExtension() */
    private final UIExtension extension;

    /** @see #getName() */
    private final String name;

    /** @see #isEnabled() */
    private final boolean enabled;

    /** @see #getFields() */
    private final List<ClassPropertyReference> fields;

    private final EnumSet<RecordElementOption> options;

    private final Map<String, String> parameters;

    /**
     * Simple constructor passing all the needed data.
     *
     * @param extension the {@link UIExtension UI extension} object defining this record element
     * @param name the {@link #getName() name} of this element
     * @param enabled whether this element is {@link #isEnabled() enabled} or not
     * @param options the {@link #getOptions() configuration options for this element}
     * @param parameters the {@link #getParameters() optional parameters} for this element
     * @param fields the list of {@link #getDisplayedFields() displayed fields}
     */
    public StaticRecordElement(
        @Nonnull final UIExtension extension,
        @Nonnull final String name,
        @Nonnull final boolean enabled,
        @Nullable final EnumSet<RecordElementOption> options,
        @Nullable final Map<String, String> parameters,
        @Nullable final List<ClassPropertyReference> fields)
    {
        this.extension = extension;
        this.name = name;
        this.enabled = enabled;
        this.fields = fields == null ? Collections.emptyList() : Collections.unmodifiableList(fields);
        this.options = options == null ? EnumSet.noneOf(RecordElementOption.class) : EnumSet.copyOf(options);
        this.parameters =
            parameters == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(parameters));
    }

    @Override
    public UIExtension getExtension()
    {
        return this.extension;
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
    public List<ClassPropertyReference> getDisplayedFields()
    {
        return this.fields;
    }

    @Override
    public EnumSet<RecordElementOption> getOptions()
    {
        return EnumSet.copyOf(this.options);
    }

    @Override
    public Map<String, String> getParameters()
    {
        return this.parameters;
    }

    @Override
    public String toString()
    {
        return getName();
    }
}
