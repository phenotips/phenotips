package org.phenotips.configuration.internal.configured;

import java.util.*;

/**
 * Configuration for what {@link org.phenotips.configuration.RecordElement}s are enabled given the consent of a patient.
 */
public class ConsentConfiguration {
    private Map<String, List<String>> configuration = new HashMap<>();

    public ConsentConfiguration() {
        List<String> real = getRealConsentDependants();
        List<String> genetic = getGeneticConsentDependants();
        List<String> share = getShareConsentDependants();
        List<String> shareImages = getShareImagesConsentDependants();
        List<String> matching = getMatchingConsentDependants();

        configuration.put("real", real);
        configuration.put("genetic", genetic);
        configuration.put("share", share);
        configuration.put("share_images", shareImages);
        configuration.put("matching", matching);
    }

    public Map<String, List<String>> getConfiguration()
    {
        return Collections.unmodifiableMap(configuration);
    }

    private List<String> getRealConsentDependants(){
        List<String> dependants = Arrays.asList();

        return dependants;
    }

    private List<String> getGeneticConsentDependants(){
        List<String> dependants = Arrays.asList();

        return dependants;
    }

    private List<String> getShareConsentDependants(){
        List<String> dependants = Arrays.asList();

        return dependants;
    }

    private List<String> getShareImagesConsentDependants(){
        List<String> dependants = Arrays.asList();

        return dependants;
    }

    private List<String> getMatchingConsentDependants(){
        List<String> dependants = Arrays.asList();

        return dependants;
    }
}
