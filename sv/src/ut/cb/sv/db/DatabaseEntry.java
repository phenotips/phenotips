package ut.cb.sv.db;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import ut.cb.sv.db.feature.Feature;
import ut.cb.sv.db.feature.LabelFeature;

/**
 * Am object described by a certain set of {@link Feature}s.
 * 
 * @version $Id$
 */
public class DatabaseEntry
{
    /** The entry's fields, as a map associating a value to ach feature name */
    private Map<String, Object> features;

    /** The entry's classification label */
    private Object label;

    /**
     * Builds an empty entry, with no features and no label.
     */
    public DatabaseEntry()
    {
        this.features = new LinkedHashMap<String, Object>();
    }

    /**
     * Sets the value of a field in this entry. if it is a {@link LabelFeature}, the value becomes the classification
     * label of the entry.
     * 
     * @param f the {@link Feature} to set value for
     * @param value the value
     * @return the previous value for that feature/label in the entry if a previous value exists, or null otherwise
     */
    public Object addFeature(Feature f, Object value)
    {
        if (!f.isValueValid(value)) {
            System.out.println(new IllegalArgumentException("<" + value + "> is not a valid value for feature "
                + f.getName()
                + ". Accepted values: " + f.displayValidValues()).getMessage());
        }
        if (f.isLabelFeature()) {
            Object previousLabel = this.label;
            setLabel(value);
            return previousLabel;
        }
        return this.features.put(f.getName(), f.prepareValueForEntry(this, value));
    }

    /**
     * Sets the label of the entry.
     * 
     * @param value the value to set
     */
    protected void setLabel(Object value)
    {
        this.label = value;
    }

    /**
     * Retrieves the value this entry has for the feature with the given name
     * 
     * @param name the feature's name
     * @return the value for that feature
     */
    public Object get(String name)
    {
        return this.features.get(name);
    }

    /**
     * Retrieves the classification label of this entry
     * 
     * @return the label or null if no label was set
     */
    protected Object getLabel()
    {
        return this.label;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DatabaseEntry clone()
    {
        DatabaseEntry clone = new DatabaseEntry();
        clone.features.putAll(this.features);
        clone.label = this.label;
        return clone;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.features == null) ? 0 : this.features.hashCode());
        result = prime * result + ((this.label == null) ? 0 : this.label.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DatabaseEntry other = (DatabaseEntry) obj;
        if (this.features == null) {
            if (other.features != null) {
                return false;
            }
        } else if (!this.features.equals(other.features)) {
            return false;
        }
        if (this.label == null) {
            if (other.label != null) {
                return false;
            }
        } else if (!this.label.equals(other.label)) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        StringBuilder str = new StringBuilder();
        int size = 0;
        for (String feature : this.features.keySet()) {
            if (feature.length() > size) {
                size = feature.length();
            }
        }

        for (String feature : this.features.keySet()) {
            str.append(String.format("%" + size + "s : %s\n", feature, this.features.get(feature)));
        }
        return str.toString();
    }

    /**
     * Format the entry for exporting the database in csv format.
     * 
     * @param featureNames an ordered set giving the feature names
     * @param separator the field separator
     * @return a the formatted entry as a String
     */
    public String format(Set<String> featuresNames, String separator)
    {
        StringBuilder str = new StringBuilder();
        for (String feature : featuresNames) {
            Object value = this.features.get(feature);
            str.append(separator).append(value == null ? this.label : value);
        }
        return str.substring(separator.length());
    }
}
