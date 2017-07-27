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
package org.phenotips.configuration.internal;

import org.phenotips.configuration.RecordConfiguration;
import org.phenotips.configuration.RecordElement;
import org.phenotips.configuration.RecordSection;

import org.xwiki.model.reference.DocumentReference;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Default implementation of the {@link RecordConfiguration} interface.
 *
 * @version $Id$
 * @since 1.4
 */
public class DefaultRecordConfiguration implements RecordConfiguration
{
    /** Default format for dates. */
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    /** @see #getAllSections() */
    private List<RecordSection> sections;

    /** @see #getPhenotypeMapping() */
    private DocumentReference phenotypeMapping;

    /** @see #getDateOfBirthFormat() */
    private String dateFormat = DATE_FORMAT;

    @Override
    public List<RecordSection> getAllSections()
    {
        return CollectionUtils.isNotEmpty(this.sections)
            ? Collections.unmodifiableList(this.sections)
            : Collections.<RecordSection>emptyList();
    }

    @Override
    public List<RecordSection> getEnabledSections()
    {
        List<RecordSection> result = new LinkedList<>();
        for (RecordSection section : getAllSections()) {
            if (section.isEnabled()) {
                result.add(section);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public void setSections(List<RecordSection> sections)
    {
        this.sections = sections;
    }

    @Override
    public List<String> getEnabledFieldNames()
    {
        final List<String> enabledFields = new LinkedList<>();
        for (final RecordSection section : getEnabledSections()) {
            for (final RecordElement element : section.getEnabledElements()) {
                enabledFields.addAll(element.getDisplayedFields());
            }
        }
        return Collections.unmodifiableList(enabledFields);
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<String> getEnabledNonIdentifiableFieldNames()
    {
        List<String> result = new LinkedList<>();
        for (RecordSection section : getEnabledSections()) {
            for (RecordElement element : section.getEnabledElements()) {
                if (!element.containsPrivateIdentifiableInformation()) {
                    result.addAll(element.getDisplayedFields());
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public List<String> getAllFieldNames()
    {
        final List<String> fields = new LinkedList<>();
        for (final RecordSection section : getAllSections()) {
            for (final RecordElement element : section.getAllElements()) {
                fields.addAll(element.getDisplayedFields());
            }
        }
        return Collections.unmodifiableList(fields);
    }

    @Override
    public DocumentReference getPhenotypeMapping()
    {
        return this.phenotypeMapping;
    }

    @Override
    public void setPhenotypeMapping(DocumentReference mapping)
    {
        this.phenotypeMapping = mapping;
    }

    @Override
    public String getISODateFormat()
    {
        return DATE_FORMAT;
    }

    @Override
    public String getDateOfBirthFormat()
    {
        return this.dateFormat;
    }

    @Override
    public void setDateOfBirthFormat(String format)
    {
        this.dateFormat = format;
    }

    @Override
    public String toString()
    {
        return StringUtils.join(getEnabledSections(), ", ");
    }
}
