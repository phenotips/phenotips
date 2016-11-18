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

import java.util.List;
import java.util.Set;

import org.json.JSONObject;

/**
 * An object containing methods for manipulating and displaying gene panels related data.
 *
 * @version $Id$
 * @since 1.3M5
 */
@Unstable("New API introduced in 1.3")
public interface GenePanel
{
    /**
     * Returns the set of provided features.
     *
     * @return the user provided set of features
     */
    Set<VocabularyTerm> getPresentFeatures();

    /**
     * Returns an ordered list of objects containing gene symbol, ID, and counts data.
     *
     * @return an ordered list containing gene data
     */
    List<PhenotypesForGene> getPhenotypesForGeneList();

    /**
     * Creates a {@link JSONObject} representation of itself.
     *
     * @return a JSON representation of itself
     */
    JSONObject toJSON();

    /**
     * The number of genes represented by the gene panel.
     *
     * @return the number of genes
     */
    int size();
}
