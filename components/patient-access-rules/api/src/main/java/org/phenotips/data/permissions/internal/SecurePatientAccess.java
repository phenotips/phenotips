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

import org.xwiki.model.reference.EntityReference;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;

public class SecurePatientAccess implements PatientAccess
{
    private final PatientAccess internalService;

    private final PermissionsManager manager;

    public SecurePatientAccess(PatientAccess internalService, PermissionsManager manager)
    {
        this.internalService = internalService;
        this.manager = manager;
    }

    @Override
    public Patient getPatient()
    {
        return this.internalService.getPatient();
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
    public boolean isOwner(EntityReference user)
    {
        return this.internalService.isOwner(user);
    }

    @Override
    public boolean setOwner(EntityReference userOrGroup)
    {
        if (hasAccessLevel("manage")) {
            return this.internalService.setOwner(userOrGroup);
        }
        return false;
    }

    @Override
    public Visibility getVisibility()
    {
        return this.internalService.getVisibility();
    }

    @Override
    public boolean setVisibility(Visibility newVisibility)
    {
        if (hasAccessLevel("manage")) {
            return this.internalService.setVisibility(newVisibility);
        }
        return false;
    }

    @Override
    public Collection<Collaborator> getCollaborators()
    {
        return this.internalService.getCollaborators();
    }

    @Override
    public boolean updateCollaborators(Collection<Collaborator> newCollaborators)
    {
        if (hasAccessLevel("manage")) {
            return this.internalService.updateCollaborators(newCollaborators);
        }
        return false;
    }

    public boolean updateCollaborators(Map<EntityReference, AccessLevel> newCollaborators)
    {
        if (hasAccessLevel("manage")) {
            Collection<Collaborator> collaborators = new LinkedHashSet<Collaborator>();
            for (Map.Entry<EntityReference, AccessLevel> collaborator : newCollaborators.entrySet()) {
                collaborators.add(new DefaultCollaborator(collaborator.getKey(), collaborator.getValue()));
            }
            return this.internalService.updateCollaborators(collaborators);
        }
        return false;
    }

    @Override
    public boolean addCollaborator(EntityReference user, AccessLevel access)
    {
        if (hasAccessLevel("manage")) {
            return this.internalService.addCollaborator(user, access);
        }
        return false;
    }

    @Override
    public boolean removeCollaborator(EntityReference user)
    {
        if (hasAccessLevel("manage")) {
            return this.internalService.removeCollaborator(user);
        }
        return false;
    }

    @Override
    public boolean removeCollaborator(Collaborator collaborator)
    {
        if (hasAccessLevel("manage")) {
            return this.internalService.removeCollaborator(collaborator);
        }
        return false;
    }

    @Override
    public AccessLevel getAccessLevel()
    {
        return this.internalService.getAccessLevel();
    }

    @Override
    public AccessLevel getAccessLevel(EntityReference user)
    {
        return this.internalService.getAccessLevel(user);
    }

    @Override
    public boolean hasAccessLevel(AccessLevel access)
    {
        return this.internalService.hasAccessLevel(access);
    }

    public boolean hasAccessLevel(String accessName)
    {
        return this.internalService.hasAccessLevel(this.manager.resolveAccessLevel(accessName));
    }

    @Override
    public boolean hasAccessLevel(EntityReference user, AccessLevel access)
    {
        return this.internalService.hasAccessLevel(user, access);
    }

    public boolean hasAccessLevel(EntityReference user, String accessName)
    {
        return this.internalService.hasAccessLevel(user, this.manager.resolveAccessLevel(accessName));
    }
}
