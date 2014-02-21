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
package org.phenotips.data.permissions.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.Collaborator;
import org.phenotips.data.permissions.Owner;
import org.phenotips.data.permissions.Visibility;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import java.util.Collection;

/**
 * @version $Id$
 */
@Role
public interface PatientAccessHelper
{
    DocumentReference getCurrentUser();

    boolean isAdministrator(Patient patient);

    Owner getOwner(Patient patient);

    boolean setOwner(Patient patient, EntityReference userOrGroup);

    Visibility getVisibility(Patient patient);

    AccessLevel getAccessLevel(Patient patient, EntityReference userOrGroup);

    boolean setVisibility(Patient patient, Visibility visibility);

    Collection<Collaborator> getCollaborators(Patient patient);

    boolean setCollaborators(Patient patient, Collection<Collaborator> newCollaborators);

    boolean addCollaborator(Patient patient, Collaborator collaborator);

    boolean removeCollaborator(Patient patient, Collaborator collaborator);

    String getType(EntityReference userOrGroup);
}
