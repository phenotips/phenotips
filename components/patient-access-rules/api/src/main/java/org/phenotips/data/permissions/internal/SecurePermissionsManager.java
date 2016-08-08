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
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.data.permissions.Visibility;

import org.xwiki.component.annotation.Component;

import java.util.Collection;
import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Secure implementation of the permissions manager service, which checks the user's access rights before performing an
 * operation.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Component
@Named("secure")
@Singleton
public class SecurePermissionsManager implements PermissionsManager
{
    @Inject
    private PermissionsManager internalService;

    @Override
    public Collection<Visibility> listVisibilityOptions()
    {
        return this.internalService.listVisibilityOptions();
    }

    @Override
    public Collection<Visibility> listAllVisibilityOptions()
    {
        return this.internalService.listAllVisibilityOptions();
    }

    @Override
    public Visibility getDefaultVisibility()
    {
        return this.internalService.getDefaultVisibility();
    }

    @Override
    public Visibility resolveVisibility(String name)
    {
        return this.internalService.resolveVisibility(name);
    }

    @Override
    public Collection<AccessLevel> listAccessLevels()
    {
        return this.internalService.listAccessLevels();
    }

    @Override
    public AccessLevel resolveAccessLevel(String name)
    {
        return this.internalService.resolveAccessLevel(name);
    }

    @Override
    public PatientAccess getPatientAccess(Patient targetPatient)
    {
        return new SecurePatientAccess(this.internalService.getPatientAccess(targetPatient), this.internalService);
    }

    @Override
    public Collection<Patient> filterByVisibility(Collection<Patient> patients, Visibility requiredVisibility)
    {
        return this.internalService.filterByVisibility(patients, requiredVisibility);
    }

    @Override
    public Iterator<Patient> filterByVisibility(Iterator<Patient> patients, Visibility requiredVisibility)
    {
        return this.internalService.filterByVisibility(patients, requiredVisibility);
    }
}
