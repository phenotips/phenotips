package ut.cb.sv.db.feature;

import java.util.LinkedHashMap;

import ut.cb.sv.db.Database;

/**
 * A set of {@link Feature}s describing a certain {@link Database}
 * 
 * @version $Id$
 */
public class FeatureSet extends LinkedHashMap<String, Feature>
{
    /** Reference to the feature in this set which is the classification label of the objects in the database */
    Feature label;

    /**
     * Inserts a new {@link Feature}.
     * 
     * @param f the feature to add
     * @return if a feature with the same name already existed in the feature set, it is removed and returned; otherwise
     *         the function returns null
     * @see Database#addFeature(Feature)
     */
    public Feature addFeature(Feature f)
    {
        if (f.isLabelFeature()) {
            this.label = f;
        }
        return this.put(f.getName(), f);
    }

    /**
     * Adds a new {@link Feature} value to the database's feature set.
     * 
     * @param featureName the feature for which the value needs to be registered
     * @param value the value to register
     * @return true if the feature exists in the feature set and the value, which must be non-null, was successfully
     *         added; false otherwise
     * @see Database#addFeatureValue(String, Object)
     * @see Feature#registerValue(Object)
     */
    public boolean addFeatureValue(String featureName, Object value)
    {
        Feature f = this.get(featureName);
        if (f == null) {
            return false;
        }
        return f.registerValue(value);
    }

    /**
     * Returns the {@link Feature} with the requested name.
     * 
     * @param name the name of the feature to retrieve
     * @return the {@link Feature} with that name, if such a feature exists, or null otherwise
     * @see Database#getFeature(String)
     */
    public Feature getFeature(String name)
    {
        return this.get(name);
    }

    /**
     * Retrieves the feature which is the classification label of the objects in the database
     * 
     * @return the label {@link Feature}
     * @see Database#getLabel();
     */
    public Feature getLabel()
    {
        return this.label;
    }
}
