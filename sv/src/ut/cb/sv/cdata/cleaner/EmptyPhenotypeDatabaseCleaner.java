package ut.cb.sv.cdata.cleaner;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import ut.cb.sv.db.Database;
import ut.cb.sv.db.DatabaseEntry;

public class EmptyPhenotypeDatabaseCleaner extends PhenotypeDatabaseCleaner
{

    @Override
    public void doClean(Database data)
    {
        Set<DatabaseEntry> toRemove = new HashSet<DatabaseEntry>();
        for (DatabaseEntry entry : data) {
            Object entryValue = entry.get(PHENOTYPE_FEATURE_NAME);
            if (!(entryValue instanceof Collection< ? >) || ((Collection< ? >) entryValue).isEmpty()) {
                toRemove.add(entry);
            }
        }
        data.removeAll(toRemove);
    }

}
