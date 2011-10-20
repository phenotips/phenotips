/**
 * 
 */
package ut.cb.sv.db.feature;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

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

    public Map<String, String> load(Feature f, String data)
    {
        data = data.trim();
        if (data.startsWith(VALUE_MAPPING_FILENAME_MARKER)) {
            return loadFromFile(f, data.substring(1));
        } else {
            return loadFromString(f, data);
        }
    }

    private Map<String, String> loadFromString(Feature f, String data)
    {
        Map<String, String> valueMapping = new LinkedHashMap<String, String>();
        String values[] = data.split(FEATURE_METADATA_VALUES_SEPARATOR);
        for (String value : values) {
            handleValueGroup(f, valueMapping, value);
        }
        return valueMapping;
    }

    private Map<String, String> loadFromFile(Feature f, String filename)
    {
        Map<String, String> valueMapping = new LinkedHashMap<String, String>();
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

    private void handleValueGroup(Feature f, Map<String, String> valueMapping, String group)
    {
        String variants[] = group.split(FEATURE_METADATA_VALUE_VARIANTS_SEPARATOR);
        if (variants.length == 1) {
            valueMapping.put(variants[0], variants[0]);
        } else {
            for (int i = 0; i < variants.length; ++i) {
                valueMapping.put(variants[i], variants[0]);
            }
        }
        if (variants.length > 0 && variants[0] != null) {
            f.registerValue(variants[0]);
        }
    }
}
