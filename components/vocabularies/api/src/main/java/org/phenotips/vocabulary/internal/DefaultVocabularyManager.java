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
package org.phenotips.vocabulary.internal;

import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

/**
 * Default implementation of the {@link VocabularyManager} component, which uses all the {@link Vocabulary vocabularies}
 * registered in the component manager.
 *
 * @version $Id$
 * @since 1.2M4 (under different names since 1.0M8)
 */
@Component
@Singleton
public class DefaultVocabularyManager implements VocabularyManager, Initializable
{
    /** The currently available vocabularies. */
    @Inject
    private Map<String, Vocabulary> vocabularies;

    @Override
    public void initialize() throws InitializationException
    {
        Map<String, Vocabulary> newVocabulariesMap = new HashMap<String, Vocabulary>();
        for (Vocabulary vocabulary : this.vocabularies.values()) {
            for (String alias : vocabulary.getAliases()) {
                newVocabulariesMap.put(alias, vocabulary);
            }
        }
        this.vocabularies = newVocabulariesMap;
    }

    @Override
    public VocabularyTerm resolveTerm(String termId)
    {
        Vocabulary vocabulary = getVocabularyForTerm(termId);
        if (vocabulary != null) {
            return vocabulary.getTerm(termId);
        }
        return null;
    }

    @Override
    public Vocabulary getVocabulary(String vocabularyId)
    {
        return this.vocabularies.get(vocabularyId);
    }

    @Override
    public List<String> getAvailableVocabularies() {
        return new ArrayList<String>(this.vocabularies.keySet());
    }

    /**
     * Finds the owner vocabulary given a term identifier. The vocabulary is identified by the term ID prefix, for
     * example {@code HP} in {@code HP:0002066}.
     *
     * @param termId the term identifier to process
     * @return the owner vocabulary, or {@code null} if the term doesn't belong to a known vocabulary
     */
    private Vocabulary getVocabularyForTerm(String termId)
    {
        String vocabularyId = StringUtils.substringBefore(termId, ":");
        if (StringUtils.isNotBlank(vocabularyId)) {
            return this.vocabularies.get(vocabularyId);
        }
        return null;
    }
}
