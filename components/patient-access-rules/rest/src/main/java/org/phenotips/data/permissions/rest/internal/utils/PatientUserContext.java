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
public class PatientUserContext
{
    private Patient patient;

    private User currentUser;

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
    public PatientUserContext(String patientId, Right minimumRight, PatientRepository repository, UserManager users,
        AuthorizationManager access, Logger logger) throws WebApplicationException
    {
        this.patient = repository.getPatientById(patientId);
        if (this.patient == null) {
            logger.debug("No such patient record: [{}]", patientId);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        this.initializeUser(this.patient, minimumRight, users, access, logger);
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
    public PatientUserContext(Patient patient, Right minimumRight, UserManager users,
        AuthorizationManager access, Logger logger) throws WebApplicationException
    {
        this.patient = patient;
        if (this.patient == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        this.initializeUser(patient, minimumRight, users, access, logger);
    }

    private void initializeUser(Patient patient, Right minimumRight, UserManager users, AuthorizationManager access,
        Logger logger)
    {
        this.currentUser = users.getCurrentUser();
        if (!access.hasAccess(minimumRight, this.currentUser == null ? null : this.currentUser.getProfileDocument(),
            patient.getDocument()))
        {
            logger.debug("{} access denied to user [{}] on patient record [{}]",
                minimumRight.getName(), this.currentUser, this.patient.getId());
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
}
