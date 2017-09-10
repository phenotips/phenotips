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
import org.phenotips.data.permissions.EntityAccess;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.entities.PrimaryEntity;

import org.xwiki.component.annotation.Component;

import java.util.Collection;
import java.util.Iterator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
    @Nonnull
    public Collection<Visibility> listVisibilityOptions()
    {
        return this.internalService.listVisibilityOptions();
    }

    @Override
    @Nonnull
    public Collection<Visibility> listAllVisibilityOptions()
    {
        return this.internalService.listAllVisibilityOptions();
    }

    @Override
    @Nonnull
    public Visibility getDefaultVisibility()
    {
        return this.internalService.getDefaultVisibility();
    }

    @Override
    @Nonnull
    public Visibility resolveVisibility(@Nullable final String name)
    {
        return this.internalService.resolveVisibility(name);
    }

    @Override
    @Nonnull
    public Collection<AccessLevel> listAccessLevels()
    {
        return this.internalService.listAccessLevels();
    }

    @Override
    @Nullable
    public AccessLevel resolveAccessLevel(@Nullable final String name)
    {
        return this.internalService.resolveAccessLevel(name);
    }

    @Override
    public EntityAccess getPatientAccess(Patient targetPatient)
    {
        return getEntityAccess(targetPatient);
    }

    @Override
    @Nonnull
    public EntityAccess getEntityAccess(@Nonnull final PrimaryEntity entity)
    {
        return new SecureEntityAccess(this.internalService.getEntityAccess(entity), this.internalService);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Patient> filterByVisibility(Collection<Patient> patients, Visibility requiredVisibility)
    {
        return (Collection<Patient>) (Collection<?>) filterVisible((Collection<PrimaryEntity>) (Collection<?>) patients,
            requiredVisibility);
    }

    @Override
    @Nonnull
    public Collection<PrimaryEntity> filterVisible(
        @Nonnull final Collection<PrimaryEntity> entities,
        @Nullable final Visibility requiredVisibility)
    {
        return this.internalService.filterVisible(entities, requiredVisibility);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<Patient> filterByVisibility(Iterator<Patient> patients, Visibility requiredVisibility)
    {
        return (Iterator<Patient>) (Iterator<?>) filterVisible((Iterator<PrimaryEntity>) (Iterator<?>) patients,
            requiredVisibility);
    }

    @Override
    @Nonnull
    public Iterator<PrimaryEntity> filterVisible(
        @Nonnull final Iterator<PrimaryEntity> entities,
        @Nullable final Visibility requiredVisibility)
    {
        return this.internalService.filterVisible(entities, requiredVisibility);
    }

    @Override
    public void fireRightsUpdateEvent(String entityId)
    {
        this.internalService.fireRightsUpdateEvent(entityId);
    }
}
