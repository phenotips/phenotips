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
import org.phenotips.data.PatientRepository;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.security.authorization.AuthorizationModule;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Implementation that allows access for a Collaborator based on the Access Level.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("collaborator-access")
@Singleton
public class CollaboratorAccessAuthorizationModule implements AuthorizationModule
{
    /**
     * Checks to see if document is a patient (DocumentReference).
     */
    @Inject
    private PatientRepository patientRepository;

    /**
     * Checks to see if it has the permission to access the document or patient.
     */
    @Inject
    private PermissionsManager permissionsManager;

    @Override
    public int getPriority()
    {
        return 300;
    }

    @Override
    public Boolean hasAccess(User user, Right access, EntityReference entity)
    {
        // Checks to see if the user, access or patient is null.
        if (user == null || access == null || entity == null) {
            return null;
        }

        // This converts the document to a patient.
        Patient patient = this.patientRepository.get(entity.toString());
        if (patient == null) {
            return null;
        }
        PatientAccess patientAccess = this.permissionsManager.getPatientAccess(patient);
        // This retrieves the access level for the patient.
        AccessLevel grantedAccess = patientAccess.getAccessLevel(user.getProfileDocument());
        // This retrieves the access level for the collaborator.
        AccessLevel requestedAccess = this.permissionsManager.resolveAccessLevel(access.getName());

        // This grants access if nothing is null and the collaborator has the required access level.
        if (grantedAccess != null && requestedAccess != null && grantedAccess.compareTo(requestedAccess) >= 0) {
            return true;
        }
        return null;
    }
}
