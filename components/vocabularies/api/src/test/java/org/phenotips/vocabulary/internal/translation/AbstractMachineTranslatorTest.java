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
package org.phenotips.vocabulary.internal.translation;

import org.phenotips.vocabulary.MachineTranslator;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyInputTerm;
import org.phenotips.vocabulary.internal.solr.SolrVocabularyInputTerm;

import org.xwiki.environment.Environment;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import static org.hamcrest.Matchers.containsInAnyOrder;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.solr.common.SolrInputDocument;

/**
 * Tests the common functionality in the abstract machine translator.
 * Works via a dummy concrete child.
 *
 * @version $Id$
 */
public class AbstractMachineTranslatorTest
{
    /**
     * The name of the vocabulary.
     */
    private static final String VOC_NAME = "dummy";

    private static final String LANG = "es";

    /**
     * The mocker.
     */
    @Rule
    public final MockitoComponentMockingRule<AbstractMachineTranslator> mocker =
        new MockitoComponentMockingRule<AbstractMachineTranslator>(DummyMachineTranslator.class);

    /**
     * The temporary home of our translator.
     */
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /**
     * The component under test.
     */
    private AbstractMachineTranslator translator;

    /**
     * A vocabulary input term meant to already exist in the dummy translation file.
     * Check out dummy_dummy_es.xliff to see how it's specified.
     */
    private VocabularyInputTerm term1;

    /**
     * A vocabulary input term that is _not_ in the dummy translation file already.
     */
    private VocabularyInputTerm term2;

    /**
     * The translation fields we care about.
     */
    private Collection<String> fields = new HashSet<>();

    /**
     * A set of synonyms.
     */
    private Set<String> synonyms = new HashSet<>();

    /**
     * The set of "translated" synonyms.
     */
    private Set<String> translatedSynonyms = new HashSet<>();

    /**
     * The added up length of the synonyms.
     */
    private int synonymsLength = 0;

    /**
     * Set up the test.
     */
    @Before
    public void setUp() throws Exception
    {
        Locale locale = new Locale(LANG);
        LocalizationContext ctx = mocker.getInstance(LocalizationContext.class);
        when(ctx.getCurrentLocale()).thenReturn(locale);

        Environment environment = mocker.getInstance(Environment.class);
        /* Gotta make sure it has a home so we can test that things are properly
         * persisted */
        when(environment.getPermanentDirectory()).thenReturn(folder.getRoot());

        Vocabulary vocabulary = mock(Vocabulary.class);

        synonyms.clear();
        translatedSynonyms.clear();
        synonyms.add("Newcastle United");
        synonyms.add("West Ham");
        synonyms.add("Blackburn Rovers");
        for (String synonym : synonyms) {
            synonymsLength += synonym.length();
            translatedSynonyms.add("El " + synonym);
        }

        term1 = new SolrVocabularyInputTerm(new SolrInputDocument(), vocabulary);
        term1.setId("DUM:0001");
        term1.setName("Dummy");
        term1.setDescription("Definition");
        term1.set("synonym", synonyms);

        term2 = new SolrVocabularyInputTerm(new SolrInputDocument(), vocabulary);
        term2.setId("DUM:0002");
        term2.setName("Whatever");
        term2.setDescription("Definitions!");
        term2.set("synonym", synonyms);

        term1 = spy(term1);
        term2 = spy(term2);

        fields.clear();
        fields.add("name");

        translator = spy(mocker.getComponentUnderTest());
        translator.loadVocabulary(VOC_NAME, LANG);
    }

    @After
    public void tearDown()
    {
        translator.unloadVocabulary(VOC_NAME, LANG);
    }

    /**
     * Test that the machine translator will not retranslate something that's already
     * in the file.
     */
    @Test
    public void testNoReTranslate()
    {
        long count = translator.translate(VOC_NAME, LANG, term1, fields);
        assertEquals(0, count);
        verify(translator, never()).doTranslate(term1.getName(), LANG);
        verify(term1).set("name_es", "El Dummy");
        verify(term1, never()).set(eq("name"), any(String.class));
    }

    /**
     * Test that the machine translator will translate when necessary.
     */
    @Test
    public void testDoTranslate()
    {
        long count = translator.translate(VOC_NAME, LANG, term2, fields);
        assertEquals(term2.getName().length(), count);
        verify(translator).doTranslate(term2.getName(), LANG);
        verify(term2).set("name_es", "El Whatever");
        verify(term2, never()).set(eq("name"), any(String.class));
    }

    /**
     * Test that previously performed translations will be cached.
     */
    @Test
    public void testRemember()
    {
        translator.translate(VOC_NAME, LANG, term2, fields);
        long count = translator.translate(VOC_NAME, LANG, term2, fields);
        assertEquals(0, count);
        verify(translator, times(1)).doTranslate(term2.getName(), LANG);
        verify(term2, times(2)).set("name_es", "El Whatever");
        verify(term2, never()).set(eq("name"), any(String.class));
    }

    /**
     * Test that we can't translate unless loadVocabulary has been invoked.
     */
    @Test
    public void testNotWhenUnloaded()
    {
        translator.unloadVocabulary(VOC_NAME, LANG);
        try {
            translator.translate(VOC_NAME, LANG, term1, fields);
            fail("Did not throw on translate when unloaded");
        } catch (IllegalStateException e) {
            /* So tearDown doesn't fail */
            translator.loadVocabulary(VOC_NAME, LANG);
        }
    }

