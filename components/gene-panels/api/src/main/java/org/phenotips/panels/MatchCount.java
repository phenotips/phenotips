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
package org.phenotips.panels;

import org.xwiki.stability.Unstable;

import java.util.Collection;

import org.json.JSONObject;

/**
 * A DTO containing the number of genes associated with a specific {@link #getId() term}.
 *
 * @version $Id$
 * @since 1.4
 */
@Unstable("New API introduced in 1.4")
public interface MatchCount extends Comparable<MatchCount>
{
    /**
     * Gets the total number of genes associated with {@link #getId()}.
     *
     * @return the total number of genes associated with {@link #getId()}
     */
    int getCount();

    /**
     * Gets the preferred term ID as string.
     *
     * @return preferred term ID as string
     */
    String getId();

    /**
     * Gets the term name as string.
     *
     * @return the term name as string
     */
    String getName();

    /**
     * Retrieves the genes being counted.
     *
     * @return a collection of gene identifiers
     */
    Collection<String> getGenes();

    /**
     * Creates a {@link JSONObject} of itself.
     *
     * @return a JSON representation of itself
     */
    JSONObject toJSON();
}
