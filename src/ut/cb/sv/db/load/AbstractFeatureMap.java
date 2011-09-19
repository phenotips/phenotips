package ut.cb.sv.db.load;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import ut.cb.sv.db.feature.Feature;
import ut.cb.sv.db.feature.FeatureFactory;
import ut.cb.sv.db.load.filter.EntryFilter;
import ut.cb.sv.db.load.filter.EntryFilterFactory;

public abstract class AbstractFeatureMap implements FeatureMap
{
    protected static final int MIN_COMPONENTS_PER_LINE = 2;

    protected static final int MAX_COMPONENTS_PER_LINE = 3;

    protected FeatureFactory featureFactory = new FeatureFactory();

    protected final Map<String, Feature> originalNameFeatureMap;

    protected final Map<String, Feature> nameFeatureMap;

    protected final Map<String, Map<String, String>> featureValueMap;

    protected final static EntryFilterFactory filterFactory = new EntryFilterFactory();

    protected final Set<EntryFilter> filters = new LinkedHashSet<EntryFilter>();

    public AbstractFeatureMap(String mappingFileName)
    {
        this.onConstructionStart();
        this.originalNameFeatureMap = new LinkedHashMap<String, Feature>();
        this.nameFeatureMap = new LinkedHashMap<String, Feature>();
        this.featureValueMap = new LinkedHashMap<String, Map<String, String>>();
        EntryFilter filter;
        try {
            BufferedReader input = new BufferedReader(new FileReader(mappingFileName));
            String line = null;
            while ((line = input.readLine()) != null) {
                if (line.startsWith(IGNORED_LINE_MARKER)) {
                    continue;
                }
                if (!this.onProcessLineStart(line)) {
                    continue;
                }
                if ((filter = filterFactory.createFilter(line)) != null) {
                    this.filters.add(filter);
                    continue;
                }
                String parts[] = line.trim().split(MAP_FIELD_SEPARATOR, MAX_COMPONENTS_PER_LINE);
                if (parts.length < MIN_COMPONENTS_PER_LINE) {
                    continue;
                }
                String originalName = parts[0];
                String name = parts[1];
                String metadata = null;
                if (parts.length > MIN_COMPONENTS_PER_LINE) {
                    metadata = parts[MIN_COMPONENTS_PER_LINE];
                } else {
                    metadata = FeatureFactory.DEFAULT_TYPE;
                }
                Feature feature = this.featureFactory.getFeatureInstanceFromString(name, metadata);
                this.originalNameFeatureMap.put(originalName, feature);
                this.nameFeatureMap.put(feature.getName(), feature);
                this.featureValueMap.put(name, this.featureFactory.getLastValueMapping());
                this.onProcessLineEnd(originalName, feature);
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        this.onConstructionEnd();
    }

    public void onConstructionStart()
    {
    }

    public boolean onProcessLineStart(String line)
    {
        return true;
    }

    public void onProcessLineEnd(String originalID, Feature feature)
    {
    }

    public void onConstructionEnd()
    {
    }

    public boolean accepts(String originalName, String entryValue)
    {
        boolean accepts = true;
        for (EntryFilter filter : this.filters) {
            accepts = accepts && filter.accepts(originalName, entryValue);
            if (!accepts) {
                return false;
            }
        }
        return accepts;
    }

    public Object getOutputValue(String featureName, String inputValue)
    {
        try {
            Feature f = this.getFeatureForName(featureName);
            String outputValue = this.featureValueMap.get(featureName).get(inputValue);
            if (outputValue == null) {
                outputValue = inputValue;
            }
            return f.processValue(outputValue);
        } catch (NullPointerException ex) {
            System.out.print(featureName + " --- " + inputValue + " ===> null");
            ex.printStackTrace();
            System.exit(0);
            return null;
        }
    }

    public String getFeatureNameForOriginalName(String originalName)
    {
        try {
            return this.originalNameFeatureMap.get(originalName).getName();
        } catch (NullPointerException ex) {
            return null;
        }
    }

    public Feature getFeatureForOriginalName(String originalName)
    {
        return this.originalNameFeatureMap.get(originalName);
    }

    public Feature getFeatureForName(String name)
    {
        return this.nameFeatureMap.get(name);
    }

    public Collection<String> getFeatureNames()
    {
        return this.originalNameFeatureMap.keySet();
    }

    public Collection<Feature> getFeatures()
    {
        return this.originalNameFeatureMap.values();
    }
}
