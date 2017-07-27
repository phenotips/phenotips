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
package org.phenotips.studies.family.internal;

import org.phenotips.configuration.RecordConfiguration;
import org.phenotips.configuration.RecordSection;
import org.phenotips.configuration.spi.RecordConfigurationModule;
import org.phenotips.configuration.spi.UIXRecordSection;

import org.xwiki.component.annotation.Component;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.UIExtensionFilter;
import org.xwiki.uiextension.UIExtensionManager;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Default (global) implementation of the {@link RecordConfiguration} role for family records. Its {@link #getPriority()
 * priority} is {@code 0}.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("families-global")
@Singleton
public class GlobalFamilyConfigurationModule implements RecordConfigurationModule
{
    /** Lists the family form sections and fields. */
    @Inject
    private UIExtensionManager uixManager;

    /** Sorts extensions by their declared order. */
    @Inject
    @Named("sortByParameter")
    private UIExtensionFilter orderFilter;

    @Override
    public RecordConfiguration process(@Nullable final RecordConfiguration config)
    {
        if (config == null) {
            return null;
        }
        final List<UIExtension> sectionExtensions = getOrderedSectionUIExtensions();
        final List<RecordSection> recordSections = new LinkedList<>();
        for (final UIExtension sectionExtension : sectionExtensions) {
            final RecordSection section = new UIXRecordSection(sectionExtension, this.uixManager, this.orderFilter);
            recordSections.add(section);
        }
        config.setSections(Collections.unmodifiableList(recordSections));
        return config;
    }

    @Override
    public int getPriority()
    {
        return 0;
    }

    @Override
    public boolean supportsRecordType(final String recordType)
    {
        return "family".equals(recordType);
    }

    /**
     * Returns all the {@link UIExtension} sections for the default family sheet, and sorts them in preferred order.
     *
     * @return a list of sorted {@link UIExtension family sheet section objects}.
     */
    private List<UIExtension> getOrderedSectionUIExtensions()
    {
        final List<UIExtension> sections = this.uixManager.get("phenotips.familyRecord.content");
        return this.orderFilter.filter(sections, "order");
    }
}
