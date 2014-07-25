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
package org.phenotips.configuration.internal.configured;

import org.phenotips.configuration.RecordElement;
import org.phenotips.configuration.RecordSection;
import org.phenotips.configuration.internal.global.DefaultRecordElement;

import org.xwiki.uiextension.UIExtension;

import java.util.List;

/**
 * Implementation of {@link RecordElement} that takes into account a {@link CustomConfiguration custom configuration}.
 *
 * @version $Id$
 * @since 1.0M9
 */
public class ConfiguredRecordElement extends DefaultRecordElement implements RecordElement
{
    /** The custom configuration affecting this element. */
    private final CustomConfiguration configuration;

    /**
     * Simple constructor passing all the needed components.
     *
     * @param configuration the custom configuration
     * @param extension the extension defining this element
     * @param section the parent {@link RecordSection section} containing this element
     */
    public ConfiguredRecordElement(CustomConfiguration configuration, UIExtension extension, RecordSection section)
    {
        super(extension, section);
        this.configuration = configuration;
    }

    @Override
    public boolean isEnabled()
    {
        List<String> overrides = this.configuration.getFieldsOverride();
        if (overrides == null || overrides.isEmpty()) {
            return super.isEnabled();
        }
        return overrides.contains(this.getExtension().getId());
    }
}
