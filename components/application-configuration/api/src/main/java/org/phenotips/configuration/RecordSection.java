/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
