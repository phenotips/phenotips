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
import org.xwiki.uiextension.UIExtension;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

/**
 * A record element represents a single field or a small subset of related fields displayed in a
 * {@code RecordSection section}. A record element can also contain only static or read-only information, such as a
 * warning, or an automatically generated list, in which case the list of displayed fields would be empty.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable
public interface RecordElement
{
    /**
     * The extension defining this element.
     *
     * @return a valid {@link UIExtension} object
     */
    @Unstable("The direct use of UIExtension is not recommended, but is the only way of rendering the needed HTML code")
    UIExtension getExtension();

    /**
     * The name of this element, displayed in the form designer.
     *
     * @return a user-friendly name for this element
     */
    @Nonnull
    String getName();

    /**
     * Whether this element is going to be displayed in the record or not.
     *
     * @return {@code true} if this element must be displayed, {@code false} otherwise
     */
    boolean isEnabled();

    /**
     * The list of fields displayed in the record by this element.
     *
     * @return an unmodifiable ordered list of field names, empty if this element doesn't display any modifiable fields
     */
    @Nonnull
    List<ClassPropertyReference> getDisplayedFields();

    /**
     * Configuration options for this element.
     *
     * @return a set of options, possibly empty.
     */
    @Nonnull
    EnumSet<RecordElementOption> getOptions();

    /**
     * Other optional parameters affecting this element.
     *
     * @return a possibly empty map of parameters
     */
    @Nonnull
    Map<String, String> getParameters();
}
