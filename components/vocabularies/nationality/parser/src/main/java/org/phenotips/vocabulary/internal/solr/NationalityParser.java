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
package org.phenotips.vocabulary.internal.solr;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


/**
 * Parses the HTML table obtained from https://en.wikipedia.org/wiki/List_of_contemporary_ethnic_groups and
 * generate a document that can be sent to Solr for indexing.
 *
 * @version $Id$
 * @since 1.3
 */
public final class NationalityParser {
    /**
     * Regular expression used for parsing the population size. Possible value formats in this version:
     * <ul>
     * <li>100,000</li>
     * <li>{@code <} 100,000</li>
     * <li>100,000 - 200,000 (sometimes with another type of dash)</li>
     * </ul>
     * Regexp says:
     * <ol>
     * <li>may start with {@code <}</li>
     * <li>optional whitespace</li>
     * <li>digits and commas (captured as group 1)</li>
     * <li>something other than digits (optional)</li>
     * <li>another set of digits and commas (captured as group 2, optional)</li>
     * </ol>
     * If the regexp matches, we'll use either the mean between the two numbers, or the first number if the second one
     * doesn't exist.
     */

    private NationalityParser() {
        // Hidden constructor to prevent initialization
    }

    /**
     * Main method, parses {@code ListNationalityGroups.csv} from the current directory
     * and generates a {@code .csv} file
     * (for debugging) and a {@code .xml} file that can be sent to Solr for indexing.
     *
     * @param args ignored, required for an executable class signature
     */
    public static void main(String[] args) {
        String listFileName = "ListNationalityGroups.csv";
        String outputName = "ListNationalityGroupsDebug.csv";
        String outputNameXml = "ListNationalityGroups.xml";

        try {
            InputStreamReader input =
                    new InputStreamReader(new FileInputStream(listFileName), StandardCharsets.UTF_8);
            CSVParser csvParser = CSVFormat.DEFAULT.parse(input);

            PrintWriter output = new PrintWriter(outputName);
            PrintWriter outputXml = new PrintWriter(outputNameXml);
            outputXml.println("<add>");
            for (CSVRecord record : csvParser)
            {
                String nationality = record.get(0);
                outputXml.println("<doc boost=\"0\"><field name=\"id\">NATION:"
                        + URLEncoder.encode(nationality.toLowerCase(), "UTF-8").replaceAll("[^a-z]", "")
                        + "</field><field name=\"name\">" + nationality + "</field></doc>");
                output.println(nationality);
            }
            outputXml.println("</add>");
            output.close();
            outputXml.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