    /**
     * Test that we can cope with new fields.
     */
    @Test
    public void testNewField()
    {
        fields.add("def");
        long count = translator.translate(VOC_NAME, LANG, term1, fields);
        assertEquals(term1.getDescription().length(), count);
        verify(term1).set("def_es", "El " + term1.getDescription());
        verify(term1, never()).set(eq("def"), any(String.class));
        verify(translator).doTranslate(term1.getDescription(), LANG);
    }

    /**
     * Test that previously performed translations are remembered accross
     * restarts of the component.
     */
    @Test
    public void testPersist()
    {
        translator.translate(VOC_NAME, LANG, term2, fields);
        translator.unloadVocabulary(VOC_NAME, LANG);
        translator.loadVocabulary(VOC_NAME, LANG);
        long count = translator.translate(VOC_NAME, LANG, term2, fields);
        assertEquals(0, count);
        verify(translator, times(1)).doTranslate(term2.getName(), LANG);
        verify(term2, times(2)).set("name_es", "El Whatever");
        verify(term2, never()).set(eq("name"), any(String.class));
    }

    /**
     * Test that a newly added field (to an already existing term)
     * will have its translation persisted.
     */
    @Test
    public void testNewFieldPersisted()
    {
        fields.add("def");
        translator.translate(VOC_NAME, LANG, term1, fields);
        translator.unloadVocabulary(VOC_NAME, LANG);
        translator.loadVocabulary(VOC_NAME, LANG);
        long count = translator.translate(VOC_NAME, LANG, term1, fields);
        assertEquals(0, count);
        /* One time for each translation */
        verify(term1, times(2)).set("def_es", "El Definition");
        /* Only once: for the first translation */
        verify(translator, times(1)).doTranslate(term1.getDescription(), LANG);
        verify(term1, never()).set(eq("def"), any(String.class));
    }

    /* FIXME These are very tightly coupled to the current implementation's practice of using append()
     * when dynamically translating and set() when just reading. That's not good. */

    /**
     * Test that multivalued terms can be read from the translation.
     */
    @Test
    public void testReadMultiValued()
    {
        fields.clear();
        fields.add("synonym");
        long count = translator.translate(VOC_NAME, LANG, term1, fields);
        assertEquals(0, count);
        verify(term1).set(eq("synonym_es"), argThat(containsInAnyOrder(translatedSynonyms.toArray())));
        verify(term1, never()).set(eq("synonym"), any(Set.class));
        verify(translator, never()).doTranslate(any(String.class), any(String.class));
    }

    /**
     * Test that multivalued terms can be translated at all.
     */
    @Test
    public void testTranslateMultiValued()
    {
        fields.clear();
        fields.add("synonym");
        long count = translator.translate(VOC_NAME, LANG, term2, fields);
        assertEquals(synonymsLength, count);
        verify(term2, never()).set(eq("synonym"), any(Set.class));
        verify(translator, times(synonyms.size())).doTranslate(any(String.class), any(String.class));
        for (String synonym : synonyms) {
            verify(translator).doTranslate(eq(synonym), eq(LANG));
        }
        for (String synonym : translatedSynonyms) {
            verify(term2).append(eq("synonym_es"), eq(synonym));
        }
    }

    /**
     * Test that multivalued fields get properly persisted.
     */
    @Test
    public void testPersistMultiValued()
    {
        fields.clear();
        fields.add("synonym");
        translator.translate(VOC_NAME, LANG, term2, fields);
        translator.unloadVocabulary(VOC_NAME, LANG);
        translator.loadVocabulary(VOC_NAME, LANG);
        long count = translator.translate(VOC_NAME, LANG, term2, fields);
        assertEquals(0, count);
        verify(term2, never()).set(eq("synonym"), any(String.class));
        verify(translator, times(synonyms.size())).doTranslate(any(String.class), any(String.class));
        for (String synonym : synonyms) {
            verify(translator, times(1)).doTranslate(eq(synonym), eq(LANG));
        }
        for (String synonym : translatedSynonyms) {
            verify(term2).append(eq("synonym_es"), eq(synonym));
        }
        verify(term2).set(eq("synonym_es"),
                argThat(containsInAnyOrder(translatedSynonyms.toArray())));
    }

    @Test
    public void testCount()
    {
        fields.add("synonym");
        fields.add("def");
        long count = translator.getMissingCharacters(VOC_NAME, LANG, term2, fields);
        assertEquals(term2.getName().length() + term2.getDescription().length() + synonymsLength,
                count);
        verify(term2, never()).set(any(String.class), any(Object.class));
    }

    /**
     * Provides a dummy implementation of machine translator methods.
     * Translates terms into Spanish following the cartoonish principle of prepending
     * the definite article "El " to the word.
     *
     * @version $Id$
     */
    public static class DummyMachineTranslator extends AbstractMachineTranslator
    {
        @Override
        public Collection<String> getSupportedLanguages()
        {
            Collection<String> retval = new HashSet<>(1);
            retval.add(LANG);
            return retval;
        }

        @Override
        public Collection<String> getSupportedVocabularies()
        {
            Collection<String> retval = new HashSet<>(1);
            retval.add(VOC_NAME);
            return retval;
        }

        @Override
        public String getIdentifier()
        {
            return "dummy";
        }

        @Override
        protected String doTranslate(String msg, String lang)
        {
            return "El " + msg;
        }
    }
}
