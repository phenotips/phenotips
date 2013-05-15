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

import org.xwiki.component.phase.InitializationException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @version $Id$
 */
public class SolrCVFUploader
{

    /** Main field separator */
    private static final String FIELD_SEPARATOR = "\t";

    /** In case document don't respect strict 4.1 formant and use spaces. Not used currently. */
    private static final String FIELD_SPACE_SEPARATOR = "[ ] *";

    /** Prefix for medatada rows. */
    private static final String METADATA_PREFIX = "##";

    /** Prefix for header rows. */
    private static final String HEADER_PREFIX = "#";

    /** Default field value. */
    private static final String DEFAULT_FIELD_VALUE = ".";

    /** Separator for multi valued fields. */
    private static final String MULTI_VALUE_SEPARATOR = ";";

    /** Separator for ALT valued fields. */
    private static final String ALT_VALUE_SEPARATOR = ",";

    /** Separator for INFO multi valued fields. */
    private static final String INFO_FIELD_VALUE_SEPARATOR = "=";

    /** Separator for INFO Solr field names. */
    private static final String INFO_FIELD_INNER_SEPARATOR = "_";

    /** Default value for INFO Solr document fields. */
    private static final String INFO_FIELD_DEFAULT_VALUE = "1";

    /** Interface to Solr. */
    private VCFService vcfSolrService;

    /*
        Initialize service
     */
    {
        vcfSolrService = new VCFService();
        try {
            vcfSolrService.initialize();
        } catch (InitializationException e) {
            e.printStackTrace();
        }
    }

    /**
     * This function currently ignores the metadata information, which is optional.
     *
     * Also, the ##fileformat line existence is NOT enforced.
     * @param input input vcf file
     */
    public void processAndIndex(File input) {

        BufferedReader in = null;

        Map<String, TermData> terms = new HashMap<String, TermData>();

        try {

            in = new BufferedReader(new FileReader(input));
            String line;


            while ((line = in.readLine()) != null) {
                TermData termData = processLine(line);
                if (termData != null) {
                    terms.put(termData.getId(), termData);
                }
            }

            vcfSolrService.index(terms);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Parse a line and save field values.
     * @param line input line
     * @return a TermData structure containing all values
     */
    private TermData processLine(String line) {
        TermData termData = new TermData();
        if (!ignoreLine(line)) {

            String[] unprocessedFields = line.split(FIELD_SEPARATOR);

            parseSingleValueField(unprocessedFields[0], TermData.CHROM, termData);
            parseSingleValueField(unprocessedFields[1], TermData.POS, termData);
            parseMultiValueField(unprocessedFields[2], TermData.ID, MULTI_VALUE_SEPARATOR, termData);
            parseSingleValueField(unprocessedFields[3], TermData.REF, termData);
            parseMultiValueField(unprocessedFields[4], TermData.ALT, ALT_VALUE_SEPARATOR, termData);
            parseSingleValueField(unprocessedFields[5], TermData.QUAL, termData);
            parseSingleValueField(unprocessedFields[6], TermData.FILTER, termData);
            parseInfoValueFields(unprocessedFields[7], TermData.INFO, MULTI_VALUE_SEPARATOR, termData);

            //generate some random id
            termData.addTo(TermData.ID_FIELD_NAME, unprocessedFields[0] + unprocessedFields[1]);
        }

        return termData;
    }

    /**
     * Save value for a single valued field.
     * @param value     value
     * @param key       key
     * @param termData  Term data structure
     */
    private void parseSingleValueField(String value, String key, TermData termData) {

        termData.addTo(key, value);
    }

    /**
     * Parse and save values for multiple valued fields.
     * @param value     the value
     * @param key       key
     * @param separator separator for multiple values
     * @param termData  term data structure for storing data
     */
    private void parseMultiValueField(String value, String key, String separator, TermData termData) {
        String[] terms = value.split(separator);

        List<String> multipleValues = Arrays.asList(terms);

        termData.addTo(key, multipleValues);
    }

    /**
     * Parse and save values for info   fields.
     * @param value     the value
     * @param key       key
     * @param separator separator for multiple values
     * @param termData  term data structure for storing data
     */
    private void parseInfoValueFields(String value, String key, String separator, TermData termData) {
        String[] terms = value.split(separator);

        for (String term: terms) {
            String[] innerTerms = term.split(INFO_FIELD_VALUE_SEPARATOR);

            String specificKey = key + INFO_FIELD_INNER_SEPARATOR + innerTerms[0];

            if (innerTerms.length > 1) {
                //parseSingleValueField(innerTerms[1], specificKey, termData);
                parseMultiValueField(innerTerms[1], specificKey, ALT_VALUE_SEPARATOR, termData);
            } else {
                parseSingleValueField(INFO_FIELD_DEFAULT_VALUE, specificKey, termData);
            }
        }
    }

    /**
     * Determine if the line should be ignore.
     *
     * Ingore metadata and header, since the columns we want are mandatory.
     * @param line      The line
     * @return          true if line should be ignored, false otherwise.
     */
    private boolean ignoreLine(String line) {
        return (line.startsWith(METADATA_PREFIX) || line.startsWith(HEADER_PREFIX));
    }



}
