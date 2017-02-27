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
package org.phenotips.panels.rest.internal;

import org.phenotips.panels.GenePanel;
import org.phenotips.panels.GenePanelFactory;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultGenePanelLoader}.
 *
 * @version $Id$
 * @since 1.3
 */
public class DefaultGenePanelLoaderTest
{
    @Rule
    public MockitoComponentMockingRule<GenePanelLoader> mocker =
        new MockitoComponentMockingRule<GenePanelLoader>(DefaultGenePanelLoader.class);

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private GenePanelLoader genePanelLoader;

    private GenePanelFactory genePanelFactory;

    private VocabularyManager vocabularyManager;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        this.genePanelLoader = this.mocker.getComponentUnderTest();
        this.genePanelFactory = this.mocker.getInstance(GenePanelFactory.class);
        this.vocabularyManager = this.mocker.getInstance(VocabularyManager.class);
    }

    @Test
    public void testGetWhenPanelIsEmpty() throws ExecutionException
    {
        final GenePanel genePanel = mock(GenePanel.class);
        when(this.genePanelFactory.build(anyListOf(VocabularyTerm.class), anyListOf(VocabularyTerm.class)))
            .thenReturn(genePanel);
        this.expectedException.expect(ExecutionException.class);
        this.genePanelLoader.get(Collections.singletonList("HP:001"));
        assertEquals(0, this.genePanelLoader.size());
    }

    @Test
    public void testGetWorksWhenNewDataIsCreatedThenRetrieved() throws ExecutionException
    {
        final GenePanel genePanel = mock(GenePanel.class);
        when(genePanel.size()).thenReturn(5);
        when(this.genePanelFactory.build(anyListOf(VocabularyTerm.class), anyListOf(VocabularyTerm.class)))
            .thenReturn(genePanel);
        final GenePanel firstPanel = this.genePanelLoader.get(
            Arrays.asList("HP:001", "HP:002", "HP:003", "HP:004", "HP:005"));
        assertEquals(1, this.genePanelLoader.size());
        assertEquals(genePanel, firstPanel);

        final GenePanel secondPanel = this.genePanelLoader.get(
            Arrays.asList("HP:001", "HP:002", "HP:003", "HP:004", "HP:005"));
        assertEquals(1, this.genePanelLoader.size());
        assertEquals(genePanel, secondPanel);
    }

    @Test
    public void testInvalidateWorks() throws ExecutionException
    {
        final VocabularyTerm term1 = mock(VocabularyTerm.class);
        final VocabularyTerm term2 = mock(VocabularyTerm.class);
        final VocabularyTerm term3 = mock(VocabularyTerm.class);
        final GenePanel genePanel1 = mock(GenePanel.class);
        final GenePanel genePanel2 = mock(GenePanel.class);

        when(genePanel1.size()).thenReturn(1);
        when(genePanel2.size()).thenReturn(3);

        when(this.genePanelFactory.build(Collections.singleton(term1), Collections.<VocabularyTerm>emptyList()))
            .thenReturn(genePanel1);
        when(this.genePanelFactory.build(new HashSet<>(Arrays.asList(term1, term2, term3)),
            Collections.<VocabularyTerm>emptyList())).thenReturn(genePanel2);

        when(this.vocabularyManager.resolveTerm("HP:001")).thenReturn(term1);
        when(this.vocabularyManager.resolveTerm("HP:002")).thenReturn(term2);
        when(this.vocabularyManager.resolveTerm("HP:003")).thenReturn(term3);

        // The first panel is generated and cached.
        final GenePanel firstPanel = this.genePanelLoader.get(Collections.singletonList("HP:001"));
        assertEquals(1, this.genePanelLoader.size());
        assertEquals(genePanel1, firstPanel);

        // The second panel is generated and cached.
        final GenePanel secondPanel = this.genePanelLoader.get(Arrays.asList("HP:001", "HP:002", "HP:003"));
        assertEquals(2, this.genePanelLoader.size());
        assertEquals(genePanel2, secondPanel);

        // The correct panel is removed from cache.
        this.genePanelLoader.invalidate(Arrays.asList("HP:001", "HP:002", "HP:003"));
        assertEquals(1, this.genePanelLoader.size());
        final GenePanel thirdPanel = this.genePanelLoader.get(Collections.singletonList("HP:001"));
        assertEquals(1, this.genePanelLoader.size());
        assertEquals(genePanel1, thirdPanel);
    }

    @Test
    public void testInvalidateAllWorks() throws ExecutionException
    {
        final VocabularyTerm term1 = mock(VocabularyTerm.class);
        final VocabularyTerm term2 = mock(VocabularyTerm.class);
        final VocabularyTerm term3 = mock(VocabularyTerm.class);
        final GenePanel genePanel1 = mock(GenePanel.class);
        final GenePanel genePanel2 = mock(GenePanel.class);

        when(genePanel1.size()).thenReturn(1);
        when(genePanel2.size()).thenReturn(3);

        when(this.genePanelFactory.build(new HashSet<>(Collections.singletonList(term1)),
            Collections.<VocabularyTerm>emptyList())).thenReturn(genePanel1);
        when(this.genePanelFactory.build(new HashSet<>(Arrays.asList(term1, term2, term3)),
            Collections.<VocabularyTerm>emptyList())).thenReturn(genePanel2);

        when(this.vocabularyManager.resolveTerm("HP:001")).thenReturn(term1);
        when(this.vocabularyManager.resolveTerm("HP:002")).thenReturn(term2);
        when(this.vocabularyManager.resolveTerm("HP:003")).thenReturn(term3);

        // The first panel is generated and cached.
        final GenePanel firstPanel = this.genePanelLoader.get(Collections.singletonList("HP:001"));
        assertEquals(1, this.genePanelLoader.size());
        assertEquals(genePanel1, firstPanel);

        // The second panel is generated and cached.
        final GenePanel secondPanel = this.genePanelLoader.get(Arrays.asList("HP:001", "HP:002", "HP:003"));
        assertEquals(2, this.genePanelLoader.size());
        assertEquals(genePanel2, secondPanel);

        // Cache is emptied.
        this.genePanelLoader.invalidateAll();
        assertEquals(0, this.genePanelLoader.size());
    }

    @Test
    public void testInvalidateWorksWhenCacheIsEmpty()
    {
        assertEquals(0, this.genePanelLoader.size());
        this.genePanelLoader.invalidate(Collections.emptyList());
        assertEquals(0, this.genePanelLoader.size());
        this.genePanelLoader.invalidate(Collections.singletonList("HP:001"));
        assertEquals(0, this.genePanelLoader.size());
    }

    @Test
    public void testInvalidateAllWorksWhenCacheIsEmpty()
    {
        assertEquals(0, this.genePanelLoader.size());
        this.genePanelLoader.invalidateAll();
        assertEquals(0, this.genePanelLoader.size());
    }
}
