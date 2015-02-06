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
