package ut.cb.sv.main;

import java.io.PrintStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import ut.cb.sv.cdata.SampleCollection;
import ut.cb.sv.db.ArffDatabaseFormatter;
import ut.cb.sv.db.CSVDatabaseFormatter;
import ut.cb.sv.db.Database;
import ut.cb.sv.db.DatabaseFormatter;
import ut.cb.sv.db.PrettyPrintDatabaseFormatter;
import ut.cb.sv.db.feature.CategoryFeature;
import ut.cb.sv.db.feature.Feature;
import ut.cb.sv.db.load.DBLoader;
import ut.cb.sv.db.load.DBLoaderFactory;

public class Main
{
    private enum CmdLineOptions
    {
        DATABASE("d", "database", true, "MANDATORY. Path of the database to load."),
        DATABASE_FORMAT("f", "database-format", true, "Database format: " + DBLoaderFactory.getSupportedFormats()
            + ". Default: " + DBLoaderFactory.getDefaultFormat(),
            DBLoaderFactory.getDefaultFormat()),
        FEATURE_MAPPING_FILE("m", "feature-mapping-file", true,
            "MANDATORY. Path of the file defining the mapping between database fields and features."),
        LIMIT_NB_OF_ENTRIES("l", "limit-nb-of-entries", true, "Only load the first <arg> entries of the input file.",
            DBLoader.DEFAULT_NB_OF_ENTRIES),
        CSV_EXPORT("c", "export-csv", true, "Generate a csv output file named <arg> with the selected data."),
        SEPARATOR("s", "separator", true, "Separator string for the output file. Default separator: \\t.",
            CSVDatabaseFormatter.DEFAULT_SEPARATOR),
        PRETTY_PRINT(
            "p",
            "pretty-print",
            true,
            "Pretty-print the selected data in a text file named <arg> with fixed-width columns. This file should not be used for later parsing"),
        ARFF_EXPORT("a", "export-arff", true,
            "Generate arff (weka-compatible) file named <arg> with the selected data."),
        HELP("h", "help", false, "Prints help and exists.");

        private final String shortOption;

        private final String longOption;

        private final boolean hasArgument;

        private final String description;

        private final Object defaultValue;

        private CmdLineOptions(String shortOption, String longOption, boolean hasArgument, String description)
        {
            this(shortOption, longOption, hasArgument, description, null);
        }

        private CmdLineOptions(String shortOption, String longOption, boolean hasArgument, String description,
            Object defaultValue)
        {
            this.shortOption = shortOption;
            this.longOption = longOption;
            this.hasArgument = hasArgument;
            this.description = description;
            this.defaultValue = defaultValue;
        }

        public String getOption()
        {
            return this.shortOption;
        }

        public String getShortOption()
        {
            return this.shortOption;
        }

        public String getLongOption()
        {
            return this.longOption;
        }

        public boolean hasArgument()
        {
            return this.hasArgument;
        }

        public String getDescription()
        {
            return this.description;
        }

        public Object getDefaultValue()
        {
            return this.defaultValue;
        }

