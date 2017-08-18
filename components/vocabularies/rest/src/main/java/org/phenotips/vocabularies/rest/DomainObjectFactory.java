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
package org.phenotips.vocabularies.rest;

import org.phenotips.rest.Autolinker;
import org.phenotips.vocabulary.Vocabulary;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Factory for converting internal java objects into their REST representations.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Unstable("New API introduced in 1.3")
@Role
public interface DomainObjectFactory
{
    /**
     * Converts a {@link Vocabulary} into its REST representation,
     * {@link org.phenotips.vocabularies.rest.model.Vocabulary}.
     *
     * @param vocabulary the vocabulary to be converted
     * @return the REST representation without links
     */
    org.phenotips.vocabularies.rest.model.Vocabulary createVocabularyRepresentation(Vocabulary vocabulary);

    /**
     * Converts a collection of {@code vocabularies} into their REST representation, a collection of
     * {@link org.phenotips.vocabularies.rest.model.Vocabulary}.
     *
     * @param vocabularies a collection of {@link Vocabulary} to be converted
     * @param linker an {@link Autolinker} to generate links for each vocabulary; this object should have all vocabulary
     *               unspecific rights and resources already set
     * @param categorySupplier a {@link Function} to get a list of
     *                         {@link org.phenotips.vocabularies.rest.model.Category} associated with each
     *                         {@link Vocabulary}; if null, no category data should be added to each vocabulary's REST
     *                         representation
     * @return the REST representation with links
     * @since 1.4
     */
    List<org.phenotips.vocabularies.rest.model.Vocabulary> createVocabulariesRepresentation(
        Collection<Vocabulary> vocabularies,
        Autolinker linker,
        Function<Vocabulary, List<org.phenotips.vocabularies.rest.model.Category>> categorySupplier);

    /**
     * Converts a {@code vocabulary} into its REST representation,
     * {@link org.phenotips.vocabularies.rest.model.Vocabulary}.
     *
     * @param vocabulary a {@link Vocabulary} to be converted
     * @param linker an {@link Autolinker} to generate links for the vocabulary; this object should have all vocabulary
     *               unspecific rights and resources already set
     * @param categorySupplier a {@link Function} to get a list of
     *                         {@link org.phenotips.vocabularies.rest.model.Category} associated with the
     *                         {@link Vocabulary}; if null, no category data should be added to the vocabulary's REST
     *                         representation
     * @return the REST representation with links
     * @since 1.4
     */
    org.phenotips.vocabularies.rest.model.Vocabulary createLinkedVocabularyRepresentation(
        Vocabulary vocabulary,
        Autolinker linker,
        Function<Vocabulary, List<org.phenotips.vocabularies.rest.model.Category>> categorySupplier);

    /**
     * Converts a {@code categoryId} into a REST representation of the vocabulary category,
     * {@link org.phenotips.vocabularies.rest.model.Category}.
     *
     * @param categoryId the identifier of the vocabulary category to be converted
     * @return the REST representation without links
     * @since 1.4
     */
    org.phenotips.vocabularies.rest.model.Category createCategoryRepresentation(String categoryId);

    /**
     * Converts a collection of {@code categoryIds} into the vocabulary category REST representation, a collection of
     * {@link org.phenotips.vocabularies.rest.model.Category}.
     *
     * @param categoryIds a collection of vocabulary category identifiers
     * @param linker an {@link Autolinker} to generate links for each category; this object should have all category
     *               unspecific rights and resources already set
     * @param vocabularySupplier a {@link Function} to get a list of
     *                           {@link org.phenotips.vocabularies.rest.model.Vocabulary} associated with the category;
     *                           if null, no vocabulary data should be added to the category REST representation
     * @return the REST representation with links
     * @since 1.4
     */
    List<org.phenotips.vocabularies.rest.model.Category> createCategoriesRepresentation(
        Collection<String> categoryIds, Autolinker linker,
        Function<String, List<org.phenotips.vocabularies.rest.model.Vocabulary>> vocabularySupplier);

    /**
     * Converts a {@code categoryId} into a REST representation of the vocabulary category,
     * {@link org.phenotips.vocabularies.rest.model.Category}.
     *
     * @param categoryId the identifier of the vocabulary category to be converted
     * @param linker an {@link Autolinker} to generate links for the category; this object should have all category
     *               unspecific rights and resources already set
     * @param vocabularySupplier a {@link Function} to get a list of
     *                           {@link org.phenotips.vocabularies.rest.model.Vocabulary} associated with the category;
     *                           if null, no vocabulary data should be added to the category REST representation
     * @return the REST representation with links
     * @since 1.4
     */
    org.phenotips.vocabularies.rest.model.Category createLinkedCategoryRepresentation(
        String categoryId,
        Autolinker linker, Function<String,
        List<org.phenotips.vocabularies.rest.model.Vocabulary>> vocabularySupplier);
}
