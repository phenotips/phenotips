package ut.cb.sv.cdata.cleaner;

import java.util.Collection;
import java.util.Set;

import ut.cb.sv.db.Database;
import ut.cb.sv.db.DatabaseEntry;

public class UnfrequentPhenotypeDatabaseCleaner extends PhenotypeDatabaseCleaner
{

    public static final int DEFAULT_PHENOTYPE_OCCURRENCE_THRESHOLD = 25;

    private final int phenotypeOccurenceThreshold;

    public UnfrequentPhenotypeDatabaseCleaner()
    {
        super();
        this.phenotypeOccurenceThreshold = DEFAULT_PHENOTYPE_OCCURRENCE_THRESHOLD;
    }

    public UnfrequentPhenotypeDatabaseCleaner(int threshold)
    {
        super();
        if (threshold >= 0) {
            this.phenotypeOccurenceThreshold = threshold;
        } else {
            this.phenotypeOccurenceThreshold = DEFAULT_PHENOTYPE_OCCURRENCE_THRESHOLD;
        }
    }

    @Override
    public void doClean(Database data)
    {
        Set<String> rarePhenotypes = this.phenotypeFeature.getUnfrequentValues(this.phenotypeOccurenceThreshold);
        for (DatabaseEntry entry : data) {
            Object entryValue = entry.get(PHENOTYPE_FEATURE_NAME);
            if (entryValue instanceof Collection< ? >) {
                ((Collection< ? >) entryValue).removeAll(rarePhenotypes);
            }
        }
    }

}
