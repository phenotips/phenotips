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
package org.phenotips.data.permissions.script;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.data.permissions.Visibility;
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.UserManager;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * @version $Id$
 * @since 1.0M9
 */
@Component
@Named("permissions")
@Singleton
public class PermissionsManagerScriptService implements ScriptService
{
    @Inject
    @Named("secure")
    private PermissionsManager manager;

    @Inject
    private PatientRepository patientRepository;

    /** Used for obtaining the current user. */
    @Inject
    private UserManager userManager;

    /** Used for checking access rights. */
    @Inject
    private AuthorizationService access;

    /**
     * Get the visibility options available, excluding {@link Visibility#isDisabled() disabled} ones.
     *
     * @return a collection of enabled visibilities, may be empty if none are enabled
     */
    public Collection<Visibility> listVisibilityOptions()
    {
        return this.manager.listVisibilityOptions();
    }

    /**
     * Get all visibility options available in the platform, including {@link Visibility#isDisabled() disabled} ones.
     *
     * @return a collection of visibilities, may be empty if none are available
     * @since 1.3M2
     */
    public Collection<Visibility> listAllVisibilityOptions()
    {
        return this.manager.listAllVisibilityOptions();
    }

    /**
     * Get the default visibility to set for new patient records.
     *
     * @return a visibility, or {@code null} if none is configured or the configured one isn't valid
     * @since 1.3M2
     */
    public Visibility getDefaultVisibility()
    {
        return this.manager.getDefaultVisibility();
    }

    public Visibility resolveVisibility(String name)
    {
        return this.manager.resolveVisibility(name);
    }

    public Collection<AccessLevel> listAccessLevels()
    {
        return this.manager.listAccessLevels();
    }

    public AccessLevel resolveAccessLevel(String name)
    {
        return this.manager.resolveAccessLevel(name);
    }

    public PatientAccess getPatientAccess(String targetPatientId)
    {
        // scripts have only access to a SecurePatient implementation of a Patient,
        // which does not support all the functionality PatientAccess needs. So
        // need to get the full Patient object here instead of taking it as an argument
        //
        // Since this is a script service, need to check access rights the same way SecurePatientReporistory does.
        //
        // TODO: rights management should be refactored so that less is done from velocity
        //       and this method won't be needed any more

        Patient patient = this.patientRepository.get(targetPatientId);
        if (patient == null) {
            return null;
        }
        if (!this.access.hasAccess(this.userManager.getCurrentUser(), Right.VIEW, patient.getDocumentReference())) {
            return null;
        }
        return this.manager.getPatientAccess(patient);
    }

    public void fireRightsUpdateEvent(String targetPatientId)
    {
        this.manager.fireRightsUpdateEvent(targetPatientId);
    }
}
