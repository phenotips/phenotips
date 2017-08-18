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
package org.phenotips.vocabularies.rest.internal;

import org.phenotips.rest.Autolinker;
import org.phenotips.vocabularies.rest.DomainObjectFactory;
import org.phenotips.vocabularies.rest.model.Category;
import org.phenotips.vocabulary.Vocabulary;

import org.xwiki.component.annotation.Component;
import org.xwiki.stability.Unstable;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Singleton;

/**
 * @version $Id$
 * @since 1.3M1
 */
@Unstable
@Component
@Singleton
public class DefaultDomainObjectFactory implements DomainObjectFactory
{
    private static final String CATEGORY_LABEL = "category";

    private static final String VOCABULARY_ID_LABEL = "vocabulary-id";

    @Override
    public org.phenotips.vocabularies.rest.model.Vocabulary createVocabularyRepresentation(Vocabulary vocabulary)
    {
        org.phenotips.vocabularies.rest.model.Vocabulary result =
            new org.phenotips.vocabularies.rest.model.Vocabulary();
        result
            .withIdentifier(vocabulary.getIdentifier())
            .withName(vocabulary.getName())
            .withAliases(vocabulary.getAliases())
            .withSize(vocabulary.size())
            .withVersion(vocabulary.getVersion());
        try {
            result.withDefaultSourceLocation(vocabulary.getDefaultSourceLocation());
        } catch (UnsupportedOperationException e) {
            // Don't do anything and leave source empty
        }
        return result;
    }

    @Override
    public List<org.phenotips.vocabularies.rest.model.Vocabulary> createVocabulariesRepresentation(
        final Collection<Vocabulary> vocabularies,
        final Autolinker linker,
        final Function<Vocabulary, List<org.phenotips.vocabularies.rest.model.Category>> categorySupplier)
    {
        return vocabularies.stream()
            .map(vocabulary -> createLinkedVocabularyRepresentation(vocabulary, linker, categorySupplier))
            .collect(Collectors.toList());
    }

    @Override
    public org.phenotips.vocabularies.rest.model.Vocabulary createLinkedVocabularyRepresentation(
        final Vocabulary vocabulary,
        final Autolinker linker,
        final Function<Vocabulary, List<org.phenotips.vocabularies.rest.model.Category>> categorySupplier)
    {
        final org.phenotips.vocabularies.rest.model.Vocabulary vocabularyRep = categorySupplier != null
                ? createVocabularyRepresentation(vocabulary).withCategories(categorySupplier.apply(vocabulary))
                : createVocabularyRepresentation(vocabulary);
        vocabularyRep.withLinks(linker.withExtraParameters(VOCABULARY_ID_LABEL, vocabulary.getIdentifier()).build());
        return vocabularyRep;
    }

    @Override
    public org.phenotips.vocabularies.rest.model.Category createCategoryRepresentation(final String categoryId)
    {
        org.phenotips.vocabularies.rest.model.Category result = new org.phenotips.vocabularies.rest.model.Category();
        result.withCategory(categoryId);
        return result;
    }

    @Override
    public List<Category> createCategoriesRepresentation(
        final Collection<String> categoryIds,
        final Autolinker linker,
        final Function<String, List<org.phenotips.vocabularies.rest.model.Vocabulary>> vocabularySupplier)
    {
        return categoryIds.stream()
            .map(categoryId -> createLinkedCategoryRepresentation(categoryId, linker, vocabularySupplier))
            .collect(Collectors.toList());
    }

    @Override
    public org.phenotips.vocabularies.rest.model.Category createLinkedCategoryRepresentation(
        final String categoryId,
        final Autolinker linker,
        final Function<String, List<org.phenotips.vocabularies.rest.model.Vocabulary>> vocabularySupplier)
    {
        final Category categoryRep = vocabularySupplier != null
                ? createCategoryRepresentation(categoryId).withVocabularies(vocabularySupplier.apply(categoryId))
                : createCategoryRepresentation(categoryId);
        categoryRep.withLinks(linker.withExtraParameters(CATEGORY_LABEL, categoryId).build());
        return categoryRep;
    }
}
