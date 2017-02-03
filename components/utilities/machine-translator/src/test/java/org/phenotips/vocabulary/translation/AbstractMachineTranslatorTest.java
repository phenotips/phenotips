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
package org.phenotips.vocabulary.translation;

import org.xwiki.environment.Environment;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.ApplicationStoppedEvent;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.internal.matchers.CapturingMatcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the common functionality in the abstract machine translator. Works via a dummy concrete child.
 *
 * @version $Id$
 */
public class AbstractMachineTranslatorTest
{
    private static final String TEST_NAME = "Name";

    private static final String TRANSLATED_NAME = "El Name";

    private static final Locale TEST_LOCALE = Locale.forLanguageTag("es");

    /** The temporary home of our translator. */
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /** Rule for managing the component under test and its dependencies. */
    @Rule
    public final MockitoComponentMockingRule<AbstractMachineTranslator> mocker =
        new MockitoComponentMockingRule<AbstractMachineTranslator>(DummyMachineTranslator.class);

    /** The component under test. */
    private AbstractMachineTranslator translator;

    private File cacheFile;

    private EventListener listener;

    @Before
    public void setUp() throws Exception
    {
        Environment environment = this.mocker.getInstance(Environment.class);
        when(environment.getPermanentDirectory()).thenReturn(this.folder.getRoot());

        Properties prop = new Properties();
        prop.setProperty("the street", "la strada");
        this.cacheFile =
            new File(new File(new File(this.folder.getRoot(), "vocabulary_translations"), "dummy"), "en,es.properties");
        this.cacheFile.getParentFile().mkdirs();
        prop.store(new FileWriter(this.cacheFile), null);

        ObservationManager om = this.mocker.getInstance(ObservationManager.class);
        CapturingMatcher<EventListener> cm = new CapturingMatcher<>();
        Mockito.doNothing().when(om).addListener(Matchers.argThat(cm));
        this.translator = spy(this.mocker.getComponentUnderTest());
        this.listener = cm.getLastValue();
    }

    /** Test that the machine translator will not retranslate something that's already in the cache file. */
    @Test
    public void testNoReTranslate()
    {
        assertEquals("la strada", this.translator.translate("the street", TEST_LOCALE));
    }

    /** Test that the machine translator will translate when necessary. */
    @Test
    public void testDoTranslate()
    {
        String translated = this.translator.translate(TEST_NAME, TEST_LOCALE);
        assertEquals(TRANSLATED_NAME, translated);

        assertEquals(TEST_NAME, this.translator.translate(TRANSLATED_NAME, TEST_LOCALE, Locale.ENGLISH));
    }

    /** Test that previously performed translations will be cached. */
    @Test
    public void testRemember()
    {
        this.translator.translate(TEST_NAME, TEST_LOCALE);
        this.translator.translate(TEST_NAME, TEST_LOCALE);
        verify(this.translator, times(1)).doTranslate(TEST_NAME, null, TEST_LOCALE);
    }

    /** Test that previously performed translations are remembered across restarts of the component. */
    @Test
    public void testPersist() throws IOException
    {
        this.translator.translate(TEST_NAME, TEST_LOCALE);
        this.listener.onEvent(new ApplicationStoppedEvent(), null, null);
        Properties cache = new Properties();
        cache.load(new FileInputStream(this.cacheFile));
        assertTrue(cache.containsKey(TEST_NAME));
    }

    /**
     * Provides a dummy implementation of machine translator methods. Translates terms into Spanish following the
     * cartoonish principle of prepending the definite article "El " to the word.
     *
     * @version $Id$
     */
    public static class DummyMachineTranslator extends AbstractMachineTranslator
    {
        @Override
        public String getIdentifier()
        {
            return "dummy";
        }

        @Override
        public Collection<Locale> getSupportedLocales()
        {
            return Collections.singleton(Locale.forLanguageTag("es"));
        }

        @Override
        public boolean isLocaleSupported(Locale locale)
        {
            return locale != null && "es".equals(locale.getLanguage());
        }

        @Override
        protected String doTranslate(String inputText, Locale inputLocale, Locale targetLocale)
        {
            if (Locale.forLanguageTag("es").equals(targetLocale)) {
                return "El " + inputText;
            } else {
                return StringUtils.removeStart(inputText, "El ");
            }
        }
    }
}
