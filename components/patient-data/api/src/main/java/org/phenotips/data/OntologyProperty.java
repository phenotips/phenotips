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

import org.xwiki.stability.Unstable;

import net.sf.json.JSONObject;

/**
 * Information about a specific {@link Patient patient} feature value, represented as a term from an ontology.
 *
 * @version $Id$
 * @since 1.0M8
 */
@Unstable
public interface OntologyProperty
{
    /**
     * The ontology term identifier associated to this value.
     *
     * @return an identifier, in the format {@code ONTOLOGY:termId}, or the empty string if this is a free text term,
     *         not from an ontology
     */
    String getId();

    /**
     * The name associated to this term in the ontology.
     *
     * @return a user-friendly name for this term, or the term itself if this is not an ontology term
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
     * @return the data about this value, using the json-lib classes
     */
    JSONObject toJSON();
}
