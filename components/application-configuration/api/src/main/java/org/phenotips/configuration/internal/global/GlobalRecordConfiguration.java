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
package org.phenotips.configuration.internal.global;

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.configuration.RecordConfiguration;
import org.phenotips.configuration.RecordElement;
import org.phenotips.configuration.RecordSection;
import org.phenotips.data.Patient;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.UIExtensionFilter;
import org.xwiki.uiextension.UIExtensionManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Default (global) implementation of the {@link RecordConfiguration} role.
 *
 * @version $Id$
 * @since 1.0M9
 */
public class GlobalRecordConfiguration implements RecordConfiguration
{
    /** The location where preferences are stored. */
    private static final EntityReference PREFERENCES_LOCATION = new EntityReference("WebHome", EntityType.DOCUMENT,
        new EntityReference("data", EntityType.SPACE));

    /** The name of the UIX parameter used for specifying the order of fields and sections. */
    private static final String SORT_PARAMETER_NAME = "order";

    /** Provides access to the current request context. */
    protected Provider<XWikiContext> xcontextProvider;

    /** Lists the patient form sections and fields. */
    protected UIExtensionManager uixManager;

    /** Sorts fields by their declared order. */
    protected UIExtensionFilter orderFilter;

    /** Logging helper object. */
    private Logger logger = LoggerFactory.getLogger(GlobalRecordConfiguration.class);

    /**
     * Simple constructor passing all the needed components.
     *
     * @param xcontextProvider provides access to the current request context
     * @param uixManager the UIExtension manager
     * @param orderFilter UIExtension filter for ordering sections and elements
     */
    public GlobalRecordConfiguration(Provider<XWikiContext> xcontextProvider, UIExtensionManager uixManager,
        UIExtensionFilter orderFilter)
    {
        this.xcontextProvider = xcontextProvider;
        this.uixManager = uixManager;
        this.orderFilter = orderFilter;
    }

    @Override
    public List<RecordSection> getAllSections()
    {
        List<RecordSection> result = new LinkedList<RecordSection>();
        List<UIExtension> sections = this.uixManager.get("org.phenotips.patientSheet.content");
        sections = this.orderFilter.filter(sections, SORT_PARAMETER_NAME);
        for (UIExtension sectionExtension : sections) {
            RecordSection section = new DefaultRecordSection(sectionExtension, this.uixManager, this.orderFilter);
            result.add(section);
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public List<RecordSection> getEnabledSections()
    {
        List<RecordSection> result = new LinkedList<RecordSection>();
        for (RecordSection section : getAllSections()) {
            if (section.isEnabled()) {
                result.add(section);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public List<String> getEnabledFieldNames()
    {
        List<String> result = new LinkedList<String>();
        for (RecordSection section : getEnabledSections()) {
            for (RecordElement element : section.getEnabledElements()) {
                result.addAll(element.getDisplayedFields());
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public List<String> getEnabledNonIdentifiableFieldNames()
    {
        List<String> result = new LinkedList<String>();
        for (RecordSection section : getEnabledSections()) {
            for (RecordElement element : section.getEnabledElements()) {
                if (!element.containsPrivateIdentifiableInformation()) {
                    result.addAll(element.getDisplayedFields());
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public List<String> getAllFieldNames()
    {
        try {
            XWikiContext context = this.xcontextProvider.get();
            BaseClass patientClass = context.getWiki().getDocument(Patient.CLASS_REFERENCE, context).getXClass();
            return Collections.unmodifiableList(Arrays.asList(patientClass.getPropertyNames()));
        } catch (XWikiException ex) {
            this.logger.error("Failed to access the patient class: {}", ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }

    @Override
    public DocumentReference getPhenotypeMapping()
    {
        try {
            String mapping = "PhenoTips.PhenotypeMapping";
            BaseObject settings = getGlobalConfigurationObject();
            mapping = StringUtils.defaultIfBlank(settings.getStringValue("phenotypeMapping"), mapping);
            DocumentReferenceResolver<String> resolver = ComponentManagerRegistry.getContextComponentManager()
                .getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
            return resolver.resolve(mapping);
        } catch (NullPointerException ex) {
            // No value set, return the default
        } catch (ComponentLookupException e) {
            // Shouldn't happen, base components must be available
        }
        return null;
    }

    @Override
    public String getISODateFormat()
    {
        return "yyyy-MM-dd";
    }

    @Override
    public String getDateOfBirthFormat()
    {
        String result = getISODateFormat();
        try {
            BaseObject settings = getGlobalConfigurationObject();
            result = StringUtils.defaultIfBlank(settings.getStringValue("dateOfBirthFormat"), result);
        } catch (NullPointerException ex) {
            // No value set, return the default
        }
        return result;
    }

    @Override
    public String toString()
    {
        return StringUtils.join(getEnabledSections(), ", ");
    }

    private BaseObject getGlobalConfigurationObject()
    {
        try {
            XWikiContext context = this.xcontextProvider.get();
            return context.getWiki().getDocument(PREFERENCES_LOCATION, context).getXObject(GLOBAL_PREFERENCES_CLASS);
        } catch (XWikiException ex) {
            this.logger.warn("Failed to read preferences: {}", ex.getMessage());
        }
        return null;
    }
}
