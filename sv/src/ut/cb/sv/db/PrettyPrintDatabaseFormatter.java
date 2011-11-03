package ut.cb.sv.db;

import java.io.PrintStream;
import java.util.Date;
import java.util.Map;

public class PrettyPrintDatabaseFormatter implements DatabaseFormatter
{

    /** Only used when pretty-printing the database */
    protected static final String FIRST_COLUMN_LABEL = "#";

    protected static final String HORIZONTAL_LINE = "-";

    protected static final String VERTICAL_LINE = "|";

    protected static final String COLUMN_SEPARATOR = " " + VERTICAL_LINE + " ";

    protected static final String LINE_INTERSECTION = "+";

    private Database database;

    /**
     * Used by {@link PrettyPrintDatabaseFormatter#print(Database, PrintStream)}. Basically informs on the number of
     * entries in the database
     * 
     * @return a string
     */
    private String getDisplayedPreamble()
    {
        return " Name:    " + this.database.getName() +
            ("".equals(this.database.getSource()) ? "" : "\n Source:  " + this.database.getSource()) +
            "\n Date:    " + new Date() + "\n" +
            "\n Entries: " + this.database.size() + "\n";
    }

    /**
     * Used by {@link PrettyPrintDatabaseFormatter#print(Database, PrintStream)}. Draws a horizontal line as wide as the
     * table
     * 
     * @return a string
     */
    private String getHorizontalSeparatorLine(int firstColSize, Map<String, Integer> maxFieldSizes)
    {
        StringBuilder str =
            new StringBuilder(String.format(String.format("%%0%dd", firstColSize + 1), 0).replace("0", HORIZONTAL_LINE));
        for (String featureName : maxFieldSizes.keySet()) {
            str.append(LINE_INTERSECTION);
            str.append(String.format(String.format(
                String.format("%%0%dd", maxFieldSizes.get(featureName) + COLUMN_SEPARATOR.length() - 1), 0)
                .replace("0", HORIZONTAL_LINE)));
        }
        str.append("\n");
        return str.toString();
    }

    /**
     * Used by {@link PrettyPrintDatabaseFormatter#print(Database, PrintStream)}
     * 
     * @return the number of characters sufficient to display the any entry's number as well as the label of the first
     *         column
     */
    private int getFirstColumnSize()
    {
        return Math.max(FIRST_COLUMN_LABEL.length(), (int) Math.ceil(Math
            .log10(this.database.size()))) + 1;
    }

    /**
     * Used by {@link PrettyPrintDatabaseFormatter#print(Database, PrintStream)}. Displays the header of the table
     * representing the database (column labels)
     * 
     * @param firstColSize the number of characters necessary for displaying the first column (the number of the entry),
     *            previously obtained via {@link Database#getFirstColumnSize()}
     * @param maxFieldSizes the number of characters necessary for displaying each feature column, previously obtained
     *            via {@link Database#getFieldSizes()}
     * @return a string
     */
    private String getDisplayedHeader(int firstColSize, Map<String, Integer> maxFieldSizes)
    {
        StringBuilder str = new StringBuilder();
        str.append(getHorizontalSeparatorLine(firstColSize, maxFieldSizes));
        str.append(String.format("%" + firstColSize + "s", FIRST_COLUMN_LABEL));
        for (String featureName : maxFieldSizes.keySet()) {
            str.append(String.format(COLUMN_SEPARATOR + "%" + maxFieldSizes.get(featureName) + "s", featureName));
        }
        str.append("\n");
        str.append(getHorizontalSeparatorLine(firstColSize, maxFieldSizes));
        return str.toString();
    }

    /**
     * Used by {@link PrettyPrintDatabaseFormatter#print(Database, PrintStream)}. Displays a row (corresponding to a
     * database entry) of the table representing the database
     * 
     * @param entryIdx the index in the database of the entry to display
     * @param firstColSize the number of characters necessary for displaying the first column, previously obtained via
     *            {@link Database#getFirstColumnSize()}
     * @param maxFieldSizes the number of characters necessary for displaying each feature column, previously obtained
     *            via {@link Database#getFieldSizes()}
     * @return a string
     */
    private String getDisplayedEntry(int entryIdx, int firstColSize, Map<String, Integer> maxFieldSizes)
    {
        StringBuilder str = new StringBuilder();
        str.append(String.format("%" + firstColSize + "d", (entryIdx + 1)));
        str.append(this.formatEntry(this.database.get(entryIdx), maxFieldSizes));
        str.append("\n");
        return str.toString();
    }

    /**
     * Format a database entry for pretty-printing the database as a table.
     * 
     * @param entry the {@link DatabaseEntry} to format
     * @param maxFieldSizes a maps associating to each feature name the number of characters the value should be
     *            displayed on, prefixed with spaces if necessary
     * @return a the formatted entry as a String
     */
    private String formatEntry(DatabaseEntry entry, Map<String, Integer> maxFieldSizes)
    {
        StringBuilder str = new StringBuilder();
        Integer size = 0;
        for (String feature : maxFieldSizes.keySet()) {
            size = maxFieldSizes.get(feature);
            Object value = entry.get(feature);
            str.append(String.format(COLUMN_SEPARATOR + "%" + (size == null ? "" : size) + "s", value == null
                ? entry.getLabel() : value));
        }
        return str.toString();
    }

    /**
     * Prints a table representing a {@link Database}, with the entry number on the first column, the feature labels on
     * the first row, and each entry of the database on subsequent rows. The last column corresponds to the entry's
     * "classification label".
     * 
     * @param database the {@link Database} to print
     * @param out the {@link PrintStream} to with the table is printed
     * @see Database#prettyPrint(PrintStream)
     */
    public void print(Database database, PrintStream out)
    {
        this.database = database;
        Map<String, Integer> maxFieldSizes = database.getFieldSizes();
        int firstColSize = this.getFirstColumnSize();
        out.print(getDisplayedPreamble());
        out.print(getDisplayedHeader(firstColSize, maxFieldSizes));
        for (int i = 0; i < this.database.size(); ++i) {
            out.print(getDisplayedEntry(i, firstColSize, maxFieldSizes));
        }
        out.print(getHorizontalSeparatorLine(firstColSize, maxFieldSizes));
        out.flush();
        this.database = null;
    }

    /**
     * Formats a {@link Database} as a table, with the entry number on the first column, the feature labels on the first
     * row, and each entry of the database on subsequent rows. The last column corresponds to the entry's
     * "classification label".
     * 
     * @param database the {@link Database} to format
     * @return the formatted {@link Database} as a {@link String}
     * @see Database#toString()
     */
    public String format(Database database)
    {
        this.database = database;
        StringBuilder str = new StringBuilder();
        Map<String, Integer> maxFieldSizes = database.getFieldSizes();
        int firstColSize = this.getFirstColumnSize();
        str.append(getDisplayedPreamble());
        str.append(getDisplayedHeader(firstColSize, maxFieldSizes));
        for (int i = 0; i < this.database.size(); ++i) {
            str.append(getDisplayedEntry(i, firstColSize, maxFieldSizes));
        }
        str.append(getHorizontalSeparatorLine(firstColSize, maxFieldSizes));
        this.database = null;
        return str.toString();
    }
}
