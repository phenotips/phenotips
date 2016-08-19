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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/* How lovely hackery is. There isn't an industry-standard way to read/write xliffv1 files, but
 * there are many many ways to write plain xml files. This class basically allows serialization
 * and deserialization to xliff, thus hopefully simplifying interactions with translations.
 */

/**
 * A POJO mapping to an xliff version 1 translation file.
 *
 * @version $Id$
 */
@JacksonXmlRootElement(localName = "xliff")
final class XLiffFile
{
    /**
     * The initial size of the hash map containing the translation units.
     */
    private static final int INITIAL_MAP_SIZE = 16384;

    /**
     * The separator for target strings.
     */
    private static final String TARGET_SEPARATOR = "#";

    /**
     * The version attribute.
     */
    @JacksonXmlProperty(localName = "version", isAttribute = true)
    public String version = "";

    /**
     * The file node.
     */
    public File file = new File();

    /**
     * Get the first value of a translation unit, or null if there isn't one.
     *
     * @param id the id of the term
     * @param property the property
     * @return the translated string
     */
    @JsonIgnore
    public String getFirstString(String id, String property)
    {
        List<String> strings = getStrings(id, property);
        if (strings == null || strings.size() < 1) {
            return null;
        }
        return strings.get(0);
    }

    /**
     * Get all the target values of a translation unit.
     *
     * @param id the id of the term
     * @param property the property
     * @return the translated string
     */
    @JsonIgnore
    public List<String> getStrings(String id, String property)
    {
        Map<String, TransUnit> properties = file.body.transUnits.get(id);
        if (properties == null) {
            return null;
        }
        TransUnit unit = properties.get(property);
        if (unit == null) {
            return null;
        }
        List<String> retval = new ArrayList<>(Arrays.asList(unit.target.split(TARGET_SEPARATOR)));
        retval.removeAll(Collections.singleton(null));
        for (int i = 0; i < retval.size(); i++) {
            retval.set(i, retval.get(i).trim());
        }
        retval.removeAll(Collections.singleton(""));
        return retval;
    }

    /**
     * Convenience method to set a translation unit's source/value as needed.
     *
     * @param id the id of the term
     * @param property the property
     * @param source the source text
     * @param target the translated target text
     */
    @JsonIgnore
    public void setString(String id, String property, String source, String target)
    {
        TransUnit unit = getOrCreateTransUnit(id, property);
        unit.source = source;
        unit.target = target;
    }

    /**
     * Add another term to a translation unit's source/value.
     *
     * @param id the id of the term
     * @param property the property
     * @param source the source text
     * @param target the target text
     */
    @JsonIgnore
    public void appendString(String id, String property, String source, String target)
    {
        TransUnit unit = getOrCreateTransUnit(id, property);
        String delimiter = " " + TARGET_SEPARATOR;
        unit.source += delimiter + source;
        unit.target += delimiter + target;
    }

    /**
     * Get or create the trans unit for the id/property given.
     *
     * @param id the id
     * @param property the property
     * @return the translation unit
     */
    @JsonIgnore
    private TransUnit getOrCreateTransUnit(String id, String property)
    {
        TransUnit unit = null;
        Map<String, TransUnit> properties = file.body.transUnits.get(id);
        if (properties != null) {
            unit = properties.get(property);
        }
        if (unit == null) {
            unit = new TransUnit();
            unit.id = String.format("%s_%s", id, property);
            file.body.addTransUnit(unit);
        }
        return unit;
    }

    /**
     * The "schema" for the file node.
     */
    static final class File
    {
        /**
         * The original attribute.
         */
        @JacksonXmlProperty(localName = "original", isAttribute = true)
        public String original = "";

        /**
         * The datatype attribute.
         */
        @JacksonXmlProperty(localName = "datatype", isAttribute = true)
        public String datatype = "";

        /**
         * The source language attribute.
         */
        @JacksonXmlProperty(localName = "source-language", isAttribute = true)
        public String sourceLanguage = "";

        /**
         * The target language attribute.
         */
        @JacksonXmlProperty(localName = "target-language", isAttribute = true)
        public String targetLanguage = "";

        /**
         * The body attribute.
         */
        public Body body = new Body();
    }

    /**
     * The "schema" for the body node.
     */
    static final class Body
    {
        /**
         * The pattern to parse ids.
         * TODO This should probably not be hardcoded...
         */
        @JsonIgnore
        private static final Pattern ID_PATTERN = Pattern.compile("^(.*)_(.*)$");

        /**
         * The translation units.
         * Mapping from id to attribute to translation unit.
         */
        private Map<String, Map<String, TransUnit>> transUnits;

        /**
         * CTOR.
         */
        Body()
        {
            transUnits = new HashMap(INITIAL_MAP_SIZE);
        }

        /**
         * Get the translation units (as a list).
         *
         * @return the translation units
         */
        @JacksonXmlProperty(localName = "trans-unit")
        @JacksonXmlElementWrapper(useWrapping = false)
        public List<TransUnit> getTransUnits()
        {
            List<TransUnit> units = new ArrayList<>(transUnits.size() * 4);
            for (Map<String, TransUnit> middle : transUnits.values()) {
                for (TransUnit unit : middle.values()) {
                    units.add(unit);
                }
            }
            return units;
        }

        /**
         * Set the translation units.
         *
         * @param units the units
         */
        @JacksonXmlProperty(localName = "trans-unit")
        @JacksonXmlElementWrapper(useWrapping = false)
        public void setTransUnits(List<TransUnit> units)
        {
            transUnits.clear();
            for (TransUnit unit : units) {
                addTransUnit(unit);
            }
        }

        /**
         * Add a translation unit.
         *
         * @param unit the unit
         */
        @JsonIgnore
        public void addTransUnit(TransUnit unit)
        {
            Matcher m = ID_PATTERN.matcher(unit.id);
            m.find();
            if (!m.matches()) {
                throw new IllegalStateException(String.format("%s doesn't match", unit.id));
            }
            String id = m.group(1).replace("_", ":");
            String attribute = m.group(2);
            Map<String, TransUnit> map = transUnits.get(id);
            if (map == null) {
                map = new HashMap<>(4);
                transUnits.put(id, map);
            }
            map.put(attribute, unit);
        }
    }

    /**
     * The "schema" for the trans-unit node.
     */
    static final class TransUnit
    {
        /**
         * The source node.
         */
        public String source = "";

        /**
         * The target node.
         */
        public String target = "";

        /**
         * The id attribute.
         */
        @JacksonXmlProperty(localName = "id", isAttribute = true)
        public String id = "";
    }

}
