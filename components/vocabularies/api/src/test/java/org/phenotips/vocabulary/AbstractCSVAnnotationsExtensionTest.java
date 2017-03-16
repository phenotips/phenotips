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
package org.phenotips.vocabulary;

import org.xwiki.component.annotation.Component;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.inject.Named;

import org.apache.commons.csv.CSVFormat;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

/**
 * Unit tests for {@link AbstractCSVAnnotationsExtension}.
 */
public class AbstractCSVAnnotationsExtensionTest
{
    @Rule
    public final MockitoComponentMockingRule<VocabularyExtension> mocker =
        new MockitoComponentMockingRule<VocabularyExtension>(TestingCSVAnnotationsExtension.class);

    private AbstractCSVAnnotationsExtension extension;

    @Mock
    private VocabularyInputTerm inputTerm;

    @Mock
    private Vocabulary vocabulary;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        when(this.vocabulary.getIdentifier()).thenReturn("hpo");
        this.extension = (AbstractCSVAnnotationsExtension) this.mocker.getComponentUnderTest();
    }

    @Test
    public void termThatHasNoAssociatedDataIsNotAltered() throws Exception
    {
        this.extension.indexingStarted(this.vocabulary);
        when(this.inputTerm.getId()).thenReturn("HP:1234567");
        this.extension.extendTerm(this.inputTerm, this.vocabulary);
        this.extension.indexingEnded(this.vocabulary);
        verify(this.inputTerm, never()).set(anyString(), any());
    }

    @Test
    public void termThatHasAssociatedDataIsModified() throws Exception
    {
        this.extension.indexingStarted(this.vocabulary);
        when(this.inputTerm.getId()).thenReturn("HP:0001518");
        this.extension.extendTerm(this.inputTerm, this.vocabulary);
        this.extension.indexingEnded(this.vocabulary);

        verify(this.inputTerm, times(1)).getId();
        verify(this.inputTerm, times(1)).set("implied_qualifiers", Arrays.asList("HP:0003623", "HP:0012832"));
        verify(this.inputTerm, times(1)).set("url",
            Arrays.asList("http://compbio.charite.de/hpoweb/showterm?id=HP:0001518"));
        verifyNoMoreInteractions(this.inputTerm);
    }

    @Test
    public void emptyDataIsNotAdded() throws Exception
    {
        this.extension.indexingStarted(this.vocabulary);
        when(this.inputTerm.getId()).thenReturn("HP:0009737");
        this.extension.extendTerm(this.inputTerm, this.vocabulary);
        this.extension.indexingEnded(this.vocabulary);

        verify(this.inputTerm, times(1)).getId();
        verify(this.inputTerm, times(1)).set("url",
            Arrays.asList("http://compbio.charite.de/hpoweb/showterm?id=HP:0009737"));
        verify(this.inputTerm, never()).set(eq("implied_qualifiers"), any());
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

    @Component
    @Named("test")
    public static final class TestingCSVAnnotationsExtension extends AbstractCSVAnnotationsExtension
    {
        @Override
        protected Collection<String> getTargetVocabularyIds()
        {
            return Collections.singleton("hpo");
        }

        @Override
        protected String getAnnotationSource()
        {
            return this.getClass().getClassLoader().getResource("annotations.tdf").toExternalForm();
        }

        @Override
        protected CSVFormat setupCSVParser(Vocabulary vocabulary)
        {
            return CSVFormat.TDF.withHeader("id", "url", null, "implied_qualifiers");
        }
    }
}
