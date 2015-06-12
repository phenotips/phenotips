package org.phenotips.configuration.internal.configured;

import org.phenotips.configuration.ConsentManager;
import org.phenotips.configuration.RecordElement;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Used to disable/enable {@link org.phenotips.configuration.RecordElement}s based on patient consent.
 */
public class ConfiguredConsentManager implements ConsentManager
{
    private List<String> consentedElements = new LinkedList<>();

    /**
     *
     * @param patient could be null. If null, assumes no consent given.
     */
    public ConfiguredConsentManager(Patient patient) {
        PatientData<String> consent = patient.getData("consent");
        new LinkedList<>();
    }

    @Override
    public Set<RecordElement> filter(Collection<RecordElement> elements)
    {
        Set<RecordElement> consented = new HashSet<>();
        for (RecordElement element : elements) {
            if (this.hasConsent(element)) {
                consented.add(element);
            }
        }
        return consented;
    }

    @Override
    public boolean noConsentPresent()
    {
        return this.consentedElements.isEmpty();
    }

    private boolean hasConsent(RecordElement element) {
        return true;
    }
}