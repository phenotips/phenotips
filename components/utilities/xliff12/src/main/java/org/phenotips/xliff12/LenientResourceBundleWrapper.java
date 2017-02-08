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
package org.phenotips.xliff12;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * A wrapper around XLIFF-based {@link ResourceBundle}s, which, instead of throwing {@link MissingResourceException}
 * whenever attempting to retrieve a translation, simply returns {@code null}. Also handles the proper loading of the
 * resource bundles.
 *
 * @version $Id$
 * @since 1.3
 */
public final class LenientResourceBundleWrapper
{
    /** An empty resource bundle used as a marker for requested translations that were not found. */
    private static final ResourceBundle NONEXISTENT_BUNDLE = new ResourceBundle()
    {
        @Override
        public Enumeration<String> getKeys()
        {
            return Collections.emptyEnumeration();
        }

        @Override
        protected Object handleGetObject(String key)
        {
            return null;
        }
    };

    /** Cache of already loaded translations. */
    private Map<String, SoftReference<? extends ResourceBundle>> translations = new HashMap<>();

    /**
     * Gets the translation for a key, in a specific language, from a translatable resource bundle.
     *
     * @param baseName the base name of the translatable resource
     * @param key the key to translate
     * @param locale the language to translate in
     * @return the translation, or {@code null} if the requested key isn't translated in the target language
     */
    public String getTranslation(String baseName, String key, Locale locale)
    {
        try {
            return getTranslation(baseName, locale).getString(key);
        } catch (MissingResourceException ex) {
            return null;
        }
    }

    /**
     * Checks if the translation file for the specified language is available.
     *
     * @param baseName the base name of the translatable resource
     * @param locale the language to check
     * @return {@code true} if the translation is available, {@code false} if not and further requests for translation
     *         in that language should not be attempted
     */
    public boolean isTranslationAvailable(String baseName, Locale locale)
    {
        return getTranslation(baseName, locale) != NONEXISTENT_BUNDLE;
    }

    private ResourceBundle getTranslation(String baseName, Locale locale)
    {
        if (!this.translations.containsKey(baseName + '_' + locale.toString())) {
            try {
                ResourceBundle result =
                    ResourceBundle.getBundle(baseName, locale, XLIFFResourceBundleControl.INSTANCE);
                this.translations.put(baseName + '_' + locale, new SoftReference<>(result));
            } catch (MissingResourceException ex) {
                this.translations.put(baseName + '_' + locale, new SoftReference<>(NONEXISTENT_BUNDLE));
            }
        }
        return this.translations.get(baseName + '_' + locale).get();
    }
}
