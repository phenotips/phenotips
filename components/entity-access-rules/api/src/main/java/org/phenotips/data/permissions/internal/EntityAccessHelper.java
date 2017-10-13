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
import org.phenotips.data.permissions.Owner;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.entities.PrimaryEntity;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import java.util.Collection;

/**
 * @version $Id$
 */
@Role
public interface EntityAccessHelper
{
    DocumentReference getCurrentUser();

    boolean isAdministrator(PrimaryEntity entity);

    boolean isAdministrator(PrimaryEntity entity, DocumentReference user);

    Owner getOwner(PrimaryEntity entity);

    boolean setOwner(PrimaryEntity entity, EntityReference userOrGroup);

    Visibility getVisibility(PrimaryEntity entity);

    AccessLevel getAccessLevel(PrimaryEntity entity, EntityReference userOrGroup);

    boolean setVisibility(PrimaryEntity entity, Visibility visibility);

    Collection<Collaborator> getCollaborators(PrimaryEntity entity);

    boolean setCollaborators(PrimaryEntity entity, Collection<Collaborator> newCollaborators);

    boolean addCollaborator(PrimaryEntity entity, Collaborator collaborator);

    boolean removeCollaborator(PrimaryEntity entity, Collaborator collaborator);

    String getType(EntityReference userOrGroup);
}
