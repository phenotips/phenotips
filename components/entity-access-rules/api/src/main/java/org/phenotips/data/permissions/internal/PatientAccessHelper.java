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

    boolean isAdministrator(Patient patient, DocumentReference user);

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
