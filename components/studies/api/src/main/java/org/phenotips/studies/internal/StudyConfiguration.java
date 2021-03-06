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
package org.phenotips.studies.internal;

import java.util.List;

import com.xpn.xwiki.objects.BaseObject;

/**
 * Exposes the custom study configuration using simple APIs instead of low-level XObject access.
 *
 * @version $Id$
 * @since 1.4
 */
public class StudyConfiguration
{
    /** The custom configuration object. */
    private final BaseObject configuration;

    /**
     * Simple constructor.
     *
     * @param configuration the configuration XObject to wrap
     */
    public StudyConfiguration(BaseObject configuration)
    {
        this.configuration = configuration;
    }

    /**
     * Custom sections configuration is represented as a list of section identifiers. If a section is present in this
     * list, then it is enabled, otherwise it's disabled. The custom order of the sections is the same order as this
     * list. If the list is empty, then the global configuration is used instead.
     *
     * @return a list of enabled section identifiers, empty if the sections aren't configured, {@code null} if the
     *         configuration object doesn't define this override at all
     */
    @SuppressWarnings("unchecked")
    public List<String> getSectionsOverride()
    {
        return this.configuration.getListValue("sections");
    }

    /**
     * Custom fields configuration is represented as a list of field identifiers. If a field is present in this list,
     * then it is enabled, otherwise it's disabled. The custom order of the fields is the same order as this list.
     * Fields can't be moved to a different section using the custom configuration, so only the relative order of the
     * enabled fields belonging to the same section determines their order in that section. If the list is empty, then
     * the global configuration is used instead.
     *
     * @return a list of enabled field identifiers, empty if the fields aren't configured, {@code null} if the
     *         configuration object doesn't define this override at all
     */
    @SuppressWarnings("unchecked")
    public List<String> getFieldsOverride()
    {
        return this.configuration.getListValue("fields");
    }

    /**
     * The custom configuration can specify a different phenotype mapping to use, as a serialized reference to the
     * mapping document.
     *
     * @return a serialized document reference, possibly empty or {@code null}
     */
    public String getPhenotypeMapping()
    {
        return this.configuration.getStringValue("mapping");
    }
}
