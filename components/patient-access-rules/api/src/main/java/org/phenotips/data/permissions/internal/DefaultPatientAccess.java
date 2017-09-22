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
package org.phenotips.data.permissions.internal;

import org.phenotips.data.Patient;

import org.phenotips.data.permissions.PatientAccess;

/**
 * @version $Id$
 */
public class DefaultPatientAccess extends DefaultEntityAccess implements PatientAccess
{
    private final Patient patient;

    public DefaultPatientAccess(Patient patient, EntityAccessHelper helper, EntityAccessManager accessManager,
        EntityVisibilityManager visibilityManager)
    {
        super(patient, helper, accessManager, visibilityManager);
        this.patient = patient;
    }

    @Override
    public Patient getPatient()
    {
        return (Patient) getEntity();
    }

    @Override
    public String toString()
    {
        return "Access rules for "
            + (this.patient != null ? this.patient.getDocumentReference() : "<unknown patient>");
    }
}
