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

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.configuration.RecordConfiguration;
import org.phenotips.configuration.RecordSection;
import org.phenotips.configuration.internal.global.GlobalRecordConfiguration;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.uiextension.UIExtensionFilter;
import org.xwiki.uiextension.UIExtensionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Implementation of {@link RecordConfiguration} that takes into account a {@link CustomConfiguration custom
 * configuration}.
 *
 * @version $Id$
 * @since 1.0M9
 */
public class ConfiguredRecordConfiguration extends GlobalRecordConfiguration implements RecordConfiguration
{
    /** The custom configuration defining this patient record configuration. */
    private final CustomConfiguration configuration;

    /**
     * Simple constructor passing all the needed components.
     *
     * @param configuration the custom configuration
     * @param execution the execution context manager
     * @param uixManager the UIExtension manager
     * @param orderFilter UIExtension filter for ordering sections and elements
     */
    public ConfiguredRecordConfiguration(CustomConfiguration configuration, Execution execution,
        UIExtensionManager uixManager, UIExtensionFilter orderFilter)
    {
        super(execution, uixManager, orderFilter);
        this.configuration = configuration;
    }

    @Override
    public List<RecordSection> getAllSections()
    {
        List<RecordSection> result = new ArrayList<RecordSection>();
        List<RecordSection> allSections = super.getAllSections();
        final List<String> overrides = this.configuration.getSectionsOverride();
        for (RecordSection section : allSections) {
            result.add(new ConfiguredRecordSection(this.configuration, section.getExtension(), this.uixManager,
                this.orderFilter));
        }
        if (overrides != null && !overrides.isEmpty()) {
            Collections.<RecordSection>sort(result, new Comparator<RecordSection>()
            {
                @Override
                public int compare(RecordSection o1, RecordSection o2)
                {
                    int i1 = overrides.indexOf(o1.getExtension().getId());
                    int i2 = overrides.indexOf(o2.getExtension().getId());
                    return (i2 == -1 || i1 == -1) ? (i2 - i1) : (i1 - i2);
                }
            });
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public DocumentReference getPhenotypeMapping()
    {
        try {
            String mapping = this.configuration.getPhenotypeMapping();
            if (StringUtils.isNotBlank(mapping)) {
                DocumentReferenceResolver<String> resolver = ComponentManagerRegistry.getContextComponentManager()
                    .getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
                return resolver.resolve(mapping);
            }
        } catch (ComponentLookupException e) {
            // Shouldn't happen, base components must be available
        }
        return super.getPhenotypeMapping();
    }
}
