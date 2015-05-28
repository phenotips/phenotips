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
 * A single field or a small subset of related fields displayed in a {@code RecordSection section}.
 *
 * @version $Id$
 * @since 1.0M9
 */
@Unstable
public interface RecordElement
{
    /**
     * The extension defining this element.
     *
     * @return a valid {@link UIExtension} object
     */
    UIExtension getExtension();

    /**
     * The name of this element, displayed in the form designer.
     *
     * @return a user-friendly name for this element
     */
    String getName();

    /**
     * Whether this element is going to be displayed in the patient record or not.
     *
     * @return {@code true} if this element must be displayed, {@code false} otherwise
     */
    boolean isEnabled();

    /**
     * Whether this element contains any private indentifiable information (PII) about the patient or not.
     *
     * @return {@code true} if this element contains PII, {@code false} otherwise
     */
    boolean containsPrivateIdentifiableInformation();

    /**
     * The list of fields displayed in the patient record by this element.
     *
     * @return an unmodifiable ordered list of field names, empty if this element doesn't display patient fields
     */
    List<String> getDisplayedFields();

    /**
     * The parent {@link RecordSection section} containing this element.
     *
     * @return a valid section
     */
    RecordSection getContainingSection();
}
