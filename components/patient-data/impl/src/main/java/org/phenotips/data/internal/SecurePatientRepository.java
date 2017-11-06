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
import org.phenotips.data.PatientRepository;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Secure implementation of patient data access service which checks the user's access rights before performing an
 * operation. If the user is authorized, the actual work is done by the default {@link PatientRepository}
 * implementation and a read-only version of a Patient object is returned (implemented by {@link SecurePatient}).
 * If the user is not authorized, a {@link SecurityException} is thrown.
 *
 * @version $Id$
 * @since 1.3M1
 */
@Component(roles = PatientRepository.class)
@Named("secure")
@Singleton
public class SecurePatientRepository extends SecurePatientEntityManager implements PatientRepository
{
    @Override
    public synchronized Patient createNewPatient()
    {
        return create();
    }

    @Override
    public Patient createNewPatient(DocumentReference creator)
    {
        return create(creator);
    }

    @Override
    public Patient getPatientById(String id)
    {
        return get(id);
    }

    @Override
    public Patient getPatientByExternalId(String externalId)
    {
        return getByName(externalId);
    }

    @Override
    public Patient loadPatientFromDocument(DocumentModelBridge document)
    {
        return load(document);
    }

    @Override
    public boolean deletePatient(String id)
    {
        final Patient patient = get(id);
        return delete(patient);
    }
}
