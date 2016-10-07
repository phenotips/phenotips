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
package org.phenotips.studies.family.exceptions;

/**
 * TODO: redesign, move to another package.
 *
 * @version $Id$
 * @since 1.4
 */
public class PTPedigreeContainesSamePatientMultipleTimesException extends PTFamilyException
{
    private final String patientId;

    /**
     * TODO: accept a list of patients?
     *
     * @param patientId the patient that is contained multiple times
     */
    public PTPedigreeContainesSamePatientMultipleTimesException(String patientId)
    {
        super();
        this.patientId = patientId;
    }

    /**
     * @return the id of the patient
     */
    public String getPatientId()
    {
        return this.patientId;
    }
}
