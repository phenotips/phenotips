package org.phenotips.configuration.internal.consent;

import org.phenotips.configuration.RecordElement;
import org.phenotips.data.Consent;
import org.phenotips.data.ConsentManager;
import org.phenotips.data.ConsentStatus;
import org.phenotips.data.Patient;

import org.xwiki.component.annotation.Component;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 */
@Component
@Singleton
public class DefaultConsentAuthorizer implements ConsentAuthorizer
{
    @Inject
    private ConsentManager consentManager;

    @Override
    public List<RecordElement> filterForm(List<RecordElement> elements, Patient patient)
    {
        List<Consent> consents = this.consentManager.loadConsentsFromPatient(patient);
        if (!authorizeInteraction(consents)) {
            return new LinkedList<>();
        }

        List<String> granted = getGrantedIds(consents);
        Iterator<RecordElement> iterator = elements.iterator();
        while(iterator.hasNext()) {
            RecordElement element = iterator.next();
            if (!this.isEnabled(element, patient, granted)) {
                iterator.remove();
            }
        }
        return elements;
    }

    @Override
    public boolean authorizeInteraction(Patient patient)
    {
        List<Consent> consents = this.consentManager.loadConsentsFromPatient(patient);
        return this.authorizeInteraction(consents);
    }

    private boolean authorizeInteraction(List<Consent> consents)
    {
        return !missingRequired(consents);
    }

    /** Should not display a form if any of the required consents are missing. */
    private static boolean missingRequired(List<Consent> consents)
    {
        for (Consent consent : consents)
        {
            if (consent.isRequired() && consent.getStatus() != ConsentStatus.YES) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isElementEnabled(RecordElement element, Patient patient) {
        List<Consent> consents = this.consentManager.loadConsentsFromPatient(patient);
        return this.isEnabled(element, patient, getGrantedIds(consents));
    }

    /**
     * @param granted must contain only ids of consents that have a {@link ConsentStatus#YES}.
     */
    private boolean isEnabled(RecordElement element, Patient patient, List<String> granted)
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
                ids.add(consent.getID());
            }
        }
        return ids;
    }
}
