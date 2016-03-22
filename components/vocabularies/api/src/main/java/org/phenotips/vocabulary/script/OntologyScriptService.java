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
package org.phenotips.vocabulary.script;

import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Provides access to the available vocabularies and their terms to public scripts. This is a deprecated service still
 * present for backwards compatibility reasons, please use {@link VocabularyScriptService $services.vocabulary} instead.
 *
 * @version $Id$
 * @since 1.2M4 (under a different package since 1.0M8)
 * @deprecated use {@link VocabularyScriptService $services.vocabularies} instead
 */
@Deprecated
@Component
@Named("ontologies")
@Singleton
public class OntologyScriptService implements ScriptService
{
    /** The vocabulary manager that actually does all the work. */
    @Inject
    private VocabularyManager manager;

    /**
     * Retrieve a term from its owner vocabulary. For this to work properly, the term identifier must contain a known
     * vocabulary prefix.
     *
     * @param termId the term identifier, in the format {@code <vocabulary prefix>:<term id>}, for example
     *            {@code HP:0002066}
     * @return the requested term, or {@code null} if the term doesn't exist in the vocabulary, or no matching
     *         vocabulary is available
     * @deprecated use {@link VocabularyScriptService#resolveTerm(String) $services.vocabularies.resolveTerm} instead
     */
    @Deprecated
    public VocabularyTerm resolveTerm(String termId)
    {
        return this.manager.resolveTerm(termId);
    }

    /**
     * Retrieve a vocabulary given its identifier.
     *
     * @param vocabularyId the vocabulary identifier, which is also used as a prefix in every term identifier from that
     *            vocabulary, or a {@link Vocabulary#getAliases() known alias} for it, for example {@code MIM},
     *            {@code hpo}, {@code HP} or {@code HPO}
     * @return the requested vocabulary, or {@code null} if it doesn't exist or isn't available in the platform
     * @deprecated use {@link VocabularyScriptService#getVocabulary(String) $services.vocabularies.getVocabulary} instead
     */
    @Deprecated
    public Vocabulary getOntology(String vocabularyId)
    {
        return this.manager.getVocabulary(vocabularyId);
    }

    /**
     * Retrieve a vocabulary given its identifier. This is a shortcut for {@link #getOntology(String)} which allows
     * scripts to use the shorter {@code $services.ontologies.hpo} notation for accessing a vocabulary.
     *
     * @param vocabularyId the vocabulary identifier, which is also used as a prefix in every term identifier from that
     *            vocabulary, or a {@link Vocabulary#getAliases() known alias} for it, for example {@code MIM},
     *            {@code HP} or {@code HPO}
     * @return the requested vocabulary, or {@code null} if it doesn't exist or isn't available in the platform
     * @deprecated use {@link VocabularyScriptService#get(String) $services.vocabularies.get} instead
     */
    @Deprecated
    public Vocabulary get(String vocabularyId)
    {
        return this.manager.getVocabulary(vocabularyId);
    }
}
