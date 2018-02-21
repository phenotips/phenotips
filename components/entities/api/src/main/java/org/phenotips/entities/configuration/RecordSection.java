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

import org.xwiki.stability.Unstable;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * Represents a section that can be displayed in a record.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable
public interface RecordSection
{
    /**
     * The name of this section, displayed in the record and in the form designer.
     *
     * @return a user-friendly title for this section
     */
    @Nonnull
    String getName();

    /**
     * Whether this section and its elements are going to be displayed in the record or not.
     *
     * @return {@code true} if this section must be displayed, {@code false} otherwise
     */
    boolean isEnabled();

    /**
     * The list of elements configured in this section, whether they are enabled or not.
     *
     * @return an unmodifiable ordered list of {@link RecordElement elements}, empty if this section doesn't have any
     *         elements
     */
    @Nonnull
    List<RecordElement> getAllElements();

    /**
     * The list of elements displayed in this section.
     *
     * @return an unmodifiable ordered list of {@link RecordElement#isEnabled() enabled elements}, empty if none are
     *         configured or enabled
     */
    @Nonnull
    List<RecordElement> getEnabledElements();

    /**
     * Configuration options for this section.
     *
     * @return a set of options, possibly empty.
     */
    @Nonnull
    EnumSet<RecordSectionOption> getOptions();

    /**
     * Other optional parameters affecting this section.
     *
     * @return a possibly empty map of parameters
     */
    @Nonnull
    Map<String, String> getParameters();
}
