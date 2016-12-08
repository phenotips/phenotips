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
import org.xwiki.stability.Unstable;

/**
 * Allows extending a base vocabulary with additional annotations. A vocabulary extension can support one or more base
 * vocabularies, identified by the {@link #getSupportedVocabularies()} method. Every time one of the supported
 * vocabularies is {@link org.phenotips.vocabulary.Vocabulary#reindex(String) reindexed}, first
 * {@link #indexingStarted(String)} is called, so that the extension can prepare its needed resources, if any. Then, for
 * each term parsed from its source the {@link #extendTerm(VocabularyInputTerm, String)} method is called, and new
 * fields can be added to it. Once all the terms have been indexed, {@link #indexingEnded(String)} is called, and any
 * resources can be freed.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Unstable("New API introduced in 1.3, it may still change")
@Role
public interface VocabularyExtension
{
    /**
     * Checks if a vocabulary is supported by this extension. This extension's other methods will only be called when
     * indexing a supported vocabulary.
     *
     * @param vocabulary the vocabulary to check
     * @return {@code true} if the target vocabulary is supported, {@code false} if not and this extension should no
     *         longer be invoked when processing that vocabulary
     */
    boolean isVocabularySupported(Vocabulary vocabulary);

    /**
     * Called when a vocabulary reindex begins, so that this extension can prepare its needed resources, if any.
     *
     * @param vocabulary the vocabulary being indexed
     */
    void indexingStarted(Vocabulary vocabulary);

    /**
     * Called for each term during vocabulary reindexing, this method modifies the parsed terms by changing, adding or
     * removing fields.
     *
     * @param term the parsed term which can be altered
     * @param vocabulary the the vocabulary being indexed
     */
    void extendTerm(VocabularyInputTerm term, Vocabulary vocabulary);

    /**
     * Called when a vocabulary reindex is done, so that this extension can clean up its resources, if any.
     *
     * @param vocabulary the vocabulary that was indexed
     */
    void indexingEnded(Vocabulary vocabulary);
}
