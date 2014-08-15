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
package org.phenotips.data;

import org.phenotips.Constants;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

import java.util.Locale;

import net.sf.json.JSONObject;

/**
 * Information about a {@link Patient patient} {@link Feature feature} property (meta-feature).
 *
 * @version $Id$
 * @since 1.0M8
 */
@Unstable
public interface FeatureMetadatum extends OntologyProperty
{
    /** The XClass used for storing phenotype metadata. */
    EntityReference CLASS_REFERENCE = new EntityReference("PhenotypeMetaClass", EntityType.DOCUMENT,
        Constants.CODE_SPACE_REFERENCE);

    /**
     * The supported metadata types.
     */
    enum Type
    {
        /** The age group in which disease manifestations appear. */
        AGE_OF_ONSET("HP:0003674"),
        /** The speed at which disease manifestations develop in time. */
        PACE_OF_PROGRESSION("HP:0003679"),
        /** The intensity or degree of a manifestation. */
        SEVERITY("HP:0012824"),
        /** The speed at which disease manifestations appear and develop. */
        TEMPORAL_PATTERN("HP:0011008"),
        /** The pattern by which a phenotype affects one or more regions of the body. */
        SPATIAL_PATTERN("HP:0012836"),
        /** The localization with respect to the side of the body of the specified phenotypic abnormality. */
        LATERALITY("HP:0012831"),
        /** The pattern in which a particular genetic trait or disorder is passed from one generation to the next. */
        SUSPECTED_MODE_OF_INHERITANCE("HP:0000005");

        /** @see #getId() */
        private final String id;

        /**
         * Constructor that initializes the {@link #getId() ontology term identifier}.
         *
         * @param id an identifier, in the format {@code ONTOLOGY:termId}
         * @see #getId()
         */
        Type(String id)
        {
            this.id = id;
        }

        @Override
        public String toString()
        {
            return this.name().toLowerCase(Locale.ROOT);
        }

        /**
         * Get the ontology term identifier associated to this type of meta-feature.
         *
         * @return an identifier, in the format {@code ONTOLOGY:termId}
         */
        public String getId()
        {
            return this.id;
        }
    }

    /**
     * The category of this meta-feature.
     *
     * @return an identifier, for example {@code age_of_onset} or {@code pace_of_progression}
     */
    String getType();

    /**
     * Retrieve information about this meta-feature in a JSON format. For example:
     *
     * <pre>
     * {
     *   "id": "HP:0003621",
     *   "name": "Juvenile onset",
     *   "type": "age_of_onset",
     * }
     * </pre>
     *
     * @return the meta-feature data, using the json-lib classes
     */
    @Override
    JSONObject toJSON();
}