        public static Options generateOptions()
        {
            Options options = new Options();
            for (CmdLineOptions option : CmdLineOptions.values()) {
                options.addOption(option.getShortOption(), option.getLongOption(), option.hasArgument(), option
                    .getDescription());
            }
            return options;
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        String databaseFile = (String) CmdLineOptions.DATABASE.getDefaultValue();
        String featureMapFile = (String) CmdLineOptions.FEATURE_MAPPING_FILE.getDefaultValue();
        int nbOfEntries = (Integer) CmdLineOptions.LIMIT_NB_OF_ENTRIES.getDefaultValue();
        String dbFormat = (String) CmdLineOptions.DATABASE_FORMAT.getDefaultValue();

        Options options = CmdLineOptions.generateOptions();

        try {
            CommandLineParser parser = new PosixParser();
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption(CmdLineOptions.HELP.getOption())
                || !cmd.hasOption(CmdLineOptions.DATABASE.getOption())
                || !cmd.hasOption(CmdLineOptions.FEATURE_MAPPING_FILE.getOption())) {
                showUsage(options);
                System.exit(cmd.hasOption(CmdLineOptions.HELP.getOption()) ? 0 : 1);
            }

            databaseFile = cmd.getOptionValue(CmdLineOptions.DATABASE.getOption());
            featureMapFile = cmd.getOptionValue(CmdLineOptions.FEATURE_MAPPING_FILE.getOption());

            if (cmd.hasOption(CmdLineOptions.LIMIT_NB_OF_ENTRIES.getOption())) {
                try {
                    nbOfEntries = Integer.parseInt(cmd.getOptionValue(CmdLineOptions.LIMIT_NB_OF_ENTRIES.getOption()));
                    if (nbOfEntries <= 0
                        && nbOfEntries != (Integer) CmdLineOptions.LIMIT_NB_OF_ENTRIES.getDefaultValue()) {
                        throw new IllegalArgumentException();
                    }
                } catch (Exception ex) {
                    System.out.println("Invalid argument for option -l. Using default: " + nbOfEntries);
                }
            }

            DBLoaderFactory dbLoaderFactory = new DBLoaderFactory();
            if (cmd.hasOption(CmdLineOptions.DATABASE_FORMAT.getOption())) {
                dbFormat = cmd.getOptionValue(CmdLineOptions.DATABASE_FORMAT.getOption());
                if (!dbLoaderFactory.isFormatSuported(dbFormat)) {
                    dbFormat = (String) CmdLineOptions.DATABASE_FORMAT.getDefaultValue();
                }
            }
            DBLoader dbLoader = null;
            try {
                dbLoader = dbLoaderFactory.getDBLoaderInstance(dbFormat, featureMapFile);
            } catch (Exception ex) {
                failWithMessage("Failed to initialize database loading process: " + ex.getMessage());
            }
            Database data = null;
            for (String dbFile : databaseFile.split(",")) {
                data = dbLoader.load(dbFile, nbOfEntries);
            }
            if (data == null) {
                failWithMessage("Failed to load database");
            }

            // GeneFunctionData gd = new GeneFunctionData();
            // gd.writeTo(System.err);
            // gd.addGOInfoToDatabase(data);

            SampleCollection sc = new SampleCollection(data);
            // System.out.println(sc);

            handleOutputOption(cmd, CmdLineOptions.PRETTY_PRINT, data);
            handleOutputOption(cmd, CmdLineOptions.CSV_EXPORT, data);
            handleOutputOption(cmd, CmdLineOptions.ARFF_EXPORT, data);

            for (Feature f : data.getFeatureSet().values()) {
                // if (f instanceof CategoricalFeature || f instanceof LabelFeature) {
                if (f.getName().equalsIgnoreCase("phenotype")) {
                    // System.out.println("\n" + f.getName() + ":");
                    for (String value : ((CategoryFeature) f).encounteredValues) {
                        System.out.println(value);
                        /*
                         * if (value == null) { continue; } for (String piece : value.split("\\s*,\\s*")) {
                         * System.out.println(piece); }
                         */
                    }
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected static void showUsage(Options options)
    {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java " + Main.class.getName() + " [options]", options);
    }

    protected static void handleOutputOption(CommandLine cmd, CmdLineOptions option, Database data)
    {
        if (!cmd.hasOption(option.getOption())) {
            return;
        }
        DatabaseFormatter formatter;
        switch (option) {
            case CSV_EXPORT:
                String separator = (String) CmdLineOptions.SEPARATOR.getDefaultValue();
                if (cmd.hasOption(CmdLineOptions.SEPARATOR.getOption())) {
                    separator = cmd.getOptionValue(CmdLineOptions.SEPARATOR.getOption());
                }
                formatter = new CSVDatabaseFormatter(separator);
                break;
            case PRETTY_PRINT:
                formatter = new PrettyPrintDatabaseFormatter();
                break;
            case ARFF_EXPORT:
                formatter = new ArffDatabaseFormatter();
                break;
            default:
                // not an output option
                return;
        }
        if (formatter != null) {
            try {
                String outputFileName = cmd.getOptionValue(option.getOption());
                PrintStream out = new PrintStream(outputFileName);
                formatter.print(data, out);
                out.close();
            } catch (Exception e) {
                System.out.println("Failed to export data for option -" + option.getOption() + ": " + e.getMessage());
            }
        }
    }

    protected static void failWithMessage(String message)
    {
        System.err.println(message);
        System.exit(1);
    }
}
