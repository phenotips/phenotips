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

import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.EntityAccess;
import org.phenotips.data.permissions.EntityPermissionsManager;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.data.permissions.events.EntityRightsUpdatedEvent.RightsUpdateEventType;
import org.phenotips.entities.PrimaryEntity;

import org.xwiki.component.annotation.Component;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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
public class SecureEntityPermissionsManager implements EntityPermissionsManager
{
    @Inject
    private EntityPermissionsManager internalService;

    @Nonnull
    @Override
    public Collection<Visibility> listVisibilityOptions()
    {
        return this.internalService.listVisibilityOptions();
    }

    @Nonnull
    @Override
    public Collection<Visibility> listAllVisibilityOptions()
    {
        return this.internalService.listAllVisibilityOptions();
    }

    @Nonnull
    @Override
    public Visibility getDefaultVisibility()
    {
        return this.internalService.getDefaultVisibility();
    }

    @Nonnull
    @Override
    public Visibility resolveVisibility(@Nullable final String name)
    {
        return this.internalService.resolveVisibility(name);
    }

    @Nonnull
    @Override
    public Collection<AccessLevel> listAccessLevels()
    {
        return this.internalService.listAccessLevels();
    }

    @Nonnull
    @Override
    public Collection<AccessLevel> listAllAccessLevels()
    {
        return this.internalService.listAllAccessLevels();
    }

    @Nonnull
    @Override
    public AccessLevel resolveAccessLevel(@Nullable final String name)
    {
        return this.internalService.resolveAccessLevel(name);
    }

    @Nonnull
    @Override
    public EntityAccess getEntityAccess(@Nullable final PrimaryEntity targetEntity)
    {
        return new SecureEntityAccess(this.internalService.getEntityAccess(targetEntity), this.internalService);
    }

    @Nonnull
    @Override
    public Collection<? extends PrimaryEntity> filterByVisibility(
        @Nullable final Collection<? extends PrimaryEntity> entities,
        @Nullable final Visibility requiredVisibility)
    {
        return this.internalService.filterByVisibility(entities, requiredVisibility);
    }

    @Nonnull
    @Override
    public Iterator<? extends PrimaryEntity> filterByVisibility(
        @Nullable final Iterator<? extends PrimaryEntity> entities,
        @Nullable final Visibility requiredVisibility)
    {
        return this.internalService.filterByVisibility(entities, requiredVisibility);
    }

    @Override
    public void fireRightsUpdateEvent(@Nonnull final String entityId)
    {
        this.internalService.fireRightsUpdateEvent(entityId);
    }

    @Override
    public void fireRightsUpdateEvent(@Nonnull final List<RightsUpdateEventType> eventTypes,
        @Nonnull final String entityId)
    {
        this.internalService.fireRightsUpdateEvent(eventTypes, entityId);
    }

    @Override
    public void fireStudyUpdateEvent(@Nonnull final String entityId, @Nonnull final String studyId)
    {
        this.internalService.fireStudyUpdateEvent(entityId, studyId);
    }
}
