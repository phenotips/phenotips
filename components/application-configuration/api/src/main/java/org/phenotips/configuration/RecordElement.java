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
     * Whether this element is going to be displayed in the record or not.
     *
     * @return {@code true} if this element must be displayed, {@code false} otherwise
     */
    boolean isEnabled();

    /**
     * Sets whether this element is going to be displayed in the record or not. All changes are done in-memory for this
     * object only, the configuration will remain unchanged.
     *
     * @param enabled {@code true} if this element should be displayed, {@code false} otherwise
     * @since 1.4
     */
    void setEnabled(boolean enabled);

    /**
     * Whether this element contains any private identifiable information (PII) or not.
     *
     * @return {@code true} if this element contains PII, {@code false} otherwise
     * @deprecated since 1.4, this functionality has moved in the Consents module
     */
    @Deprecated
    boolean containsPrivateIdentifiableInformation();

    /**
     * The list of fields displayed in the record by this element.
     *
     * @return an unmodifiable ordered list of field names, empty if this element doesn't display any modifiable fields
     */
    List<String> getDisplayedFields();

    /**
     * The parent {@link RecordSection section} containing this element.
     *
     * @return a valid section
     */
    RecordSection getContainingSection();
}
