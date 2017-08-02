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
package org.phenotips.configuration;

import org.xwiki.stability.Unstable;
import org.xwiki.uiextension.UIExtension;

import java.util.List;

/**
 * Represents a section that can be displayed in a record.
 *
 * @version $Id$
 * @since 1.0M9
 */
@Unstable
public interface RecordSection
{
    /**
     * The extension defining this section.
     *
     * @return a valid {@link UIExtension} object
     */
    UIExtension getExtension();

    /**
     * The name of this section, displayed in the record and in the form designer.
     *
     * @return a user-friendly title for this section
     */
    String getName();

    /**
     * Whether this section and its elements are going to be displayed in the record or not.
     *
     * @return {@code true} if this section must be displayed, {@code false} otherwise
     */
    boolean isEnabled();

    /**
     * Set whether this section and its elements are going to be displayed in the record or not. All changes are done
     * in-memory for this object only, the configuration will remain unchanged.
     *
     * @param enabled {@code true} if this section must be displayed, {@code false} otherwise
     * @since 1.4
     */
    void setEnabled(boolean enabled);

    /**
     * Whether this section should be expanded and fully visible in edit mode by default.
     *
     * @return {@code true} if this section must be expanded, {@code false} otherwise
     */
    boolean isExpandedByDefault();

    /**
     * Set whether this section should be expanded and fully visible in edit mode by default. All changes are done
     * in-memory for this object only, the configuration will remain unchanged.
     *
     * @param expanded {@code true} if this section must be expanded, {@code false} otherwise
     * @since 1.4
     */
    void setExpandedByDefault(boolean expanded);

    /**
     * The list of elements configured in this section, whether they are enabled or not.
     *
     * @return an unmodifiable ordered list of {@link RecordElement elements}, empty if this section doesn't have any
     *         elements
     */
    List<RecordElement> getAllElements();

    /**
     * The list of elements displayed in this section.
     *
     * @return an unmodifiable ordered list of {@link RecordElement#isEnabled() enabled elements}, empty if none are
     *         configured or enabled
     */
    List<RecordElement> getEnabledElements();

    /**
     * Set the list of elements configured in this section. Each element still decides whether it is enabled or not. All
     * changes are done in-memory for this object only, the configuration will remain unchanged.
     *
     * @param elements the new list of elements to be included in this section, may be empty
     * @since 1.4
     */
    void setElements(List<RecordElement> elements);
}
