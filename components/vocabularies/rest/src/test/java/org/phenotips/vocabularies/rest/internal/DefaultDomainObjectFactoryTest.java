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
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.UriInfo;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link DefaultDomainObjectFactory} class.
 */
public class DefaultDomainObjectFactoryTest
{
    private static final String VOCAB_1_ID = "vocabulary1";

    private static final String VOCAB_2_ID = "vocabulary2";

    private static final String VOCAB_3_ID = "vocabulary3";

    private static final String ALIAS_1 = "alias1";

    private static final String VOCAB_1_VERSION = "2.3";

    private static final String VOCAB_2_VERSION = "1.0";

    private static final String VOCAB_1_SOURCE = "vocab 1 source";

    private static final String VOCAB_3_SOURCE = "vocab 3 source";

    private static final long VOCAB_1_SIZE = 11002;

    private static final long VOCAB_2_SIZE = 111002;

    private static final long VOCAB_3_SIZE = 9002;

    private static final String SYMBOL_LABEL = "symbol";

    private static final String TERM_1_ID = "term1";

    private static final String TERM_1_DESCRIPTION = "term 1 description";

    private static final String TERM_1_NAME = "term1_name";

    private static final String TERM_1_SYMBOL = "term1_symbol";

    private static final String CATEGORY_1_NAME = "category1";

    private static final String CATEGORY_2_NAME = "category2";

    @Rule
    public MockitoComponentMockingRule<DomainObjectFactory> mocker =
        new MockitoComponentMockingRule<>(DefaultDomainObjectFactory.class);

    private DomainObjectFactory component;

    @Mock
    private Vocabulary vocabulary1;

    @Mock
    private Vocabulary vocabulary2;

    @Mock
    private Vocabulary vocabulary3;

    @Mock
    private org.phenotips.vocabularies.rest.model.Vocabulary vocabularyRep1;

    @Mock
    private org.phenotips.vocabularies.rest.model.Vocabulary vocabularyRep2;

    @Mock
    private org.phenotips.vocabularies.rest.model.Vocabulary vocabularyRep3;

    @Mock
    private org.phenotips.vocabularies.rest.model.Category categoryRep1;

    @Mock
    private org.phenotips.vocabularies.rest.model.Category categoryRep2;

    @Mock
    private VocabularyTerm term1;

    @Mock
    private Autolinker autolinker;

    @Mock
    private UriInfo uriInfo;

