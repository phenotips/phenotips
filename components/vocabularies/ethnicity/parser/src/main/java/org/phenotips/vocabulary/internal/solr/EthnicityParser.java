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

import java.io.File;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Parses the HTML table obtained from {@link https://en.wikipedia.org/wiki/List_of_contemporary_ethnic_groups} and
 * generate a document that can be sent to Solr for indexing.
 *
 * @version $Id$
 * @since 1.3
 */
public final class EthnicityParser
{
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
    private static final Pattern POPSIZE_PATTERN = Pattern.compile("^<?\\s*([0-9,]+)(?:[^0-9]+)?([0-9,]+)?");

    private EthnicityParser()
    {
        // Hidden constructor to prevent initialization
    }

    /**
     * Main method, parses {@code ListEthnicGroups.html} from the current directory and generates a {@code .csv} file
     * (for debugging) and a {@code .xml} file that can be sent to Solr for indexing.
     *
     * @param args ignored, required for an executable class signature
     */
    public static void main(String[] args)
    {
        String listFileName = "ListEthnicGroups.html";
        String outputName = "ListEthnicGroups.csv";
        String outputNameXml = "ListEthnicGroups.xml";
        File listFile = new File(listFileName);

        try {
            Document doc = Jsoup.parse(listFile, "UTF-8");
            Elements rows = doc.select("tr");
            PrintWriter output = new PrintWriter(outputName);
            PrintWriter outputXml = new PrintWriter(outputNameXml);
            outputXml.println("<add>");
            for (Element row : rows) {
                String ethnicity = row.select("a").get(0).text();
                // If no population information is present, then assume a low, but not 0 population size, so that the
                // term isn't completely ignored by queries; since most ethncities without a known population size in
                // the input file tend to be small, that's a probably good average
                int number = 1000;
                try {
                    Matcher matcher = POPSIZE_PATTERN.matcher(row.select("td").get(3).ownText());
                    if (matcher.find()) {
                        number = Integer.parseInt(matcher.group(1).replaceAll(",", ""));
                        if (matcher.group(2) != null) {
                            number = (number + Integer.parseInt(matcher.group(2).replaceAll(",", ""))) / 2;
                        }
                    }
                } catch (Exception ex) {
                    // Do nothing
                }

                outputXml.println("<doc boost=\"" + Math.log(number) + "\"><field name=\"id\">ETHNO:"
                    + URLEncoder.encode(ethnicity.toLowerCase(), "UTF-8").replaceAll("[^a-z]", "")
                    + "</field><field name=\"name\">" + ethnicity + "</field><field name=\"popsize\">" + number
                    + "</field></doc>");
                output.println(ethnicity + "\t" + number);
            }
            outputXml.println("</add>");
            output.close();
            outputXml.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
