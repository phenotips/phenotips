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

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import java.util.Collection;
import java.util.Locale;

/**
 * A component capable of machine translating text.
 *
 * @version $Id$
 * @since 1.3
 */
@Unstable("New API introduced in 1.3")
@Role
public interface MachineTranslator
{
    /**
     * Returns an identifier for this machine translator.
     *
     * @return a short identifier
     */
    String getIdentifier();

    /**
     * Returns the set of locales supported by this translator.
     *
     * @return the supported locales, a non-null, non-empty collection
     */
    Collection<Locale> getSupportedLocales();

    /**
     * Checks if the specified locale is supported by this translator.
     *
     * @param locale the locale to check
     * @return {@code true} if the target locale is supported, {@code false} if not and further requests for translation
     *         in that locale should not be attempted
     */
    boolean isLocaleSupported(Locale locale);

    /**
     * Translates some text from English into the target locale. This builds a cache on disk, and whenever possible will
     * read from it to avoid translating the same text multiple times.
     *
     * @param inputText the text to translate, in English
     * @param targetLocale the locale to translate into
     * @return the translated text, or {@code null} if an error occurred
     */
    String translate(String inputText, Locale targetLocale);

    /**
     * Translates some text from the source locale into the target locale. This builds a cache on disk, and whenever
     * possible will read from it to avoid translating the same text multiple times.
     *
     * @param inputText the text to translate
     * @param inputLocale the locale of the original text, if {@code null} then English is assumed
     * @param targetLocale the locale to translate into, must not be {@code null}
     * @return the translated text, or {@code null} if an error occurred
     */
    String translate(String inputText, Locale inputLocale, Locale targetLocale);
}
