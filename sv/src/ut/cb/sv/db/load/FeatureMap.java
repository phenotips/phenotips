package ut.cb.sv.db.load;

import java.util.Collection;
import java.util.Set;

import ut.cb.sv.db.feature.Feature;

public interface FeatureMap
{
    public static final String MAP_FIELD_SEPARATOR = "\\s+";

    public static final String IGNORED_LINE_MARKER = "##";

    void onConstructionStart();

    boolean onProcessLineStart(String line);

    void onProcessLineEnd(String originalID, Feature feature);

    void onConstructionEnd();

    boolean accepts(String originalName, String entryValue);

    Set<Object> getOutputValue(String featureName, String inputValue);

    String getFeatureNameForOriginalName(String originalName);

    Feature getFeatureForOriginalName(String originalName);

    Feature getFeatureForName(String name);

    Collection<String> getFeatureNames();

    Collection<Feature> getFeatures();
}
