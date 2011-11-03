package ut.cb.sv.db;

import java.io.PrintStream;
import java.util.LinkedList;

import ut.cb.sv.db.feature.BooleanFeature;
import ut.cb.sv.db.feature.CategoryFeature;
import ut.cb.sv.db.feature.Feature;
import ut.cb.sv.db.feature.IntegerFeature;
import ut.cb.sv.db.feature.NumericFeature;

public class ArffDatabaseFormatter implements DatabaseFormatter
{
    private static final String RELATION_MARKER = "@relation";

    private static final String ATTRIBURE_MARKER = "@attribute";

    private static final String DATA_MARKER = "@data";

    private static final String MISSING_VALUE_MARKER = "?";

    private static final String ENTRY_VALUE_SEPARATOR = ",";

    Database database;

    LinkedList<String> featureNames = new LinkedList<String>();

    private String getHeaderSection()
    {
        StringBuilder str = new StringBuilder();
        str.append(RELATION_MARKER).append(" \"").append(this.database.getName()).append("\"\n\n");
        Feature classFeature = null;
        for (Feature f : this.database.getFeatureSet().values()) {
            if (f.isLabelFeature()) {
                classFeature = f;
                continue;
            }
            str.append(getAttributeDefinition(f));
            this.featureNames.add(f.getName());
        }
        if (classFeature != null) {
            str.append(getAttributeDefinition(classFeature));
            this.featureNames.add(classFeature.getName());
        }
        str.append("\n");
        return str.toString();
    }

    private String getAttributeDefinition(Feature f)
    {
        return ATTRIBURE_MARKER + " " + f.getName() + " " + getArffCompatibleFeatureType(f) + "\n";
    }

    private String getArffCompatibleFeatureType(Feature f)
    {
        if (f instanceof NumericFeature || f instanceof IntegerFeature) {
            return "Numeric";
        } else if (f instanceof CategoryFeature && !((CategoryFeature) f).getValues().isEmpty()) {
            return ((CategoryFeature) f).getValues().toString().replace("[", "{\"").replace("]", "\"}").replaceAll(
                ",\\s*", "\",\"");
        } else if (f instanceof BooleanFeature) {
            return "{true, false}";
        } else {
            return "String";
        }
        // Date type in arff not supported here
    }

    private String getDataSectionHeader()
    {
        return DATA_MARKER + "\n";
    }

    private String formatDataEntry(DatabaseEntry entry)
    {
        StringBuilder str = new StringBuilder();
        for (String featureName : this.featureNames) {
            Object value = entry.get(featureName);
            Feature f;
            if (value == null && (f = this.database.getFeature(featureName)) != null && f.isLabelFeature()) {
                value = entry.getLabel();
            }
            String displayedValue = MISSING_VALUE_MARKER;
            if (value != null) {
                displayedValue = value.toString();
                if (displayedValue.indexOf(' ') > 0) {
                    displayedValue = "\"" + displayedValue + "\"";
                }
            }
            str.append(ENTRY_VALUE_SEPARATOR).append(displayedValue);
        }
        str.append("\n");
        return str.substring(ENTRY_VALUE_SEPARATOR.length());
    }

    public String format(Database database)
    {
        this.database = database;
        this.featureNames.clear();
        StringBuilder str = new StringBuilder();
        str.append(getHeaderSection());
        str.append(getDataSectionHeader());
        for (DatabaseEntry entry : this.database) {
            str.append(formatDataEntry(entry));
        }
        this.database = null;
        return str.toString();
    }

    public void print(Database database, PrintStream out)
    {
        this.database = database;
        this.featureNames.clear();
        out.print(getHeaderSection());
        out.print(getDataSectionHeader());
        for (DatabaseEntry entry : this.database) {
            out.print(formatDataEntry(entry));
        }
        this.database = null;
    }

}
