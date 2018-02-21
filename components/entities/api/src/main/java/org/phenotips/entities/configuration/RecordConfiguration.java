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

import org.xwiki.model.reference.ClassPropertyReference;
import org.xwiki.stability.Unstable;

import java.util.List;

import javax.annotation.Nonnull;

/**
 * Exposes the contents of the sheets used for displaying primary entity records, split into {@link RecordSection
 * sections}, which contain {@link RecordElement elements} displaying zero, one, or more fields.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable
public interface RecordConfiguration
{
    /**
     * The list of sections enabled for this record type.
     *
     * @return an unmodifiable ordered list of sections, empty if none are enabled or the configuration is missing
     */
    @Nonnull
    List<RecordSection> getEnabledSections();

    /**
     * The list of available sections, enabled or disabled, that can be displayed in this type of record.
     *
     * @return an unmodifiable ordered list of sections, or an empty list if none are defined
     */
    @Nonnull
    List<RecordSection> getAllSections();

    /**
     * The list of fields enabled for this record or record type.
     *
     * @return an unmodifiable ordered list of field references, empty if none are enabled or the configuration is
     *         missing
     */
    @Nonnull
    List<ClassPropertyReference> getEnabledFields();

    /**
     * The list of available fields, enabled or disabled, that can be displayed in this type of record.
     *
     * @return an unmodifiable ordered list of field references, empty if none are available or the configuration is
     *         missing
     */
    @Nonnull
    List<ClassPropertyReference> getAllFields();
}
