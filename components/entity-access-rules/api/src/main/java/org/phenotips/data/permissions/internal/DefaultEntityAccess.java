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
import org.phenotips.data.permissions.internal.access.AccessHelper;
import org.phenotips.data.permissions.internal.visibility.VisibilityHelper;
import org.phenotips.entities.PrimaryEntity;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import java.util.Collection;
import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @version $Id$
 * @since 1.4
 */
public class DefaultEntityAccess implements EntityAccess
{
    private final PrimaryEntity entity;

    private final PermissionsHelper permissionsHelper;

    private final AccessHelper accessHelper;

    private final VisibilityHelper visibilityHelper;

    DefaultEntityAccess(
        @Nullable final PrimaryEntity entity,
        @Nonnull final PermissionsHelper permissionsHelper,
        @Nonnull final AccessHelper accessHelper,
        @Nonnull final VisibilityHelper visibilityHelper)
    {
        this.entity = entity;
        this.permissionsHelper = permissionsHelper;
        this.accessHelper = accessHelper;
        this.visibilityHelper = visibilityHelper;
    }

    @Override
    public PrimaryEntity getEntity()
    {
        return this.entity;
    }

    @Override
    public Owner getOwner()
    {
        return this.entity == null ? null : this.accessHelper.getOwner(this.entity);
    }

    @Override
    public boolean isOwner()
    {
        return isOwner(this.permissionsHelper.getCurrentUser());
    }

    @Override
    public boolean isOwner(@Nullable final EntityReference user)
    {
        return this.entity != null && user != null && user.equals(this.accessHelper.getOwner(this.entity).getUser());
    }

    @Override
    public boolean setOwner(@Nullable final EntityReference userOrGroup)
    {
        return this.entity != null && this.accessHelper.setOwner(this.entity, userOrGroup);
    }

    @Override
    public Visibility getVisibility()
    {
        return this.entity == null ? null : this.visibilityHelper.getVisibility(this.entity);
    }

    @Override
    public boolean setVisibility(@Nullable final Visibility newVisibility)
    {
        return this.entity != null && this.visibilityHelper.setVisibility(this.entity, newVisibility);
    }

    @Override
    public Collection<Collaborator> getCollaborators()
    {
        return this.entity == null ? Collections.emptyList() : this.accessHelper.getCollaborators(this.entity);
    }

    @Override
    public boolean updateCollaborators(@Nonnull final Collection<Collaborator> newCollaborators)
    {
        return this.entity != null && this.accessHelper.setCollaborators(this.entity, newCollaborators);
    }

    @Override
    public boolean addCollaborator(@Nonnull final EntityReference user, @Nonnull final AccessLevel access)
    {
        return this.entity != null
            && this.accessHelper.addCollaborator(this.entity, new DefaultCollaborator(user, access, null));
    }

    @Override
    public boolean removeCollaborator(@Nonnull final EntityReference user)
    {
        final Collaborator collaborator = new DefaultCollaborator(user, null, null);
        return removeCollaborator(collaborator);
    }

    @Override
    public boolean removeCollaborator(@Nonnull final Collaborator collaborator)
    {
        return this.entity != null && this.accessHelper.removeCollaborator(this.entity, collaborator);
    }

    @Override
    public AccessLevel getAccessLevel()
    {
        return getAccessLevel(this.permissionsHelper.getCurrentUser());
    }

    @Override
    public AccessLevel getAccessLevel(@Nullable final EntityReference user)
    {
        if (this.entity == null || user == null) {
            return getVisibility().getDefaultAccessLevel();
        }
        if (isOwner(user) || this.accessHelper.isAdministrator(this.entity, new DocumentReference(user))) {
            return this.accessHelper.resolveAccessLevel("owner");
        }
        final AccessLevel userAccess = this.accessHelper.getAccessLevel(this.entity, user);
        final AccessLevel defaultAccess = getVisibility().getDefaultAccessLevel();
        if (userAccess.compareTo(defaultAccess) > 0) {
            return userAccess;
        }
        return defaultAccess;
    }

    @Override
    public boolean hasAccessLevel(@Nonnull final AccessLevel access)
    {
        return hasAccessLevel(this.permissionsHelper.getCurrentUser(), access);
    }

    @Override
    public boolean hasAccessLevel(@Nullable final EntityReference user, @Nonnull final AccessLevel access)
    {
        final AccessLevel realAccess = getAccessLevel(user);
        return realAccess.compareTo(access) >= 0;
    }

    @Override
    public String toString()
    {
        return "Access rules for "
               + (this.entity != null ? this.entity.getDocumentReference() : "<unknown entity>");
    }
}
