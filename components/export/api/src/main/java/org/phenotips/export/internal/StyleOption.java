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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.phenotips.export.internal;

/**
 * An enum of all the possible internal style options for a cell.
 *
 * @version $Id$
 * @since 1.0RC1
 */
public enum StyleOption
{
    /** Regular header cell. */
    HEADER,
    /** Phenotypes present. */
    YES,
    /** Phenotypes not present or rejected genes. */
    NO,
    /** Left border between logical field sections, such as Identifiers, Patient Information, etc. */
    SECTION_BORDER_LEFT,
    /** Right logical field section border. */
    SECTION_BORDER_RIGHT,
    /** The border between patient records. */
    PATIENT_BORDER,
    /** The bottom border of the header. */
    HEADER_BOTTOM,
    /** Separates phenotypic features of one patient. */
    FEATURE_SEPARATOR,
    /** A header with enlarged font. */
    LARGE_HEADER,
    /** Separates one patients "present" and "not present" phenotype blocks. */
    YES_NO_SEPARATOR
}
