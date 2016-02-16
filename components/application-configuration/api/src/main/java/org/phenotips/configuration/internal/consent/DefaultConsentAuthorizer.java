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
package org.phenotips.configuration.internal.consent;

import org.phenotips.configuration.RecordElement;
import org.phenotips.data.Consent;
import org.phenotips.data.ConsentManager;
import org.phenotips.data.Patient;

import org.xwiki.component.annotation.Component;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

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

    @Override public boolean consentsGloballyEnabled()
    {
        return !consentManager.getSystemConsents().isEmpty();
    }

    @Override
    public List<RecordElement> filterForm(List<RecordElement> elements, Patient patient)
    {
        if (!this.consentsGloballyEnabled()) {
            return elements;
        }
        Set<Consent> missingConsents = this.consentManager.getMissingConsentsForPatient(patient);
        if (missingConsents == null) {
            // return an empty list in case of any errors
            return new LinkedList<>();
        }

        if (!containsRequiredConsents(missingConsents)) {
            return new LinkedList<>();
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
        return this.containsRequiredConsents(this.consentManager.getMissingConsentsForPatient(patient));
    }

    @Override
    public boolean authorizeInteraction(Set<String> grantedConsents)
    {
        Set<Consent> systemConsents = this.consentManager.getSystemConsents();
        if (systemConsents == null) {
            return true;
        }
        Set<Consent> missingConsents = new HashSet<Consent>();
        for (Consent consent : systemConsents) {
            if (!grantedConsents.contains(consent.getId())) {
                missingConsents.add(consent);
            }
        }
        return containsRequiredConsents(missingConsents);
    }

    private boolean containsRequiredConsents(Set<Consent> missingConsents)
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
     * @param granted must contain only ids of consents that have a {@link ConsentStatus#YES}.
     */
    private boolean isElementEnabled(RecordElement element, Set<String> missingFields)
    {
        if (missingFields == null) {
            return true;
        }
        if (missingFields.size() == 0 || missingFields.contains(element.getExtension().getId())) {
            return false;
        }
        return true;
    }

    /**
     * Returns the union of all fields which the provided set of missing consents prevents form
     * being used. Returns an empty list of all fields are affected. Returns null if no fields are affected.
     *
     * @param missingConsents a set of presumably not granted consents
     */
    private Set<String> getNonConsentedFieldSet(Set<Consent> missingConsents)
    {
        Set<String> notConsentedFields = new HashSet<String>();
        for (Consent consent : missingConsents) {
            if (consent.affectsAllFields()) {
                // if at least one of the consents affects all fields no point to examine other consents
                // since all fields are affected anyway
                return new HashSet<String>();
            }
            if (!consent.affectsSomeFields()) {
                continue;
            }
            // special cases: affects all fields and affects no fields
            notConsentedFields.addAll(consent.getFields());
        }
        // empty list implies "all affected", null implies "none affected"
        if (notConsentedFields.size() == 0) {
            return null;
        }
        return notConsentedFields;
    }
}
