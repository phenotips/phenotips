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

import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.stability.Unstable;

import org.json.JSONObject;

/**
 * A DTO containing a set of features associated with a specific gene.
 *
 * @version $Id$
 * @since 1.3M5
 */
@Unstable("New API introduced in 1.3")
public interface PhenotypesForGene
{
    /**
     * Adds the HPO {@link VocabularyTerm} to the set.
     *
     * @param term the HPO term to be added
     */
    void addTerm(final VocabularyTerm term);

    /**
     * Gets the total size of the set of HPO terms.
     *
     * @return the total size of the HPO terms set
     */
    int getCount();

    /**
     * Creates a {@link JSONObject} from stored data.
     *
     * @return a JSON representation of the class
     */
    JSONObject toJSON();
}
