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
import org.phenotips.data.permissions.Visibility;
import org.phenotips.entities.PrimaryEntity;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @version $Id$
 */
public class DefaultEntityAccess implements EntityAccess
{
    private static final String OWNER = "owner";

    private static final String NONE = "none";

    private final PrimaryEntity entity;

    private final EntityAccessHelper helper;

    private final EntityAccessManager accessManager;

    private final EntityVisibilityManager visibilityManager;

    public DefaultEntityAccess(
        @Nullable final PrimaryEntity entity,
        @Nonnull final EntityAccessHelper helper,
        @Nonnull final EntityAccessManager accessManager,
        @Nonnull final EntityVisibilityManager visibilityManager)
    {
        this.entity = entity;
        this.helper = helper;
        this.accessManager = accessManager;
        this.visibilityManager = visibilityManager;
    }

    @Override
    public PrimaryEntity getEntity()
    {
        return this.entity;
    }

    @Override
    public Owner getOwner()
    {
        return this.accessManager.getOwner(this.entity);
    }

    @Override
    public boolean isOwner()
    {
        return isOwner(this.helper.getCurrentUser());
    }

    @Override
    public boolean isOwner(EntityReference user)
    {
        Owner owner = this.accessManager.getOwner(this.entity);
        if (owner == null) {
            return false;
        }
        if (user == null) {
            return owner.getUser() == null;
        }

        return user.equals(owner.getUser());
    }

    @Override
    public boolean setOwner(EntityReference userOrGroup)
    {
        return this.accessManager.setOwner(this.entity, userOrGroup);
    }

    @Override
    public Visibility getVisibility()
    {
        return this.visibilityManager.getVisibility(this.entity);
    }

    @Override
    public boolean setVisibility(Visibility newVisibility)
    {
        return this.visibilityManager.setVisibility(this.entity, newVisibility);
    }

    @Override
    public Collection<Collaborator> getCollaborators()
    {
        return this.accessManager.getCollaborators(this.entity);
    }

    @Override
    public boolean updateCollaborators(Collection<Collaborator> newCollaborators)
    {
        return this.accessManager.setCollaborators(this.entity, newCollaborators);
    }

    @Override
    public boolean addCollaborator(EntityReference user, AccessLevel access)
    {
        Collaborator collaborator = new DefaultCollaborator(user, access, null);
        return this.accessManager.addCollaborator(this.entity, collaborator);
    }

    @Override
    public boolean removeCollaborator(EntityReference user)
    {
        Collaborator collaborator = new DefaultCollaborator(user, null, null);
        return removeCollaborator(collaborator);
    }

    @Override
    public boolean removeCollaborator(Collaborator collaborator)
    {
        return this.accessManager.removeCollaborator(this.entity, collaborator);
    }

    @Override
    public AccessLevel getAccessLevel()
    {
        return getAccessLevel(this.helper.getCurrentUser());
    }

    @Override
    public AccessLevel getAccessLevel(EntityReference user)
    {
        if (user == null) {
            if (this.getOwner().getUser() == null) {
                return this.accessManager.resolveAccessLevel(OWNER);
            }
            return this.accessManager.resolveAccessLevel(NONE);
        }
        if (isOwner(user) || this.accessManager.isAdministrator(this.entity, new DocumentReference(user))) {
            return this.accessManager.resolveAccessLevel(OWNER);
        }
        AccessLevel userAccess = this.accessManager.getAccessLevel(this.entity, user);
        AccessLevel defaultAccess = getVisibility().getDefaultAccessLevel();
        if (userAccess.compareTo(defaultAccess) > 0) {
            return userAccess;
        }
        return defaultAccess;
    }

    @Override
    public boolean hasAccessLevel(AccessLevel access)
    {
        return hasAccessLevel(this.helper.getCurrentUser(), access);
    }

    @Override
    public boolean hasAccessLevel(EntityReference user, AccessLevel access)
    {
        AccessLevel realAccess = getAccessLevel(user);
        return realAccess.compareTo(access) >= 0;
    }

    @Override
    public String toString()
    {
        return "Access rules for "
            + (this.entity != null ? this.entity.getDocumentReference() : "<unknown entity>");
    }
}
