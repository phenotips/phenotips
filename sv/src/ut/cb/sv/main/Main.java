package ut.cb.sv.main;

import java.io.PrintStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.StringUtils;

import ut.cb.sv.cdata.SampleCollection;
import ut.cb.sv.cdata.cleaner.DatabaseCleaner;
import ut.cb.sv.cdata.cleaner.EmptyPhenotypeDatabaseCleaner;
import ut.cb.sv.cdata.cleaner.IrrelevantPhenotypeDatabaseCleaner;
import ut.cb.sv.cdata.cleaner.UnfrequentPhenotypeDatabaseCleaner;
import ut.cb.sv.db.ArffDatabaseFormatter;
import ut.cb.sv.db.CSVDatabaseFormatter;
import ut.cb.sv.db.Database;
import ut.cb.sv.db.DatabaseFormatter;
import ut.cb.sv.db.PrettyPrintDatabaseFormatter;
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
        HELP("h", "help", false, "Prints help and exists."),
        REMOVE_IRRELEVANT_PHENOTYPES(
            "r",
            "remove-irrelevant-phenotypes",
            false,
            "[DATABASE CLEANUP] " +
            "From each sample, remove phenotype values that are of no interest for the analysis. " +
            "This action is not performed by default (i.e. all phenotype values encountered in the " +
            "initial database are kept for each sample). " +
            "If no phenotypes are given as argument to this parameter, the default ones are removed: "
            + IrrelevantPhenotypeDatabaseCleaner.DEFAULT_IRRELEVANT_PHENOTYPES,
            IrrelevantPhenotypeDatabaseCleaner.DEFAULT_IRRELEVANT_PHENOTYPES),
        REMOVE_UNFREQUENT_PHENOTYPES("u",
            "remove-irrelevant-phenotypes",
            false,
            "[DATABASE CLEANUP] " +
            "From each sample, remove phenotype values that have low support (number of occurences) " +
            "in the database. " +
            "This action is not performed by default. " +
            "The number of occurences of a phenotype value is considered low if it is below a theshold " +
            "given as argument to this parameter. By default, the threshold is "
            + UnfrequentPhenotypeDatabaseCleaner.DEFAULT_PHENOTYPE_OCCURRENCE_THRESHOLD,
            UnfrequentPhenotypeDatabaseCleaner.DEFAULT_PHENOTYPE_OCCURRENCE_THRESHOLD),
        REMOVE_EMPTY_PHENOTYPES("e",
            "remove-empty-phenotypes",
            false,
            "[DATABASE CLEANUP] " +
            "Remove samples with empty phenotypes. Not done by default."),
        CREATE_SAMPLE_COLLECTION("S",
            "sample-collection",
            true,
            "Generate sample collection data. If an argument is given to this parameter, " +
            "export this data as csv into the file indicated by this argument.");

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
            handleCleanupOption(cmd, CmdLineOptions.REMOVE_IRRELEVANT_PHENOTYPES, data);
            handleCleanupOption(cmd, CmdLineOptions.REMOVE_UNFREQUENT_PHENOTYPES, data);
            handleCleanupOption(cmd, CmdLineOptions.REMOVE_EMPTY_PHENOTYPES, data);

            // SampleCollection sc =
            handleSampleCollectionOption(cmd, data);
            // System.out.println(sc);

            handleOutputOption(cmd, CmdLineOptions.PRETTY_PRINT, data);
            handleOutputOption(cmd, CmdLineOptions.CSV_EXPORT, data);
            handleOutputOption(cmd, CmdLineOptions.ARFF_EXPORT, data);

            // for (Feature f : data.getFeatureSet().values()) {
            // // if (f instanceof CategoricalFeature || f instanceof LabelFeature) {
            // if (f.getName().equalsIgnoreCase("phenotype")) {
            // // System.out.println("\n" + f.getName() + ":");
            // for (String value : ((CategoryFeature) f).getEncounteredValues()) {
            // System.out.println(value + " : " + ((CategoryFeature) f).getValueCounter(value));
            // /*
            // * if (value == null) { continue; } for (String piece : value.split("\\s*,\\s*")) {
            // * System.out.println(piece); }
            // */
            // }
            // }
            // }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected static void showUsage(Options options)
    {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(120, "java " + Main.class.getName() + " [options]", "", options, "");
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

    protected static void handleCleanupOption(CommandLine cmd, CmdLineOptions option, Database data)
    {
        if (!cmd.hasOption(option.getOption())) {
            return;
        }
        DatabaseCleaner cleaner;
        switch (option) {
            case REMOVE_IRRELEVANT_PHENOTYPES:
                cleaner = new IrrelevantPhenotypeDatabaseCleaner(cmd.getOptionValue(option.getOption()));
                break;
            case REMOVE_UNFREQUENT_PHENOTYPES:
                int threshold = -1;
                try {
                    Integer.parseInt(cmd.getOptionValue(option.getOption()));
                } catch (Exception ex) {
                    threshold = (Integer) option.getDefaultValue();
                }
                cleaner = new UnfrequentPhenotypeDatabaseCleaner(threshold);
                break;
            case REMOVE_EMPTY_PHENOTYPES:
                cleaner = new EmptyPhenotypeDatabaseCleaner();
                break;
            default:
                // not an output option
                return;
        }
        if (cleaner != null) {
            if (cleaner.clean(data) == null) {
                System.err.println("Failed to clean data for option -" + option.getOption());
            }
        }
    }

    protected static SampleCollection handleSampleCollectionOption(CommandLine cmd, Database data)
    {
        String option = CmdLineOptions.CREATE_SAMPLE_COLLECTION.getOption();
        if (!cmd.hasOption(option)) {
            return null;
        }
        SampleCollection sc = new SampleCollection(data);

        String outputFileName = cmd.getOptionValue(option);
        if (!StringUtils.isBlank(outputFileName)) {
            String sep = cmd.getOptionValue(CmdLineOptions.SEPARATOR.getOption(),
                (String) CmdLineOptions.SEPARATOR.getDefaultValue());
            try {
                PrintStream out = new PrintStream(outputFileName);
                out.print(sc.toCSV(sep));
                out.close();
            } catch (Exception ex) {
                System.err.println("Failed output sample collection data: " + ex.getClass() + ": " + ex.getMessage());
            }
        }
        return sc;
    }

    protected static void failWithMessage(String message)
    {
        System.err.println(message);
        System.exit(1);
    }
}
