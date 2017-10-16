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
import org.phenotips.data.permissions.EntityPermissionsManager;
import org.phenotips.data.permissions.Owner;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.entities.PrimaryEntity;

import org.xwiki.model.reference.EntityReference;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.collections4.MapUtils;

/**
 * A secure implementation of {@link EntityAccess}.
 *
 * @version $Id$
 * @since 1.4
 */
public class SecureEntityAccess implements EntityAccess
{
    private static final String MANAGE = "manage";

    private final EntityAccess internalService;

    private final EntityPermissionsManager manager;

    /**
     * The default constructor taking in an {@code internalService} object, and the {@code manager} object.
     *
     * @param internalService an {@link EntityAccess} object
     * @param manager the {@link EntityPermissionsManager}
     */
    public SecureEntityAccess(@Nonnull EntityAccess internalService, @Nonnull EntityPermissionsManager manager)
    {
        this.internalService = internalService;
        this.manager = manager;
    }

    @Nullable
    @Override
    public PrimaryEntity getEntity()
    {
        return this.internalService.getEntity();
    }

    @Nullable
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
    public boolean isOwner(@Nullable EntityReference user)
    {
        return this.internalService.isOwner(user);
    }

    @Override
    public boolean setOwner(@Nullable EntityReference userOrGroup)
    {
        if (hasAccessLevel(MANAGE)) {
            return this.internalService.setOwner(userOrGroup);
        }
        return false;
    }

    @Nonnull
    @Override
    public Visibility getVisibility()
    {
        return this.internalService.getVisibility();
    }

    @Override
    public boolean setVisibility(@Nullable Visibility newVisibility)
    {
        if (hasAccessLevel(MANAGE)) {
            return this.internalService.setVisibility(newVisibility);
        }
        return false;
    }

    @Nonnull
    @Override
    public Collection<Collaborator> getCollaborators()
    {
        return this.internalService.getCollaborators();
    }

    @Override
    public boolean updateCollaborators(@Nullable Collection<Collaborator> newCollaborators)
    {
        if (hasAccessLevel(MANAGE)) {
            return this.internalService.updateCollaborators(newCollaborators);
        }
        return false;
    }

    /**
     * Sets the collaborators and access levels for {@link #getEntity() entity}.
     *
     * @param newCollaborators a map containing new {@link EntityReference collaborators} and their {@link AccessLevel}
     * @return true iff the collaborators were updated successfully, false otherwise
     */
    public boolean updateCollaborators(@Nullable Map<EntityReference, AccessLevel> newCollaborators)
    {
        if (hasAccessLevel(MANAGE)) {
            Collection<Collaborator> collaborators = new LinkedHashSet<>();
            if (MapUtils.isEmpty(newCollaborators)) {
                return this.internalService.updateCollaborators(collaborators);
            }
            for (Map.Entry<EntityReference, AccessLevel> collaborator : newCollaborators.entrySet()) {
                collaborators.add(new DefaultCollaborator(collaborator.getKey(), collaborator.getValue(), null));
            }
            return this.internalService.updateCollaborators(collaborators);
        }
        return false;
    }

    @Override
    public boolean addCollaborator(@Nullable EntityReference user, @Nullable AccessLevel access)
    {
        if (hasAccessLevel(MANAGE)) {
            return this.internalService.addCollaborator(user, access);
        }
        return false;
    }

    @Override
    public boolean removeCollaborator(@Nullable EntityReference user)
    {
        if (hasAccessLevel(MANAGE)) {
            return this.internalService.removeCollaborator(user);
        }
        return false;
    }

    @Override
    public boolean removeCollaborator(@Nullable Collaborator collaborator)
    {
        if (hasAccessLevel(MANAGE)) {
            return this.internalService.removeCollaborator(collaborator);
        }
        return false;
    }

    @Nonnull
    @Override
    public AccessLevel getAccessLevel()
    {
        return this.internalService.getAccessLevel();
    }

    @Nonnull
    @Override
    public AccessLevel getAccessLevel(@Nullable EntityReference user)
    {
        return this.internalService.getAccessLevel(user);
    }

    @Override
    public boolean hasAccessLevel(@Nullable AccessLevel access)
    {
        return this.internalService.hasAccessLevel(access);
    }

    /**
     * Returns true iff the current user has an access level above or equal to the provided {@code accessName}.
     *
     * @param accessName the name of the {@link AccessLevel}
     * @return true iff the current user has the required access level, false otherwise
     */
    public boolean hasAccessLevel(@Nullable String accessName)
    {
        return this.internalService.hasAccessLevel(this.manager.resolveAccessLevel(accessName));
    }

    @Override
    public boolean hasAccessLevel(@Nullable EntityReference user, @Nullable AccessLevel access)
    {
        return this.internalService.hasAccessLevel(user, access);
    }

    /**
     * Returns true iff the {@code user} has an access level above or equal to the provided {@code accessName}.
     *
     * @param user the {@link EntityReference} for the user of interest
     * @param accessName the name of the {@link AccessLevel}
     * @return true iff the specified {@code user} has the required access level, false otherwise
     */
    public boolean hasAccessLevel(@Nullable EntityReference user, @Nullable String accessName)
    {
        return this.internalService.hasAccessLevel(user, this.manager.resolveAccessLevel(accessName));
    }
}
