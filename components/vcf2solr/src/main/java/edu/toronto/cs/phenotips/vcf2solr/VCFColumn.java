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

/**
 * Encapsulte the processing logic for VCF column values.
 *
 * @version $Id $
 */
public enum VCFColumn {

    /** CHROM column.  */
    CHROM("chrom") {
        @Override
        public VariantData process(String fieldValue, VariantData variantData) {
            return processSingleValueField(fieldValue, getName(), variantData);
        }
    },
    /** POS column.  */
    POS("pos") {
        @Override
        public VariantData process(String fieldValue, VariantData variantData) {
            return processSingleValueField(fieldValue, getName(), variantData);
        }
    },
    /** ID column.  */
    ID("id") {
        @Override
        public VariantData process(String fieldValue, VariantData variantData) {
            return processMultiValueField(fieldValue, getName(), MULTI_VALUE_SEPARATOR, variantData);
        }
    },
    /** REF column.  */
    REF("ref") {
        @Override
        public VariantData process(String fieldValue, VariantData variantData) {
            return processSingleValueField(fieldValue, getName(), variantData);
        }
    },
    /** ALT column.  */
    ALT("alt") {
        @Override
        public VariantData process(String fieldValue, VariantData variantData) {
            return processMultiValueField(fieldValue, getName(), ALT_VALUE_SEPARATOR, variantData);
        }
    },
    /** QUAL column.  */
    QUAL("qual") {
        @Override
        public VariantData process(String fieldValue, VariantData variantData) {
            return processSingleValueField(fieldValue, getName(), variantData);
        }
    },
    /** FILTER column.  */
    FILTER("filter") {
        @Override
        public VariantData process(String fieldValue, VariantData variantData) {
            return processSingleValueField(fieldValue, getName(), variantData);
        }
    },
    /** INFO column.  */
    INFO("info") {
        @Override
        public VariantData process(String fieldValue, VariantData variantData) {
            return processInfoValueFields(fieldValue, getName(), MULTI_VALUE_SEPARATOR, variantData);
        }
    };


    /** Separator for multi valued fields. */
    /*private static final String ID_VALUE_SEPARATOR = ";";*/

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
     * Name of the column.
     */
    private String name;

    /**
     * Constructor.
     * @param name  The name of the column.
     */
    private VCFColumn(String name) {
        this.name = name;
    }

    /**
     * Getter for the name.
     * @return  The column name.
     */
    public String getName() {
        return name;
    }

    /**
     * Process a column field value and add the valus to the Variant Data object.
     * @param fieldValue        The column filed value.
     * @param variantData       The Variant Data object with the column value added.
     * @return The Variant Data updated with the column value.
     */
    public abstract VariantData process(String fieldValue, VariantData variantData);

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

            String specificKey = key + INFO_FIELD_INNER_SEPARATOR + innerTerms[0];

            //compare to the list of reserved value and only index those fields that are in the reserved words
            if (ArrayUtils.contains(VariantData.INFO_RESERVED, innerTerms[0])) {
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
