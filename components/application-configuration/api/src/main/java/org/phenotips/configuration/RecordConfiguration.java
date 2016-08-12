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
package org.phenotips.configuration;

import org.phenotips.Constants;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

import java.util.List;

/**
 * Exposes the configuration for displaying records.
 *
 * @version $Id$
 * @since 1.0M9
 */
@Unstable
public interface RecordConfiguration
{
    /** The XClass used for storing the global configuration. */
    EntityReference GLOBAL_PREFERENCES_CLASS =
        new EntityReference("DBConfigurationClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    /**
     * The XClass used for storing group- and user-specific configurations.
     *
     * @deprecated since 1.4, this is specific to studies and has been moved in a different module
     */
    @Deprecated
    EntityReference CUSTOM_PREFERENCES_CLASS =
        new EntityReference("TemplateClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    /**
     * The list of sections enabled for this record type.
     *
     * @return an unmodifiable ordered list of sections, empty if none are enabled or the configuration is missing
     */
    List<RecordSection> getEnabledSections();

    /**
     * The list of available sections, enabled or disabled, that can be displayed in this type of record.
     *
     * @return an unmodifiable ordered list of sections, or an empty list if none are defined
     */
    List<RecordSection> getAllSections();

    /**
     * Update the list of available section. All changes are done in-memory for this object only, the configuration will
     * remain unchanged.
     *
     * @param sections a list of sections, may be empty
     * @see #getAllSections()
     * @since 1.4
     */
    void setSections(List<RecordSection> sections);

    /**
     * The list of fields enabled for this record type.
     *
     * @return an unmodifiable ordered list of field names, empty if none are enabled or the configuration is missing
     */
    List<String> getEnabledFieldNames();

    /**
     * The list of non-identifiable fields displayed in the patient record (i.e. fields which contain no personal
     * information which can be tracked to a particular patient)
     *
     * @return an unmodifiable ordered list of field names, empty if no non-identifiable fields are enabled or the
     *         configuration is missing
     * @deprecated since 1.4, this functionality has moved in the Consents module
     */
    @Deprecated
    List<String> getEnabledNonIdentifiableFieldNames();

    /**
     * The list of available fields, enabled or disabled, that can be displayed in this type of record.
     *
     * @return an unmodifiable ordered list of field names, empty if none are available or the configuration is missing
     */
    List<String> getAllFieldNames();

    /**
     * The custom predefined phenotypes displayed in the "Clinical Symptoms" section are configured in a document, and
     * this type of configuration is called a "Phenotype Mapping". Multiple such mappings can exist in a PhenoTips
     * instance, and one of these mappings can be selected as the preferred mapping to be used globally or for each
     * group. Warning! The return type of this method is likely to change once a more specific class for representing
     * mappings will be implemented.
     *
     * @return the selected phenotype mapping, as a reference to the document where the mapping is defined
     */
    @Unstable
    DocumentReference getPhenotypeMapping();

    /**
     * Update the preferred phenotype mapping. All changes are done in-memory for this object only, the configuration
     * will remain unchanged.
     *
     * @param mapping a reference to a document containing a phenotype mapping definition
     * @see #getPhenotypeMapping()
     * @since 1.4
     */
    @Unstable
    void setPhenotypeMapping(DocumentReference mapping);

    /**
     * The date format compliant with the ISO 8601 standard, in the {@link java.text.SimpleDateFormat Java date format}.
     *
     * @return the configured date format
     */
    String getISODateFormat();

    /**
     * The format to use for entering and displaying the date of birth and other dates. For privacy reasons, certain
     * PhenoTips instances may only record the year and month, or even just the year, so formats like {@code yyyy},
     * {@code MM/yyyy}, or {@code MMMM yyyy} are supported. This also allows switching between different formats such as
     * {@code yyyy-MM-dd}, {@code dd/MM/yyyy}, or {@code MM/dd/yyyy}.
     *
     * @return the configured date format, in the {@link java.text.SimpleDateFormat Java date format}
     */
    String getDateOfBirthFormat();

    /**
     * Update the format of the date of birth. All changes are done in-memory for this object only, the configuration
     * will remain unchanged.
     *
     * @param format the new date format, in the {@link java.text.SimpleDateFormat Java date format}
     * @see #getDateOfBirthFormat()
     * @since 1.4
     */
    void setDateOfBirthFormat(String format);
}
