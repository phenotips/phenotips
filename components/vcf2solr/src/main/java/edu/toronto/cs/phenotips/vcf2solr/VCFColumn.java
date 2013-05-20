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

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Encapsulte the processing logic for VCF column values.
 *
 * @version $Id $
 */
public enum VCFColumn {

    /** CHROM column.  */
    CHROM,
    /** POS column.  */
    POS,
    /** REF column.  */
    REF,
    /** QUAL column.  */
    QUAL,
    /** FILTER column.  */
    FILTER,
    /** ID column.  */
    ID {
        @Override
        public VariantData process(String fieldValue, VariantData variantData) {
            String name = name().toLowerCase(Locale.ROOT);
            return processMultiValueField(fieldValue, name, MULTI_VALUE_SEPARATOR, variantData);
        }
    },
    /** ALT column.  */
    ALT {
        @Override
        public VariantData process(String fieldValue, VariantData variantData) {
            String name = name().toLowerCase(Locale.ROOT);
            return processMultiValueField(fieldValue, name, ALT_VALUE_SEPARATOR, variantData);
        }
    },
    /** INFO column.  */
    INFO {
        @Override
        public VariantData process(String fieldValue, VariantData variantData) {
            String name = name().toLowerCase(Locale.ROOT);
            return processInfoValueFields(fieldValue, name, MULTI_VALUE_SEPARATOR, variantData);
        }
    };

    /** Separator for ALT valued fields. */
    private static final String ALT_VALUE_SEPARATOR = ",";

    /** Separator for INFO multi valued fields. */
    private static final String INFO_FIELD_VALUE_SEPARATOR = "=";

    /** Separator for multi valued fields. */
    private static final String MULTI_VALUE_SEPARATOR = ";";

    /** Separator for INFO Solr field names. */
    private static final String INFO_FIELD_INNER_SEPARATOR = "_";

    /** Default value for INFO Solr document fields. */
    private static final String INFO_FIELD_DEFAULT_VALUE = "1";


    /**
     * Process a column field value and add the value to the Variant Data object.
     *
     * This method considers the fieldValue contains a single value. This method is overridden for
     * fields that need to be parsed in a different manner.
     *
     * @param fieldValue        The column field value.
     * @param variantData       The Variant Data object with the column value added.
     * @return The Variant Data updated with the column value.
     */
    public VariantData process(String fieldValue, VariantData variantData) {
        String name = name().toLowerCase(Locale.ROOT);
        return processSingleValueField(fieldValue, name, variantData);
    }

    /**
     * Save value for a single valued field.
     * @param value     value
     * @param key       key
     * @param variantData  variant data structure
     * @return variant data updated with the column value.
     */
    private static VariantData processSingleValueField(String value,
                                                       String key,
                                                       VariantData variantData) {

        variantData.addTo(key, value);

        return variantData;
    }

    /**
     * Parse and save values for multiple valued fields.
     * @param value     the value
     * @param key       key
     * @param separator separator for multiple values
     * @param variantData  variant data structure for storing data
     * @return variant data structure updated with the column values.
     */
    private static VariantData processMultiValueField(String value,
                                                      String key,
                                                      String separator,
                                                      VariantData variantData) {
        String[] terms = value.split(separator);

        List<String> multipleValues = Arrays.asList(terms);

        variantData.addTo(key, multipleValues);

        return variantData;
    }

    /**
     * Parse and save values for info   fields.
     * @param value     the value
     * @param key       key
     * @param separator separator for multiple values
     * @param variantData  variant data structure for storing data
     * @return  variant data structure updated with the info column values.
     */
    private static VariantData processInfoValueFields(String value,
                                                      String key,
                                                      String separator,
                                                      VariantData variantData) {
        String[] terms = value.split(separator);

        for (String term: terms) {
            String[] innerTerms = term.split(INFO_FIELD_VALUE_SEPARATOR);

            //compare to the list of reserved value and only index those fields that are in the reserved words
            if (ArrayUtils.contains(VariantData.INFO_RESERVED, innerTerms[0])) {

                String specificKey = key + INFO_FIELD_INNER_SEPARATOR + innerTerms[0];

                if (innerTerms.length > 1) {
                    processMultiValueField(innerTerms[1], specificKey, ALT_VALUE_SEPARATOR, variantData);
                } else {
                    processSingleValueField(INFO_FIELD_DEFAULT_VALUE, specificKey, variantData);
                }
            }
        }

        return variantData;
    }

}
