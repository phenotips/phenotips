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
package org.phenotips.data.permissions.rest.internal.utils;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.data.permissions.script.SecurePatientAccess;

import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

/**
 * Default context that securely provides the current user and patient. In case that the current user does not have the
 * minimum required rights, the context will fail to initialize.
 *
 * @version $Id$
 * @since 1.3M1
 */
public class PatientAccessContext
{
    private Patient patient;

    private User currentUser;

    private PatientAccess patientAccess;

    /**
     * Initializes the context, making sure that the patient exists, and that the current user has sufficient rights. If
     * any of these conditions are not met, initialization fails.
     *
     * @param patientId by which to find a patient record
     * @param minimumRight that the current must have or exceed
     * @param repository used to find the patient record
     * @param users used to get the current user
     * @param access used to check that the user has a certain access level
     * @param logger for logging failures
     * @throws WebApplicationException if the patient could not be found, or the current user has insufficient rights
     */
    public PatientAccessContext(String patientId, AccessLevel minimumAccessLevel, PatientRepository repository, UserManager users,
        PermissionsManager manager, Logger logger) throws WebApplicationException
    {
        this.patient = repository.getPatientById(patientId);
        if (this.patient == null) {
            logger.debug("No such patient record: [{}]", patientId);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        this.patientAccess = new SecurePatientAccess(manager.getPatientAccess(this.patient), manager);
        this.initializeUser(minimumAccessLevel, users, logger);
    }

    /**
     * Initializes the context, making sure that the current user has sufficient rights. If the current user does not
     * have sufficient rights, initialization fails.
     *
     * @param patient instance which will be returned by the context upon request
     * @param minimumRight that the current must have or exceed
     * @param users used to get the current user
     * @param access used to check that the user has a certain access level
     * @param logger for logging failures
     * @throws WebApplicationException if the patient instance was {@link null}, or the current user has insufficient
     * rights
     */
    public PatientAccessContext(Patient patient, AccessLevel minimumAccessLevel, UserManager users,
        PermissionsManager manager, Logger logger) throws WebApplicationException
    {
        this.patient = patient;
        if (this.patient == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        this.patientAccess = new SecurePatientAccess(manager.getPatientAccess(this.patient), manager);
        this.initializeUser(minimumAccessLevel, users, logger);
    }

    private void initializeUser(AccessLevel minimumAccessLevel, UserManager users, Logger logger)
    {
        this.currentUser = users.getCurrentUser();
        if (!this.patientAccess.hasAccessLevel(this.currentUser.getProfileDocument(), minimumAccessLevel));
        {
            logger.debug("{} access denied to user [{}] on patient record [{}]",
                minimumAccessLevel.getName(), this.currentUser, this.patient.getId());
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
    }

    /**
     * The main use of the context is this method.
     *
     * @return a patient that was either found by internal id, or was passed in during initialization
     */
    public Patient getPatient()
    {
        return this.patient;
    }

    /**
     * This method is not used.
     *
     * @return the current user
     */
    public User getCurrentUser()
    {
        return this.currentUser;
    }

    public PatientAccess getPatientAccess()
    {
        return patientAccess;
    }
}
