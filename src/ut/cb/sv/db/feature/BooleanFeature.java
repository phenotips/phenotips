package ut.cb.sv.db.feature;

/**
 * A {@link Feature} with boolean values.
 * 
 * @version $Id$
 */
public class BooleanFeature extends Feature
{
    /** {@inheritDoc} */
    public BooleanFeature(String name)
    {
        super(name);
    }

    /**
     * {@inheritDoc} A value is valid if it is a boolean value
     */
    @Override
    public boolean isValueValid(Object value)
    {
        return super.isValueValid(value)
            && (value.getClass().equals(Boolean.class) || value.equals(0) || value.equals(1));
    }

    /**
     * {@inheritDoc} Boolean values cannot be registered.
     */
    @Override
    public boolean registerValue(Object value)
    {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxValueSize()
    {
        return Math.max(Boolean.TRUE.toString().length(), Boolean.FALSE.toString().length());
    }

    /**
     * {@inheritDoc} Parse the String into a boolean value. Return null if parsing fails.
     */
    @Override
    public Object processValue(String valueString)
    {
        try {
            return Boolean.parseBoolean(valueString);
        } catch (Exception ex) {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String displayValidValues()
    {
        return Boolean.TRUE.toString() + ", " + Boolean.FALSE.toString();
    }
}
