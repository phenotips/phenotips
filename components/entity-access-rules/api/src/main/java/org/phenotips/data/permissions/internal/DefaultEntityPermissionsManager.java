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
import org.phenotips.data.permissions.events.EntityRightsUpdatedEvent;
import org.phenotips.data.permissions.events.EntityRightsUpdatedEvent.RightsUpdateEventType;
import org.phenotips.data.permissions.events.EntityStudyUpdatedEvent;
import org.phenotips.entities.PrimaryEntity;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.ObservationManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @version $Id$
 */
@Component
@Singleton
public class DefaultEntityPermissionsManager implements EntityPermissionsManager
{
    @Inject
    private ObservationManager observationManager;

    @Inject
    private EntityAccessHelper helper;

    @Inject
    private EntityVisibilityManager visibilityManager;

    @Inject
    private EntityAccessManager accessManager;

    @Nonnull
    @Override
    public Collection<Visibility> listVisibilityOptions()
    {
        return this.visibilityManager.listVisibilityOptions();
    }

    @Nonnull
    @Override
    public Collection<Visibility> listAllVisibilityOptions()
    {
        return this.visibilityManager.listAllVisibilityOptions();
    }

    @Nonnull
    @Override
    public Visibility getDefaultVisibility()
    {
        return this.visibilityManager.getDefaultVisibility();
    }

    @Nonnull
    @Override
    public Visibility resolveVisibility(@Nullable final String name)
    {
        return this.visibilityManager.resolveVisibility(name);
    }

    @Nonnull
    @Override
    public Collection<AccessLevel> listAccessLevels()
    {
        return this.accessManager.listAccessLevels();
    }

    @Nonnull
    @Override
    public Collection<AccessLevel> listAllAccessLevels()
    {
        return this.accessManager.listAllAccessLevels();
    }

    @Nonnull
    @Override
    public AccessLevel resolveAccessLevel(@Nullable final String name)
    {
        return this.accessManager.resolveAccessLevel(name);
    }

    @Nonnull
    @Override
    public EntityAccess getEntityAccess(@Nullable final PrimaryEntity targetPatient)
    {
        return new DefaultEntityAccess(targetPatient, this.helper, this.accessManager, this.visibilityManager);
    }

    @Nonnull
    @Override
    public Collection<? extends PrimaryEntity> filterByVisibility(
        @Nullable final Collection<? extends PrimaryEntity> entities,
        @Nullable final Visibility requiredVisibility)
    {
        return this.visibilityManager.filterByVisibility(entities, requiredVisibility);
    }

    @Nonnull
    @Override
    public Iterator<? extends PrimaryEntity> filterByVisibility(
        @Nullable final Iterator<? extends PrimaryEntity> entities,
        @Nullable final Visibility requiredVisibility)
    {
        return this.visibilityManager.filterByVisibility(entities, requiredVisibility);
    }

    @Override
    public void fireRightsUpdateEvent(@Nonnull final String entityId)
    {
        List<RightsUpdateEventType> allEventTypes = Arrays.asList(RightsUpdateEventType.ENTITY_OWNER_UPDATED,
            RightsUpdateEventType.ENTITY_COLLABORATORS_UPDATED, RightsUpdateEventType.ENTITY_VISIBILITY_UPDATED);
        this.observationManager.notify(new EntityRightsUpdatedEvent(allEventTypes, entityId), null);
    }

    @Override
    public void fireRightsUpdateEvent(@Nonnull final List<RightsUpdateEventType> eventTypes,
        @Nonnull final String entityId)
    {
        this.observationManager.notify(new EntityRightsUpdatedEvent(eventTypes, entityId), null);
    }

    @Override
    public void fireStudyUpdateEvent(@Nonnull final String entityId, @Nonnull final String studyId)
    {
        this.observationManager.notify(new EntityStudyUpdatedEvent(entityId, studyId), null);
    }
}
