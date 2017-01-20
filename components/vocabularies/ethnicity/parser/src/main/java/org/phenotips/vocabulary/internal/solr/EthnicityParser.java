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
 * Parses the HTML table obtained from https://en.wikipedia.org/wiki/List_of_contemporary_ethnic_groups and generate a
 * document tha can be sent to Solr for indexing.
 *
 * @version $Id$
 * @since 1.3
 */
public final class EthnicityParser
{
    private EthnicityParser()
    {
        // Hidden constructor to prevent initialization
    }

    /**
     * Main method, parses ListEthnicGroups.csv from the current directory and generates a .csv file (for debugging) and
     * a .xml file that can be sent to Solr for indexing.
     *
     * @param args ignored, requied for an executable class signature
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
                int number = 1000;
                try {
                    // Possible value formats in this version:
                    // 100,000
                    // < 100,000
                    // 100,000 - 200,000 (sometimes with another type of dash)
                    //
                    // Regexp says:
                    // - may start with <
                    // - optional whitespace
                    // - digits and commas (captured as group 1)
                    // - something other than digits (optional)
                    // - another set of digits and commas (captured as group 3, optional)
                    //
                    // If the regexp matches, we'll use either the mean between the two numbers,
                    // or the first number if the second one doesn't exist
                    Matcher matcher = Pattern.compile("^<?\\s*([0-9,]+)([^0-9]+)?([0-9,]+)?")
                        .matcher(row.select("td").get(3).ownText());
                    if (matcher.find()) {
                        number = Integer.parseInt(matcher.group(1).replaceAll(",", ""));
                        if (matcher.group(3) != null) {
                            number = (number + Integer.parseInt(matcher.group(3).replaceAll(",", ""))) / 2;
                        }
                    }
                } catch (Exception ex) {
                    //Do nothing
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
