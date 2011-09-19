package ut.cb.sv.db;

import java.io.PrintStream;

public interface DatabaseFormatter
{
    void print(Database database, PrintStream out);

    String format(Database database);
}
