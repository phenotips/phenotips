/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package edu.toronto.cs.phenotips.vcf2solr;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import java.io.File;

/**
 * Main class for importing vcf.
 *
 * @version $Id$
 */
public final class Main
{

    /**
     * Help option.
     */
    public static final String HELP_OPTION = "h";

    /**
     * Input file location.
     */
    public static final String VCF_INPUT_LOCATION_OPTION = "i";

    /**
     * Private constructor.
     */
    private Main()
    { }


    /**
     * Main method.
     * @param args  args
     */
    public static void main(String[] args) {

        Options options = generateOptions();
        try {
            CommandLineParser parser = new PosixParser();
            CommandLine cmd = parser.parse(options, args);

            if (!cmd.hasOption(VCF_INPUT_LOCATION_OPTION) || cmd.hasOption(HELP_OPTION)) {
                showUsage(options);
                System.exit(cmd.hasOption(HELP_OPTION) ? 0 : 1);
            }

            String inputLocation = cmd.getOptionValue(VCF_INPUT_LOCATION_OPTION);

            File input = new File(inputLocation);

            SolrVCFUploader solrVCFUploader = new SolrVCFUploader();

            solrVCFUploader.processAndIndex(input);

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Show the usage options.
     * @param options usage options
     */
    protected static void showUsage(Options options) {

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java " + Main.class.getName() + " [options]", options);

    }

    /**
     * Generate usage opinions.
     * @return opinions
     */
    protected static Options generateOptions() {
        Options options = new Options();
        options.addOption(VCF_INPUT_LOCATION_OPTION, "vcf-input-location", true,
                "Path or URL of the input database (MANDATORY).");
        options.addOption(HELP_OPTION, "help", false, "Displays help and exits.");
        return options;
    }



}
