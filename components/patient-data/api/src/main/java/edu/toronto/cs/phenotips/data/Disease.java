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
 * Information about a specific disease recorded for a {@link Patient patient}.
 * 
 * @version $Id$
 */
public interface Disease
{
    /**
     * The ontology term identifier associated to this disease.
     * 
     * @return an identifier, in the format {@code ONTOLOGY:termId}, or the empty string if this is not an ontology term
     */
    String getId();

    /**
     * The name associated to this disease in the ontology.
     * 
     * @return a user-friendly name for this disease, or the disease itself if this is not an ontology term
     */
    String getName();

    /**
     * Retrieve all information about this symptom and its associated metadata in a JSON format. For example:
     * 
     * <pre>
     * {
     *   "id": "MIM:136140",
     *   "name": "#136140 FLOATING-HARBOR SYNDROME; FLHS",
     * }
     * </pre>
     * 
     * @return the disease data, using the json-lib classes
     */
    JSONObject toJSON();
}
