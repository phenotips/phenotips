package ut.cb.sv.db;

import java.io.File;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import ut.cb.sv.db.feature.Feature;
import ut.cb.sv.db.feature.FeatureSet;

/**
 * Database containing a collection of objects described by the same set of {@link Feature}s.
 * 
 * @version $Id$
 */
public class Database extends LinkedList<DatabaseEntry>
{
    /** The name of the database */
    String name = "";

    /** The source of the database (a file path) */
    String source = "";

    /** Features of the objects in the database */
    FeatureSet featureSet = new FeatureSet();

    /**
     * @return the name
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * @return the source
     */
    public String getSource()
    {
        return this.name;
    }

    public void setIdentity(String source)
    {
        this.source = source;
        File sourceFile = new File(source);
        this.source = sourceFile.getAbsolutePath();
        String name = sourceFile.getName();
        int extensionStart = name.lastIndexOf('.');
        if (extensionStart <= 0) {
            this.name = name;
        } else {
            this.name = name.substring(0, extensionStart);
        }
    }

    public void setIdentity(String source, String name)
    {
        this.source = new File(source).getAbsolutePath();
        this.name = name;
    }

    /**
     * Wrapper function for {@link FeatureSet#addFeature(Feature)}. Adds a new feature to the database's feature set.
     * 
     * @param f the feature to add
     * @return if a feature with the same name already existed in the feature set, it is removed and returned; otherwise
     *         the function returns null
     * @see FeatureSet#addFeature(Feature)
     */
    public Feature addFeature(Feature f)
    {
        return this.featureSet.addFeature(f);
    }

    /**
     * Wrapper function for {@link FeatureSet#addFeatureValue(String, Object)}. Adds a new feature value to the
     * database's feature set.
     * 
     * @param featureName the feature for which the value needs to be registered
     * @param value the value to register
     * @return true if the feature exists in the feature set and the value, which must be non-null, was successfully
     *         added; false otherwise
     * @see FeatureSet#addFeatureValue(String, Object)
     * @see Feature#registerValue(Object)
     */
    public boolean addFeatureValue(String featureName, Object value)
    {
        return this.featureSet.addFeatureValue(featureName, value);
    }

    /**
     * Wrapper function for {@link FeatureSet#getFeature(String)}. Returns the {@link Feature} with the requested name
     * from the database's feature set.
     * 
     * @param name the name of the feature to retrieve
     * @return the {@link Feature} with that name, if such a feature exists in the database, or null otherwise
     * @see FeatureSet#getFeature(String)
     */
    public Feature getFeature(String name)
    {
        return this.featureSet.getFeature(name);
    }

    /**
     * Wrapper function for {@link FeatureSet#getLabel()}. Retrieves the feature which is the classification label of
     * the objects in the database
     * 
     * @return the label {@link Feature}
     * @see FeatureSet#getLabel();
     */
    public Feature getLabel()
    {
        return this.featureSet.getLabel();
    }

    /**
     * Provides access to the database's feature set.
     * 
     * @return the featureSet
     */
    public FeatureSet getFeatureSet()
    {
        return this.featureSet;
    }

    /**
     * Used by {@link Database#prettyPrint(PrintStream)}
     * 
     * @return a map which associates to each feature name an integer that is the number of characters sufficient to
     *         display any value of that feature from the database as well as the feature's name
     */
    protected Map<String, Integer> getFieldSizes()
    {
        Map<String, Integer> maxFieldSize = new LinkedHashMap<String, Integer>();
        for (String featureName : this.featureSet.keySet()) {
            maxFieldSize.put(featureName, this.featureSet.getFeature(featureName).getMaxFieldSize());
        }
        // make sure the label is at the end:
        Feature label = this.featureSet.getLabel();
        if (label != null) {
            maxFieldSize.remove(label.getName());
            maxFieldSize.put(label.getName(), label.getMaxFieldSize());
        }
        return maxFieldSize;
    }

    /**
     * Generates a table representing the database, with the entry number on the first column, the feature labels on the
     * first row, and each entry of the database on subsequent rows. The last column corresponds to the entry's
     * "classification label".
     * 
     * @param out the {@link PrintStream} to with the table is printed
     * @see PrettyPrintDatabaseFormatter#format(Database)
     */
    @Override
    public String toString()
    {
        return new PrettyPrintDatabaseFormatter().format(this);
    }
}
