package ut.cb.sv.cdata.cleaner;

import ut.cb.sv.db.Database;
import ut.cb.sv.db.feature.CategoryFeature;

public abstract class PhenotypeDatabaseCleaner implements DatabaseCleaner
{
    protected static final String PHENOTYPE_FEATURE_NAME = "PHENOTYPE";

    protected CategoryFeature phenotypeFeature;

    protected boolean initializeClean(Database data)
    {
        try {
            this.phenotypeFeature = (CategoryFeature) data.getFeature(PHENOTYPE_FEATURE_NAME);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public abstract void doClean(Database data);
    
    public Database clean(Database data) {
        if (!initializeClean(data)) {
            return null;
        }
        doClean(data);
        return data;
    }

}
