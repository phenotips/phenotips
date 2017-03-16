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
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

/**
 * Unit tests for {@link PositiveHPOAnnotationsExtension}.
 */
public class PositiveHPOAnnotationsExtensionTest
{
    private static final String ANNOTATION_SOURCE = "http://compbio.charite.de/jenkins/job/hpo.annotations/"
        + "lastStableBuild/artifact/misc/phenotype_annotation.tab";

    private static final String DIRECT_PHENOTYPES_LABEL = "actual_symptom";

    private static final String ALL_ANCESTOR_PHENOTYPES_LABEL = "symptom";

    private static final String ROW_RESOURCE = "src/test/resources/annotation.tab";

    private static final Collection<String> TARGETED_VOCABS =
        Collections.unmodifiableList(Arrays.asList("omim", "orphanet", "decipher"));

    @Rule
    public final MockitoComponentMockingRule<VocabularyExtension> mocker =
        new MockitoComponentMockingRule<VocabularyExtension>(PositiveHPOAnnotationsExtension.class);

    private PositiveHPOAnnotationsExtension extension;

    @Mock
    private VocabularyInputTerm inputTerm;

    @Mock
    private Vocabulary vocabulary;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        this.extension = (PositiveHPOAnnotationsExtension) this.mocker.getComponentUnderTest();
        final VocabularyManager vocabularyManager = this.mocker.getInstance(VocabularyManager.class);

        when(this.vocabulary.getIdentifier()).thenReturn("orphanet");

        final VocabularyTerm hpoTerm1 = mock(VocabularyTerm.class);

        final Set<VocabularyTerm> term1Ancestors = new HashSet<>();
        final VocabularyTerm hpoTerm1Ancestor1 = mock(VocabularyTerm.class);
        when(hpoTerm1Ancestor1.getId()).thenReturn("HP:0003117");
        term1Ancestors.add(hpoTerm1Ancestor1);
        final VocabularyTerm hpoTerm1Ancestor2 = mock(VocabularyTerm.class);
        when(hpoTerm1Ancestor2.getId()).thenReturn("HP:0000818");
        term1Ancestors.add(hpoTerm1Ancestor2);
        when(hpoTerm1.getAncestors()).thenReturn(term1Ancestors);

        final VocabularyTerm hpoTerm2 = mock(VocabularyTerm.class);
        final Set<VocabularyTerm> term2Ancestors = Collections.emptySet();
        when(hpoTerm2.getAncestors()).thenReturn(term2Ancestors);

        when(vocabularyManager.resolveTerm("HP:0003155")).thenReturn(hpoTerm1);
        when(vocabularyManager.resolveTerm("HP:0003162")).thenReturn(null);
        when(vocabularyManager.resolveTerm("HP:0004359")).thenReturn(hpoTerm2);
        createRowsData();
    }

    @Test
    public void getTargetVocabularyIds() throws Exception
    {
        Assert.assertEquals(TARGETED_VOCABS, this.extension.getTargetVocabularyIds());
    }

    @Test
    public void getAnnotationSource() throws Exception
    {
        Assert.assertEquals(ANNOTATION_SOURCE, this.extension.getAnnotationSource());
    }

    @Test
    public void getDirectPhenotypesLabel() throws Exception
    {
        Assert.assertEquals(DIRECT_PHENOTYPES_LABEL, this.extension.getDirectPhenotypesLabel());
    }

    @Test
    public void getAllAncestorPhenotypesLabel() throws Exception
    {
        Assert.assertEquals(ALL_ANCESTOR_PHENOTYPES_LABEL, this.extension.getAllAncestorPhenotypesLabel());
    }

    @Test
    public void extendTermVocabularyTermThatHasNoAssociatedPhenotypeDataIsNotModified() throws Exception
    {
        when(this.inputTerm.getId()).thenReturn("ORPHA:1457");
        this.extension.extendTerm(this.inputTerm, this.vocabulary);
        verify(this.inputTerm, never()).set(anyString(), any());
    }

    @Test
    public void extendTermVocabularyTermThatHasAssociatedPhenotypeDataIsModified() throws Exception
    {
        when(this.inputTerm.getId()).thenReturn("ORPHA:263455");
        this.extension.extendTerm(this.inputTerm, this.vocabulary);
        final Set<String> directSymptoms = new HashSet<>();
        directSymptoms.add("HP:0003155");
        directSymptoms.add("HP:0003162");
        directSymptoms.add("HP:0004359");

        final Set<String> allSymptoms = new HashSet<>();
        allSymptoms.add("HP:0003155");
        allSymptoms.add("HP:0003162");
        allSymptoms.add("HP:0004359");
        allSymptoms.add("HP:0003117");
        allSymptoms.add("HP:0000818");

        verify(this.inputTerm, times(1)).set(DIRECT_PHENOTYPES_LABEL, directSymptoms);
        verify(this.inputTerm, times(1)).set(ALL_ANCESTOR_PHENOTYPES_LABEL, allSymptoms);
        verify(this.inputTerm, times(1)).getId();
        verifyNoMoreInteractions(this.inputTerm);
    }

    @Test
    public void isVocabularySupported() throws Exception
    {
        Assert.assertTrue(this.extension.isVocabularySupported(this.vocabulary));

        final Vocabulary wrongVocabulary = mock(Vocabulary.class);
        when(wrongVocabulary.getIdentifier()).thenReturn("WRONG");

        Assert.assertFalse(this.extension.isVocabularySupported(wrongVocabulary));
    }

    @Test
    public void tdfParserIsUsed() throws Exception
    {
        final CSVFormat result = this.extension.setupCSVParser(this.vocabulary);
        Assert.assertEquals(CSVFormat.TDF, result);
    }

    /**
     * Creates annotation data.
     */
    private void createRowsData() throws FileNotFoundException, IOException
    {
        try (BufferedReader in = new BufferedReader(
            new InputStreamReader(
                new FileInputStream(ROW_RESOURCE)))) {
            for (final CSVRecord row : this.extension.setupCSVParser(this.vocabulary).parse(in)) {
                this.extension.processCSVRecordRow(row, this.vocabulary);
            }
        }
    }
}
