package ut.cb.sv.db;

import java.io.PrintStream;

public class CSVDatabaseFormatter implements DatabaseFormatter
{
    String separator;

    public static final String DEFAULT_SEPARATOR = "\t";

    public CSVDatabaseFormatter()
    {
        this(DEFAULT_SEPARATOR);
    }

    public CSVDatabaseFormatter(String separator)
    {
        this.separator = separator;
    }

    public void print(Database database, PrintStream out)
    {
        boolean isFirst = true;
        for (String featureName : database.getFeatureSet().keySet()) {
            if (!isFirst) {
                out.print(this.separator);
            } else {
                isFirst = false;
            }
            out.print(featureName);
        }
        out.println();
        for (DatabaseEntry entry : database) {
            out.println(entry.format(database.getFeatureSet().keySet(), this.separator));
        }
        out.flush();
    }

    public String format(Database database)
    {
        StringBuilder str = new StringBuilder();
        boolean isFirst = true;
        for (String featureName : database.getFeatureSet().keySet()) {
            if (!isFirst) {
                str.append(this.separator);
            } else {
                isFirst = false;
            }
            str.append(featureName);
        }
        str.append('\n');
        for (DatabaseEntry entry : database) {
            str.append(entry.format(database.getFeatureSet().keySet(), this.separator)).append('\n');
        }
        return str.toString();
    }

}
