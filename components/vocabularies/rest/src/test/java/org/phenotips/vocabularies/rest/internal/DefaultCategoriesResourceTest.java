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
import org.phenotips.rest.model.Link;
import org.phenotips.security.authorization.AuthorizationService;
import org.phenotips.vocabularies.rest.CategoriesResource;
import org.phenotips.vocabularies.rest.DomainObjectFactory;
import org.phenotips.vocabularies.rest.model.Categories;
import org.phenotips.vocabularies.rest.model.Category;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Provider;
import javax.ws.rs.core.UriInfo;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWikiContext;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

/**
 * Tests for the {@link DefaultCategoriesResource} class.
 */
public class DefaultCategoriesResourceTest
{
    private static final String CATEGORY_A_LABEL = "A";

    private static final String CATEGORY_B_LABEL = "B";

    private static final String CATEGORY_C_LABEL = "C";

    @Rule
    public MockitoComponentMockingRule<CategoriesResource> mocker =
        new MockitoComponentMockingRule<CategoriesResource>(DefaultCategoriesResource.class);

    @Mock
    private Provider<Autolinker> autolinkerProvider;

    @Mock
    private Vocabulary vocabA1;

    @Mock
    private Vocabulary vocabA2;

    @Mock
    private Vocabulary vocabB1;

    @Mock
    private Vocabulary vocabC1;

    @Mock
    private Vocabulary vocabC2;

    @Mock
    private org.phenotips.vocabularies.rest.model.Vocabulary vocabRepA1;

    @Mock
    private org.phenotips.vocabularies.rest.model.Vocabulary vocabRepA2;

    @Mock
    private org.phenotips.vocabularies.rest.model.Vocabulary vocabRepB1;

    @Mock
    private org.phenotips.vocabularies.rest.model.Vocabulary vocabRepC1;

    @Mock
    private org.phenotips.vocabularies.rest.model.Vocabulary vocabRepC2;

    @Mock
    private Category categoryA;

    @Mock
    private Category categoryB;

    @Mock
    private Category categoryC;

