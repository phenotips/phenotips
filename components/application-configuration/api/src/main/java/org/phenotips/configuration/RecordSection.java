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
 * Represents a section that can be displayed in the patient record.
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
     * The name of this section, displayed in the patient record and in the form designer.
     *
     * @return a user-friendly title for this section
     */
    String getName();

    /**
     * Whether this section and its elements are going to be displayed in the patient record or not.
     *
     * @return {@code true} if this section must be displayed, {@code false} otherwise
     */
    boolean isEnabled();

    /**
     * Whether this section should be expanded and fully visible in edit mode by default.
     *
     * @return {@code true} if this section must be expanded, {@code false} otherwis
     */
    boolean isExpandedByDefault();

    /**
     * The list of elements configured in this section, whether they are enabled or not.
     *
     * @return an unmodifiable ordered list of {@link RecordElement elements}, empty if this section doesn't have any
     *         elements
     */
    List<RecordElement> getAllElements();

    /**
     * The list of elements displayed in the patient record by this section.
     *
     * @return an unmodifiable ordered list of {@link RecordElement#isEnabled() enabled elements}, empty if none are
     *         configured or enabled
     */
    List<RecordElement> getEnabledElements();
}
