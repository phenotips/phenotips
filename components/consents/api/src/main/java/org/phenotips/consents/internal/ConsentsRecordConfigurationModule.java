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
package org.phenotips.consents.internal;

import org.phenotips.configuration.RecordConfiguration;
import org.phenotips.configuration.RecordElement;
import org.phenotips.configuration.RecordSection;
import org.phenotips.configuration.spi.RecordConfigurationModule;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.ArrayUtils;

/**
 * Implementation of {@link RecordConfiguration} that disables all elements that require a consent which hasn't been
 * granted. Its {@link #getPriority() priority} is {@code 90}.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("consents")
@Singleton
public class ConsentsRecordConfigurationModule implements RecordConfigurationModule
{
    private static final String PATIENT_TYPE_LABEL = "patient";

    private static final String[] SUPPORTED_RECORD_TYPES = new String[] { PATIENT_TYPE_LABEL };

    @Inject
    private PatientRepository patients;

    @Inject
    private DocumentAccessBridge dab;

    @Inject
    private ConsentAuthorizer consentAuthorizer;

    @Override
    public RecordConfiguration process(RecordConfiguration config)
    {
        if (config == null || this.dab.getCurrentDocumentReference() == null) {
            return config;
        }

        final Patient patient = this.patients.get(this.dab.getCurrentDocumentReference());
        if (patient == null) {
            return config;
        }

        for (final RecordSection section : config.getEnabledSections()) {
            // Filter elements by consents.
            final List<RecordElement> enabledElements = Collections.unmodifiableList(section.getEnabledElements());
            final List<RecordElement> consentedElements = this.consentAuthorizer.filterForm(enabledElements, patient);
            disableNonConsentedElements(enabledElements, consentedElements);
            disableSectionIfNoEnabledElements(section);
        }

        return config;
    }

    /**
     * Disables {@code section} if it contains no enabled elements.
     *
     * @param section the patient record section
     */
    private void disableSectionIfNoEnabledElements(@Nonnull final RecordSection section)
    {
        if (section.getEnabledElements().isEmpty()) {
            section.setEnabled(false);
        }
    }

    /**
     * Given a list of enabled section elements and a list of consented section elements, disables any elements in
     * {@code enabledElements} that do not have the required consents.
     *
     * @param enabledElements the section elements that are currently enabled
     * @param consentedElements the section elements that have all the required consents
     */
    private void disableNonConsentedElements(@Nonnull final List<RecordElement> enabledElements,
        @Nonnull final List<RecordElement> consentedElements)
    {
        for (final RecordElement element : enabledElements) {
            if (!consentedElements.contains(element)) {
                element.setEnabled(false);
            }
        }
    }

    @Override
    public int getPriority()
    {
        return 90;
    }

    @Override
    public boolean supportsRecordType(String recordType)
    {
        return ArrayUtils.contains(SUPPORTED_RECORD_TYPES, recordType);
    }
}
