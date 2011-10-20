package ut.cb.sv.db.feature;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

import ut.cb.sv.db.Database;

/**
 * A factory of {@link Feature}s
 * 
 * @version $Id$
 */
public class FeatureFactory
{
    /** The generic feature class name prefix */
    protected static final String GENERIC_TYPE = "";

    /** The default feature type */
    public static final String DEFAULT_TYPE = GENERIC_TYPE;

    /** Separator between the feature type and feature value list in the mapping file */
    private static final String FEATURE_METADATA_TYPE_VALUE_SEPARATOR = ":";

    /** The package of the feature classes */
    protected static final String DEFAULT_FEATURE_CLASS_PREFIX = Feature.class.getPackage().getName();

    /** The suffix of all feature class names */
    protected static final String DEFAULT_FEATURE_CLASS_SUFFIX = Feature.class.getSimpleName();

    /** Mapping between the values encountered in the input database file and the value to be stored and process */
    private Map<String, String> valueMapping = new LinkedHashMap<String, String>();

    /**
     * Creates an instance of {@link Feature} of a certain type
     * 
     * @param name the name of the feature
     * @param type the type of the feature, which is the prefix of the name of the feature class to instantiate
     * @return A feature object of a class derived from {@link Feature} as indicated by the type parameter. If the type
     *         is not valid, a instance of {@link Feature} is returned.
     */
    public final Feature getFeatureInstance(String name, String type)
    {
        try {
            return (Feature) (Class.forName(DEFAULT_FEATURE_CLASS_PREFIX + '.' + type + DEFAULT_FEATURE_CLASS_SUFFIX)
                .getConstructor(String.class).newInstance(name));
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            return new Feature(name);// getFeatureInstance(name, DEFAULT_TYPE);
        } catch (InstantiationException ex) {
            ex.printStackTrace();
            return null;
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
            return null;
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            return null;
        } catch (SecurityException ex) {
            ex.printStackTrace();
            return null;
        } catch (InvocationTargetException ex) {
            ex.printStackTrace();
            return null;
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a feature of a given name, with the type and possible values to be retrieved from a string
     * 
     * @param name the name of the feature to create
     * @param _metadata a string of the form
     *            [FeatureType[:value1ToStore[=value1fromInputFile][|value2ToStore[=value2fromInputFile]]...]
     * @return A feature object of a class derived from {@link Feature} as indicated by the type in the _metadata
     *         parameter, with values set as indicated by the _metadata parameter. If the type is not valid, a instance
     *         of {@link Feature} is returned.
     */
    public Feature getFeatureInstanceFromString(String name, String _metadata)
    {
        Feature f = null;
        this.valueMapping.clear();
        String metadata = _metadata.trim();
        int sepPosition = metadata.indexOf(FEATURE_METADATA_TYPE_VALUE_SEPARATOR);
        if (sepPosition < 0) {
            return getFeatureInstance(name, metadata);
        }
        String type = metadata.substring(0, sepPosition).trim();
        f = getFeatureInstance(name, type);
        this.valueMapping = new FeatureValueMapping().load(f, metadata.substring(sepPosition + 1));
        return f;
    }

    /**
     * Immediately after parsing a feature from an input string, the factory stores the value mapping read from that
     * string if any (mapping between the values read from the input file and the values that should actually be stored
     * in the {@link Database} to be processed. This function retrieves the value mapping of the last {@link Feature}
     * created by this factory.
     * 
     * @return the {@link #valueMapping} field, populated by {@link #getFeatureInstanceFromString(String, String)}.
     * @see #getFeatureInstanceFromString(String, String)
     */
    public Map<String, String> getLastValueMapping()
    {
        Map<String, String> clone = new LinkedHashMap<String, String>();
        clone.putAll(this.valueMapping);
        return clone;
    }
}
