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
package org.phenotips.configuration.internal.configured;

import org.phenotips.configuration.RecordElement;
import org.phenotips.configuration.RecordSection;
import org.phenotips.configuration.internal.global.DefaultRecordSection;

import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.UIExtensionFilter;
import org.xwiki.uiextension.UIExtensionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Implementation of {@link RecordSection} that takes into account a {@link CustomConfiguration custom configuration}.
 *
 * @version $Id$
 * @since 1.0M9
 */
public class ConfiguredRecordSection extends DefaultRecordSection implements RecordSection
{
    /** The custom configuration affecting this section. */
    private final CustomConfiguration configuration;

    /**
     * Simple constructor passing all the needed components.
     *
     * @param configuration the custom configuration
     * @param extension the extension defining this element
     * @param uixManager the UIExtension manager
     * @param orderFilter UIExtension filter for ordering sections and elements
     */
    public ConfiguredRecordSection(CustomConfiguration configuration, UIExtension extension,
        UIExtensionManager uixManager, UIExtensionFilter orderFilter)
    {
        super(extension, uixManager, orderFilter);
        this.configuration = configuration;
    }

    @Override
    public boolean isEnabled()
    {
        List<String> overrides = this.configuration.getSectionsOverride();
        if (overrides == null || overrides.isEmpty()) {
            return super.isEnabled();
        }
        return overrides.contains(this.getExtension().getId());
    }

    @Override
    public List<RecordElement> getAllElements()
    {
        List<RecordElement> result = new ArrayList<RecordElement>();
        List<RecordElement> allElements = super.getAllElements();
        final List<String> overrides = this.configuration.getFieldsOverride();
        for (RecordElement element : allElements) {
            result.add(new ConfiguredRecordElement(this.configuration, element.getExtension(), this));
        }
        if (overrides != null && !overrides.isEmpty()) {
            Collections.<RecordElement>sort(result, new Comparator<RecordElement>()
            {
                @Override
                public int compare(RecordElement o1, RecordElement o2)
                {
                    int i1 = overrides.indexOf(o1.getExtension().getId());
                    int i2 = overrides.indexOf(o2.getExtension().getId());
                    return (i2 == -1 || i1 == -1) ? (i2 - i1) : (i1 - i2);
                }
            });
        }
        return Collections.unmodifiableList(result);
    }
}