    @Before
    public void setUp() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);

        this.component = this.mocker.getComponentUnderTest();

        // Set up Vocabulary mocks.
        final Set<String> aliasSet1 = new HashSet<>();
        aliasSet1.add(VOCAB_1_ID);
        aliasSet1.add(ALIAS_1);
        when(this.vocabulary1.getIdentifier()).thenReturn(VOCAB_1_ID);
        when(this.vocabulary1.getName()).thenReturn(VOCAB_1_ID);
        when(this.vocabulary1.getAliases()).thenReturn(aliasSet1);
        when(this.vocabulary1.size()).thenReturn(VOCAB_1_SIZE);
        when(this.vocabulary1.getVersion()).thenReturn(VOCAB_1_VERSION);
        when(this.vocabulary1.getDefaultSourceLocation()).thenReturn(VOCAB_1_SOURCE);

        final Set<String> aliasSet2 = new HashSet<>();
        aliasSet2.add(VOCAB_2_ID);
        when(this.vocabulary2.getIdentifier()).thenReturn(VOCAB_2_ID);
        when(this.vocabulary2.getName()).thenReturn(VOCAB_2_ID);
        when(this.vocabulary2.getAliases()).thenReturn(aliasSet2);
        when(this.vocabulary2.size()).thenReturn(VOCAB_2_SIZE);
        when(this.vocabulary2.getVersion()).thenReturn(VOCAB_2_VERSION);
        when(this.vocabulary2.getDefaultSourceLocation()).thenThrow(UnsupportedOperationException.class);

        final Set<String> aliasSet3 = new HashSet<>();
        aliasSet3.add(VOCAB_3_ID);
        when(this.vocabulary3.getIdentifier()).thenReturn(VOCAB_3_ID);
        when(this.vocabulary3.getName()).thenReturn(VOCAB_3_ID);
        when(this.vocabulary3.getAliases()).thenReturn(aliasSet3);
        when(this.vocabulary3.size()).thenReturn(VOCAB_3_SIZE);
        when(this.vocabulary3.getVersion()).thenReturn(null);
        when(this.vocabulary3.getDefaultSourceLocation()).thenReturn(VOCAB_3_SOURCE);

        // Set up VocabularyTerm mocks.
        when(this.term1.getId()).thenReturn(TERM_1_ID);
        when(this.term1.getName()).thenReturn(TERM_1_NAME);
        when(this.term1.getDescription()).thenReturn(TERM_1_DESCRIPTION);
        final JSONObject jsonObject1 = new JSONObject().put(SYMBOL_LABEL, TERM_1_SYMBOL);
        when(this.term1.toJSON()).thenReturn(jsonObject1);

        // Set up links mocks.
        when(this.autolinker.forSecondaryResource(any(Class.class), eq(this.uriInfo))).thenReturn(this.autolinker);
        when(this.autolinker.withActionableResources(any(Class.class)))
            .thenReturn(this.autolinker);
        when(this.autolinker.withExtraParameters(anyString(), anyString())).thenReturn(this.autolinker);
        when(this.autolinker.withGrantedRight(any(Right.class))).thenReturn(this.autolinker);
        when(this.autolinker.build()).thenReturn(Collections.emptyList());
    }

    @Test(expected = NullPointerException.class)
    public void createVocabularyRepresentationWhenVocabularyIsNull()
    {
        this.component.createVocabularyRepresentation(null);
        Assert.fail("Passing a null vocabulary object does not result in a null pointer exception.");
    }

    @Test
    public void createVocabularyRepresentationWhenDefaultSourceLocationIsNotImplemented()
    {
        final org.phenotips.vocabularies.rest.model.Vocabulary vocabulary =
            this.component.createVocabularyRepresentation(this.vocabulary2);

        Assert.assertEquals(VOCAB_2_ID, vocabulary.getIdentifier());
        Assert.assertEquals(VOCAB_2_ID, vocabulary.getName());
        Assert.assertEquals(Collections.singletonList(VOCAB_2_ID), vocabulary.getAliases());
        Assert.assertEquals(VOCAB_2_SIZE, vocabulary.getSize());
        Assert.assertEquals(VOCAB_2_VERSION, vocabulary.getVersion());
        Assert.assertNull(vocabulary.getDefaultSourceLocation());
    }

    @Test
    public void createVocabularyRepresentationCreatesARestRepresentation()
    {
        final org.phenotips.vocabularies.rest.model.Vocabulary vocabulary =
            this.component.createVocabularyRepresentation(this.vocabulary3);

        Assert.assertEquals(VOCAB_3_ID, vocabulary.getIdentifier());
        Assert.assertEquals(VOCAB_3_ID, vocabulary.getName());
        Assert.assertEquals(Collections.singletonList(VOCAB_3_ID), vocabulary.getAliases());
        Assert.assertEquals(VOCAB_3_SIZE, vocabulary.getSize());
        Assert.assertNull(vocabulary.getVersion());
        Assert.assertEquals(VOCAB_3_SOURCE, vocabulary.getDefaultSourceLocation());
    }

    @Test
    public void createCategoryRepresentationContainsExpectedData()
    {
        final Category category = this.component.createCategoryRepresentation(CATEGORY_1_NAME);
        Assert.assertEquals(CATEGORY_1_NAME, category.getCategory());
        Assert.assertNull(category.getVocabularies());
        verify(this.autolinker, never()).build();
    }

    @Test
    public void createLinkedCategoryRepresentationContainsExpectedData()
    {
        final Category category = this.component.createLinkedCategoryRepresentation(CATEGORY_1_NAME, this.autolinker,
            this::vocabulariesSupplier);
        Assert.assertEquals(CATEGORY_1_NAME, category.getCategory());
        Assert.assertEquals(Arrays.asList(this.vocabularyRep1, this.vocabularyRep2, this.vocabularyRep3),
            category.getVocabularies());
        verify(this.autolinker, times(1)).build();
    }

    @Test
    public void createCategoriesListCreatesCorrectCategoryRepresentations()
    {
        final List<String> categoryNames = Arrays.asList(CATEGORY_1_NAME, CATEGORY_2_NAME);
        final List<Category> categories =
            this.component.createCategoriesRepresentation(categoryNames, this.autolinker, null);
        Assert.assertEquals(2, categories.size());
        Assert.assertEquals(CATEGORY_1_NAME, categories.get(0).getCategory());
        Assert.assertNull(categories.get(0).getVocabularies());
        Assert.assertEquals(CATEGORY_2_NAME, categories.get(1).getCategory());
        Assert.assertNull(categories.get(1).getVocabularies());
    }

    @Test
    public void createCategoriesListWithSupplierCreatesCorrectCategoryRepresentations()
    {
        final List<org.phenotips.vocabularies.rest.model.Vocabulary> vocabReps =
            Arrays.asList(this.vocabularyRep1, this.vocabularyRep2, this.vocabularyRep3);
        final List<String> categoryNames = Arrays.asList(CATEGORY_1_NAME, CATEGORY_2_NAME);
        final List<Category> categories =
            this.component.createCategoriesRepresentation(categoryNames, this.autolinker, this::vocabulariesSupplier);
        Assert.assertEquals(2, categories.size());
        Assert.assertEquals(CATEGORY_1_NAME, categories.get(0).getCategory());
        Assert.assertEquals(vocabReps, categories.get(0).getVocabularies());
        Assert.assertEquals(CATEGORY_2_NAME, categories.get(1).getCategory());
        Assert.assertEquals(vocabReps, categories.get(1).getVocabularies());
    }

    @Test
    public void createVocabularyRepresentationContainsExpectedData()
    {
        final org.phenotips.vocabularies.rest.model.Vocabulary vocabulary =
            this.component.createVocabularyRepresentation(this.vocabulary1);
        Assert.assertEquals(VOCAB_1_ID, vocabulary.getIdentifier());
        Assert.assertNull(vocabulary.getCategories());
        verify(this.autolinker, never()).build();
    }

    @Test
    public void createLinkedVocabularyRepresentationContainsExpectedData()
    {
        final List<org.phenotips.vocabularies.rest.model.Category> categoryReps =
            Arrays.asList(this.categoryRep1, this.categoryRep2);

        final org.phenotips.vocabularies.rest.model.Vocabulary vocabulary =
            this.component.createLinkedVocabularyRepresentation(this.vocabulary1, this.autolinker,
                this::categoriesSupplier);
        Assert.assertEquals(VOCAB_1_ID, vocabulary.getIdentifier());
        Assert.assertEquals(categoryReps, vocabulary.getCategories());
        verify(this.autolinker, times(1)).build();
    }

    @Test
    public void createVocabulariesListCreatesCorrectVocabularyRepresentations()
    {
        final Set<Vocabulary> vocabularies = new HashSet<>();
        vocabularies.add(this.vocabulary1);
        vocabularies.add(this.vocabulary2);
        vocabularies.add(this.vocabulary3);
        final List<org.phenotips.vocabularies.rest.model.Vocabulary> vocabularyReps =
            this.component.createVocabulariesRepresentation(vocabularies, this.autolinker, null);
        Assert.assertEquals(3, vocabularyReps.size());

        // Sort by identifier for testing purposes.
        sortById(vocabularyReps);

        final Set<String> aliasSet1 = new HashSet<>();
        aliasSet1.add(VOCAB_1_ID);
        aliasSet1.add(ALIAS_1);
        final org.phenotips.vocabularies.rest.model.Vocabulary vocabRep1 = vocabularyReps.get(0);
        Assert.assertEquals(VOCAB_1_ID, vocabRep1.getIdentifier());
        Assert.assertEquals(VOCAB_1_ID, vocabRep1.getName());
        Assert.assertEquals(2, vocabRep1.getAliases().size());
        Assert.assertEquals(aliasSet1, new HashSet<>(vocabRep1.getAliases()));
        Assert.assertEquals(VOCAB_1_SOURCE, vocabRep1.getDefaultSourceLocation());
        Assert.assertEquals(VOCAB_1_SIZE, vocabRep1.getSize());
        Assert.assertEquals(VOCAB_1_VERSION, vocabRep1.getVersion());
        Assert.assertNull(vocabRep1.getCategories());

        final Set<String> aliasSet2 = new HashSet<>();
        aliasSet2.add(VOCAB_2_ID);
        final org.phenotips.vocabularies.rest.model.Vocabulary vocabRep2 = vocabularyReps.get(1);
        Assert.assertEquals(VOCAB_2_ID, vocabRep2.getIdentifier());
        Assert.assertEquals(VOCAB_2_ID, vocabRep2.getName());
        Assert.assertEquals(1, vocabRep2.getAliases().size());
        Assert.assertEquals(aliasSet2, new HashSet<>(vocabRep2.getAliases()));
        Assert.assertNull(vocabRep2.getDefaultSourceLocation());
        Assert.assertEquals(VOCAB_2_SIZE, vocabRep2.getSize());
        Assert.assertEquals(VOCAB_2_VERSION, vocabRep2.getVersion());
        Assert.assertNull(vocabRep2.getCategories());

        final Set<String> aliasSet3 = new HashSet<>();
        aliasSet3.add(VOCAB_3_ID);
        final org.phenotips.vocabularies.rest.model.Vocabulary vocabRep3 = vocabularyReps.get(2);
        Assert.assertEquals(VOCAB_3_ID, vocabRep3.getIdentifier());
        Assert.assertEquals(VOCAB_3_ID, vocabRep3.getName());
        Assert.assertEquals(1, vocabRep3.getAliases().size());
        Assert.assertEquals(aliasSet3, new HashSet<>(vocabRep3.getAliases()));
        Assert.assertEquals(VOCAB_3_SOURCE, vocabRep3.getDefaultSourceLocation());
        Assert.assertEquals(VOCAB_3_SIZE, vocabRep3.getSize());
        Assert.assertNull(vocabRep3.getVersion());
        Assert.assertNull(vocabRep1.getCategories());
    }

    @Test
    public void createVocabulariesListWithSupplierCreatesCorrectVocabularyRepresentations()
    {
        final List<org.phenotips.vocabularies.rest.model.Category> categoryReps =
            Arrays.asList(this.categoryRep1, this.categoryRep2);

        final Set<Vocabulary> vocabularies = new HashSet<>();
        vocabularies.add(this.vocabulary1);
        vocabularies.add(this.vocabulary2);
        vocabularies.add(this.vocabulary3);
        final List<org.phenotips.vocabularies.rest.model.Vocabulary> vocabularyReps =
            this.component.createVocabulariesRepresentation(vocabularies, this.autolinker, this::categoriesSupplier);
        Assert.assertEquals(3, vocabularyReps.size());

        // Sort by identifier for testing purposes.
        sortById(vocabularyReps);

        final Set<String> aliasSet1 = new HashSet<>();
        aliasSet1.add(VOCAB_1_ID);
        aliasSet1.add(ALIAS_1);
        final org.phenotips.vocabularies.rest.model.Vocabulary vocabRep1 = vocabularyReps.get(0);
        Assert.assertEquals(VOCAB_1_ID, vocabRep1.getIdentifier());
        Assert.assertEquals(VOCAB_1_ID, vocabRep1.getName());
        Assert.assertEquals(2, vocabRep1.getAliases().size());
        Assert.assertEquals(aliasSet1, new HashSet<>(vocabRep1.getAliases()));
        Assert.assertEquals(VOCAB_1_SOURCE, vocabRep1.getDefaultSourceLocation());
        Assert.assertEquals(VOCAB_1_SIZE, vocabRep1.getSize());
        Assert.assertEquals(VOCAB_1_VERSION, vocabRep1.getVersion());
        Assert.assertEquals(categoryReps, vocabRep1.getCategories());

        final Set<String> aliasSet2 = new HashSet<>();
        aliasSet2.add(VOCAB_2_ID);
        final org.phenotips.vocabularies.rest.model.Vocabulary vocabRep2 = vocabularyReps.get(1);
        Assert.assertEquals(VOCAB_2_ID, vocabRep2.getIdentifier());
        Assert.assertEquals(VOCAB_2_ID, vocabRep2.getName());
        Assert.assertEquals(1, vocabRep2.getAliases().size());
        Assert.assertEquals(aliasSet2, new HashSet<>(vocabRep2.getAliases()));
        Assert.assertNull(vocabRep2.getDefaultSourceLocation());
        Assert.assertEquals(VOCAB_2_SIZE, vocabRep2.getSize());
        Assert.assertEquals(VOCAB_2_VERSION, vocabRep2.getVersion());
        Assert.assertEquals(categoryReps, vocabRep2.getCategories());

        final Set<String> aliasSet3 = new HashSet<>();
        aliasSet3.add(VOCAB_3_ID);
        final org.phenotips.vocabularies.rest.model.Vocabulary vocabRep3 = vocabularyReps.get(2);
        Assert.assertEquals(VOCAB_3_ID, vocabRep3.getIdentifier());
        Assert.assertEquals(VOCAB_3_ID, vocabRep3.getName());
        Assert.assertEquals(1, vocabRep3.getAliases().size());
        Assert.assertEquals(aliasSet3, new HashSet<>(vocabRep3.getAliases()));
        Assert.assertEquals(VOCAB_3_SOURCE, vocabRep3.getDefaultSourceLocation());
        Assert.assertEquals(VOCAB_3_SIZE, vocabRep3.getSize());
        Assert.assertNull(vocabRep3.getVersion());
        Assert.assertEquals(categoryReps, vocabRep1.getCategories());
    }

    private void sortById(final List<org.phenotips.vocabularies.rest.model.Vocabulary> vocabularyReps)
    {
        vocabularyReps.sort(Comparator.comparing(org.phenotips.vocabularies.rest.model.Vocabulary::getIdentifier));
    }

    private List<org.phenotips.vocabularies.rest.model.Vocabulary> vocabulariesSupplier(final String categoryId)
    {
        return Arrays.asList(this.vocabularyRep1, this.vocabularyRep2, this.vocabularyRep3);
    }

    private List<org.phenotips.vocabularies.rest.model.Category> categoriesSupplier(final Vocabulary vocabulary)
    {
        return Arrays.asList(this.categoryRep1, this.categoryRep2);
    }
}
