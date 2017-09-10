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
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.data.permissions.EntityAccess;
import org.phenotips.data.permissions.Owner;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.entities.PrimaryEntity;

import org.xwiki.model.reference.EntityReference;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Restricted access implementation of {@link EntityAccess}.
 */
public class SecureEntityAccess implements EntityAccess
{
    private static final String MANAGE = "manage";

    private final EntityAccess internalService;

    private final PermissionsManager manager;

    SecureEntityAccess(EntityAccess internalService, PermissionsManager manager)
    {
        this.internalService = internalService;
        this.manager = manager;
    }

    @Override
    public PrimaryEntity getEntity()
    {
        return this.internalService.getEntity();
    }

    @Override
    public Owner getOwner()
    {
        return this.internalService.getOwner();
    }

    @Override
    public boolean isOwner()
    {
        return this.internalService.isOwner();
    }

    @Override
    public boolean isOwner(@Nullable final EntityReference user)
    {
        return this.internalService.isOwner(user);
    }

    @Override
    public boolean setOwner(@Nullable final EntityReference userOrGroup)
    {
        return hasAccessLevel(MANAGE) && this.internalService.setOwner(userOrGroup);
    }

    @Override
    public Visibility getVisibility()
    {
        return this.internalService.getVisibility();
    }

    @Override
    public boolean setVisibility(@Nullable final Visibility newVisibility)
    {
        return hasAccessLevel(MANAGE) && this.internalService.setVisibility(newVisibility);
    }

    @Override
    public Collection<Collaborator> getCollaborators()
    {
        return this.internalService.getCollaborators();
    }

    @Override
    public boolean updateCollaborators(@Nonnull final Collection<Collaborator> newCollaborators)
    {
        return hasAccessLevel(MANAGE) && this.internalService.updateCollaborators(newCollaborators);
    }

    public boolean updateCollaborators(@Nonnull final Map<EntityReference, AccessLevel> newCollaborators)
    {
        if (hasAccessLevel(MANAGE)) {
            final Collection<Collaborator> collaborators = new LinkedHashSet<>();
            newCollaborators.forEach((key, value) -> collaborators.add(new DefaultCollaborator(key, value, null)));
            return this.internalService.updateCollaborators(collaborators);
        }
        return false;
    }

    @Override
    public boolean addCollaborator(@Nonnull final EntityReference user, @Nonnull final AccessLevel access)
    {
        return hasAccessLevel(MANAGE) && this.internalService.addCollaborator(user, access);
    }

    @Override
    public boolean removeCollaborator(@Nonnull final EntityReference user)
    {
        return hasAccessLevel(MANAGE) && this.internalService.removeCollaborator(user);
    }

    @Override
    public boolean removeCollaborator(@Nonnull final Collaborator collaborator)
    {
        return hasAccessLevel(MANAGE) && this.internalService.removeCollaborator(collaborator);
    }

    @Override
    public AccessLevel getAccessLevel()
    {
        return this.internalService.getAccessLevel();
    }

    @Override
    public AccessLevel getAccessLevel(@Nullable final EntityReference user)
    {
        return this.internalService.getAccessLevel(user);
    }

    @Override
    public boolean hasAccessLevel(@Nonnull final AccessLevel access)
    {
        return this.internalService.hasAccessLevel(access);
    }

    public boolean hasAccessLevel(@Nullable final String accessName)
    {
        return this.internalService.hasAccessLevel(this.manager.resolveAccessLevel(accessName));
    }

    @Override
    public boolean hasAccessLevel(@Nullable final EntityReference user, @Nonnull final AccessLevel access)
    {
        return this.internalService.hasAccessLevel(user, access);
    }

    public boolean hasAccessLevel(@Nullable final EntityReference user, @Nullable final String accessName)
    {
        return this.internalService.hasAccessLevel(user, this.manager.resolveAccessLevel(accessName));
    }
}
