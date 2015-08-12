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
package org.phenotips.studies.family.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.security.authorization.AuthorizationService;
import org.phenotips.studies.family.Validation;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Collection of checks for checking if certain actions are allowed.
 *
 * @version $Id$
 * @since 1.2RC1
 */
@Component
@Singleton
public class ValidationImpl implements Validation
{
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> referenceResolver;

    @Inject
    private PermissionsManager permissionsManager;

    @Inject
    private UserManager userManager;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    @Named("edit")
    private AccessLevel editAccess;

    @Inject
    @Named("view")
    private AccessLevel viewAccess;

    @Override
    public boolean hasPatientViewAccess(Patient patient)
    {
        User currentUser = this.userManager.getCurrentUser();
        return hasPatientAccess(patient, this.viewAccess, currentUser);
    }

    @Override
    public boolean hasPatientViewAccess(Patient patient, User user)
    {
        return hasPatientAccess(patient, this.viewAccess, user);
    }

    private boolean hasPatientAccess(Patient patient, AccessLevel accessLevel, User user)
    {
        PatientAccess patientAccess = this.permissionsManager.getPatientAccess(patient);
        AccessLevel patientAccessLevel = patientAccess.getAccessLevel(user.getProfileDocument());
        return patientAccessLevel.compareTo(accessLevel) >= 0;
    }

    @Override
    public boolean hasAccess(DocumentReference document, String permissions)
    {
        Right right = Right.toRight(permissions);
        return hasAccess(document, right);
    }

    @Override
    public boolean hasAccess(DocumentReference document, Right right)
    {
        User currentUser = this.userManager.getCurrentUser();
        return this.authorizationService.hasAccess(currentUser, right, document);
    }
}
