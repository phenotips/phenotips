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
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

/**
 * @version $Id$
 */
@Component
@Singleton
public class DefaultPermissionsManager implements PermissionsManager
{
    @Inject
    private Logger logger;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManager;

    @Override
    public Collection<Visibility> listVisibilityOptions()
    {
        try {
            Collection<Visibility> result = new TreeSet<>();
            result.addAll(this.componentManager.get().<Visibility>getInstanceList(Visibility.class));
            return result;
        } catch (ComponentLookupException ex) {
            return Collections.emptyList();
        }
    }

    @Override
    public Visibility resolveVisibility(String name)
    {
        try {
            if (StringUtils.isNotBlank(name)) {
                return this.componentManager.get().getInstance(Visibility.class, name);
            }
        } catch (ComponentLookupException ex) {
            this.logger.warn("Invalid patient visibility requested: {}", name);
        }
        return null;
    }

    @Override
    public Collection<AccessLevel> listAccessLevels()
    {
        try {
            Collection<AccessLevel> result = new TreeSet<>();
            result.addAll(this.componentManager.get().<AccessLevel>getInstanceList(AccessLevel.class));
            Iterator<AccessLevel> it = result.iterator();
            while (it.hasNext()) {
                if (!it.next().isAssignable()) {
                    it.remove();
                }
            }
            return result;
        } catch (ComponentLookupException ex) {
            return Collections.emptyList();
        }
    }

    @Override
    public AccessLevel resolveAccessLevel(String name)
    {
        try {
            if (StringUtils.isNotBlank(name)) {
                return this.componentManager.get().getInstance(AccessLevel.class, name);
            }
        } catch (ComponentLookupException ex) {
            this.logger.warn("Invalid patient access level requested: {}", name);
        }
        return null;
    }

    @Override
    public PatientAccess getPatientAccess(Patient targetPatient)
    {
        return new DefaultPatientAccess(targetPatient, getHelper(), this);
    }

    @Override
    public Collection<Patient> filterByVisibility(Collection<Patient> patients, Visibility requiredVisibility)
    {
        Collection<Patient> patientsWithVisibility = new LinkedList<>();
        for (Patient patient : patients) {
            if (patient != null) {
                Visibility patientVisibility = this.getPatientAccess(patient).getVisibility();
                if (requiredVisibility.compareTo(patientVisibility) <= 0) {
                    patientsWithVisibility.add(patient);
                }
            }
        }

        return patientsWithVisibility;
    }

    private PatientAccessHelper getHelper()
    {
        try {
            return this.componentManager.get().getInstance(PatientAccessHelper.class);
        } catch (ComponentLookupException ex) {
            this.logger.error("Mandatory component [PatientAccessHelper] missing: {}", ex.getMessage(), ex);
        }
        return null;
    }
}
