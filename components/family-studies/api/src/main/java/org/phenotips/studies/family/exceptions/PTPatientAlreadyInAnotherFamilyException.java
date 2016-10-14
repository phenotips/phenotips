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
public class PTPatientAlreadyInAnotherFamilyException extends PTFamilyException
{
    private final String patientId;

    private final String otherFamilyId;

    /**
     * TODO: accept a list of <patient, family> pairs?
     *
     * @param patientId the patient
     * @param otherFamilyId the id o fthe other family the patient already belongs to
     */
    public PTPatientAlreadyInAnotherFamilyException(String patientId, String otherFamilyId)
    {
        super();
        this.patientId = patientId;
        this.otherFamilyId = otherFamilyId;
    }

    /**
     * @return the patient id
     */
    public String getPatientId()
    {
        return this.patientId;
    }

    /**
     * @return the id of the other family patient belongs to
     */
    public String getOtherFamilyId()
    {
        return this.otherFamilyId;
    }
}
