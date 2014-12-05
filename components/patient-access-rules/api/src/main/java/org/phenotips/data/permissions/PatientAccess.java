/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
