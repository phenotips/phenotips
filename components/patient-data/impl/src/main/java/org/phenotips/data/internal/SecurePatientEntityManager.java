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
import org.phenotips.entities.PrimaryEntityManager;
import org.phenotips.entities.spi.AbstractSecurePrimaryEntityManager;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Secure implementation of patient data access service using XWiki as the storage backend.
 *
 * @version $Id$
 * @since 1.4
 */
@Named("Patient/secure")
@Component(roles = { PrimaryEntityManager.class })
@Singleton
public class SecurePatientEntityManager extends AbstractSecurePrimaryEntityManager<Patient>
{
    @Override
    public EntityReference getEntityType()
    {
        return Patient.CLASS_REFERENCE;
    }

    @Override
    public EntityReference getDataSpace()
    {
        return Patient.DEFAULT_DATA_SPACE;
    }

    @Override
    public String getIdPrefix()
    {
        return "P";
    }

    @Override
    public String getType()
    {
        return "patients";
    }

    @Override
    protected DocumentReference getEntityXClassReference()
    {
        return this.referenceResolver.resolve(Patient.CLASS_REFERENCE);
    }

    @Override
    protected Class<? extends Patient> getEntityClass()
    {
        return PhenoTipsPatient.class;
    }

    @Override
    protected Class<? extends Patient> getSecureEntityClass()
    {
        return SecurePatient.class;
    }
}
