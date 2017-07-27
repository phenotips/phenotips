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
package org.phenotips.data.internal;

import org.phenotips.configuration.RecordConfiguration;
import org.phenotips.configuration.RecordSection;
import org.phenotips.configuration.spi.RecordConfigurationModule;
import org.phenotips.configuration.spi.UIXRecordSection;
import org.phenotips.data.Patient;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.UIExtensionFilter;
import org.xwiki.uiextension.UIExtensionManager;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default (global) implementation of the {@link RecordConfiguration} role for patient records. Its
 * {@link #getPriority() priority} is {@code 0}.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("patients-global")
@Singleton
public class GlobalPatientRecordConfigurationModule implements RecordConfigurationModule
{
    /** The location where preferences are stored. */
    private static final EntityReference PREFERENCES_LOCATION = new EntityReference("WebHome", EntityType.DOCUMENT,
        Patient.DEFAULT_DATA_SPACE);

    /** The name of the class storing default phenotype mapping. */
    private static final String PHENOTYPE_MAPPING_CLASSNAME = "PhenoTips.PhenotypeMapping";

    /** The phenotype mapping label. */
    private static final String PHENOTYPE_MAPPING_LABEL = "phenotypeMapping";

    /** Date of birth format label. */
    private static final String DOB_FORMAT_LABEL = "dateOfBirthFormat";

    /** Lists the patient form sections and fields. */
    @Inject
    private UIExtensionManager uixManager;

    /** Sorts extensions by their declared order. */
    @Inject
    @Named("sortByParameter")
    private UIExtensionFilter orderFilter;

    /** Provides access to the current request context. */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> referenceResolver;

    /** Logging helper object. */
    @Inject
    private Logger logger;

    @Override
    public RecordConfiguration process(RecordConfiguration config)
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
        config.setSections(recordSections);

        DocumentReference mapping = getPhenotypeMapping();
        if (mapping != null) {
            config.setPhenotypeMapping(mapping);
        }
        String dobFormat = getDateOfBirthFormat();
        if (StringUtils.isNotBlank(dobFormat)) {
            config.setDateOfBirthFormat(dobFormat);
        }

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
        return "patient".equals(recordType);
    }

    /**
     * Returns all the {@link UIExtension} sections for the default patient sheet, and sorts them in preferred order.
     *
     * @return a list of sorted {@link UIExtension patient sheet section objects}.
     */
    private List<UIExtension> getOrderedSectionUIExtensions()
    {
        final List<UIExtension> sections = this.uixManager.get("org.phenotips.patientSheet.content");
        return this.orderFilter.filter(sections, "order");
    }

    private DocumentReference getPhenotypeMapping()
    {
        BaseObject settings = getGlobalConfigurationObject();
        if (settings == null) {
            return null;
        }
        final String mapping = StringUtils.defaultIfBlank(settings.getStringValue(PHENOTYPE_MAPPING_LABEL),
            PHENOTYPE_MAPPING_CLASSNAME);
        return this.referenceResolver.resolve(mapping);

    }

    private String getDateOfBirthFormat()
    {
        BaseObject settings = getGlobalConfigurationObject();
        if (settings != null) {
            return settings.getStringValue(DOB_FORMAT_LABEL);
        }
        return null;
    }

    private BaseObject getGlobalConfigurationObject()
    {
        try {
            XWikiContext context = this.xcontextProvider.get();
            return context.getWiki().getDocument(PREFERENCES_LOCATION, context)
                .getXObject(RecordConfiguration.GLOBAL_PREFERENCES_CLASS);
        } catch (XWikiException ex) {
            this.logger.warn("Failed to read preferences: {}", ex.getMessage());
        }
        return null;
    }
}
