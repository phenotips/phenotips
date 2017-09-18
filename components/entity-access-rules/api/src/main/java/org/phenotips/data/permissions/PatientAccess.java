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
package org.phenotips.data.permissions;

import org.phenotips.data.Patient;

import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

import java.util.Collection;

/**
 * @version $Id$
 * @since 1.0M9
 */
@Unstable
public interface PatientAccess
{
    Patient getPatient();

    Owner getOwner();

    boolean isOwner();

    boolean isOwner(EntityReference user);

    boolean setOwner(EntityReference userOrGroup);

    Visibility getVisibility();

    boolean setVisibility(Visibility newVisibility);

    Collection<Collaborator> getCollaborators();

    boolean updateCollaborators(Collection<Collaborator> newCollaborators);

    boolean addCollaborator(EntityReference user, AccessLevel access);

    boolean removeCollaborator(EntityReference user);

    boolean removeCollaborator(Collaborator collaborator);

    AccessLevel getAccessLevel();

    AccessLevel getAccessLevel(EntityReference user);

    boolean hasAccessLevel(AccessLevel access);

    boolean hasAccessLevel(EntityReference user, AccessLevel access);
}
