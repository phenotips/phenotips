package ut.cb.sv.db.load;

import java.util.Set;

import ut.cb.sv.db.Database;
import ut.cb.sv.db.DatabaseEntry;
import ut.cb.sv.db.feature.Feature;

public abstract class AbstractDBLoader implements DBLoader
{
    private Database database = new Database();

    private DatabaseEntry crtDBEntry = new DatabaseEntry();

    private final FeatureMap featureMap;

    private int maxNbOfEntries;

    public AbstractDBLoader(FeatureMap featureMap)
    {
        this.featureMap = featureMap;
        for (Feature f : this.featureMap.getFeatures()) {
            this.database.addFeature(f);
        }
    }

    public AbstractDBLoader(String mappingFileName)
    {
        this.featureMap = getFeatureMapInstance(mappingFileName);
        for (Feature f : this.featureMap.getFeatures()) {
            this.database.addFeature(f);
        }
    }

    public abstract FeatureMap getFeatureMapInstance(String mappingFileName);

    protected Database getDatabase()
    {
        return this.database;
    }

    public int getMaxNbOfEntries()
    {
        return this.getMaxNbOfEntries();
    }

    public FeatureMap getFeatureMap()
    {
        return this.featureMap;
    }

    public boolean hasReachedMaxNumberOfEntries()
    {
        return this.database.size() == this.maxNbOfEntries;
    }

    public void loadFeatureValueToCrtDBEntry(String featureName, String value)
    {
        Set<Object> valueSet = getFeatureMap().getOutputValue(featureName, value);
        if (valueSet != null) {
            for (Object valueObj : valueSet) {
                if (valueObj == null) {
                    continue;
                }
                getDatabase().addFeatureValue(featureName, valueObj);
                this.crtDBEntry.addFeature(getDatabase().getFeature(featureName), valueObj);
            }
        }
    }

    public void saveCrtDBEntry()
    {
        getDatabase().add(this.crtDBEntry.clone());
    }

    public Database load(String filename, int nbOfEntries)
    {
        this.maxNbOfEntries = nbOfEntries;
        this.database.setIdentity(filename);
        parseDocument(filename);
        return this.database;
    }

    public Database load(String filename)
    {
        return load(filename, DEFAULT_NB_OF_ENTRIES);
    }

    public abstract void parseDocument(String filename);

}
