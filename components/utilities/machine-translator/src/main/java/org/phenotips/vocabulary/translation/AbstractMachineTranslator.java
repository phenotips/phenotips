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

import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.environment.Environment;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.ApplicationStoppedEvent;
import org.xwiki.observation.event.Event;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

/**
 * Base class for any machine translator implementations.
 *
 * @version $Id$
 */
public abstract class AbstractMachineTranslator implements MachineTranslator, Initializable
{
    /** The name of the directory containing the translation caches. */
    protected static final String CACHE_DIR_NAME = "vocabulary_translations";

    /** The execution environment, knows where we can store cached translations. */
    @Inject
    private Environment environment;

    @Inject
    private ObservationManager om;

    /**
     * Our logger.
     */
    @Inject
    private Logger logger;

    /** The home directory, where translation caches are stored. */
    private File cacheDir;

    private Map<Pair<Locale, Locale>, Properties> cacheResources = new HashMap<>();

    @Override
    public void initialize() throws InitializationException
    {
        this.cacheDir = this.environment.getPermanentDirectory();
        this.cacheDir = new File(this.cacheDir, CACHE_DIR_NAME);
        this.cacheDir = new File(this.cacheDir, getIdentifier());
        if (!this.cacheDir.exists()) {
            this.cacheDir.mkdirs();
        }
        this.om.addListener(new AbstractEventListener(this.getClass().getCanonicalName(),
            Arrays.<Event>asList(new ApplicationStoppedEvent()))
        {
            @Override
            public void onEvent(Event event, Object source, Object data)
            {
                for (Map.Entry<Pair<Locale, Locale>, Properties> entry : AbstractMachineTranslator.this.cacheResources
                    .entrySet()) {
                    File file = new File(getCacheDir(), getFileName(entry.getKey()));
                    try {
                        entry.getValue().store(new FileOutputStream(file), null);
                    } catch (IOException ex) {
                        // The cache is not critical, we'll just ignore this exception
                    }
                }
            }
        });
    }

    @Override
    public String translate(String inputText, Locale targetLocale)
    {
        return translate(inputText, null, targetLocale);
    }

    @Override
    public String translate(String inputText, Locale inputLocale, Locale targetLocale)
    {
        Properties cache = this.getCache(inputLocale, targetLocale);
        String cachedTranslation = cache.getProperty(inputText);
        if (cachedTranslation != null) {
            return cachedTranslation;
        } else {
            String result = doTranslate(inputText, inputLocale, targetLocale);
            if (result != null) {
                cache.setProperty(inputText, result);
            }
            return result;
        }
    }

    /**
     * Actually run the input given through a machine translation. It's possible to return null here in which case no
     * translation will be saved. This allows for graceful failing of the translator.
     *
     * @param inputText the text to translate
     * @param inputLocale the locale of the original text
     * @param targetLocale the locale to translate into
     * @return the translated string
     */
    protected abstract String doTranslate(String inputText, Locale inputLocale, Locale targetLocale);

    /**
     * Get the name of the translations cache file for the given locales.
     *
     * @param inputLocale the locale of the original text
     * @param targetLocale the locale to translate into
     * @return the translations cache file name
     */
    protected String getFileName(Locale inputLocale, Locale targetLocale)
    {
        return String.format("%s,%s.properties", inputLocale == null ? "en" : inputLocale.toString(),
            targetLocale.toString());
    }

    /**
     * Get the name of the translations cache file for the given locales.
     *
     * @param inputLocale the locale of the original text
     * @param targetLocale the locale to translate into
     * @return the translations cache file name
     */
    protected String getFileName(Pair<Locale, Locale> locales)
    {
        return getFileName(locales.getLeft(), locales.getRight());
    }

    /**
     * Get the home directory for the machine translating component.
     *
     * @return the home directory
     */
    protected File getCacheDir()
    {
        return this.cacheDir;
    }

    protected Properties getCache(Locale inputLocale, Locale targetLocale)
    {
        Properties result = this.cacheResources.get(Pair.of(inputLocale, targetLocale));
        if (result == null) {
            result = new Properties();
            this.cacheResources.put(Pair.of(inputLocale, targetLocale), result);
            File file = new File(this.getCacheDir(), getFileName(inputLocale, targetLocale));
            if (file.exists()) {
                try (FileInputStream in = new FileInputStream(file)) {
                    result.load(in);
                } catch (IOException ex) {
                    // Weird, this shouldn't happen
                    this.logger.warn("Failed to read cached translations from [{}]: {}", file.getAbsolutePath(),
                        ex.getMessage());
                }
            }
        }
        return result;
    }
}
