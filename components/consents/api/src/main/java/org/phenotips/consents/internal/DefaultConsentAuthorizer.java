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

import org.phenotips.configuration.RecordElement;
import org.phenotips.consents.Consent;
import org.phenotips.consents.ConsentManager;
import org.phenotips.data.Patient;

import org.xwiki.component.annotation.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;

/**
 * The default implementation of a {@link ConsentAuthorizer}. This is a temporary implementation, as the whole
 * application-configuration section must be redesigned.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Component
@Singleton
public class DefaultConsentAuthorizer implements ConsentAuthorizer
{
    @Inject
    private ConsentManager consentManager;

    @Override
    public boolean consentsGloballyEnabled()
    {
        return !this.consentManager.getSystemConsents().isEmpty();
    }

    @Override
    public List<RecordElement> filterForm(List<RecordElement> elements, Patient patient)
    {
        if (CollectionUtils.isEmpty(elements)) {
            return Collections.emptyList();
        }

        if (!this.consentsGloballyEnabled() || patient == null) {
            return elements;
        }

        Set<Consent> missingConsents = this.consentManager.getMissingConsentsForPatient(patient);
        // If !containsRequiredConsents(missingConsents) is true, then one of the consents is mandatory, which means
        // that no sections should be displayed; missingConsents will only be null if patient is null.
        if (missingConsents == null || !containsRequiredConsents(missingConsents)) {
            return Collections.emptyList();
        }

        Set<String> nonConsentedFields = this.getNonConsentedFieldSet(missingConsents);

        List<RecordElement> updatedElements = new LinkedList<>();
        for (RecordElement element : elements) {
            if (this.isElementEnabled(element, nonConsentedFields)) {
                updatedElements.add(element);
            }
        }
        return updatedElements;
    }

    @Override
    public boolean authorizeInteraction(Patient patient)
    {
        return (patient != null)
            && this.containsRequiredConsents(this.consentManager.getMissingConsentsForPatient(patient));
    }

    @Override
    public boolean authorizeInteraction(Set<String> grantedConsents)
    {
        Set<Consent> systemConsents = this.consentManager.getSystemConsents();
        if (CollectionUtils.isEmpty(systemConsents)) {
            return true;
        }

        if (CollectionUtils.isEmpty(grantedConsents)) {
            return containsRequiredConsents(systemConsents);
        }

        Set<Consent> missingConsents = new HashSet<>();
        for (Consent consent : systemConsents) {
            if (!grantedConsents.contains(consent.getId())) {
                missingConsents.add(consent);
            }
        }
        return containsRequiredConsents(missingConsents);
    }

    /**
     * Given a set of missing consents for a {@link Patient} object, returns true iff none of the
     * {@code missingConsents} is required ({@link Consent#isRequired()}), false otherwise.
     *
     * @param missingConsents a set of missing {@link Consent} objects
     * @return true iff none of the {@code missingConsents} are required, false otherwise
     */
    private boolean containsRequiredConsents(@Nonnull Set<Consent> missingConsents)
    {
        for (Consent consent : missingConsents) {
            if (consent.isRequired()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isElementConsented(RecordElement element, Patient patient)
    {
        Set<Consent> missingConsents = this.consentManager.getMissingConsentsForPatient(patient);
        return this.isElementEnabled(element, this.getNonConsentedFieldSet(missingConsents));
    }

    /**
     * Given a form element and a set of non consented elements, returns true iff {@code element} is enabled, false
     * otherwise.
     *
     * @param element the {@link RecordElement} being examined
     * @param nonConsentedFields IDs of field that have not received the required consents
     * @return true iff {@code element} is not part of {@code nonConsentedFields}, false otherwise
     */
    private boolean isElementEnabled(@Nonnull RecordElement element, @Nonnull Set<String> nonConsentedFields)
    {
        return !(nonConsentedFields == null) && !nonConsentedFields.contains(element.getExtension().getId());
    }

    /**
     * Given a set of {@code missingConsents missing consents}, returns the union of all {@link Consent#getFields()
     * fields}, which the provided set of {@code missingConsents} prevents from being used. Returns an empty set if no
     * fields are affected.
     *
     * @param missingConsents a set of not granted {@link Consent} consents.
     * @return {@code null} if mandatory consents are missing, an empty list of no fields are missing consents, or a
     *         list of field names that are missing a required consent
     */
    private Set<String> getNonConsentedFieldSet(@Nonnull Set<Consent> missingConsents)
    {
        Set<String> notConsentedFields = new HashSet<>();
        for (Consent consent : missingConsents) {
            if (consent.affectsAllFields() || consent.isRequired()) {
                // if at least one of the consents affects all fields no point to examine other consents
                // since all fields are affected anyway
                return null;
            }
            if (!consent.affectsSomeFields()) {
                continue;
            }
            notConsentedFields.addAll(consent.getFields());
        }
        return notConsentedFields;
    }
}
