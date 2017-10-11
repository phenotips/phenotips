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

import org.phenotips.data.permissions.EntityAccess;
import org.phenotips.data.permissions.EntityPermissionsManager;
import org.phenotips.data.permissions.PatientAccess;

public class SecurePatientAccess extends SecureEntityAccess implements PatientAccess
{
    private final EntityAccess internalService;

    private final EntityPermissionsManager manager;

    public SecurePatientAccess(EntityAccess internalService, EntityPermissionsManager manager)
    {
        super(internalService, manager);
        this.internalService = internalService;
        this.manager = manager;
    }

    @Override
    public Patient getPatient()
    {
        return (Patient) getEntity();
    }
}
