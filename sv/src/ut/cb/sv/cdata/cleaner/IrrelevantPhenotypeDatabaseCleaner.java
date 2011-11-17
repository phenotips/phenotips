package ut.cb.sv.cdata.cleaner;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import ut.cb.sv.db.Database;
import ut.cb.sv.db.DatabaseEntry;

public class IrrelevantPhenotypeDatabaseCleaner extends PhenotypeDatabaseCleaner
{
    public static final Set<String> DEFAULT_IRRELEVANT_PHENOTYPES = new TreeSet<String>()
            {
        {
            add("IRRELEVANT");
            add("FOLLOW_UP");
            add("CHRABN");
            add("");
            add("[]");
            add("SYNDROME");
        }
    };

    private final Set<String> irrelevantPhenotypes = new TreeSet<String>();

    public IrrelevantPhenotypeDatabaseCleaner()
    {
        super();
        this.irrelevantPhenotypes.addAll(DEFAULT_IRRELEVANT_PHENOTYPES);
    }

    public IrrelevantPhenotypeDatabaseCleaner(String irrelevantPhenotypes)
    {
        this();
        if (!StringUtils.isBlank(irrelevantPhenotypes)) {
            this.irrelevantPhenotypes.clear();
            this.irrelevantPhenotypes.addAll(Arrays.asList(irrelevantPhenotypes.split(",")));
        }
    }

    public IrrelevantPhenotypeDatabaseCleaner(Collection<String> irrelevantPhenotypes)
    {
        this();
        if (irrelevantPhenotypes != null && !irrelevantPhenotypes.isEmpty()) {
            this.irrelevantPhenotypes.clear();
            this.irrelevantPhenotypes.addAll(irrelevantPhenotypes);
        }
    }

    @Override
    public void doClean(Database data)
    {
        for (DatabaseEntry entry : data) {
            Object entryValue = entry.get(PHENOTYPE_FEATURE_NAME);
            if (entryValue instanceof Collection< ? >) {
                ((Collection< ? >) entryValue).removeAll(this.irrelevantPhenotypes);
            }
        }
    }
}
