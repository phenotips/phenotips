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
import org.phenotips.data.permissions.EntityPermissionsManager;
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsConfiguration;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.data.permissions.Visibility;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;

import java.util.Collection;
import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

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

    @Inject
    private PermissionsConfiguration configuration;

    @Inject
    private EntityPermissionsManager entityPermissionsManager;

    @Override
    public Collection<Visibility> listVisibilityOptions()
    {
        return this.entityPermissionsManager.listVisibilityOptions();
    }

    @Override
    public Collection<Visibility> listAllVisibilityOptions()
    {
        return this.entityPermissionsManager.listAllVisibilityOptions();
    }

    @Override
    public Visibility getDefaultVisibility()
    {
        return this.entityPermissionsManager.resolveVisibility(this.configuration.getDefaultVisibility());
    }

    @Override
    public Visibility resolveVisibility(String name)
    {
        return this.entityPermissionsManager.resolveVisibility(name);
    }

    @Override
    public Collection<AccessLevel> listAccessLevels()
    {
        return this.entityPermissionsManager.listAccessLevels();
    }

    @Override
    public AccessLevel resolveAccessLevel(String name)
    {
        return this.entityPermissionsManager.resolveAccessLevel(name);
    }

    @Override
    public PatientAccess getPatientAccess(Patient targetPatient)
    {
        return new DefaultPatientAccess(targetPatient, getHelper(), this.entityPermissionsManager);
    }

    @Override
    public Collection<Patient> filterByVisibility(Collection<Patient> patients, Visibility requiredVisibility)
    {
        return (Collection<Patient>) this.entityPermissionsManager.filterByVisibility(patients, requiredVisibility);
    }

    @Override
    public Iterator<Patient> filterByVisibility(Iterator<Patient> patients, Visibility requiredVisibility)
    {
        return (Iterator<Patient>) this.entityPermissionsManager.filterByVisibility(patients, requiredVisibility);
    }

    private EntityAccessHelper getHelper()
    {
        try {
            return this.componentManager.get().getInstance(EntityAccessHelper.class);
        } catch (ComponentLookupException ex) {
            this.logger.error("Mandatory component [EntityAccessHelper] missing: {}", ex.getMessage(), ex);
        }
        return null;
    }

    public void fireRightsUpdateEvent(String patientId)
    {
        this.entityPermissionsManager.fireRightsUpdateEvent(patientId);
    }
}
