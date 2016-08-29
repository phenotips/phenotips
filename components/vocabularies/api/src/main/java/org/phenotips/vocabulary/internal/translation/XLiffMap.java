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

import java.util.HashMap;
import java.util.Map;

/**
 * A container for XLiffFiles that maps from vocabulary/language pairs to
 * the corresponding XLiffFile or throws an exception if none is to be found.
 *
 * @version $Id$
 */
class XLiffMap
{
    /**
     * The inner hash map.
     */
    private Map<String, XLiffFile> map = new HashMap<>();

    /**
     * Get the key for the vocabulary/language given.
     *
     * @param vocabulary the vocabulary
     * @param lang the language
     * @return the key.
     */
    private String getKey(String vocabulary, String lang)
    {
        return vocabulary + "_" + lang;
    }

    /**
     * Put the xliff file given into the map.
     *
     * @param vocabulary the vocabulary
     * @param lang the language
     * @param xliff the xliff
     */
    public void put(String vocabulary, String lang, XLiffFile xliff)
    {
        map.put(getKey(vocabulary, lang), xliff);
    }

    /**
     * Get the xliff file for the vocabulary and language given or throw
     * an exception if there isn't one.
     *
     * @param vocabulary the vocabulary
     * @param lang the language
     * @return the xliff file
     * @throws IllegalStateException if there is no corresponding xliff.
     */
    public XLiffFile get(String vocabulary, String lang)
    {
        XLiffFile xliff = map.get(getKey(vocabulary, lang));
        if (xliff == null) {
            throw new IllegalStateException(String.format("Vocabulary %s, lang %s not initialized",
                        vocabulary, lang));
        }
        return xliff;
    }

    /**
     * Remove the xliff file for the vocabulary and language given.
     *
     * @param vocabulary the vocabulary
     * @param lang the language
     * @return the removed xliff or null if it wasn't there
     */
    public XLiffFile remove(String vocabulary, String lang)
    {
        return map.remove(getKey(vocabulary, lang));
    }
}
