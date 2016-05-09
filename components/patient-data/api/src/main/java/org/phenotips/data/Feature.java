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
package org.phenotips.data;

import org.xwiki.stability.Unstable;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;

/**
 * Information about a specific feature recorded for a {@link Patient patient}.
 *
 * @version $Id$
 * @since 1.0M8
 */
@Unstable
public interface Feature extends VocabularyProperty
{
    /**
     * The category of this feature: {@code phenotype}, {@code prenatal_phenotype}, {@code past_phenotype}, etc. This
     * does not include the {@code negative_} prefix for {@link #isPresent() negative observations}.
     *
     * @return the feature type, usually a string ending in {@code phenotype}
     */
    String getType();

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
     * @return an unmodifiable map with the {@link FeatureMetadatum#getType() metadatum type} as the key and the actual
     *         {@link FeatureMetadatum metadatum} as the value, or an empty map if no metadata is recorded
     */
    Map<String, ? extends FeatureMetadatum> getMetadata();

    /**
     * Return optional notes/comments about this feature, entered as free text.
     *
     * @return a free hand text, may be {@code null} or the empty string if no notes are present
     */
    String getNotes();

    /**
     * Retrieve all information about this feature and its associated metadata in a JSON format. For example:
     *
     * <pre>
     * {
     *   "id": "HP:0100247",
     *   "name": "Recurrent singultus",
     *   "type": "phenotype",
     *   "isPresent": true,
     *   "metadata": [
     *     // See the documentation for {@link FeatureMetadatum#toJSON()}
     *   ]
     * }
     * </pre>
     *
     * @return the feature data, using the org.json classes
     */
    @Override
    JSONObject toJSON();

    /**
     * Returns PhenoTips name of this feature. In current implementation it is either the ID, or the name iff ID is null
     * or empty.
     *
     * @return feature name
     * @todo move to VocabularyProperty and/or implement a VocabularyProperty-to-PhenotipsPropertyName mapping service
     */
    String getValue();

    /**
     * Returns a feature property name. Constructed from type property prefixing the {@code negative_} prefix for
     * {@link #isPresent() negative observations}.
     *
     * @return the feature property name, usually a string ending in {@code phenotype}
     */
    String getPropertyName();

    /**
     * Returns optional custom categories for this non-standard feature.
     *
     * @return the feature categories string list
     */
    List<String> getCategories();
}
