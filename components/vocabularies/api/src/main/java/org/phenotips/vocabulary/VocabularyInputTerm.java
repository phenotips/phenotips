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

import java.util.Set;

/**
 * A vocabulary term that has been parsed from a vocabulary source during {@link Vocabulary#reindex(String) reindexing},
 * exposing the already parsed fields and allowing new fields to be added.
 *
 * @version $Id$
 * @since 1.3M1
 */
public interface VocabularyInputTerm extends VocabularyTerm
{
    /**
     * Sets the (mandatory) term identifier. This is usually already present in the vocabulary source, so this method
     * should rarely be used.
     *
     * @param id the term identifier to set, in the format {@code <vocabulary prefix>:<term id>}, for example
     *            {@code HP:0002066} or {@code MIM:260540}
     * @return the same term, for method chaining
     */
    VocabularyInputTerm setId(String id);

    /**
     * Sets the short human-readable term name.
     *
     * @param name the term name to set
     * @return the same term, for method chaining
     */
    VocabularyInputTerm setName(String name);

    /**
     * Sets the human-readable term description, usually a longer phrase or paragraph that describes the term.
     *
     * @param description the term description to set
     * @return the same term, for method chaining
     */
    VocabularyInputTerm setDescription(String description);

    /**
     * Sets the parents (direct ancestors) of this term.
     *
     * @param parents a set of vocabulary terms, {@code null} or an empty set means that the term has no parents
     * @return the same term, for method chaining
     */
    VocabularyInputTerm setParents(Set<VocabularyTerm> parents);

    /**
     * Generic property setter. Calling this method discards any potential values already stored, setting the passed
     * value as the only value, or if the value is a collection, the entire collection of values. If the value is a
     * collection, that collection object is set, so future modifications to it will be taken into account.
     *
     * @param name the name of the property to set
     * @param value the value to set for the requested property, or {@code null} if this field should be removed
     * @return the same term, for method chaining
     */
    VocabularyInputTerm set(String name, Object value);

    /**
     * Generic property setter, additive. Calling this method keeps any potential values already present, appending the
     * passed value. If a {@code null} value or an empty collection is passed, then the call is ignored and nothing is
     * changed. If there isn't a value already stored, the passed value is stored as-is. If the stored value is a single
     * object and the passed value is a single object, then a new set is created with the two values, and stored. If the
     * stored value is a single object and the passed value is a collection, then the stored value is added to the
     * passed collection, and the passed collection is stored as-is. If a collection is already stored and the passed
     * value is a single object, then the passed value is added to that collection. If a collection is already stored
     * and the passed value is another collection, then the passed collection is merged into the stored one.
     *
     * @param name the name of the property to set
     * @param value the value to append for the requested property; {@code null} and empty collections are ignored
     * @return the same term, for method chaining
     */
    VocabularyInputTerm append(String name, Object value);
}
