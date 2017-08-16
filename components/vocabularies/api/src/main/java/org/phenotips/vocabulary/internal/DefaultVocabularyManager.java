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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

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
    private static final String SCORE_LABEL = "score";

    /** The currently available vocabularies. */
    @Inject
    private Map<String, Vocabulary> vocabularies;

    /** Get the logging object. */
    @Inject
    private Logger logger;

    /** The available vocabularies, including keys for each of their aliases. */
    private Map<String, Vocabulary> aliasVocabularies;

    /** The available vocabularies, stored under their respective categories. */
    private Map<String, Set<Vocabulary>> vocabulariesByCategory;

    @Override
    public void initialize() throws InitializationException
    {
        this.aliasVocabularies = new HashMap<>();
        for (Vocabulary vocabulary : this.vocabularies.values()) {
            for (String alias : vocabulary.getAliases()) {
                this.aliasVocabularies.put(alias, vocabulary);
            }
        }
        this.vocabulariesByCategory = constructVocabulariesByCategory();
    }

    /**
     * Constructs a map of vocabularies, categorized according to their type.
     *
     * @return a map where the key is the vocabulary category, and the value is the collection of {@link Vocabulary
     *         vocabularies} implementing that category
     */
    private Map<String, Set<Vocabulary>> constructVocabulariesByCategory()
    {
        final Map<String, Set<Vocabulary>> categorizedVocabularies = new HashMap<>();
        for (final Vocabulary vocabulary : this.vocabularies.values()) {
            final Collection<String> supportedCategories = vocabulary.getSupportedCategories();
            for (final String category : supportedCategories) {
                final Set<Vocabulary> vocabularySet = generateVocabSetForCategory(category, categorizedVocabularies);
                vocabularySet.add(vocabulary);
            }
        }
        return Collections.unmodifiableMap(categorizedVocabularies);
    }

    /**
     * Given a {@code category} label, returns a set of {@link Vocabulary vocabularies} that fall under that category,
     * as per the {@link #vocabulariesByCategory map}. If there are no vocabularies associated with that category yet,
     * then the category is added to {@link #vocabulariesByCategory} and an empty set is returned.
     *
     * @param category the vocabulary category of interest
     * @param categorizedVocabularies the available vocabularies, stored under their respective categories
     * @return a set of vocabulary objects associated with the given {@code category}
     */
    private Set<Vocabulary> generateVocabSetForCategory(@Nonnull final String category,
        @Nonnull Map<String, Set<Vocabulary>> categorizedVocabularies)
    {
        if (categorizedVocabularies.containsKey(category)) {
            return categorizedVocabularies.get(category);
        }
        final Set<Vocabulary> vocabularySet = new HashSet<>();
        categorizedVocabularies.put(category, vocabularySet);
        return vocabularySet;
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
        return this.aliasVocabularies.get(vocabularyId);
    }

    @Override
    public Set<Vocabulary> getVocabularies(final String category)
    {
        if (!this.vocabulariesByCategory.containsKey(category)) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(this.vocabulariesByCategory.get(category));
    }

    @Override
    public List<String> getAvailableVocabularies()
    {
        return new ArrayList<>(this.vocabularies.keySet());
    }

    @Override
    public List<String> getAvailableCategories()
    {
        return new ArrayList<>(this.vocabulariesByCategory.keySet());
    }

    @Override
    public List<VocabularyTerm> search(@Nullable final String input, @Nullable final String category,
        final int maxResults)
    {
        // If either the category or the input is blank, then there is nothing to search. Return empty list.
        if (StringUtils.isBlank(input) || StringUtils.isBlank(category)) {
            return Collections.emptyList();
        }
        // Try to get the vocabularies that belong to the provided category. If none returned, return empty list.
        final Set<Vocabulary> categorizedVocabularies = this.vocabulariesByCategory.get(category);
        if (CollectionUtils.isEmpty(categorizedVocabularies)) {
            this.logger.warn("No vocabularies associated with the specified category: {}", category);
            return Collections.emptyList();
        }

        return search(input, maxResults, category, categorizedVocabularies);
    }

    @Override
    public boolean hasVocabulary(final String vocabulary)
    {
        return this.vocabularies.containsKey(vocabulary);
    }

    @Override
    public boolean hasCategory(final String category)
    {
        return this.vocabulariesByCategory.containsKey(category);
    }

    /**
     * Performs a search for {@code input query string} using the provided set of {@code categorizedVocabularies}, and
     * returns the specified {@code maxResults number of results}, sorted by score (in descending order).
     *
     * @param input the input query string
     * @param maxResults the maximum number of results to return
     * @param category the vocabulary category
     * @param categorizedVocabularies the {@link Vocabulary} objects to search {@code input} in
     * @return an list of {@link VocabularyTerm} objects to search in
     */
    private List<VocabularyTerm> search(@Nonnull final String input, final int maxResults,
        @Nonnull final String category, @Nonnull final Set<Vocabulary> categorizedVocabularies)
    {
        final List<VocabularyTerm> results = new ArrayList<>();
        for (final Vocabulary vocabulary : categorizedVocabularies) {
            results.addAll(vocabulary.search(input, category, maxResults, null, null));
        }

        sortTermsByScore(results);

        final int resultsSize = results.size();
        final int rows = maxResults <= resultsSize ? maxResults : resultsSize;
        return results.subList(0, rows);
    }

    /**
     * Sorts the provided list of {@code results} by score, in descending order.
     *
     * @param results a list of {@link VocabularyTerm vocabulary terms} that will be sorted by score, in place
     */
    private void sortTermsByScore(@Nonnull final List<VocabularyTerm> results)
    {
        Collections.sort(results, new Comparator<VocabularyTerm>()
        {
            @Override
            public int compare(final VocabularyTerm o1, final VocabularyTerm o2)
            {
                return compareScores(o1, o2);
            }
        });
    }

    /**
     * Compares the scores for {@code o1} and {@code o2}.
     *
     * @param o1 the {@link VocabularyTerm} that is being compared
     * @param o2 the {@link VocabularyTerm} that {@code o1} is being compared to
     * @return {@code 0} if {@code o1} and {@code o2} are equivalent, a value less than {@code 0} if {@code o2} should
     *         be ahead of {@code o1}, a value greater than {@code 0} if {@code o1} should be ahead of {@code o2}
     */
    private int compareScores(@Nonnull final VocabularyTerm o1, @Nonnull final VocabularyTerm o2)
    {
        final Object scoreObj1 = o1.get(SCORE_LABEL);
        final Object scoreObj2 = o2.get(SCORE_LABEL);
        final Float scoreO1 = scoreObj1 != null ? (Float) scoreObj1 : 0;
        final Float scoreO2 = scoreObj2 != null ? (Float) scoreObj2 : 0;
        return scoreO2.compareTo(scoreO1);
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
            return this.aliasVocabularies.get(vocabularyId);
        }
        return null;
    }
}
