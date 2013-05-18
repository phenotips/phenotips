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
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * @version $Id$
 */
public class SolrVCFUploader
{

    /** Prefix for medatada rows. */
    private static final String METADATA_PREFIX = "##";

    /** Prefix for header rows. */
    private static final String HEADER_PREFIX = "#";

    /** Interface to Solr. */
    private VCFService vcfSolrService;

    /**
     * The list with the parsed columns.
     */
    private List<VCFColumn> columns = new ArrayList<VCFColumn>();
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

        List<VariantData> variants = new ArrayList<VariantData>();

        try {

            in = new BufferedReader(new FileReader(input));
            String line;

            while ((line = in.readLine()) != null) {
                VariantData variantData = processLine(line);
                if (variantData != null && variantData.size() > 0) {
                    variantData.addUUID();
                    variants.add(variantData);
                }
            }

            vcfSolrService.index(variants);

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
     * @return a VariantData structure containing all values
     */
    private VariantData processLine(String line) {

        VariantData variantData = null;

        if (!ignoreLine(line)) {

            if (headerLine(line)) {
                columns = extractColumns(line);
            } else {
                variantData = extractValues(line);
            }
        }

        return variantData;
    }

    /**
     * Extract the columns and stored them into a list.
     *
     * @param line  The current row in the document.
     * @return      The list of columns that match the know column names.
     */
    private List<VCFColumn> extractColumns(String line) {

        /* Disregard the leading # */
        Scanner scanner = new Scanner(line.substring(1));

        /* Parse columns and store them to an array */
        while (scanner.hasNext()) {
            String columnToken = scanner.next();
            VCFColumn column = getColumn(columnToken);

            if (column != null) {
                columns.add(column);
            }
        }

        return columns;
    }

    /**
     * Match the current column token to the know column names and return the suitable enum. If a match is not
     * found return null.
     * @param token     The current column token.
     * @return          The corresponding column enum, or null if a match is not possible.
     */
    private VCFColumn getColumn(String token) {
        String lkToken = token.toLowerCase();

        VCFColumn matchedColumn = null;

        for (VCFColumn column : VCFColumn.values()) {
            if (column.getName().equals(lkToken)) {
                matchedColumn = column;
            }
        }

        return matchedColumn;
    }

    /**
     * Extract the values for the current vcf row. The columns parsed beforehand are used to determine the order.
     * @param line  The current line in the document.
     * @return      A VariantData object containing the column value information extracted.
     */
    private VariantData extractValues(String line) {

        VariantData variantData = new VariantData();

        Scanner scanner = new Scanner(line);

        int index = 0;

        while (scanner.hasNext() && index < columns.size()) {
            String token = scanner.next();
            VCFColumn column = columns.get(index);
            variantData = column.process(token, variantData);
            index++;
        }
        return variantData;
    }

    /**
     * Determine if the line should be ignored.
     *
     * Ingore metadata since the columns we want are mandatory.
     * @param line      The line
     * @return          true if line should be ignored, false otherwise.
     */
    private boolean ignoreLine(String line) {
        return line.startsWith(METADATA_PREFIX);
    }


    /**
     * Determine if the line should be ignored.
     *
     * Ingore metadata since the columns we want are mandatory.
     * @param line      The line
     * @return          true if line should be ignored, false otherwise.
     */
    private boolean headerLine(String line) {
        return line.startsWith(HEADER_PREFIX);
    }

}
