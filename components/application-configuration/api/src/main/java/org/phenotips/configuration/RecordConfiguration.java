/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.configuration;

import org.phenotips.Constants;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

import java.util.List;

/**
 * Exposes the current configuration related to the patient record.
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

    /** The XClass used for storing group- and user-specific configurations. */
    EntityReference CUSTOM_PREFERENCES_CLASS =
        new EntityReference("StudyClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    /**
     * The list of sections enabled in the patient record.
     *
     * @return an unmodifiable ordered list of sections, empty if none are enabled or the configuration is missing
     */
    List<RecordSection> getEnabledSections();

    /**
     * The list of available sections, enabled or disabled, that can be displayed in the patient record.
     *
     * @return an unmodifiable ordered list of sections, or an empty list if none are defined
     */
    List<RecordSection> getAllSections();

    /**
     * The list of fields displayed in the patient record.
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
     */
    List<String> getEnabledNonIdentifiableFieldNames();

    /**
     * The list of possible fields defined in the application. This doesn't include metadata stored in separate
     * entities, such as measurements, relatives, additional files, etc.
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
     * The date format compliant with the ISO 8601 standard, in the {@link java.text.SimpleDateFormat Java date format}.
     *
     * @return the configured date format
     */
    String getISODateFormat();

    /**
     * The format of the date of birth, in the standard {@link java.text.SimpleDateFormat Java date format}.
     *
     * @return the configured date format
     */
    String getDateOfBirthFormat();
}
