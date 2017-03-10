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
package org.phenotips.vocabulary.annotation.internal;

import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyExtension;
import org.phenotips.vocabulary.VocabularyInputTerm;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
 * Unit tests for {@link DiseaseNotSymptomAnnotationExtension}.
 */
public class DiseaseNotSymptomAnnotationExtensionTest
{
    private static final String ORPHANET_LABEL = "orphanet";

    private static final String ORPHANET_CAPS_LABEL = "ORPHANET";

    private static final String ANNOTATION_SOURCE = "http://compbio.charite.de/jenkins/job/hpo.annotations/"
        + "lastStableBuild/artifact/misc/negative_phenotype_annotation.tab";

    private static final String DIRECT_PHENOTYPES_LABEL = "actual_not_symptom";

    private static final String ALL_ANCESTOR_PHENOTYPES_LABEL = "not_symptom";

    private static final String ROW_RESOURCE = "src/test/resources/negative_annotation.tab";

    private static final String ANCESTORS_LABEL = "term_category";

    private static final Collection<String> TARGETED_VOCABS = new HashSet<>();

    @Rule
    public final MockitoComponentMockingRule<VocabularyExtension> mocker =
        new MockitoComponentMockingRule<VocabularyExtension>(DiseaseNotSymptomAnnotationExtension.class);

    private DiseaseNotSymptomAnnotationExtension extension;

    @Mock
    private VocabularyInputTerm inputTerm;

    @Mock
    private Vocabulary vocabulary;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        this.extension = (DiseaseNotSymptomAnnotationExtension) this.mocker.getComponentUnderTest();
        final VocabularyManager vocabularyManager = this.mocker.getInstance(VocabularyManager.class);

        TARGETED_VOCABS.add(ORPHANET_LABEL);

        when(this.vocabulary.getIdentifier()).thenReturn(ORPHANET_LABEL);

        final VocabularyTerm hpoTerm1 = mock(VocabularyTerm.class);
        final Set<String> ancestors1 = new HashSet<>();
        ancestors1.add("HP:0100006");
        ancestors1.add("HP:0030692");
        final Set<String> term1Ancestors = Collections.unmodifiableSet(ancestors1);
        when(hpoTerm1.get(ANCESTORS_LABEL)).thenReturn(term1Ancestors);

        final VocabularyTerm hpoTerm2 = mock(VocabularyTerm.class);
        final Set<String> term2Ancestors = Collections.emptySet();
        when(hpoTerm2.get(ANCESTORS_LABEL)).thenReturn(term2Ancestors);

        when(vocabularyManager.resolveTerm("HP:0002185")).thenReturn(null);
        when(vocabularyManager.resolveTerm("HP:0030692")).thenReturn(hpoTerm1);
        when(vocabularyManager.resolveTerm("HP:0100315")).thenReturn(hpoTerm2);
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
    public void getDBNameColNumber() throws Exception
    {
        Assert.assertEquals(0, this.extension.getDBNameColNumber());
    }

    @Test
    public void getDiseaseColNumber() throws Exception
    {
        Assert.assertEquals(1, this.extension.getDiseaseColNumber());
    }

    @Test
    public void getPhenotypeColNumber() throws Exception
    {
        Assert.assertEquals(4, this.extension.getPhenotypeColNumber());
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
        when(this.inputTerm.getId()).thenReturn("ORPHA:100070");
        this.extension.extendTerm(this.inputTerm, this.vocabulary);
        final Set<String> directSymptoms = new HashSet<>();
        directSymptoms.add("HP:0002185");
        directSymptoms.add("HP:0030692");
        directSymptoms.add("HP:0100315");

        final Set<String> allSymptoms = new HashSet<>();
        allSymptoms.add("HP:0002185");
        allSymptoms.add("HP:0030692");
        allSymptoms.add("HP:0100315");
        allSymptoms.add("HP:0100006");

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
    public void getRowItem() throws Exception
    {
        final CSVRecord record = getRecordRowData();
        Assert.assertNotNull(record);
        final String dbName = this.extension.getRowItem(record, 0);
        final String diseaseNum = this.extension.getRowItem(record, 1);
        final String symptom = this.extension.getRowItem(record, 4);
        Assert.assertEquals(ORPHANET_CAPS_LABEL, dbName);
        Assert.assertEquals("100070", diseaseNum);
        Assert.assertEquals("HP:0002185", symptom);
    }

    /**
     * Creates annotation data.
     */
    private void createRowsData()
    {
        try (final BufferedReader in = new BufferedReader(
            new InputStreamReader(
                new FileInputStream(ROW_RESOURCE)))) {
            for (final CSVRecord row : CSVFormat.TDF.withSkipHeaderRecord().parse(in)) {
                this.extension.processCSVRecordRow(row, ORPHANET_LABEL);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the CSVRecord row.
     *
     * @return {@link CSVRecord} object
     */
    private CSVRecord getRecordRowData()
    {
        try (final BufferedReader in = new BufferedReader(
            new InputStreamReader(
                new FileInputStream(ROW_RESOURCE)))) {
            return CSVFormat.TDF.withSkipHeaderRecord().parse(in).iterator().next();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
