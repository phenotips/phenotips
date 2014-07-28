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
 * Implementation that allows all access to the owner of a patient.
 *
 * @version $Id$
 * @since 1.0RC1
 */
@Component
@Named("owner-access")
@Singleton
public class OwnerAccessAuthorizationModule implements AuthorizationModule
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
        return 400;
    }

    @Override
    public Boolean hasAccess(User user, Right access, DocumentReference document)
    {
        if (user == null || document == null) {
            return null;
        }
        // This converts the document to a patient.
        Patient patient = this.patientRepository.getPatientById(document.toString());
        if (patient == null) {
            return null;
        }
        // This retrieves the access rules for the patient.
        PatientAccess patientAccess = this.permissionsManager.getPatientAccess(patient);
        // If the target user is the owner, allow all access to patient.
        if (patientAccess.isOwner(user.getProfileDocument())) {
            return true;
        }
        return null;
    }
}
