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

import java.util.Map;

import net.sf.json.JSONObject;

import org.xwiki.stability.Unstable;

/**
 * Information about a specific feature recorded for a {@link Patient patient}.
 * 
 * @version $Id$
 */
@Unstable
public interface Phenotype
{
    /**
     * The category of this feature: {@code phenotype}, {@code prenatal_phenotype}, {@code past_phenotype}, etc. This
     * does not include the {@code negative_} prefix for {@link #isPresent() negative observations}.
     * 
     * @return the symptom type, a string ending in {@code phenotype}
     */
    String getType();

    /**
     * The ontology term identifier associated to this feature.
     * 
     * @return an identifier, in the format {@code ONTOLOGY:termId}, or the empty string if this is not an ontology term
     */
    String getId();

    /**
     * The name associated to this feature in the ontology.
     * 
     * @return a user-friendly name for this feature, or the feature itself if this is not an ontology term
     */
    String getName();

    /**
     * Is this a positive or a negative observation. Positive observations indicate a feature that was observed in the
     * patient, while negative observations are pertinent features that were not observed in the patient.
     * 
     * @return {@code true} for a positive observation, {@code false} for a negative one
     */
    boolean isPresent();

    /**
     * Return the list of associated metadata, like age of onset or pace of progression.
     * 
     * @return an unmodifiable map with the {@link PhenotypeMetadatum#getType() metadatum type} as the key and the
     *         actual {@link PhenotypeMetadatum metadatum} as the value, or an empty map if no metadata is recorded
     */
    Map<String, PhenotypeMetadatum> getMetadata();

    /**
     * Retrieve all information about this symptom and its associated metadata in a JSON format. For example:
     * 
     * <pre>
     * {
     *   "type": "phenotype",
     *   "id": "HP:0100247",
     *   "name": "Recurrent singultus",
     *   "isPresent": true,
     *   "metadata": [
     *     // See the documentation for {@link PhenotypeMetadatum#toJSON()}
     *   ]
     * }
     * </pre>
     * 
     * @return the feature data, using the json-lib classes
     */
    JSONObject toJSON();
}
