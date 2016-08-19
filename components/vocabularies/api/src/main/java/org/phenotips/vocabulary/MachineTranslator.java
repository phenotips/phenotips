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

import org.xwiki.component.annotation.Role;

import java.util.Collection;

/**
 * A component capable of machine translating a vocabulary.
 * Will operate based on the default language.
 *
 * @version $Id$
 */
@Role
public interface MachineTranslator
{
    /**
     * Get the number of characters that would have to be translated - excluding any strings
     * that would be read from disk.
     *
     * @param vocabulary the vocabulary id
     * @param term the term that would be translated
     * @param fields the fields to look at
     * @return the number of non-translated items.
     */
    long getMissingCharacters(String vocabulary, VocabularyTerm term, Collection<String> fields);

    /**
     * Translate the term given.
     * Whenever possible will read from disk.
     *
     * @param vocabulary the vocabulary id
     * @param term the term to translate
     * @param fields the fields to translate in the terms
     * @return the number of translated characters.
     */
    long translate(String vocabulary, VocabularyInputTerm term, Collection<String> fields);

    /**
     * Return the set of supported vocabularies.
     *
     * @return the supported vocabularies
     */
    Collection<String> getSupportedVocabularies();

    /**
     * Return the set of supported languages.
     *
     * @return the supported languages
     */
    Collection<String> getSupportedLanguages();

    /**
     * Set things up to machine translate the vocabulary given.
     *
     * @param vocabulary the vocabulary.
     * @return whether it succeeded
     */
    boolean loadVocabulary(String vocabulary);

    /**
     * Tear things down from the machine translation of the vocabulary given.
     *
     * @param vocabulary the vocabulary.
     * @return whether it succeeded
     */
    boolean unloadVocabulary(String vocabulary);
}
