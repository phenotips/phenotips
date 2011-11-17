package ut.cb.sv.db.feature;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import ut.cb.util.maps.CounterMap;

/**
 * A {@link Feature} with a finite set of values (categories).
 * 
 * @version $Id$
 */
public class CategoryFeature extends Feature
{
    /** The categories */
    Set<String> values = new LinkedHashSet<String>();

    protected CounterMap<String> encounteredValues = new CounterMap<String>();

    /** {@inheritDoc} */
    public CategoryFeature(String name)
    {
        super(name);
    }

    /**
     * @return the values
     */
    public Set<String> getValues()
    {
        return this.values;
    }

    /**
     * {@inheritDoc} A value is valid for a categorical feature if it appears in the {{@link #values} set or if no
     * values have been provided (the {@link #values} set is empty).
     */
    @Override
    public boolean isValueValid(Object value)
    {
        // if (this.values.isEmpty()) {
        this.encounteredValues.addTo(value.toString());
        // }
        return super.isValueValid(value) && (this.values.isEmpty() || this.values.contains(value));
    }

    /**
     * {@inheritDoc} Upon registration into a {@link CategoryFeature}, a value is inserted in thr {@link #values} set of
     * this feature.
     */
    @Override
    public boolean registerValue(Object value)
    {
        if (super.registerValue(value)) {
            return this.values.add(value + "");
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Object postProcessValue(String valueString)
    {
        if (isValueValid(valueString)) {
            return valueString;
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String displayValidValues()
    {
        return this.values.toString();
    }

    public Set<String> getEncounteredValues()
    {
        return this.encounteredValues.keySet();

    }

    public CounterMap<String> getEncounteredValueCounters()
    {
        CounterMap<String> clone = new CounterMap<String>();
        clone.putAll(this.encounteredValues);
        return clone;

    }

    public int getValueCounter(String value)
    {
        try {
            return this.encounteredValues.get(value);
        } catch (NullPointerException ex) {
            return 0;
        }

    }

    public Set<String> getFrequentValues(int threshold)
    {
        Set<String> result = new HashSet<String>();
        for (String value : this.getEncounteredValues()) {
            if (this.getValueCounter(value) >= threshold) {
                result.add(value);
            }
        }
        return result;
    }

    public Set<String> getUnfrequentValues(int threshold)
    {
        Set<String> result = new HashSet<String>();
        for (String value : this.getEncounteredValues()) {
            if (this.getValueCounter(value) < threshold) {
                result.add(value);
            }
        }
        return result;
    }
}
