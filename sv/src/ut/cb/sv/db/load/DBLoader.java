package ut.cb.sv.db.load;

import ut.cb.sv.db.Database;

/**
 * Loads a {@link Database} from an input file
 * 
 * @version $Id$
 */
public interface DBLoader
{

    /** By default, the number of entries is not limited, i.e. all entries are loaded. */
    public static final int DEFAULT_NB_OF_ENTRIES = -1; // no limit;

    /**
     * Loads the data from a file and creates a {@link Database}
     * 
     * @param filename the name of the file containing the data
     * @return the generated {@link Database} instance
     */
    Database load(String filename);

    /**
     * Loads a certain number of entries data from a file and creates a {@link Database} with these entries.
     * 
     * @param filename the name of the file containing the data
     * @param nbOfEntries the number of entries to load from the source
     * @return the generated {@link Database} instance
     */
    Database load(String filename, int nbOfEntries);
}
