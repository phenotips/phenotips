package ut.cb.sv.db.feature;

import ut.cb.sv.db.Database;
import ut.cb.sv.db.DatabaseEntry;

/**
 * Describes an atomic piece of information about an object in a database.
 * 
 * @version $Id$
 */
public class Feature
{
    /** The name of the feature */
    protected final String name;

    /**
     * The number of characters necessary to display any value of this feature. Used when pretty-printing a database, by
     * {@link Database#prettyPrint(java.io.PrintStream)}.
     * 
     * @see Database#prettyPrint(java.io.PrintStream)
     */
    protected int maxValueSize = 0;

    /** Creates an empty feature with a given name. */
    public Feature(String name)
    {
        this.name = name;
    }

    /**
     * Access to the name of the feature
     * 
     * @return the name
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * Register a value encountered for this feature.
     * 
     * @param value
     * @return @true if the value is valid and was successfully registered, @false otherwise
     */
    public boolean registerValue(Object value)
    {
        if (value == null) {
            return false;
        }
        this.maxValueSize = Math.max(this.maxValueSize, (value.toString()).length());
        return true;
    }

    /**
     * Checks the validity of a value for the current feature.
     * 
     * @param value the value to check
     * @return @true of the value is valid, @false otherwise
     */
    public boolean isValueValid(Object value)
    {
        return value != null;
    }

    /**
     * Processes a string to retrieve a feature value
     * 
     * @param valueString the string to process
     * @return an the processed value
     */
    public Object preProcessValue(final String valueString)
    {
        return valueString;
    }

    /**
     * Processes a string to retrieve a feature value
     * 
     * @param valueString the string to process
     * @return an the processed value
     */
    public Object postProcessValue(final String valueString)
    {
        return valueString;
    }

    /**
     * Get the number of characters necessary to display any value of this feature. Used when pretty-printing a
     * database, by {@link Database#prettyPrint(java.io.PrintStream)}.
     * 
     * @return the field {@link Feature#maxValueSize}
     * @see Database#prettyPrint(java.io.PrintStream)
     */
    public int getMaxValueSize()
    {
        return this.maxValueSize;
    }

    /**
     * Get the number of characters necessary to display any value of this feature, as well as its label. Used when
     * pretty-printing a database, by {@link Database#prettyPrint(java.io.PrintStream)}.
     * 
     * @return the maximum between {@link Feature#maxValueSize} and the length of the feature name
     * @see Database#prettyPrint(java.io.PrintStream)
     */
    public int getMaxFieldSize()
    {
        return Math.max(this.name.length(), this.getMaxValueSize());
    }

    /**
     * Gives information about the values that are valid for this feature. For display purposes only, not intended for
     * further processing.
     * 
     * @return a {@link String} containing information about acceptable values.
     */
    public String displayValidValues()
    {
        return "ANY";
    }

    /**
     * States whether this feature can hold the classification label of a {@link DatabaseEntry}.By default, features are
     * not label features.
     * 
     * @return false
     * @see {@link LabelFeature}
     */
    public boolean isLabelFeature()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
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
        Feature other = (Feature) obj;
        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
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
        return this.getClass().getSimpleName() + "[name=" + this.name + ", values=" + this.displayValidValues() + "]\n";
    }
}
