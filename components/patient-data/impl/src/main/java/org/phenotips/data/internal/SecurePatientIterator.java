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

import org.phenotips.components.ComponentManagerRegistry;
import org.phenotips.data.Patient;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.slf4j.Logger;

/**
 * An iterator on an immutable, secure patients collection. The function next() guarantees not to return null.
 *
 * @version $Id$
 */
public class SecurePatientIterator implements Iterator<Patient>
{
    private static final AuthorizationManager ACCESS;

    private static final DocumentAccessBridge BRIDGE;

    private static final Logger LOGGER;

    private Iterator<Patient> patientIterator;

    private DocumentReference currentUser;

    private Patient nextPatient;

    static {
        Logger logger = null;
        AuthorizationManager access = null;
        DocumentAccessBridge bridge = null;
        try {
            logger = ComponentManagerRegistry.getContextComponentManager().getInstance(Logger.class);
            access = ComponentManagerRegistry.getContextComponentManager().getInstance(AuthorizationManager.class);
            bridge = ComponentManagerRegistry.getContextComponentManager().getInstance(DocumentAccessBridge.class);
        } catch (ComponentLookupException e) {
            logger.error("Error loading static components: {}", e.getMessage(), e);
        }

        LOGGER = logger;
        ACCESS = access;
        BRIDGE = bridge;
    }

    /**
     * Default constructor.
     *
     * @param patientIterator Iterator for a collection of patients that this class wraps with security.
     */
    public SecurePatientIterator(Iterator<Patient> patientIterator)
    {
        this.patientIterator = patientIterator;
        this.currentUser = SecurePatientIterator.BRIDGE.getCurrentUserReference();

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

        Patient toReturn = nextPatient;
        this.findNextPatient();

        return toReturn;
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    private void findNextPatient()
    {
        this.nextPatient = null;

        while (patientIterator.hasNext() && this.nextPatient == null) {
            Patient potentialNextPatient = this.patientIterator.next();
            if (SecurePatientIterator.ACCESS.hasAccess(
                    Right.VIEW, this.currentUser, potentialNextPatient.getDocument())) {
                this.nextPatient = potentialNextPatient;
            }
        }
    }
}
