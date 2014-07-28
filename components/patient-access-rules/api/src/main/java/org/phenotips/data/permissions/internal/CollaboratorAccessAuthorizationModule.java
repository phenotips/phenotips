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
import org.phenotips.data.PatientRepository;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.security.authorization.AuthorizationModule;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Implementation that allows access to a Collaborator based on the Access Level.
 *
 * @version $Id$
 * @since 1.0RC1
 */
@Component
@Named("collaborator-access")
@Singleton
public class CollaboratorAccessAuthorizationModule implements AuthorizationModule
{

    /** Checks to see if document is a patient (DocumentReference). */
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
        return 350;
    }

    @Override
    public Boolean hasAccess(User user, Right access, DocumentReference document)
    {
        // Checks to see if the user, access or patient is null.
        if (user == null || access == null || document == null) {
            return null;
        }

        // This converts the document to a patient.
        Patient patient = this.patientRepository.getPatientById(document.toString());
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
