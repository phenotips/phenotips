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

import org.json.JSONObject;

/**
 * Information about a specific {@link Patient patient} feature value, represented as a term from a vocabulary.
 *
 * @version $Id$
 * @since 1.0M8
 */
@Unstable
public interface VocabularyProperty
{
    /**
     * The vocabulary term identifier associated to this value.
     *
     * @return an identifier, in the format {@code VOCABULARY:termId}, or the empty string if this is a free text term,
     *         not from a vocabulary
     */
    String getId();

    /**
     * The name associated to this term in the vocabulary.
     *
     * @return a user-friendly name for this term, or the term itself if this is not an vocabulary term
     */
    String getName();

    /**
     * Retrieve all information about this term and its associated metadata in a JSON format. For example:
     *
     * <pre>
     * {
     *   "id": "MIM:136140",
     *   "name": "#136140 FLOATING-HARBOR SYNDROME; FLHS"
     *   // plus any other specific information
     * }
     * </pre>
     *
     * @return the data about this value, using the org.json classes
     */
    JSONObject toJSON();
}
