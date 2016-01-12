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
import org.phenotips.data.ConsentStatus;
import org.phenotips.data.Patient;

import org.xwiki.component.annotation.Component;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.codec.binary.StringUtils;

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
        List<Consent> consents = this.consentManager.loadConsentsFromPatient(patient);
        if (!authorizeInteraction(consents)) {
            return new LinkedList<>();
        }

        List<String> granted = getGrantedIds(consents);
        List<RecordElement> updatedElements = new LinkedList<>();
        for (RecordElement element : elements) {
            if (this.isElementEnabled(element, granted)) {
                updatedElements.add(element);
            }
        }
        return updatedElements;
    }

    @Override
    public boolean authorizeInteraction(Patient patient)
    {
        List<Consent> consents = this.consentManager.loadConsentsFromPatient(patient);
        return this.authorizeInteraction(consents);
    }

    @Override public boolean authorizeInteraction(Iterable<String> grantedConsents)
    {
        for (Consent consent : this.consentManager.getSystemConsents()) {
            boolean found = false;
            if (consent.isRequired()) {
                for (String granted : grantedConsents) {
                    if (StringUtils.equals(granted, consent.getId())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean authorizeInteraction(List<Consent> consents)
    {
        return !missingRequired(consents);
    }

    /** Should not display a form if any of the required consents are missing. */
    private static boolean missingRequired(List<Consent> consents)
    {
        for (Consent consent : consents) {
            if (consent.isRequired() && consent.getStatus() != ConsentStatus.YES) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isElementConsented(RecordElement element, Patient patient)
    {
        List<Consent> consents = this.consentManager.loadConsentsFromPatient(patient);
        return this.isElementEnabled(element, getGrantedIds(consents));
    }

    /**
     * @param granted must contain only ids of consents that have a {@link ConsentStatus#YES}.
     */
    private boolean isElementEnabled(RecordElement element, List<String> granted)
    {
        String requiredConsentsString = element.getExtension().getParameters().get("required_consents");
        if (requiredConsentsString != null) {
            String[] requiredConsents = requiredConsentsString.split(",");
            for (String requiredConsent : requiredConsents) {
                if (!granted.contains(requiredConsent.trim())) {
                    return false;
                }
            }
        }
        return true;
    }

    private static List<String> getGrantedIds(List<Consent> consents)
    {
        List<String> ids = new LinkedList<>();
        for (Consent consent : consents) {
            if (consent.getStatus() == ConsentStatus.YES) {
                ids.add(consent.getId());
            }
        }
        return ids;
    }
}
