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

import org.xwiki.component.annotation.Component;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.UserManager;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;

import groovy.lang.Singleton;

/**
 * Default implementation of {@link SecureContextFactory}. The purpose is to reduce the number of common
 * injections between default implementations of the REST resources.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Component
@Singleton
public class DefaultSecureContextFactory implements SecureContextFactory
{
    @Inject
    private Logger logger;

    @Inject
    private PatientRepository repository;

    @Inject
    private AuthorizationManager access;

    @Inject
    private UserManager users;

    @Override
    public PatientUserContext getContext(String patientId, Right minimumRight) throws WebApplicationException
    {
        return new PatientUserContext(patientId, minimumRight, repository, users, access, logger);
    }

    @Override
    public PatientUserContext getContext(Patient patient, Right minimumRight) throws WebApplicationException
    {
        return new PatientUserContext(patient, minimumRight, users, access, logger);
    }
}
