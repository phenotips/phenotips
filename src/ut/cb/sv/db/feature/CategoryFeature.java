package ut.cb.sv.db.feature;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A {@link Feature} with a finite set of values (categories).
 * 
 * @version $Id$
 */
public class CategoryFeature extends Feature
{
    /** The categories */
    Set<String> values = new LinkedHashSet<String>();

    public Set<String> encounteredValues = new LinkedHashSet<String>();

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
        this.encounteredValues.add(value.toString());
        // }
        return super.isValueValid(value) && (this.values.isEmpty() || this.values.contains(value));
    }

    /**
     * {@inheritDoc} Upon registration into a {@link CategoryFeature}, a value is inserted in thr {@link #values} set
     * of this feature.
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
}
