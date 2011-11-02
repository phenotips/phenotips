/**
 * 
 */
package ut.cb.sv.db.feature;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @version $Id$
 */
public class FeatureValueMapping
{
    public static final String VALUE_MAPPING_FILENAME_MARKER = "@";

    /** Separator between the acceptable feature values in the mapping file */
    private static final String FEATURE_METADATA_VALUES_SEPARATOR = "\\|";

    /** Separates the variants of a feature's value in the mapping file */
    private static final String FEATURE_METADATA_VALUE_VARIANTS_SEPARATOR = "\\s*=\\s*";

    public FeatureValueMapping()
    {
    }

    public Map<String, Object> load(Feature f, String data)
    {
        data = data.trim();
        if (data.startsWith(VALUE_MAPPING_FILENAME_MARKER)) {
            return loadFromFile(f, data.substring(1));
        } else {
            return loadFromString(f, data);
        }
    }

    private Map<String, Object> loadFromString(Feature f, String data)
    {
        Map<String, Object> valueMapping = new LinkedHashMap<String, Object>();
        String values[] = data.split(FEATURE_METADATA_VALUES_SEPARATOR);
        for (String value : values) {
            handleValueGroup(f, valueMapping, value);
        }
        return valueMapping;
    }

    private Map<String, Object> loadFromFile(Feature f, String filename)
    {
        Map<String, Object> valueMapping = new LinkedHashMap<String, Object>();
        BufferedReader in;
        try {
            in = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = in.readLine()) != null) {
                handleValueGroup(f, valueMapping, line);
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return valueMapping;
    }

    private void handleValueGroup(Feature f, Map<String, Object> valueMapping, String group)
    {
        if ("".equals(group.trim())) {
            return;
        }
        String variants[] = group.trim().split(FEATURE_METADATA_VALUE_VARIANTS_SEPARATOR);
        if (variants.length == 0) {
            return;
        }
        Set<String> values = getValuesFromList(variants[0]);
        for (String val : values) {
            valueMapping.put(val, val);
            f.registerValue(val);
        }

        for (int i = 1; i < variants.length; ++i) {
            valueMapping.put(variants[i], values);
        }
    }

    public static Set<String> getValuesFromList(String list)
    {
        Set<String> result = new LinkedHashSet<String>();
        String values[] = list.trim().split("\\s*[,]\\s*");
        for (String value : values) {
            result.add(value);
        }
        return result;
    }
}
