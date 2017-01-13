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

import java.util.List;

/**
 * Provides access to the available vocabularies and their terms.
 *
 * @version $Id$
 * @since 1.2M4 (under different names since 1.0M8)
 */
@Unstable
@Role
public interface VocabularyManager
{
    /**
     * Retrieve a term from its owner vocabulary. For this to work properly, the term identifier must contain a known
     * vocabulary prefix.
     *
     * @param termId the term identifier, in the format {@code <vocabulary prefix>:<term id>}, for example
     *            {@code HP:0002066}
     * @return the requested term, or {@code null} if the term doesn't exist in the vocabulary, or no matching
     *         vocabulary is available
     */
    VocabularyTerm resolveTerm(String termId);

    /**
     * Retrieve a vocabulary given its identifier.
     *
     * @param vocabularyId the vocabulary identifier, which is also used as a prefix in every term identifier from that
     *            vocabulary, for example {@code HP} or {@code MIM}, or one of its {@link Vocabulary#getAliases() known
     *            aliases}
     * @return the requested vocabulary, or {@code null} if it doesn't exist or isn't available in the platform
     */
    Vocabulary getVocabulary(String vocabularyId);

    /**
     * Retrieves a list of vocabulary ids that are available for use with {@link #getVocabulary(String)}.
     *
     * @return a list of {@link Vocabulary#getIdentifier() vocabulary identifiers}, one for each vocabulary
     */
    List<String> getAvailableVocabularies();
}
