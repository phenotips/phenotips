package org.phenotips.configuration.internal.configured;

import org.phenotips.configuration.ConsentTracker;
import org.phenotips.configuration.RecordElement;
import org.phenotips.data.Patient;

import java.util.LinkedList;
import java.util.List;

/**
 * Used to disable/enable {@link org.phenotips.configuration.RecordElement}s based on patient consent.
 */
public class ConfiguredConsentManager implements ConsentTracker {


    private List<String> consentedElements = new LinkedList<>();

    /**
     *
     * @param patient could be null. If null, assumes no consent given.
     */
    public ConfiguredConsentManager(Patient patient) {

    }

    @Override
    public boolean hasConsent(RecordElement element) {
        return false;
    }
}