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
package org.phenotips.vocabulary.internal.hpoannotations;

import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyExtension;
import org.phenotips.vocabulary.VocabularyInputTerm;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GeneForPhenotypesAnnotationsExtension}.
 */
public class GeneForPhenotypeAnnotationsExtensionTest
{
    private static final String HPO_LABEL = "hpo";

    private static final String ANNOTATION_SOURCE = "http://compbio.charite.de/jenkins/job/hpo.annotations.monthly/"
        + "lastStableBuild/artifact/annotation/ALL_SOURCES_ALL_FREQUENCIES_phenotype_to_genes.txt";

    private static final String GENES_LABEL = "associated_genes";

    @Rule
    public final MockitoComponentMockingRule<VocabularyExtension> mocker =
        new MockitoComponentMockingRule<VocabularyExtension>(GeneForPhenotypesAnnotationsExtension.class);

    private GeneForPhenotypesAnnotationsExtension extension;

    @Mock
    private Vocabulary vocabulary;

    @Mock
    private VocabularyInputTerm inputTerm;

    @Before
    public void setUp() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);
        this.extension = Mockito.spy((GeneForPhenotypesAnnotationsExtension) this.mocker.getComponentUnderTest());

        when(this.vocabulary.getIdentifier()).thenReturn(HPO_LABEL);
    }

    @Test
    public void extendTermTermNotModifiedIfNoAssociatedGenes()
    {
        when(this.extension.getAnnotationSource())
            .thenReturn(this.getClass().getClassLoader().getResource("gene-annotations.txt").toExternalForm());
        when(this.inputTerm.getId()).thenReturn("HP:0004691");
        this.extension.indexingStarted(this.vocabulary);
        this.extension.extendTerm(this.inputTerm, this.vocabulary);
        this.extension.indexingEnded(this.vocabulary);

        verify(this.inputTerm, never()).set(anyString(), any());
    }

    @Test
    public void extendTermTermISModifiedIfAssociatedGenesExist()
    {
        when(this.extension.getAnnotationSource())
            .thenReturn(this.getClass().getClassLoader().getResource("gene-annotations.txt").toExternalForm());
        when(this.inputTerm.getId()).thenReturn("HP:0010708");
        this.extension.indexingStarted(this.vocabulary);
        this.extension.extendTerm(this.inputTerm, this.vocabulary);
        this.extension.indexingEnded(this.vocabulary);

        final List<String> associatedGenes = Arrays.asList("SHH", "LMBR1");

        verify(this.inputTerm, times(1)).set(GENES_LABEL, associatedGenes);
        verify(this.inputTerm, times(1)).getId();
        verifyNoMoreInteractions(this.inputTerm);
    }

    @Test
    public void getTargetVocabularyIds()
    {
        final List<String> targetedVocabs = new LinkedList<>();
        targetedVocabs.add(HPO_LABEL);
        Assert.assertEquals(targetedVocabs, this.extension.getTargetVocabularyIds());
    }

    @Test
    public void getAnnotationSource()
    {
        // Assert.assertEquals(ANNOTATION_SOURCE, this.extension.getAnnotationSource());
    }

    @Test
    public void isVocabularySupported()
    {
        Assert.assertTrue(this.extension.isVocabularySupported(this.vocabulary));

        final Vocabulary wrongVocabulary = mock(Vocabulary.class);
        when(wrongVocabulary.getIdentifier()).thenReturn("WRONG");
        Assert.assertFalse(this.extension.isVocabularySupported(wrongVocabulary));
    }
}
