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
package org.phenotips.studies.internal;

import org.phenotips.Constants;
import org.phenotips.configuration.RecordConfiguration;
import org.phenotips.configuration.RecordElement;
import org.phenotips.configuration.RecordSection;
import org.phenotips.configuration.spi.RecordConfigurationModule;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Implementation of {@link RecordConfigurationModule} that takes into account a {@link StudyConfiguration custom
 * configuration}. Its {@link #getPriority() priority} is {@code 50}. The current implementation, does not allow
 * {@link StudyRecordConfigurationModule} to add any sections or fields. It only permits the module to disable sections
 * and/or fields for an existing configuration.
 *
 * @version $Id$
 * @since 1.4
 */
@Named("studies")
public class StudyRecordConfigurationModule implements RecordConfigurationModule
{
    /** The XClass used for storing the study configuration. */
    public static final EntityReference STUDY_CLASS_REFERENCE =
        new EntityReference("StudyClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    /** The XClass which allows to bind a specific study to a patient record. */
    public static final EntityReference STUDY_BINDING_CLASS_REFERENCE = new EntityReference("StudyBindingClass",
        EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    /** The XProperty where the study binding is stored. */
    public static final String STUDY_REFERENCE_PROPERTY_LABEL = "studyReference";

    /** Provides access to the data. */
    @Inject
    private DocumentAccessBridge dab;

    /** Completes xclass references with the current wiki. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> resolver;

    /** Provides access to the current request context. */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /** Parses serialized document references into proper references. */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> referenceParser;

    /** Logging helper. */
    @Inject
    private Logger logger;

    @Override
    public RecordConfiguration process(RecordConfiguration config)
    {
        if (config == null) {
            return null;
        }

        final StudyConfiguration configObj = getBoundConfiguration();

        // If no study record configuration is provided, then return the default configuration, unchanged.
        if (configObj == null) {
            return config;
        }

        // A study may change the phenotype mapping
        String customPhenotypeMapping = configObj.getPhenotypeMapping();
        if (StringUtils.isNotBlank(customPhenotypeMapping)) {
            config.setPhenotypeMapping(this.referenceParser.resolve(customPhenotypeMapping));
        }

        // Get the sections and fields overrides.
        final List<String> sectionOverrides = configObj.getSectionsOverride();
        final List<String> fieldOverrides = configObj.getFieldsOverride();

        // If no overrides for enabled sections or fields are specified, then everything should be unchanged.
        if (CollectionUtils.isEmpty(sectionOverrides) || CollectionUtils.isEmpty(fieldOverrides)) {
            return config;
        }
        // Otherwise, update the configuration.
        updateStudyConfiguration(config, sectionOverrides, fieldOverrides);
        return config;
    }

    /**
     * Given the old {@code config form configuration}, the list of {@code sectionOverrides enabled sections} and
     * {@code fieldOverrides enabled fields}, update the {@code config configuration} with the new section settings.
     *
     * @param config the {@link RecordConfiguration configuration} that needs to be updated
     * @param sectionOverrides the list of identifiers for enabled sections
     * @param fieldOverrides the list of identifiers for enabled fields
     */
    private void updateStudyConfiguration(final RecordConfiguration config, final List<String> sectionOverrides,
        final List<String> fieldOverrides)
    {
        List<RecordSection> updatedSections = new ArrayList<>(config.getAllSections());
        Collections.<RecordSection>sort(updatedSections, new Comparator<RecordSection>()
        {
            @Override
            public int compare(RecordSection o1, RecordSection o2)
            {
                int i1 = sectionOverrides.indexOf(o1.getExtension().getId());
                int i2 = sectionOverrides.indexOf(o2.getExtension().getId());
                return (i2 == -1 || i1 == -1) ? (i2 - i1) : (i1 - i2);
            }
        });
        for (final RecordSection section : updatedSections) {
            // If section ID is not in sectionOverrides, it's not enabled. Disable all elements for section.
            if (!sectionOverrides.contains(section.getExtension().getId())) {
                section.setEnabled(false);
            } else {
                configureFields(section, fieldOverrides);
            }
        }
        config.setSections(updatedSections);
    }

    /**
     * Given the {@code section} that needs to be configured, and the {@code fieldOverrides enabled fields}, configures
     * the {@code section} elements.
     *
     * @param section the {@link RecordSection record section} that needs to be configured
     * @param fieldOverrides the list of identifiers for enabled fields
     */
    private void configureFields(final RecordSection section, final List<String> fieldOverrides)
    {
        List<RecordElement> updatedElements = new ArrayList<>(section.getAllElements());
        Collections.<RecordElement>sort(updatedElements, new Comparator<RecordElement>()
        {
            @Override
            public int compare(RecordElement o1, RecordElement o2)
            {
                int i1 = fieldOverrides.indexOf(o1.getExtension().getId());
                int i2 = fieldOverrides.indexOf(o2.getExtension().getId());
                return (i2 == -1 || i1 == -1) ? (i2 - i1) : (i1 - i2);
            }
        });
        for (final RecordElement element : updatedElements) {
            if (!fieldOverrides.contains(element.getExtension().getId())) {
                element.setEnabled(false);
            }
        }
        section.setElements(updatedElements);
    }

    @Override
    public int getPriority()
    {
        return 50;
    }

    @Override
    public boolean supportsRecordType(String recordType)
    {
        return "patient".equals(recordType);
    }

    /**
     * If the current document is a patient record, and it has a valid specific study binding specified, then return
     * that configuration.
     *
     * @return a form configuration, if one is bound to the current document, or {@code null} otherwise
     */
    private StudyConfiguration getBoundConfiguration()
    {
        if (this.dab.getCurrentDocumentReference() == null) {
            // Non-interactive requests, use the default configuration
            return null;
        }
        String boundConfig = (String) this.dab.getProperty(this.dab.getCurrentDocumentReference(),
            this.resolver.resolve(STUDY_BINDING_CLASS_REFERENCE), STUDY_REFERENCE_PROPERTY_LABEL);
        if (StringUtils.isNotBlank(boundConfig)) {
            try {
                XWikiContext context = this.xcontextProvider.get();
                XWikiDocument doc = context.getWiki().getDocument(this.referenceParser.resolve(boundConfig), context);
                if (doc == null || doc.isNew() || doc.getXObject(STUDY_CLASS_REFERENCE) == null) {
                    // Inaccessible, deleted, or invalid document, use default configuration
                    return null;
                }
                return new StudyConfiguration(doc.getXObject(STUDY_CLASS_REFERENCE));
            } catch (Exception ex) {
                this.logger.warn("Failed to read the bound configuration [{}] for [{}]: {}", boundConfig,
                    this.dab.getCurrentDocumentReference(), ex.getMessage());
            }
        }
        return null;
    }
}
