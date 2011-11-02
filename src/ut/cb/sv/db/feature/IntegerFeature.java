package ut.cb.sv.db.feature;


public class IntegerFeature extends Feature
{
    public IntegerFeature(String name)
    {
        super(name);
    }

    /**
     * {@inheritDoc} A value is valid if it is an integer value
     */
    @Override
    public boolean isValueValid(Object value)
    {
        return super.isValueValid(value) && (value.getClass().equals(Integer.class));
    }

    /**
     * {@inheritDoc} Parse the String into an integer value. Return null if parsing fails.
     */
    @Override
    public Integer postProcessValue(final String valueString)
    {
        try {
            return Integer.parseInt(valueString);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String displayValidValues()
    {
        return "Integer umbers";
    }

}
