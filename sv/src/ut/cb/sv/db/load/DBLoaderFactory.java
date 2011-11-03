package ut.cb.sv.db.load;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;

/**
 * Creates specific {@link DBLoader}s for different source file formats.
 * 
 * @version $Id$
 */
public class DBLoaderFactory
{
    /**
     * Creates a loader for a certain format, with a certain feature mapping
     * 
     * @param format the database format
     * @param mappingFileName the name of the file containing the feature mapping
     * @return a database loaded (instance of a class implementing {@link DBLoader}
     * @see #getSupportedFormats()
     * @see FeatureMap
     */
    public DBLoader getDBLoaderInstance(String format, String mappingFileName)
    {
        try {
            return (DBLoader) Class.forName(getDBLoaderClassName(format)).getConstructor(String.class).newInstance(
                mappingFileName);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            return getDefaultLoaderInstance(mappingFileName);
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
     * Indicates whether a certain format is valid
     * 
     * @param format the format
     * @return true if the given format is one of the valid ones, false otherwise
     * @see #getSupportedFormats()
     */
    public boolean isFormatSuported(String format)
    {
        return getSupportedFormats().contains(format);
    }

    /**
     * TODO make cleaner. Indicates the supported formats as a list of strings
     * 
     * @return
     */
    public static List<String> getSupportedFormats()
    {
        List<String> packageNames = new LinkedList<String>();
        packageNames.add("xml");
        packageNames.add("csv");
        return packageNames;
    }

    /**
     * TODO make cleaner. Indicates the default format.
     * 
     * @return
     */
    public static String getDefaultFormat()
    {
        return "xml";
    }

    /**
     * Construct the name of the class implementing {@link DBLoader} that corresponds to a given format. The name is
     * constructed from the given format according to a the naming convention DBLoader.package + "." +
     * format.toLowerCase() + "." + format.toUpperCase() + DBLoader. There is no guarantee that a class with the name
     * returned by this function actually exists.
     * 
     * @param format the format
     * @return a {@link String} indicating the fully qualified name
     */
    public String getDBLoaderClassName(String format)
    {
        return DBLoader.class.getPackage().getName() + "." + format.toLowerCase() + "." + format.toUpperCase()
            + DBLoader.class.getSimpleName();
    }

    /**
     * Creates a {@link DBLoader} instance for the default database input file format
     * 
     * @param mappingFileName the input file containing the feature mapping to be used by te loader
     * @return a default {@link DBLoader}
     * @see #getDefaultFormat()
     * @see #getDBLoaderInstance(String, String)
     */
    protected DBLoader getDefaultLoaderInstance(String mappingFileName)
    {
        return getDBLoaderInstance(getDefaultFormat(), mappingFileName);
    }
}
