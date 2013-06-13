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
package edu.toronto.cs.phenotips.data;

import net.sf.json.JSONObject;

/**
 * Information about a {@link Patient patient} {@link Phenotype feature} property (meta-feature).
 * 
 * @version $Id$
 */
public interface PhenotypeMetadatum
{
    static final String AGE_OF_ONSET = "age_of_onset";

    static final String SPEED_OF_ONSET = "speed_of_onset";

    static final String PACE_OF_PROGRESSION = "pace_of_progression";

    String getType();

    /**
     * The ontology term identifier associated to this meta-feature.
     * 
     * @return an identifier, in the format {@code ONTOLOGY:termId}
     */
    String getId();

    /**
     * The name associated to this meta-feature in the ontology.
     * 
     * @return a user-friendly name
     */
    String getName();

    /**
     * Retrieve all information about this symptom and its associated metadata in a JSON format. For example:
     * 
     * <pre>
     * {
     *   "type": "age_of_onset",
     *   "id": "HP:0003621",
     *   "name": "Juvenile onset",
     * }
     * </pre>
     * 
     * @return the meta-feature data, using the json-lib classes
     */
    JSONObject toJSON();
}
