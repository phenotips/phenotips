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

import edu.toronto.cs.phenotips.obo2solr.maps.SetMap;

import java.util.Collection;


/**
 * Used to store term values for a document.
 * @version $Id$
 */
public class VariantData extends SetMap<String, String>
{

    /** Solr ID fields. */
    public static final String ID_FIELD_NAME = "variant_id";

    /** CHROM fields. */
    public static final String CHROM = "chrom";

    /** POS fields. */
    public static final String POS = "pos";

    /** ID fields. */
    public static final String ID = "id";

    /** REF fields. */
    public static final String REF = "ref";

    /** ALT fields. */
    public static final String ALT = "alt";

    /** QUAL fields. */
    public static final String QUAL = "qual";

    /** FILTER fields. */
    public static final String FILTER = "filter";

    /** INFO fields. */
    public static final String INFO = "info";

    /** Reserved info fields. */
    public static final String[] INFO_RESERVED  = {"AA", "AC", "AF", "AN", "BQ", "CIGAR",
        "DB", "DP", "END", "H2", "H3", "MQ", "MQ0", "NS", "SB", "SOMATIC", "VALIDATED", "100G"};
    /** Id of opinion. */
    private String id;

    /**
     * Add field to Term Data.
     * @param key           key
     * @param value         value
     * @return  true if addition successful, false otherwise
     */
    @Override
    public boolean addTo(String key, String value)
    {
        if (ID_FIELD_NAME.equals(key)) {
            this.id = value;
        }
        return super.addTo(key, value);
    }

    /**
     * Add multi valued field.
     * @param key           key
     * @param values        value
     * @return  true if addition successful, false otherwise
     */
    @Override
    public boolean addTo(String key, Collection<String> values)
    {
        boolean result = true;
        for (String value : values) {
            result &= this.addTo(key, value);
        }
        return result;
    }

    /**
     * Getter for id.
     * @return  the id
     */
    public String getId() {
        return id;
    }

    /**
     * Setter for id .
     * @param id    the id
     */
    public void setId(String id) {
        this.id = id;
    }
}
