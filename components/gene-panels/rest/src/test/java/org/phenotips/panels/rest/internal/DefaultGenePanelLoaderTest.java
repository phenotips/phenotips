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

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultGenePanelLoader}.
 *
 * @version $Id$
 * @since 1.3
 */
public class DefaultGenePanelLoaderTest
{
    private static final String TERM_1 = "HP:001";

    private static final String TERM_2 = "HP:002";

    private static final String TERM_3 = "HP:003";

    private static final String TERM_4 = "HP:004";

    private static final String TERM_5 = "HP:005";

    private static final String GENE_1 = "gene1";

    private static final String GENE_2 = "gene2";

    @Rule
    public MockitoComponentMockingRule<GenePanelLoader> mocker =
        new MockitoComponentMockingRule<>(DefaultGenePanelLoader.class);

    @Mock
    private GenePanel genePanel1;

    @Mock
    private GenePanel genePanel2;

    @Mock
    private VocabularyTerm term1;

    @Mock
    private VocabularyTerm term2;

    @Mock
    private VocabularyTerm term3;

    private GenePanelLoader genePanelLoader;

    private GenePanelFactory genePanelFactory;

    private Set<String> presentSet1;

    private Set<String> presentSet2;

    private Set<String> presentSet3;

    private Set<String> geneSet;

    private Set<VocabularyTerm> presentTerms1;

    private Set<VocabularyTerm> presentTerms2;

    @Before
    public void setUp() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        this.genePanelLoader = this.mocker.getComponentUnderTest();
        this.genePanelFactory = this.mocker.getInstance(GenePanelFactory.class);

        final VocabularyManager vocabularyManager = this.mocker.getInstance(VocabularyManager.class);

        when(vocabularyManager.resolveTerm(TERM_1)).thenReturn(this.term1);
        when(vocabularyManager.resolveTerm(TERM_2)).thenReturn(this.term2);
        when(vocabularyManager.resolveTerm(TERM_3)).thenReturn(this.term3);

        when(this.genePanelFactory.build(anyCollectionOf(VocabularyTerm.class), anyCollectionOf(VocabularyTerm.class),
            anyCollectionOf(VocabularyTerm.class))).thenReturn(this.genePanel1);

        this.presentSet1 = new HashSet<>();
        this.presentSet1.add(TERM_1);

        this.presentSet2 = new HashSet<>();
        this.presentSet2.add(TERM_1);
        this.presentSet2.add(TERM_2);
        this.presentSet2.add(TERM_3);

        this.presentSet3 = new HashSet<>();
        this.presentSet3.add(TERM_1);
        this.presentSet3.add(TERM_2);
        this.presentSet3.add(TERM_3);
        this.presentSet3.add(TERM_4);
        this.presentSet3.add(TERM_5);

        this.geneSet = new HashSet<>();
        this.geneSet.add(GENE_1);
        this.geneSet.add(GENE_2);

        this.presentTerms1 = new HashSet<>();
        this.presentTerms1.add(this.term1);

