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
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.data.permissions.Visibility;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import java.util.Collection;

/**
 * @version $Id$
 */
public class DefaultPatientAccess implements PatientAccess
{
    private final Patient patient;

    private final PatientAccessHelper helper;

    private final PermissionsManager manager;

    public DefaultPatientAccess(Patient patient, PatientAccessHelper helper, PermissionsManager manager)
    {
        this.patient = patient;
        this.helper = helper;
        this.manager = manager;
    }

    @Override
    public Patient getPatient()
    {
        return this.patient;
    }

    @Override
    public Owner getOwner()
    {
        return this.helper.getOwner(this.patient);
    }

    @Override
    public boolean isOwner()
    {
        return isOwner(this.helper.getCurrentUser());
    }

    @Override
    public boolean isOwner(EntityReference user)
    {
        Owner owner = this.helper.getOwner(this.patient);
        if (user == null || owner == null) {
            return false;
        }

        return user.equals(owner.getUser());
    }

    @Override
    public boolean setOwner(EntityReference userOrGroup)
    {
        return this.helper.setOwner(this.patient, userOrGroup);
    }

    @Override
    public Visibility getVisibility()
    {
        Visibility result = this.helper.getVisibility(this.patient);
        if (result == null) {
            result = this.manager.resolveVisibility("private");
        }
        return result;
    }

    @Override
    public boolean setVisibility(Visibility newVisibility)
    {
        return this.helper.setVisibility(this.patient, newVisibility);
    }

    @Override
    public Collection<Collaborator> getCollaborators()
    {
        return this.helper.getCollaborators(this.patient);
    }

    @Override
    public boolean updateCollaborators(Collection<Collaborator> newCollaborators)
    {
        return this.helper.setCollaborators(this.patient, newCollaborators);
    }

    @Override
    public boolean addCollaborator(EntityReference user, AccessLevel access)
    {
        Collaborator collaborator = new DefaultCollaborator(user, access, null);
        return this.helper.addCollaborator(this.patient, collaborator);
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
        return this.helper.removeCollaborator(this.patient, collaborator);
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
            return getVisibility().getDefaultAccessLevel();
        }
        if (isOwner(user) || this.helper.isAdministrator(this.patient, new DocumentReference(user))) {
            return this.manager.resolveAccessLevel("owner");
        }
        AccessLevel userAccess = this.helper.getAccessLevel(this.patient, user);
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
               + (this.patient != null ? this.patient.getDocumentReference() : "<unknown patient>");
    }
}
