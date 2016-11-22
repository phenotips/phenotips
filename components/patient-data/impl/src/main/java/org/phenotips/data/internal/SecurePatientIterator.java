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
package org.phenotips.data.internal;

import org.phenotips.data.Patient;
import org.phenotips.security.authorization.AuthorizationService;

import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator on an immutable, patients collection, which only returns patients that the current user has access to.
 *
 * @version $Id$
 * @since 1.3M2
 */
public class SecurePatientIterator implements Iterator<Patient>
{
    private Iterator<Patient> patientIterator;

    private User currentUser;

    private AuthorizationService access;

    private Patient nextPatient;

    /**
     * Default constructor.
     *
     * @param patientIterator Iterator for a collection of patients that this class wraps with security.
     * @param access the authorization manager actually responsible for checking if a patient is accessible
     * @param currentUser the current user, may be {@code null}
     */
    public SecurePatientIterator(Iterator<Patient> patientIterator, AuthorizationService access, User currentUser)
    {
        this.patientIterator = patientIterator;
        this.currentUser = currentUser;
        this.access = access;

        this.findNextPatient();
    }

    @Override
    public boolean hasNext()
    {
        return this.nextPatient != null;
    }

    @Override
    public Patient next()
    {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        Patient toReturn = this.nextPatient;
        this.findNextPatient();

        return this.createSecurePatient(toReturn);
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    private void findNextPatient()
    {
        this.nextPatient = null;
        if (this.patientIterator == null) {
            return;
        }

        while (this.patientIterator.hasNext() && this.nextPatient == null) {
            Patient potentialNextPatient = this.patientIterator.next();
            if (this.access.hasAccess(this.currentUser, Right.VIEW, potentialNextPatient.getDocumentReference())) {
                this.nextPatient = potentialNextPatient;
            }
        }
    }

    /**
     * Returns a SecurePatient wrapper around a given patient.
     *
     * TODO: see comments for {@link SecurePatientRepository.createSecurePatient(Patient patient)}
     *
     * @param patient a patient object
     * @return a SecurePatient wrapper around the given patient
     */
    protected SecurePatient createSecurePatient(Patient patient)
    {
        return new SecurePatient(patient);
    }
}
