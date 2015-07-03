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
package org.phenotips.studies.family;

import org.phenotips.data.Patient;

import org.xwiki.component.annotation.Role;

/**
 * Utility methods for manipulating families.
 *
 * @version $Id$
 * @since 1.2RC1
 */
@Role
public interface FamilyRepository
{
    /**
     * @param patient whose family the function return
     * @return family for which family.isMember(patient) is true
     */
    Family getFamilyForPatient(Patient patient);

    /**
     * @param id of family to return
     * @return family for which family.getId().equals(id) is true
     */
    Family getFamilyById(String id);

    /**
     * Creates a new empty family.
     *
     * @return new Family object
     */
    Family createFamily();
}