    @Before
    public void setUp() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);

        final Execution execution = mock(Execution.class);
        final ExecutionContext executionContext = mock(ExecutionContext.class);
        final ComponentManager componentManager = this.mocker.getInstance(ComponentManager.class, "context");
        when(componentManager.getInstance(Execution.class)).thenReturn(execution);
        when(execution.getContext()).thenReturn(executionContext);
        when(executionContext.getProperty("xwikicontext")).thenReturn(mock(XWikiContext.class));

        final VocabularyManager vocabularyManager = this.mocker.getInstance(VocabularyManager.class);
        final DomainObjectFactory objectFactory = this.mocker.getInstance(DomainObjectFactory.class);
        final UserManager users = this.mocker.getInstance(UserManager.class);
        final AuthorizationService authorizationService = this.mocker.getInstance(AuthorizationService.class);

        final Set<Vocabulary> categoryAVocabs = new HashSet<>(Arrays.asList(this.vocabA1, this.vocabA2));
        final Set<Vocabulary> categoryBVocabs = new HashSet<>(Collections.singletonList(this.vocabB1));
        final Set<Vocabulary> categoryCVocabs = new HashSet<>(Arrays.asList(this.vocabC1, this.vocabC2));

        when(vocabularyManager.getAvailableCategories()).thenReturn(
            Arrays.asList(CATEGORY_A_LABEL, CATEGORY_B_LABEL, CATEGORY_C_LABEL));
        when(vocabularyManager.getVocabularies(CATEGORY_A_LABEL)).thenReturn(categoryAVocabs);
        when(vocabularyManager.getVocabularies(CATEGORY_B_LABEL)).thenReturn(categoryBVocabs);
        when(vocabularyManager.getVocabularies(CATEGORY_C_LABEL)).thenReturn(categoryCVocabs);

        final List<org.phenotips.vocabularies.rest.model.Vocabulary> categoryAVocabReps =
            Arrays.asList(this.vocabRepA1, this.vocabRepA2);
        final List<org.phenotips.vocabularies.rest.model.Vocabulary> categoryBVocabReps =
            Collections.singletonList(this.vocabRepB1);
        final List<org.phenotips.vocabularies.rest.model.Vocabulary> categoryCVocabReps =
            Arrays.asList(this.vocabRepC1, this.vocabRepC2);

        when(objectFactory.createVocabulariesList(eq(categoryAVocabs), any(Autolinker.class),
            any(UriInfo.class), anyBoolean())).thenReturn(categoryAVocabReps);
        when(objectFactory.createVocabulariesList(eq(categoryBVocabs), any(Autolinker.class),
            any(UriInfo.class), anyBoolean())).thenReturn(categoryBVocabReps);
        when(objectFactory.createVocabulariesList(eq(categoryCVocabs), any(Autolinker.class),
            any(UriInfo.class), anyBoolean())).thenReturn(categoryCVocabReps);

        when(objectFactory.createCategoryRepresentation(CATEGORY_A_LABEL)).thenReturn(categoryA);
        when(objectFactory.createCategoryRepresentation(CATEGORY_B_LABEL)).thenReturn(categoryB);
        when(objectFactory.createCategoryRepresentation(CATEGORY_C_LABEL)).thenReturn(categoryC);

        final User user = mock(User.class);
        when(users.getCurrentUser()).thenReturn(user);
        when(authorizationService.hasAccess(eq(user), eq(Right.ADMIN), any(DocumentReference.class)))
            .thenReturn(true);

        ReflectionUtils.setFieldValue(this.mocker.getComponentUnderTest(), "autolinker", this.autolinkerProvider);

        final Autolinker autolinker = mock(Autolinker.class);
        when(this.autolinkerProvider.get()).thenReturn(autolinker);
        when(autolinker.forSecondaryResource(any(Class.class), any(UriInfo.class))).thenReturn(autolinker);
        when(autolinker.forResource(any(Class.class), any(UriInfo.class))).thenReturn(autolinker);
        when(autolinker.withActionableResources(any(Class.class))).thenReturn(autolinker);
        when(autolinker.withExtraParameters(anyString(), anyString())).thenReturn(autolinker);
        when(autolinker.withGrantedRight(any(Right.class))).thenReturn(autolinker);
        when(autolinker.build()).thenReturn(Collections.singletonList(mock(Link.class)));

        when(categoryA.withLinks(anyListOf(Link.class))).thenReturn(categoryA);
        when(categoryA.withVocabularies(categoryAVocabReps)).thenCallRealMethod();
        when(categoryB.withLinks(anyListOf(Link.class))).thenReturn(categoryB);
        when(categoryB.withVocabularies(categoryBVocabReps)).thenCallRealMethod();
        when(categoryC.withLinks(anyListOf(Link.class))).thenReturn(categoryC);
        when(categoryC.withVocabularies(categoryCVocabReps)).thenCallRealMethod();
    }

    @Test
    public void getAllCategories() throws Exception
    {
        final Categories categories = this.mocker.getComponentUnderTest().getAllCategories();
        final Collection<Category> categoryCollection = categories.getCategories();
        Assert.assertEquals(3, categoryCollection.size());
        Assert.assertTrue(categoryCollection.contains(categoryA));
        Assert.assertTrue(categoryCollection.contains(categoryB));
        Assert.assertTrue(categoryCollection.contains(categoryC));
        verify(categoryA, times(1)).setVocabularies(Arrays.asList(this.vocabRepA1, this.vocabRepA2));
        verify(categoryB, times(1)).setVocabularies(Collections.singletonList(this.vocabRepB1));
        verify(categoryC, times(1)).setVocabularies(Arrays.asList(this.vocabRepC1, this.vocabRepC2));
    }
}
