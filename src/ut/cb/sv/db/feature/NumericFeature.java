package ut.cb.sv.db.feature;

/**
 * A {@link Feature} with numeric({@link Double)) values.
 * 
 * @version $Id$
 */
public class NumericFeature extends Feature
{
    public NumericFeature(String name)
    {
        super(name);
    }

    /**
     * {@inheritDoc} A value is valid if it is a boolean value
     */
    @Override
    public boolean isValueValid(Object value)
    {
        return super.isValueValid(value) && (value.getClass().equals(Double.class));
    }

    /**
     * {@inheritDoc} Parse the String into a double value. Return null if parsing fails.
     */
    @Override
    public Object processValue(String valueString)
    {
        try {
            return Double.parseDouble(valueString);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String displayValidValues()
    {
        return "Numbers";
    }
}
