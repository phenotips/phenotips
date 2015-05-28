/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.obo2solr;

import java.io.File;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

public class Main
{
    public static final String OBO_DB_LOCATION_OPTION = "i";

    public static final String OUTPUT_XML_LOCATION_OPTION = "o";

    public static final String INDEX_FILEDS_OPTION = "f";

    public static final String DEFAULT_OUTPUT_XML_LOCATION = "out.xml";

    public static final String HELP_OPTION = "h";

    public static void main(String[] args) throws Exception
    {
        Options options = generateOptions();
        CommandLineParser parser = new PosixParser();
        CommandLine cmd = parser.parse(options, args);
        if (!cmd.hasOption(OBO_DB_LOCATION_OPTION) || cmd.hasOption(HELP_OPTION)) {
            showUsage(options);
            return;
        }
        ParameterPreparer paramPrep = new ParameterPreparer();
        SolrUpdateGenerator generator = new SolrUpdateGenerator();
        File input = paramPrep.getInputFileHandler(cmd.getOptionValue(OBO_DB_LOCATION_OPTION));
        File output =
            paramPrep.getOutputFileHandler(cmd.getOptionValue(OUTPUT_XML_LOCATION_OPTION,
                DEFAULT_OUTPUT_XML_LOCATION));
        Map<String, Double> fieldSelection =
            paramPrep.getFieldSelection(cmd.getOptionValue(INDEX_FILEDS_OPTION, ""));
        generator.transform(input, output, fieldSelection);
    }

    protected static Options generateOptions()
    {
        Options options = new Options();
        options.addOption(OBO_DB_LOCATION_OPTION, "obo-db-location", true,
            "Path or URL of the input database (MANDATORY).");
        options.addOption(OUTPUT_XML_LOCATION_OPTION, "output-file", true, "Path of the output file. Default: "
            + DEFAULT_OUTPUT_XML_LOCATION);
        options.addOption(INDEX_FILEDS_OPTION, "fields-to-index", true,
            "Fields to index. By default, all fields are marked for indexing.");
        options.addOption(HELP_OPTION, "help", false, "Displays help and exits.");
        return options;
    }

    protected static void showUsage(Options options)
    {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java " + Main.class.getName() + " [options]", options);
    }
}
