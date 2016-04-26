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

/**
 * An iterator on an immutable, secure patients collection. The iterator returns null if current user has no access to
 * the loaded patient.
 *
 * @version $Id$
 */
public class SecurePatientIterator implements Iterator<Patient>
{
    private static AuthorizationManager access;

    private static DocumentAccessBridge bridge;

    private Iterator<Patient> patientIterator;

    private DocumentReference currentUser;

    static {
        try {
            SecurePatientIterator.access =
                ComponentManagerRegistry.getContextComponentManager().getInstance(AuthorizationManager.class);
            SecurePatientIterator.bridge =
                    ComponentManagerRegistry.getContextComponentManager().getInstance(DocumentAccessBridge.class);
        } catch (ComponentLookupException e) {
            e.printStackTrace();
        }
    }

    /**
     * Default constructor.
     *
     * @param patientIterator Iterator for a collection of patients that this class wraps with security.
     */
    public SecurePatientIterator(Iterator<Patient> patientIterator) {
        this.patientIterator = patientIterator;
        this.currentUser = SecurePatientIterator.bridge.getCurrentUserReference();
    }

    @Override
    public boolean hasNext() {
        return patientIterator.hasNext();
    }

    @Override
    public Patient next() {
        Patient patient = this.patientIterator.next();
        if (SecurePatientIterator.access.hasAccess(Right.VIEW, this.currentUser, patient.getDocument())) {
            return patient;
        } else {
            return null;
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