        this.presentTerms2 = new HashSet<>();
        this.presentTerms2.add(this.term1);
        this.presentTerms2.add(this.term2);
        this.presentTerms2.add(this.term3);
    }

    @Test(expected = ExecutionException.class)
    public void testGetWhenPanelIsEmpty() throws ExecutionException
    {
        final PanelData data = new PanelData(this.presentSet1, Collections.emptySet(),
            Collections.emptySet());
        this.genePanelLoader.get(data);
        assertEquals(0, this.genePanelLoader.size());
    }

    @Test
    public void testGetWorksWhenNewDataIsCreatedThenRetrieved() throws ExecutionException
    {
        when(this.genePanel1.size()).thenReturn(5);
        final PanelData data1 = new PanelData(this.presentSet3, Collections.emptySet(), this.geneSet);
        final GenePanel firstPanel = this.genePanelLoader.get(data1);
        assertEquals(1, this.genePanelLoader.size());
        assertEquals(this.genePanel1, firstPanel);

        final PanelData data2 = new PanelData(this.presentSet3, Collections.emptySet(), this.geneSet);
        final GenePanel secondPanel = this.genePanelLoader.get(data2);
        // The panel should only be built once. The second time it should be retrieved from cache.
        verify(this.genePanelFactory, times(1)).build(anyCollectionOf(VocabularyTerm.class),
            anyCollectionOf(VocabularyTerm.class), anyCollectionOf(VocabularyTerm.class));
        assertEquals(1, this.genePanelLoader.size());
        assertEquals(this.genePanel1, secondPanel);
    }

    @Test
    public void testInvalidateWorks() throws ExecutionException
    {
        when(this.genePanel1.size()).thenReturn(1);
        when(this.genePanel2.size()).thenReturn(3);

        when(this.genePanelFactory.build(this.presentTerms1, Collections.emptySet(),
            Collections.emptySet())).thenReturn(this.genePanel1);
        when(this.genePanelFactory.build(this.presentTerms2, Collections.emptySet(),
            Collections.emptySet())).thenReturn(this.genePanel2);

        // The first panel is generated and cached.
        final PanelData data1 = new PanelData(this.presentSet1, Collections.emptySet(),
            Collections.emptySet());
        final GenePanel firstPanel = this.genePanelLoader.get(data1);
        assertEquals(1, this.genePanelLoader.size());
        assertEquals(this.genePanel1, firstPanel);

        // The second panel is generated and cached.
        final PanelData data2 = new PanelData(this.presentSet2, Collections.emptySet(),
            Collections.emptySet());
        final GenePanel secondPanel = this.genePanelLoader.get(data2);
        assertEquals(2, this.genePanelLoader.size());
        assertEquals(this.genePanel2, secondPanel);

        // The correct panel is removed from cache.
        final PanelData data3 = new PanelData(this.presentSet2, Collections.emptySet(),
            Collections.emptySet());
        this.genePanelLoader.invalidate(data3);
        assertEquals(1, this.genePanelLoader.size());
        final GenePanel thirdPanel = this.genePanelLoader.get(data1);
        assertEquals(1, this.genePanelLoader.size());
        assertEquals(this.genePanel1, thirdPanel);
    }

    @Test
    public void testInvalidateAllWorks() throws ExecutionException
    {
        when(this.genePanel1.size()).thenReturn(1);
        when(this.genePanel2.size()).thenReturn(3);

        when(this.genePanelFactory.build(this.presentTerms1, Collections.emptySet(),
            Collections.emptySet())).thenReturn(this.genePanel1);
        when(this.genePanelFactory.build(this.presentTerms2, Collections.emptySet(),
            Collections.emptySet())).thenReturn(this.genePanel2);

        // The first panel is generated and cached.
        final PanelData data1 = new PanelData(this.presentSet1, Collections.emptySet(),
            Collections.emptySet());
        final GenePanel firstPanel = this.genePanelLoader.get(data1);
        assertEquals(1, this.genePanelLoader.size());
        assertEquals(this.genePanel1, firstPanel);

        // The second panel is generated and cached.
        final PanelData data2 = new PanelData(this.presentSet2, Collections.emptySet(),
            Collections.emptySet());
        final GenePanel secondPanel = this.genePanelLoader.get(data2);
        assertEquals(2, this.genePanelLoader.size());
        assertEquals(this.genePanel2, secondPanel);

        // Cache is emptied.
        this.genePanelLoader.invalidateAll();
        assertEquals(0, this.genePanelLoader.size());
    }

    @Test
    public void testInvalidateWorksWhenCacheIsEmpty()
    {
        assertEquals(0, this.genePanelLoader.size());
        this.genePanelLoader.invalidate(Collections.emptyList());
        final PanelData data1 = new PanelData(this.presentSet1, Collections.emptySet(),
            Collections.emptySet());
        assertEquals(0, this.genePanelLoader.size());
        this.genePanelLoader.invalidate(data1);
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
