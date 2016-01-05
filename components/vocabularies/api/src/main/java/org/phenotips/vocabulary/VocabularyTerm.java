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
package org.phenotips.vocabulary;

import org.xwiki.stability.Unstable;

import java.util.Set;

import org.json.JSONObject;

/**
 * A term from a {@link Vocabulary}. A few common properties are available as explicit individual methods, and any
 * property defined for the term can be accessed using the generic {@link #get(String)} method. As a minimum, each term
 * should have an identifier and a name. Terms can be accessed either using the owner {@link Vocabulary}, or the generic
 * {@link VocabularyManager}.
 *
 * @version $Id$
 * @since 1.2M4 (under different names since 1.0M8)
 */
@Unstable
public interface VocabularyTerm
{
    /**
     * The (mandatory) term identifier, in the format {@code <vocabulary prefix>:<term id>}, for example
     * {@code HP:0002066} or {@code MIM:260540}.
     *
     * @return the term identifier, or {@code null} if the term doesn't have an associated identifier
     */
    String getId();

    /**
     * The short human-readable term name, for example {@code Gait ataxia}.
     *
     * @return the term name, or {@code null} if the term doesn't have an associated identifier
     */
    String getName();

    /**
     * The human-readable term description, usually a longer phrase or paragraph that describes the term.
     *
     * @return the term description, or {@code null} if the term doesn't have a description
     */
    String getDescription();

    /**
     * Returns the parents (direct ancestors) of this term.
     *
     * @return a set of vocabulary terms, or an empty set if the term doesn't have any ancestors in the vocabulary
     */
    Set<VocabularyTerm> getParents();

    /**
     * Returns the ancestors (both direct and indirect ancestors) of this term.
     *
     * @return a set of vocabulary terms, or an empty set if the term doesn't have any ancestors in the vocabulary
     */
    Set<VocabularyTerm> getAncestors();

    /**
     * Returns the ancestors (both direct and indirect ancestors) of this term <em>and</em> the term itself.
     *
     * @return a set of vocabulary terms, or a set with one term (this) if the term doesn't have any ancestors in the
     *         vocabulary
     */
    Set<VocabularyTerm> getAncestorsAndSelf();

    /**
     * Find the distance to another term in the same vocabulary, if the owner vocabulary is a structured ontology that
     * supports computing such a distance.
     *
     * @param other the term to which the distance should be computed
     * @return the minimum number of edges that connect the two terms in the DAG representing the ontology, and
     *         {@code -1} if this is an unstructured vocabulary, the terms are not connected, or if the other term is
     *         {@code null}
     */
    long getDistanceTo(VocabularyTerm other);

    /**
     * Generic property access. Any property defined in the vocabulary for this term can be accessed this way.
     *
     * @param name the name of the property to access
     * @return the value defined for the requested property in the vocabulary, or {@code null} if no value is defined
     */
    Object get(String name);

    /**
     * Returns the vocabulary where this term is defined.
     *
     * @return the owner vocabulary
     */
    Vocabulary getVocabulary();

    /**
     * Serialize all the known information about this term as a JSON object.
     *
     * @return a JSON map with information about this term, with the keys being the term's properties, and the values
     *         either a simple string, or a list of strings, for example:
     *
     *         <pre>
     * {
     *    "id":"HP:0001824",
     *    "name":"Weight loss",
     *    "def":"Reduction in existing body weight.",
     *    "is_a":[
     *      "HP:0004325 ! Decreased body weight"
     *    ],
     *    "term_category":[
     *      "HP:0004325",
     *      "HP:0004323",
     *      "HP:0000001",
     *      "HP:0001507",
     *      "HP:0000118"
     *    ],
     *    "xref":[
     *      "MeSH:D015431 \"Weight Loss\"",
     *      "UMLS:C0043096 \"Decreased body weight\""
     *    ]
     * }
     * </pre>
     * @since 1.1-rc1
     */
    JSONObject toJSON();
}
